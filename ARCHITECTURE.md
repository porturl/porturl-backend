# PortUrl Multi-Realm Architecture

PortUrl now supports linking applications to external Keycloak Clients, potentially residing in different realms. This allows for more flexible integration with existing infrastructure.

## Core Concepts

### 1. Linked vs. Unlinked Apps
*   **Unlinked Apps (Legacy)**: PortUrl creates and manages roles locally (e.g., `ROLE_GRAFANA_ADMIN`). These are Realm Roles in the PortUrl realm.
*   **Linked Apps (New)**: PortUrl links to an existing Keycloak Client (via `clientId` and `realm`). Roles are fetched dynamically from that client.

### 2. Role Assignment
*   **Linked**: When you assign a role to a user for a linked app, PortUrl assigns the **Client Role** (e.g., `admin`) on the target client in the target realm to the user.
*   **Unlinked**: PortUrl assigns a composite Realm Role in the PortUrl realm.

### 3. User Identity
*   **Assumption**: To support cross-realm role assignment, PortUrl assumes that the user exists in both realms with the **same username** (or email).
*   **Flow**:
    1.  Admin requests role assignment for User A on App B (Realm R).
    2.  PortUrl looks up User A's username in the local realm.
    3.  PortUrl searches for that username in Realm R.
    4.  If found, PortUrl assigns the client role to the user ID found in Realm R.

### 4. Visibility Control
*   **Linked**: A user sees the app on their dashboard if they possess **any** role for that client in the target realm.
*   **Unlinked**: A user sees the app if they possess the generated `APP_..._ACCESS` role.

## Configuration

*   **`clientId`**: The public Client ID (e.g., `grafana`).
*   **`realm`**: The realm where the client resides. Defaults to PortUrl's own realm if empty.

## Auto-Discovery

PortUrl provides an endpoint to scan a target realm and list all available clients, simplifying the process of adding existing apps.

`GET /api/admin/realms/{realm}/clients`
