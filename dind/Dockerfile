# Outer container for Docker-in-Docker (DinD).
#
# It runs its own Docker daemon and starts the Task Manager image (built from
# the Dockerfile in the project root) as a nested, inner container.
#
#   docker build -t taskmanager-dind:local dind/
#
# The official docker:dind image already contains dockerd, containerd and the
# 'dind' helper that sets up cgroups and mounts for a nested daemon.
FROM docker:28.5.2-dind

COPY --chmod=0755 entrypoint.sh /usr/local/bin/taskmanager-dind.sh

# The daemon only listens on a local Unix socket, so no TLS certificates are needed
ENV DOCKER_TLS_CERTDIR=""

# Port of the inner Task Manager container, published on the outer container
EXPOSE 8080

# Docker needs root and a privileged container to run a daemon inside a container.
# The inner Task Manager container still runs as non-root user 10001 with no
# capabilities and a read-only filesystem (see entrypoint.sh).
ENTRYPOINT ["/usr/local/bin/taskmanager-dind.sh"]
