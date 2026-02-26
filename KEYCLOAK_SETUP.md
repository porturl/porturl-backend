# Keycloak Setup for PortUrl

This guide describes how to manually configure an existing Keycloak installation to work with PortUrl.

PortUrl uses a dual-client architecture:
1.  **PortUrl Realm**: A dedicated realm for PortUrl's own users and application-specific roles.
2.  **Master Realm (Cross-Realm)**: Used only for managing users and roles in *other* realms (e.g., if you want to link an app from a "Production" realm to PortUrl).

---

## 1. The PortUrl Realm (`porturl`)

Create a new realm named `porturl` (or your preferred name).

### Step 1.1: Create the Management Client
This client allows the backend to manage roles and users within the PortUrl realm.

1.  Navigate to **Clients** -> **Create client**.
2.  **Client ID**: `porturl-management-client`
3.  **Name**: `PortUrl Local Management`
4.  **Client authentication**: `On`
5.  **Authorization**: `Off`
6.  **Authentication flow**: Uncheck everything except **Service accounts roles**.
7.  Click **Save**.

**Assign Permissions:**
1.  Go to the **Service account roles** tab for this client.
2.  Click **Assign role**.
3.  Filter by client and search for `realm-management`.
4.  Assign the `realm-admin` role from the `realm-management` client.

**Obtain Secret:**
1.  Go to the **Credentials** tab and copy the **Client secret**.

### Step 1.2: Create the Android App Client (Public)
1.  **Client ID**: `porturl-android`
2.  **Name**: `PortUrl Android App`
3.  **Client authentication**: `Off` (Public)
4.  **Standard flow**: `On`
5.  **PKCE Challenge Method**: `S256` (Recommended)
6.  **Valid Redirect URIs**: `org.friesoft.porturl:/*` and `http://localhost:*`
7.  **Web Origins**: `*`

### Step 1.3: Create the Swagger UI Client (Public)
This client is used by the Swagger UI to authenticate for testing API endpoints.

1.  **Client ID**: `porturl-backend-swagger-client`
2.  **Name**: `Swagger UI`
3.  **Client authentication**: `Off` (Public)
4.  **Standard flow**: `On`
5.  **PKCE Challenge Method**: `S256`
6.  **Valid Redirect URIs**: `http://localhost:8080/swagger-ui/oauth2-redirect.html` (or your backend's Swagger redirect URL)
7.  **Web Origins**: `*`

### Step 1.4: Define the Administrator Role
1.  Go to **Realm Roles** -> **Create role**.
2.  **Role Name**: `ROLE_ADMIN`
3.  Assign this role to any user who should be a PortUrl Administrator.

---

## 2. Cross-Realm Management (Master Realm)

If you plan to link applications from realms other than the PortUrl realm, you need a management client in the `master` realm.

1.  Switch to the `master` realm.
2.  Navigate to **Clients** -> **Create client**.
3.  **Client ID**: `porturl-cross-realm-client`
4.  **Client authentication**: `On`
5.  **Authentication flow**: **Service accounts roles** only.
6.  Click **Save**.

**Assign Permissions:**
1.  Go to the **Service account roles** tab.
2.  Click **Assign role**.
3.  Search for `admin`.
4.  Assign the `admin` role (this is a realm role in `master` that allows managing ALL realms).

**Obtain Secret:**
1.  Go to the **Credentials** tab and copy the **Client secret**.

---

## 3. Backend Configuration

Update your `application.yaml` (or environment variables) with the secrets and URLs obtained above:

```yaml
porturl:
  keycloak:
    realm: porturl  # Your PortUrl realm name
    admin:
      server-url: https://your-keycloak:8443
      realm: porturl
      client-id: porturl-management-client
      client-secret: <PORTURL_REALM_CLIENT_SECRET>
    cross-realm:
      server-url: https://your-keycloak:8443
      realm: master
      client-id: porturl-cross-realm-client
      client-secret: <MASTER_REALM_CLIENT_SECRET>

spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://your-keycloak:8443/realms/porturl
```

### Environment Variables Equivalent:
- `PORTURL_KEYCLOAK_REALM`
- `PORTURL_KEYCLOAK_ADMIN_SERVER_URL`
- `PORTURL_KEYCLOAK_ADMIN_REALM`
- `PORTURL_KEYCLOAK_ADMIN_CLIENT_ID`
- `PORTURL_KEYCLOAK_ADMIN_CLIENT_SECRET`
- `PORTURL_KEYCLOAK_CROSS_REALM_SERVER_URL`
- `PORTURL_KEYCLOAK_CROSS_REALM_REALM`
- `PORTURL_KEYCLOAK_CROSS_REALM_CLIENT_ID`
- `PORTURL_KEYCLOAK_CROSS_REALM_CLIENT_SECRET`
- `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI`

---

## 4. Multi-Realm Single Sign-On (SSO)

To allow seamless SSO between the PortUrl realm and other application realms (e.g., a "Production" realm), configure **Identity Brokering**.

### Step 4.1: Configure PortUrl Realm (The Identity Provider)
1.  In the **PortUrl Realm**, create a new client.
2.  **Client ID**: `porturl-broker`
3.  **Client authentication**: `On`
4.  **Valid Redirect URIs**: `https://<your-keycloak>/realms/*/broker/porturl-idp/endpoint` (using a wildcard allows multiple realms).
5.  Save and copy the **Client Secret** from the **Credentials** tab.

### Step 4.2: Configure the Target Realm (The Service Provider)
1.  Switch to the **Target Realm** (where your app resides).
2.  Go to **Identity Providers** -> **Add provider** -> **Keycloak OpenID Connect**.
3.  **Alias**: `porturl-idp`
4.  **Display Name**: `PortUrl Login`
5.  **Endpoints**: Use "Import from URL" with `https://<your-keycloak>/realms/porturl/.well-known/openid-configuration`.
6.  **Client ID**: `porturl-broker`
7.  **Client Secret**: (The secret from Step 4.1).
8.  Set **Sync Mode** to `Import` or `Force`.
9.  Click **Save**.

### Step 4.3: Ensure Username Consistency
1.  In the **Target Realm**, go to the **Mappers** tab of the newly created `porturl-idp`.
2.  Click **Add mapper**.
3.  **Name**: `username-importer`
4.  **Mapper Type**: `Username Template Importer`
5.  **Template**: `${CLAIM.preferred_username}`
6.  **Target**: `LOCAL`
7.  Click **Save**. *This ensures PortUrl can find the user by username to assign roles.*

### Step 4.4: Enable Seamless Redirect (Skip Login Screen)
1.  In the **Target Realm**, go to **Authentication**.
2.  Select the **Browser** flow (or create a copy).
3.  Find the **Identity Provider Redirector** execution.
4.  Click the **Config** icon (gear).
5.  **Alias**: `porturl-redirect`
6.  **Default Identity Provider**: `porturl-idp`
7.  Click **Save**.
8.  Ensure the **Identity Provider Redirector** is set to `ALTERNATIVE` and has a higher priority than the forms (or just set it as the first step).

