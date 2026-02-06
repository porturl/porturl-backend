# Development Guide

This guide explains how to set up your local development environment for the `porturl-backend`.

## Prerequisites

- Java 25 (GraalVM recommended)
- Podman (or Docker) with `podman-compose`
- A Grafana Cloud account

## Local Observability with Grafana Alloy

To monitor the application locally and send data to Grafana Cloud, we use Grafana Alloy.

### 1. Configure Grafana Cloud Credentials

Copy the sample environment file and fill in your OTLP credentials:

```bash
cp .env.alloy.sample .env.alloy
```

Edit `.env.alloy` and provide:
- `GRAFANA_CLOUD_OTLP_URL`: Your OTLP endpoint (e.g., `https://otlp-gateway-.../otlp`)
- `GRAFANA_CLOUD_USER`: Your **Numeric Instance ID**
- `GRAFANA_CLOUD_API_KEY`: Your Cloud Access Policy Token

### 2. Start Alloy

Run Alloy using Podman Compose:

```bash
podman-compose -f docker-compose.alloy.yml --env-file .env.alloy up -d
```

**Verification:**
- Check logs: `podman logs -f alloy-dev`
- Alloy UI: [http://localhost:12345](http://localhost:12345)

### 3. Configure the Backend

The backend is configured to act as a gateway for OTLP traffic. This means both the backend itself and any other clients (like the mobile app) can send data to the backend, which then proxies it to Alloy.

**How it works:**
- **Backend Port**: 8080
- **Gateway Path**: `/otlp/**` -> Proxies to `localhost:4318` (Alloy)

**Configuration in `src/main/resources/application-DEV.yaml`:**

```yaml
management:
  otlp:
    metrics:
      export:
        url: http://localhost:8080/otlp/v1/metrics
    tracing:
      export:
        url: http://localhost:8080/otlp/v1/traces
    logging:
      export:
        url: http://localhost:8080/otlp/v1/logs
```

Then run the application with the `DEV` profile:

```bash
./gradlew bootRun --args='--spring.profiles.active=DEV'
```

**Verification:**
- You should see successful POST requests to `/otlp/...` in the backend logs (status 200).
- Data should appear in the Alloy UI.

## Testing

### Automated Tests
```bash
./gradlew test
```

### Manual API Testing
The project includes Bruno tests in the `bruno-tests` directory.