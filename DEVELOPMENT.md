# Development Guide

This guide explains how to set up your local development environment for the `porturl-backend`.

## Prerequisites

- Java 25 (GraalVM recommended)
- Podman (or Docker) with `podman-compose`

## Local Development Stack

We use a centralized Docker Compose stack located in the `local-dev/` directory. This includes:
- **Keycloak**: For authentication and authorization.
- **PostgreSQL**: Database for Keycloak.
- **Grafana Alloy**: For local observability and telemetry.

### 1. Setup Environment

Navigate to the `local-dev` directory and initialize your environment:

```bash
cd local-dev
cp .env.sample .env
```

If you want to send telemetry to Grafana Cloud, edit `.env` and provide your OTLP credentials.

### 2. Start the Stack

```bash
podman-compose up -d
```

**Verification:**
- Keycloak UI: [http://localhost:8081](http://localhost:8081) (Admin: `admin`/`admin`)
- Alloy UI: [http://localhost:12345](http://localhost:12345)

### 3. Run the Backend

To point the backend to the local Keycloak instance, run with the `LOCALDEV` profile:

```bash
./gradlew bootRun --args='--spring.profiles.active=LOCALDEV'
```

## Testing

### Automated Tests
```bash
./gradlew test
```

### Manual API Testing
The project includes Bruno tests in the `bruno-tests` directory. See the README in that folder for instructions on how to run them against your local setup.