# porturl-backend
Spring Boot REST Backend

## Endpoints
### GET /applications

### POST /applications

## Enable OAuth2 Security (Keycloak example)
```
application:
  security:
    enabled: true
spring:
  security:
    oauth2:
      client:
        registration:
          keycloak:
            client-id: <placeholder>
            client-secret: <placeholder>
        provider:
          keycloak:
            issuer-uri: https://<domain>/auth/realms/<realmname>
            user-name-attribute: sub
```