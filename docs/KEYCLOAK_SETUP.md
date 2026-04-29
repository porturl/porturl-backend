# Keycloak Setup for PortUrl

This guide describes how to configure Keycloak for PortUrl. It focuses on the **Identity Brokering** strategy, which allows you to keep your users in their existing realms while using PortUrl for centralized access management.

## 1. The PortUrl Realm (`porturl`)

Create a dedicated realm for PortUrl. This realm acts as an "Aggregator" and "Management Layer."

### Step 1.1: Create the Management Client
Allows the backend to manage roles and user assignments within the PortUrl realm.

1.  **Client ID**: `porturl-management-client`
2.  **Client authentication**: `On`
3.  **Authentication flow**: **Service accounts roles** only.
4.  **Permissions**: Assign `realm-admin` from the `realm-management` client in the **Service account roles** tab.

### Step 1.2: Create the Android App Client (Public)
1.  **Client ID**: `porturl-android`
2.  **Client authentication**: `Off` (Public)
3.  **Standard flow**: `On`
4.  **PKCE Challenge Method**: `S256`
5.  **Valid Redirect URIs**: `org.friesoft.porturl:/*`

---

## 2. Cross-Realm Management (Master Realm)

Required if you want to manage roles in realms other than the PortUrl realm.

1.  Switch to the `master` realm.
2.  **Client ID**: `porturl-cross-realm-client`
3.  **Client authentication**: `On`
4.  **Authentication flow**: **Service accounts roles** only.
5.  **Permissions**: Assign the `admin` role in the **Service account roles** tab.

---

## 3. Identity Brokering (SSO with Existing Realms)

This setup allows users to log into PortUrl using accounts from your existing "Production" or "Main" realm.

### Step 3.1: Configure the Existing Realm (The Identity Provider)
In your **Existing Realm** (where your users are):

1.  Create a new client: `porturl-broker-client`.
2.  **Client authentication**: `On`.
3.  **Standard flow**: `On`.
4.  **Valid Redirect URIs**: `https://<your-keycloak>/realms/porturl/broker/<idp-alias>/endpoint`.

### Step 3.2: Configure the PortUrl Realm (The Service Provider)
In the **PortUrl Realm**:

1.  Go to **Identity Providers** -> **Add provider** -> **Keycloak OpenID Connect**.
2.  **Alias**: (e.g., `production-idp`).
3.  **Endpoints**: Import from `<keycloak-url>/realms/<existing-realm>/.well-known/openid-configuration`.
4.  **Client ID**: `porturl-broker-client`.
5.  **Client Secret**: (From Step 3.1).
6.  **Mappers**: Go to the **Mappers** tab of your new provider and click **Add mapper**:
    *   **Name**: `username-importer`
    *   **Sync mode override**: `Inherit`
    *   **Mapper Type**: `Username Template Importer`
    *   **Template**: `${CLAIM.preferred_username}`
    *   **Target**: `LOCAL`
    *   *This ensures that the user's username in PortUrl exactly matches their username in the source realm, which is critical for cross-realm role assignment.*

### Step 3.3: Seamless Redirect (Optional)
To skip the PortUrl login screen and go straight to your existing realm's login:

1.  In **PortUrl Realm** -> **Authentication** -> **Browser** flow.
2.  Configure **Identity Provider Redirector**.
3.  **Default Identity Provider**: `<idp-alias>` (from Step 3.2).

---

## 4. Session & Timeout Strategy

To achieve a long-lived app session while maintaining security for individual services, configure different timeouts for your realms.

### Step 4.1: PortUrl Realm (Long Session)
This session keeps the Android app logged in and acts as the "Source" for silent SSO.
1.  Go to **Realm Settings** -> **Sessions**.
    *   **SSO Session Idle**: `180 Days`.
    *   **SSO Session Max**: `180 Days`.
    *   **Offline Session Idle/Max**: `180 Days`.
2.  Go to **Realm Settings** -> **Tokens**.
    *   **Revoke Refresh Token**: `On`.
    *   **Refresh Token Max Reuse**: `1` (Provides a tiny grace period to prevent App/Browser race conditions).
    *   *Note: Ensure the `porturl-android` client settings do not override these with shorter values.*

### Step 4.2: Target Realms (Short Session)
These are the realms where your actual services (Grafana, etc.) live.
1.  Go to **Realm Settings** -> **Sessions**.
2.  **SSO Session Idle**: `30 Minutes` (or your preferred short duration).
3.  **SSO Session Max**: `30 Minutes`.

### How it works together:
*   The PortUrl Android app uses **standard (non-ephemeral) Custom Tabs** to share cookies with the system browser.
*   When you open an app from PortUrl, the browser uses the 6-month cookie from the PortUrl realm to silently authorize a new 30-minute session in the target realm.
*   If you access the service directly in a browser, you will be prompted for a login once the 30-minute session expires (unless you have an active 6-month PortUrl session and "Seamless Redirect" enabled).

---

## 5. Backend Configuration

```yaml
porturl:
  keycloak:
    realm: porturl
    admin:
      server-url: https://your-keycloak:8443
      realm: porturl
      client-id: porturl-management-client
      client-secret: <SECRET>
    cross-realm:
      server-url: https://your-keycloak:8443
      realm: master
      client-id: porturl-cross-realm-client
      client-secret: <SECRET>
```

---

## 6. How Roles Work

PortUrl manages two types of roles:
1.  **Access Roles**: Created in the `porturl` realm (e.g., `ROLE_APP_ACCESS`). These determine if the app shows up in the Android app.
2.  **Client Roles**: Assigned in the **Target Realm** (where the app actually resides). 

When you assign a role to a user in PortUrl, the backend:
1.  Finds the user's **username** in the `porturl` realm.
2.  Searches for the **same username** in the target realm.
3.  Assigns the specific client role (e.g., `admin` in Grafana) to that user in the target realm.
