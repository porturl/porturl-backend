# Local Development Environment

This directory contains the full Docker Compose stack required for local development and testing, including Keycloak, PostgreSQL, and Grafana Alloy.

## Quick Start

1.  **Initialize Environment:**
    ```bash
    cp .env.sample .env
    ```
    - Optionally, fill in your Grafana Cloud credentials for observability.
    - Set `ANDROID_PROJECT_PATH` if you want automatic TLS certificate synchronization with the Android app.

2.  **Start/Reset the Stack:**
    Use the provided automation script for the first start and any subsequent state resets:
    ```bash
    ./scripts/restart-dev.sh
    ```
    This script will:
    - Generate fresh TLS certificates (valid for 90 days).
    - Automatically copy the certificates to the Android project (if configured).
    - Wipe the local Keycloak database for a clean state.
    - Start all containers via `podman-compose`.
    - Follow the container logs.

## Keycloak Access
- **HTTPS (Standard):** [https://localhost:8443](https://localhost:8443)
- **HTTP (Legacy):** [http://localhost:8081](http://localhost:8081)
- **Credentials:** `admin` / `admin` (default from `.env`)

### Automatic Realm Import
Place any realm export JSON files in the `keycloak-import/` directory. They will be automatically imported when the Keycloak container starts. Note that `restart-dev.sh` wipes the database, so your JSON files are the source of truth.

## Running the Backend

To point the backend to this local Keycloak instance, run with the `LOCALDEV` profile.

```bash
./gradlew bootRun --args='--spring.profiles.active=LOCALDEV'
```

### Trusting the Local CA
Because Keycloak uses a self-signed certificate signed by a local Root CA, the backend must be told to trust it. The `restart-dev.sh` script automatically creates a `truststore.jks` for you.

## Android App Integration

The Android app requires HTTPS for Keycloak (due to AppAuth library constraints).
1. Ensure `ANDROID_PROJECT_PATH` is set in `.env`.
2. Run `./scripts/restart-dev.sh`.
3. The app will automatically trust the generated `local_ca.crt` via its `network_security_config.xml`.

## Observability
- **Alloy UI:** [http://localhost:12345](http://localhost:12345)
- **OTLP Endpoints:** Exposed on `4317` (gRPC) and `4318` (HTTP).
