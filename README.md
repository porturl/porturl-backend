# porturl-backend
Spring Boot REST Backend

## Endpoints
### GET /applications

### POST /applications

## Configure your oauth2 resourceserver
src/main/resources/application-DEV.yaml
```
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://sso.yourdomain.com/auth/realms/<yourrealm>
```

Start application using:
```
-Dspring.profiles.active=DEV
```