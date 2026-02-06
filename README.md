# porturl Backend: Admin & Permissions Guide

This document outlines how to configure Keycloak and use the backend API to dynamically create applications and manage user permissions.

## 1. Keycloak Configuration

The backend uses Keycloak as a single source of truth for all user permissions. It can dynamically create and manage the entire role hierarchy for new applications. To enable this, the backend requires a dedicated service account with administrative privileges.

### Step 1: Create a Service Account Client

This client allows the backend to authenticate itself against the Keycloak Admin API.

1.  Navigate to your Keycloak Admin Console -> **Clients** -> **Create client**.
2.  **Client ID:** `porturl-backend-api-client`
3.  **Client authentication:** `On`
4.  Click **Save**.

### Step 2: Grant Administrative Permissions

1.  After creating the client, stay on its settings page and go to the **Service account roles** tab.
2.  Click **Assign role**.
3.  In the search box, type `realm-admin` and assign this role. This composite role grants all the necessary permissions for the backend to manage users and roles.

### Step 3: Obtain the Client Secret

1.  Go to the **Credentials** tab for the `porturl-backend-api-client`.
2.  You will see a field labeled **Client Secret**. Copy this value.
3.  Add this secret to your backend's `application.properties` file:

    ```properties
    keycloak.admin.server-url=http://localhost:8080
    keycloak.admin.realm=your-realm-name
    keycloak.admin.client-id=porturl-backend-api-client
    keycloak.admin.client-secret=PASTE_THE_COPIED_SECRET_HERE
    ```

### Step 4: Define a Port-URL Administrator Role

For the administrative endpoints to work, you must have a role for your `porturl` administrators.

1.  Go to **Realm Roles** -> **Create role**.
2.  **Role Name:** `ROLE_ADMIN` (or a name of your choice).
3.  Assign this role to any user who should have administrative privileges within the `porturl` application itself.

---

## 2. Automated Role Hierarchy

When a new application is created, the backend automatically generates a full set of roles in Keycloak.

**Example:** Creating an application named "Grafana" with roles `["admin", "viewer"]`.

The backend will create the following 5 roles in Keycloak:
-   `APP_GRAFANA_ACCESS`: The base role to grant access.
-   `PERM_GRAFANA_ADMIN`: The permission for admin actions.
-   `PERM_GRAFANA_VIEWER`: The permission for viewing actions.
-   `ROLE_GRAFANA_ADMIN`: A **composite role** that includes `APP_GRAFANA_ACCESS` and `PERM_GRAFANA_ADMIN`.
-   `ROLE_GRAFANA_VIEWER`: A **composite role** that includes `APP_GRAFANA_ACCESS` and `PERM_GRAFANA_VIEWER`.

This entire structure is created automatically. Administrators only need to assign the high-level `ROLE_...` to users.

---

## 3. Backend REST API Endpoints

### User-Facing Endpoint

-   **`GET /api/applications`**
    -   **Description:** This is a smart endpoint with dual functionality.
        -   **For a regular user:** It returns a list of only the applications they have been granted access to (i.e., where they have the corresponding `APP_..._ACCESS` role).
        -   **For a user with `ROLE_ADMIN`:** It returns a list of *all* applications in the system, allowing the admin UI to manage them.

### Administrative Endpoints

The following endpoints are available for managing users and applications. They all require the caller to have the `ROLE_ADMIN` authority.

-   **`GET /api/users`**: Retrieves a list of all users for management purposes.

-   **`POST /api/applications`**
    -   **Description:** Creates a new application and its entire role hierarchy in Keycloak.
    -   **Body:**
        ```json
        {
          "name": "My New App",
          "url": "https://app.example.com",
          "roles": ["admin", "editor", "viewer"]
        }
        ```

-   **`POST /api/applications/{appId}/assign/{userId}/{role}`**
    -   **Description:** Grants a user a specific role for an application.
    -   **Example:** `.../assign/123/456/admin` - Assigns the `ROLE_MY_NEW_APP_ADMIN` composite role to the user.

-   **`POST /api/applications/{appId}/unassign/{userId}/{role}`**
    -   **Description:** Revokes a user's specific role for an application.
    -   **Example:** `.../unassign/123/456/admin` - Removes the `ROLE_MY_NEW_APP_ADMIN` composite role from the user.

## 4. Monitoring & Observability (Grafana Cloud)

The backend is instrumented with OpenTelemetry (OTLP) to send metrics and traces to Grafana Cloud.

### Configuration
By default, monitoring is enabled. The backend sends all telemetry (logs, metrics, and traces) to a local **Grafana Alloy** instance. It also acts as a proxy for the Android app's telemetry.

#### Telemetry Flow
1.  **Android App** -> `Backend (:8080/otlp)` -> `Alloy (:4318)` -> **Grafana Cloud**
2.  **Backend** -> `Alloy (:4318)` -> **Grafana Cloud**

| Property | Environment Variable | Default | Description |
| :--- | :--- | :--- | :--- |
| `management.tracing.enabled` | `MANAGEMENT_TRACING_ENABLED` | `true` | Enables/disables tracing. |
| `management.otlp.metrics.export.url` | `OTEL_EXPORTER_OTLP_METRICS_ENDPOINT` | `http://localhost:4318/v1/metrics` | Local Alloy endpoint. |
| `management.tracing.sampling.probability` | `MANAGEMENT_TRACING_SAMPLING_PROBABILITY` | `1.0` | Sampling rate (1.0 = 100%). |

### Running with Monitoring
To start the backend with monitoring enabled:
```bash
export GRAFANA_OTLP_AUTH="Basic <your_base64_auth_token>"
./gradlew bootRun
```

---

## 5. Administrator Workflow: How to Grant App Access

### Scenario: An admin wants to grant "Bob" admin rights for the new "Grafana" app.

1.  **The admin's frontend loads the user management page.**
    -   The frontend calls `GET /api/users` to get a list of all users. The admin finds "Bob" and gets his `userId`.
    -   The frontend calls `GET /api/applications` (as an admin) to get a list of all applications. The admin finds "Grafana" and gets its `applicationId`.
    -   The UI presents the available roles for Grafana ("admin", "viewer").

2.  **The admin clicks an "Assign Admin" button in the UI.**
    -   The frontend makes a call to:
        `POST /api/applications/{grafana-app-id}/assign/{bob-user-id}/admin`

3.  **The backend performs the magic.**
    -   It looks up the "Grafana" application.
    -   It looks up "Bob" to find his Keycloak provider ID.
    -   It calls the Keycloak Admin API to find the role named `ROLE_GRAFANA_ADMIN`.
    -   It assigns this composite role to Bob in Keycloak.

4.  **Confirmation.**
    -   The API returns a `200 OK`. The next time Bob logs in, his JWT will contain `ROLE_GRAFANA_ADMIN`, `APP_GRAFANA_ACCESS`, and `PERM_GRAFANA_ADMIN`, granting him full access and permissions.
