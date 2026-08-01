# Patient Management System

A production-ready **Spring Boot Microservices** backend for managing patients, authentication, billing, and analytics — deployable locally via Docker and to AWS (emulated by LocalStack using AWS CDK).

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1.0-6DB33F?style=flat&logo=spring-boot&logoColor=white)
![Spring Cloud](https://img.shields.io/badge/Spring_Cloud-2025.1.2-6DB33F?style=flat&logo=spring&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=flat&logo=docker&logoColor=white)
![LocalStack](https://img.shields.io/badge/LocalStack-AWS_Emulation-FF9900?style=flat&logo=amazon-aws&logoColor=white)
![Kafka](https://img.shields.io/badge/Apache_Kafka-Event_Streaming-231F20?style=flat&logo=apache-kafka&logoColor=white)
![gRPC](https://img.shields.io/badge/gRPC-Inter_Service-244C5A?style=flat)

---

## Architecture Overview

```
                           ┌────────────────────────────┐
                           │         Clients             │
                           │  (REST / Browser / Tests)   │
                           └─────────────┬──────────────┘
                                         │ :8085
                           ┌─────────────▼──────────────┐
                           │        API Gateway          │
                           │  Spring Cloud Gateway (WF)  │
                           │  JWT Validation Filter      │
                           └──────┬──────────┬───────────┘
                                  │          │
                    /auth/**      │          │  /api/patients/**
              ┌───────────────────┘          └───────────────────────┐
              │ :8086                                                  │ :8081
 ┌────────────▼───────────┐                             ┌────────────▼────────────┐
 │      Auth Service       │                             │     Patient Service      │
 │  Spring Security + JWT  │                             │  JPA + gRPC Client      │
 │  PostgreSQL (port 4001) │                             │  Kafka Producer         │
 └─────────────────────────┘                             │  PostgreSQL (port 4000) │
                                                         └──────────┬──────┬───────┘
                                                                    │      │
                                             gRPC :8083             │      │  Kafka :9092
                                       ┌────────────────────────────┘      │
                                       │                                    │
                          ┌────────────▼───────────┐          ┌────────────▼────────────┐
                          │    Billing Service      │          │   Analytics Service      │
                          │  gRPC Server (8083)     │          │   Kafka Consumer         │
                          │  REST API   (8082)      │          │   REST API  (8084)       │
                          └─────────────────────────┘          └──────────────────────────┘

 ─────────────────────────── Infrastructure (AWS CDK → LocalStack) ───────────────────────────

          VPC  ·  RDS PostgreSQL  ·  MSK (Kafka)  ·  ECS Fargate  ·  ALB  ·  CloudWatch
```

### Communication Patterns

| Pattern | Services | Purpose |
|---|---|---|
| REST (HTTP) | All services via API Gateway | External client-facing API |
| gRPC | PatientService → BillingService | Create billing account on patient creation |
| Kafka events | PatientService → AnalyticsService | `PatientEvent` published on patient lifecycle events |

---

## Microservices & Ports

| Service | HTTP Port | gRPC Port | Role |
|---|---|---|---|
| **API Gateway** | `8085` | — | Reactive gateway; JWT validation; routes to all services |
| **Auth Service** | `8086` | — | User login, JWT issuance (JJWT 0.12), Spring Security |
| **Patient Service** | `8081` | — | Patient CRUD; gRPC billing client; Kafka producer |
| **Billing Service** | `8082` | `8083` | gRPC server; creates billing accounts per patient |
| **Analytics Service** | `8084` | — | Kafka consumer; processes `PatientEvent` messages |
| **patient-service-db** | `4000` (host→5432) | — | PostgreSQL for Patient Service |
| **auth-service-db** | `4001` (host→5432) | — | PostgreSQL for Auth Service |
| **Kafka** | `9094` (host), `9092` (internal) | — | Apache Kafka (KRaft mode) |
| **LocalStack** | `4566` | — | AWS services emulator |

---

## Prerequisites

| Tool | Version | Notes |
|---|---|---|
| JDK | 21+ | [Eclipse Temurin](https://adoptium.net/) recommended |
| Maven | 3.9+ | Used by all service `pom.xml` files |
| Git | Any | For cloning the repo |
| Docker Desktop | 4.x+ | Must be running before starting any service |
| AWS CLI | v2 | For LocalStack resource verification |
| IntelliJ IDEA | 2024.1+ | For the bundled Run Configurations |

Verify your environment before proceeding:

```bash
java -version        # openjdk 21.x.x
mvn -version         # Apache Maven 3.9.x
docker info          # Docker Engine running
aws --version        # aws-cli/2.x.x
```

---

## Setup

### Step 1 — Clone the Repository

```bash
git clone https://github.com/<your-username>/patient-management-system.git
cd "patient-management-system"
```

---

### Step 2 — Create the Docker Network

All containers communicate over a shared bridge network named `internal`. Create it once:

```bash
docker network create internal
```

> If the network already exists Docker will report an error — that is safe to ignore.

---

### Step 3 — Deploy AWS Infrastructure via LocalStack

The `Infrastructure` module is a **Java AWS CDK** project that synthesises a CloudFormation template and deploys it to LocalStack. This provisions the VPC, RDS instances, MSK Kafka cluster, ECS services, and ALB that mirror the production AWS topology.

#### 3a — Start LocalStack

```bash
docker run --rm -d \
  --name localstack \
  -p 4566:4566 \
  -e SERVICES=ec2,rds,msk,ecs,elbv2,cloudwatch,logs,secretsmanager,servicediscovery \
  -v /var/run/docker.sock:/var/run/docker.sock \
  localstack/localstack:latest
```

Wait ~15 seconds, then verify it is healthy:

```bash
curl -s http://localhost:4566/_localstack/health | python3 -m json.tool
```

#### 3b — Synthesise the CDK Template

```bash
cd Infrastructure
mvn compile exec:java -Dexec.mainClass="com.example.stack.LocalStack"
```

This writes `cdk.out/localstack.template.json`.

#### 3c — Deploy the CloudFormation Stack

```bash
chmod +x localstack-deploy.sh
./localstack-deploy.sh
```

<details>
<summary>What the script does</summary>

```bash
#!/bin/bash
set -e
aws --endpoint-url=http://localhost:4566 cloudformation deploy \
    --stack-name patient-management \
    --template-file "./cdk.out/localstack.template.json"

aws --endpoint-url=http://localhost:4566 elbv2 describe-load-balancers \
    --query "LoadBalancers[0].DNSName" --output text
```

</details>

Alternatively, run the **`LocalStack`** Run Configuration from IntelliJ (see Step 4) — it executes the same CDK main class.

---

### Step 4 — Run Microservices via IntelliJ Run Configurations

The `/Run Configs` directory contains nine pre-built IntelliJ run configurations. No manual `docker run` commands are required.

#### Import the Run Configurations

1. Open IntelliJ IDEA and open the project root (`Patient Management System/`).
2. Go to **Run → Edit Configurations**.
3. Click the **⚙ gear icon → Import Run Configurations from File**.
4. Navigate to the `Run Configs/` directory and import all `.run.xml` files.

The following configurations will be available:

| Run Config | Type | Starts |
|---|---|---|
| `LocalStack` | Application | CDK synthesise + deploy to LocalStack |
| `patient-service-db` | Docker Image | PostgreSQL for Patient Service on host port `4000` |
| `auth-service-db` | Docker Image | PostgreSQL for Auth Service on host port `4001` |
| `kafka` | Docker Image | Apache Kafka (KRaft) on ports `9092` / `9094` |
| `auth-service` | Dockerfile | Builds & runs Auth Service on port `8086` |
| `patient-service` | Dockerfile | Builds & runs Patient Service on port `8081` |
| `billing-service` | Dockerfile | Builds & runs Billing Service on ports `8082` / `8083` |
| `analytics-service` | Dockerfile | Builds & runs Analytics Service on port `8084` |
| `api-gateway` | Dockerfile | Builds & runs API Gateway on port `8085` |

#### Recommended Start Order

Start the configurations in this order to respect service dependencies:

```
1. patient-service-db    ← PostgreSQL for PatientService
2. auth-service-db       ← PostgreSQL for AuthService
3. kafka                 ← Kafka broker (required by PatientService & AnalyticsService)
4. billing-service       ← gRPC server (required by PatientService at startup)
5. auth-service          ← JWT issuer (required by API Gateway)
6. patient-service       ← Depends on DB, Kafka, BillingService
7. analytics-service     ← Kafka consumer
8. api-gateway           ← Entry point; must start last
```

> Each Dockerfile configuration performs a **multi-stage Maven build** inside Docker — no local `mvn package` step is needed.

#### Building Services Manually (optional)

If you prefer to build JARs locally before running:

```bash
# Build a specific service
cd PatientService && mvn clean package -DskipTests

# Run with Docker
docker build -t patient-service .
docker run --rm -d \
  --name patient-service \
  --network internal \
  -p 8081:8081 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://patient-service-db:5432/db \
  -e SPRING_DATASOURCE_USERNAME=admin \
  -e SPRING_DATASOURCE_PASSWORD=admin \
  -e BILLING_SERVICE_ADDRESS=billing-service \
  -e BILLING_SERVICE_GRPC_PORT=8083 \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  patient-service
```

---

## Configuration

### Key Environment Variables

| Variable | Service | Value | Purpose |
|---|---|---|---|
| `SPRING_DATASOURCE_URL` | PatientService, AuthService | `jdbc:postgresql://<db-host>:5432/db` | Database connection |
| `SPRING_DATASOURCE_USERNAME` | PatientService, AuthService | `admin` | DB credentials |
| `SPRING_DATASOURCE_PASSWORD` | PatientService, AuthService | `admin` | DB credentials |
| `BILLING_SERVICE_ADDRESS` | PatientService | `billing-service` | gRPC target host |
| `BILLING_SERVICE_GRPC_PORT` | PatientService | `8083` | gRPC target port |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | PatientService, AnalyticsService | `kafka:9092` | Kafka broker address |
| `AUTH_SERVICE_URL` | APIGateway | `http://auth-service:8086` | Auth service upstream |
| `jwt.secret` | AuthService | `<base64-secret>` | JWT signing key |

### API Gateway Routing

| Path Prefix | Upstream | Auth Required |
|---|---|---|
| `/auth/**` | `http://auth-service:8086` | No |
| `/api/patients/**` | `http://patient-service:8081` | Yes (JWT via `JwtValidation` filter) |
| `/api-docs/auth` | `http://auth-service:8086/v3/api-docs` | No |
| `/api-docs/patients` | `http://patient-service:8081/v3/api-docs` | No |

> The `application-prod.yaml` profile replaces internal Docker hostnames with `host.docker.internal:<port>` for running the gateway on the host machine while other services run in containers.

---

## Verification & Testing

### Health Checks

```bash
curl http://localhost:8081/actuator/health   # PatientService
curl http://localhost:8082/actuator/health   # BillingService
curl http://localhost:8084/actuator/health   # AnalyticsService
curl http://localhost:8085/actuator/health   # API Gateway
curl http://localhost:8086/actuator/health   # AuthService
```

### OpenAPI / Swagger UI

| Service | Swagger URL |
|---|---|
| Auth Service | http://localhost:8086/swagger-ui.html |
| Patient Service | http://localhost:8081/swagger-ui.html |

### Quick Smoke Test (Auth → Patient)

```bash
# 1. Obtain a JWT token
TOKEN=$(curl -s -X POST http://localhost:8085/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"testuser@test.com","password":"password123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

# 2. Call the protected patients endpoint
curl -H "Authorization: Bearer $TOKEN" http://localhost:8085/api/patients
```

### Integration Tests

The `IntegrationTests` module uses REST Assured and targets the API Gateway at `http://localhost:8085`. Run them after all services are up:

```bash
cd IntegrationTests
mvn test
```

Tests covered:
- `AuthIntegrationTest` — valid login returns 200 + JWT; invalid login returns 401.
- `PatientIntegrationTest` — authenticated GET `/api/patients` returns 200.

### Verify LocalStack Resources

<details>
<summary>AWS CLI commands targeting LocalStack</summary>

```bash
export AWS_DEFAULT_REGION=us-east-1
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export LOCALSTACK=http://localhost:4566

# List deployed CloudFormation stack
aws --endpoint-url=$LOCALSTACK cloudformation list-stacks \
  --query "StackSummaries[?StackName=='patient-management']"

# Describe RDS instances
aws --endpoint-url=$LOCALSTACK rds describe-db-instances \
  --query "DBInstances[*].{ID:DBInstanceIdentifier,Status:DBInstanceStatus}"

# Describe MSK cluster
aws --endpoint-url=$LOCALSTACK kafka list-clusters \
  --query "ClusterInfoList[*].{Name:ClusterName,State:State}"

# List ECS services
aws --endpoint-url=$LOCALSTACK ecs list-services \
  --cluster patient-management-cluster

# Describe the ALB
aws --endpoint-url=$LOCALSTACK elbv2 describe-load-balancers \
  --query "LoadBalancers[*].{Name:LoadBalancerName,DNS:DNSName}"
```

</details>

---

## AWS Infrastructure (via CDK)

The `Infrastructure` module defines the full production-grade AWS topology using the **AWS CDK for Java** (`aws-cdk-lib 2.178.1`). LocalStack emulates these services locally.

| Resource | AWS Type | Details |
|---|---|---|
| VPC | `AWS::EC2::VPC` | CIDR `10.0.0.0/16`, 2 AZs, public + private subnets |
| Auth DB | `AWS::RDS::DBInstance` | PostgreSQL 17.2, `db.t2.micro`, 20 GB |
| Patient DB | `AWS::RDS::DBInstance` | PostgreSQL 17.2, `db.t2.micro`, 20 GB |
| Kafka | `AWS::MSK::Cluster` | Kafka 2.8.0, 1 broker, `kafka.m5.xlarge` |
| ECS Cluster | `AWS::ECS::Cluster` | Fargate, service discovery namespace `patient-management.local` |
| ECS Services | `AWS::ECS::Service` × 5 | Auth, Patient, Billing, Analytics, API Gateway |
| Load Balancer | `AWS::ElasticLoadBalancingV2::LoadBalancer` | Internet-facing ALB → API Gateway port 8085 |
| Secrets Manager | `AWS::SecretsManager::Secret` | DB master credentials |
| CloudWatch Logs | `AWS::Logs::LogGroup` | Per-service log groups, 1-day retention |

---

## Troubleshooting

| Symptom | Likely Cause | Fix |
|---|---|---|
| `Connection refused` on port `8085` | API Gateway not yet started | Start API Gateway last; wait for dependent services to be healthy first |
| `Network internal not found` | Docker network missing | `docker network create internal` |
| `PatientService` fails to start | `billing-service` gRPC server not ready | Start `billing-service` before `patient-service` |
| Kafka consumer not receiving events | Wrong bootstrap server address | Ensure `SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092` (internal) not `localhost` |
| LocalStack `ConnectionRefused` | LocalStack container not running | `docker ps | grep localstack`; restart if absent |
| CDK deploy fails | `cdk.out/` missing | Run `mvn compile exec:java` in `Infrastructure/` first |
| PostgreSQL `FATAL: password authentication failed` | DB container not initialised | Stop and remove the DB container + its volume, then restart |
| Port already in use | Stale container from a previous run | `docker ps -a` → `docker rm -f <container>` |

<details>
<summary>Full teardown commands</summary>

```bash
# Stop and remove all project containers
docker rm -f api-gateway auth-service patient-service billing-service \
             analytics-service kafka patient-service-db auth-service-db localstack

# Remove the Docker network
docker network rm internal

# Remove persisted DB volumes (destructive — resets all data)
rm -rf ~/db_volumes/patient-service-db ~/db_volumes/auth-service-db
```

</details>

---

## Project Structure

```
Patient Management System/
├── APIGateway/          # Spring Cloud Gateway (WebFlux), JWT filter, routing
├── AuthService/         # Spring Security, JWT issuance, PostgreSQL
├── PatientService/      # Patient CRUD, gRPC client → Billing, Kafka producer
├── BillingService/      # gRPC server, billing account management
├── AnalyticsService/    # Kafka consumer, patient event processing
├── Infrastructure/      # AWS CDK (Java) — VPC, RDS, MSK, ECS, ALB
├── IntegrationTests/    # REST Assured end-to-end tests via API Gateway
└── Run Configs/         # IntelliJ Docker & Application run configurations
```

---

## License

This project is licensed under the [MIT License](LICENSE).
