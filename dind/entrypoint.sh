#!/bin/sh
# Outer container (Docker-in-Docker).
#
#   Pod
#    └── outer container  (this script, runs its own private Docker daemon)
#         └── inner container  (the Task Manager app, started on that daemon)
#
# 1. start a Docker daemon that belongs only to this container
# 2. pull the Task Manager image from Azure Container Registry
# 3. run it as a nested container, publishing port 8080 on the pod
# 4. stay in the foreground, forward the app logs, and stop cleanly on SIGTERM
#
# Settings (environment variables, set in k8s/deployment.yaml):
#   APP_IMAGE      image for the inner container (required)
#   INNER_NAME     name of the inner container          (default: taskmanager)
#   APP_PORT       port published on the pod             (default: 8080)
#   INNER_MEMORY   memory limit of the inner container   (default: 640m)
#   INNER_CPUS     CPU limit of the inner container      (default: 0.8)
#   DOCKER_CONFIG  folder holding the registry login (config.json), mounted
#                  from the acr-pull Kubernetes secret
set -eu

: "${APP_IMAGE:?APP_IMAGE must be set to the Task Manager image}"
INNER_NAME="${INNER_NAME:-taskmanager}"
APP_PORT="${APP_PORT:-8080}"
INNER_MEMORY="${INNER_MEMORY:-640m}"
INNER_CPUS="${INNER_CPUS:-0.8}"

log() { echo "[dind] $(date -u +%H:%M:%S) $*"; }

# --- 1. private Docker daemon -------------------------------------------------
# 'dind' (from the docker:dind image) prepares cgroups and mounts for nesting.
# The daemon listens only on a local Unix socket - no TCP port is opened.
log "starting the Docker daemon of this outer container"
dind dockerd \
    --host=unix:///var/run/docker.sock \
    --storage-driver=overlay2 \
    --data-root=/var/lib/docker \
    --log-level=warn &
DOCKERD_PID=$!

tries=0
until docker info >/dev/null 2>&1; do
    tries=$((tries + 1))
    if [ "$tries" -gt 60 ] || ! kill -0 "$DOCKERD_PID" 2>/dev/null; then
        log "ERROR: Docker daemon did not start"
        exit 1
    fi
    sleep 1
done
log "Docker daemon is ready ($(docker version --format '{{.Server.Version}}'))"

# Stop the inner container and the daemon cleanly when Kubernetes stops the pod
shutdown() {
    log "stopping inner container $INNER_NAME"
    docker stop -t 20 "$INNER_NAME" >/dev/null 2>&1 || true
    kill "$DOCKERD_PID" 2>/dev/null || true
    wait "$DOCKERD_PID" 2>/dev/null || true
    exit 0
}
trap shutdown TERM INT

# --- 2. pull the application image ---------------------------------------------
tries=0
until docker pull --quiet "$APP_IMAGE"; do
    tries=$((tries + 1))
    if [ "$tries" -ge 5 ]; then
        log "ERROR: could not pull $APP_IMAGE"
        exit 1
    fi
    log "pull failed, retrying in 5s ($tries/5)"
    sleep 5
done

# --- 3. run the Task Manager as the inner container ----------------------------
docker rm -f "$INNER_NAME" >/dev/null 2>&1 || true
log "starting inner container $INNER_NAME from $APP_IMAGE"
docker run -d \
    --name "$INNER_NAME" \
    --hostname "$(hostname)-inner" \
    --restart unless-stopped \
    --publish "$APP_PORT:8080" \
    --memory "$INNER_MEMORY" \
    --cpus "$INNER_CPUS" \
    --pids-limit 256 \
    --read-only \
    --tmpfs /tmp:rw,size=64m,mode=1777 \
    --cap-drop ALL \
    --security-opt no-new-privileges \
    "$APP_IMAGE" >/dev/null

docker ps --filter "name=$INNER_NAME"

# --- 4. stay in the foreground -------------------------------------------------
# App logs appear in 'kubectl logs'. If the daemon dies, this container exits
# and Kubernetes restarts it.
docker logs -f "$INNER_NAME" 2>&1 &
wait "$DOCKERD_PID"
log "Docker daemon exited"
exit 1
