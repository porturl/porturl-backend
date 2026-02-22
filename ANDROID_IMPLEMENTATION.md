# Updating Android App for PortUrl 1.1 (Multi-Realm Support)

This guide details the changes required in the Android application to support the new linked-application features.

## 1. DTO Updates

Update your Retrofit or network models to include the new `clientId` and `realm` fields.

### `Application` / `ApplicationCreateRequest` / `ApplicationUpdateRequest`

```kotlin
data class Application(
    // ... existing fields ...
    val clientId: String? = null,
    val realm: String? = null
)

data class ApplicationCreateRequest(
    // ... existing fields ...
    val clientId: String? = null,
    val realm: String? = null
)
```

### New Model: `KeycloakClientDto`

```kotlin
data class KeycloakClientDto(
    val id: String,
    val clientId: String,
    val name: String?
)
```

## 2. API Interface Updates

Add the new auto-discovery endpoint to your API service interface.

```kotlin
interface PortUrlApi {
    // ... existing calls ...

    @GET("api/admin/realms/{realm}/clients")
    suspend fun getRealmClients(@Path("realm") realm: String): List<KeycloakClientDto>
}
```

## 3. UI Changes: "Add Application" Screen

### New Fields
*   Add an optional **"Client ID"** input field.
*   Add an optional **"Realm"** input field (defaulting to empty/current).

### Auto-Discovery Feature (Optional but Recommended)
*   Add a "Scan" button next to the Realm field.
*   When clicked, call `getRealmClients(realm)`.
*   Show a dialog/bottom sheet listing the returned clients.
*   On selection, populate the **Client ID** and **Name** fields automatically.

## 4. UI Changes: Application Details / Edit

*   Show the linked Client ID and Realm if present (read-only or editable).
*   **Role Management**: When assigning roles for a linked app, the `availableRoles` list returned by `getApplicationsForCurrentUser` (for admins) or `getRolesForApplication` will now contain actual client roles (e.g., `admin`, `editor`) instead of `ROLE_GRAFANA_ADMIN`. Display them as-is.

## 5. Behavior Changes

*   **Visibility**: No client-side changes needed. The list of visible apps is filtered by the backend based on the new logic. If the user sees it in the list, they have access.
*   **Icons**: Ensure you handle apps that might not have custom icons (linked apps might not have an icon uploaded yet). The existing fallback logic should apply.
