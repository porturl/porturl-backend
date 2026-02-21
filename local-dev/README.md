# Local Development Environment

This directory contains the full Docker Compose stack required for local development and testing, including Keycloak, PostgreSQL, and Grafana Alloy.

## Quick Start

1.  **Initialize Environment:**
    ```bash
    cp .env.sample .env
    ```
    (Optionally, fill in your Grafana Cloud credentials if you want observability).

2.  **Start the Stack:**
    ```bash
    podman-compose up -d
    ```

3.  **Keycloak Access:**
    - **URL:** [http://localhost:8081](http://localhost:8081)
    - **Credentials:** `admin` / `admin` (default from `.env`)

4.  **Keycloak Realm Export/Import:**
    - Place any realm export JSON files in the `keycloak-import/` directory.
    - They will be automatically imported when the Keycloak container starts.

## Running the Backend

To point the backend to this local Keycloak instance, run with the `LOCALDEV` profile. The backend is pre-configured to look at `localhost:8081`.

```bash
./gradlew bootRun --args='--spring.profiles.active=LOCALDEV'
```

## Observability
- **Alloy UI:** [http://localhost:12345](http://localhost:12345)
- **OTLP Endpoints:** Exposed on `4317` (gRPC) and `4318` (HTTP).
