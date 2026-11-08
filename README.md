# CIA2 Task Manager – End-to-End CI/CD on Azure

A simple Java (Spring Boot) to-do web application, used to demonstrate a complete
CI/CD pipeline: **Git → Gitleaks → Maven → SonarQube → Docker → Azure Container Registry → Ansible → Azure Kubernetes Service**.

Every push to `main` is scanned for secrets, built, tested, checked for code quality,
packaged as a Docker image, pushed to the registry and rolled out to Kubernetes automatically.

## Architecture

```text
 Developer ──git push──► GitHub repo (main)
                              │  triggers
                              ▼
 ┌──────────────────── GitHub Actions: .github/workflows/ci-cd.yml ────────────────────┐
 │ 1. Secret scan      Gitleaks scans the full Git history (fails on any leak)         │
 │ 2. Build & test     mvn verify  → compile, 15 JUnit tests, JaCoCo coverage, JAR     │
 │    Code quality     SonarQube analysis + quality gate (fails if gate fails)         │
 │ 3. Docker           docker build → tag :<commit-sha> and :latest → push to ACR      │
 │ 4. Deploy           Ansible playbook applies k8s/ manifests to AKS, waits for the   │
 │                     rolling update, checks /actuator/health and the new version     │
 └──────────────────────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
 Azure Container Registry ──image pull──► Azure Kubernetes Service (namespace taskmanager)
                                           Deployment: 2 pods, probes, non-root
                                           Service: LoadBalancer ──► http://<public-ip>
```

## Tools

| Purpose | Tool |
|---|---|
| Source control | Git + GitHub |
| Build & unit tests | Maven 3.9, JUnit 5, JaCoCo (coverage) |
| Application | Java 21, Spring Boot 4 (REST API + static web page + Actuator health) |
| Secret scanning | Gitleaks 8.30 (pre-commit hook + pipeline stage) |
| Code quality | SonarQube (local Community server + SonarQube Cloud in the pipeline) |
| Containers | Docker (multi-stage build, non-root user) |
| Registry | Azure Container Registry (ACR) |
| Orchestration | Azure Kubernetes Service (AKS) |
| Deployment automation | Ansible (`kubernetes.core` collection) |
| CI/CD | GitHub Actions |

## Repository layout

```text
.
├── src/main/java/...            Spring Boot app (controllers, service, model)
├── src/main/resources/static/   Web page (index.html, app.js, style.css)
├── src/test/java/...            Unit tests (service) + API tests (MockMvc)
├── pom.xml                      Maven build, JaCoCo and SonarQube plugins
├── Dockerfile / .dockerignore   Multi-stage image build
├── k8s/                         Namespace, Deployment, Service manifests
├── ansible/                     deploy.yml playbook + inventory.ini
├── .github/workflows/ci-cd.yml  The CI/CD pipeline
├── .gitleaks.toml               Gitleaks rules
├── .githooks/pre-commit         Blocks commits that contain secrets
├── sonarqube/docker-compose.yml Local SonarQube server
└── docs/SETUP.md                Step-by-step Azure / SonarQube / GitHub setup
```

## Application endpoints

| URL | What it does |
|---|---|
| `/` | Web page: add, complete and delete tasks; shows version and pod name |
| `GET /api/tasks` | List tasks |
| `POST /api/tasks` `{"title":"..."}` | Create a task (title required, max 100 chars) |
| `PUT /api/tasks/{id}/complete` | Mark a task done |
| `DELETE /api/tasks/{id}` | Delete a task |
| `GET /api/tasks/stats` | Total / completed / pending counts |
| `GET /api/info` | App name, version (Git commit), message, pod host name |
| `GET /actuator/health` | Health check used by Kubernetes probes |

Tasks are stored in memory (no database) to keep the project simple. Each pod has its own list.

## Run it locally

Requirements: JDK 21+, Maven 3.9+, Docker Desktop, Gitleaks.

```bash
mvn verify                          # compile + 15 tests + coverage report (target/site/jacoco)
java -jar target/taskmanager.jar    # open http://localhost:8080
```

```bash
docker build --build-arg APP_VERSION=local -t taskmanager:local .
docker run -p 8080:8080 taskmanager:local
```

### Secret scanning (Gitleaks)

```bash
git config core.hooksPath .githooks          # enable the pre-commit hook (once per clone)
gitleaks git . --config .gitleaks.toml -v    # scan the whole Git history
gitleaks dir . --config .gitleaks.toml -v    # scan the files on disk
```

### Code quality (SonarQube)

```bash
docker compose -f sonarqube/docker-compose.yml up -d      # http://localhost:9000
mvn clean verify sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token=<token> -Dsonar.qualitygate.wait=true
```

### Local Kubernetes (optional, Docker Desktop with Kubernetes enabled)

```bash
kubectl apply -f k8s/
kubectl get pods -n taskmanager
```

## Cloud deployment

See **[docs/SETUP.md](docs/SETUP.md)** for creating the Azure resources, SonarQube Cloud project
and GitHub secrets, and for the change-and-redeploy demonstration.

## Demonstrating a change

1. Edit `app.message` in `src/main/resources/application.properties` (for example "Version 2").
2. `git commit -am "Change welcome message" && git push`
3. The pipeline runs automatically; the deploy stage prints the URL and the new version.
4. Refresh the browser: the new message and the new commit SHA are shown.
