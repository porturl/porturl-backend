package org.friesoft.porturl;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class KeycloakIntegrationTest {

    @Container
    static final KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:26.0.8")
            .withRealmImportFile("keycloak/realm-export.json");

    @DynamicPropertySource
    static void registerKeycloakProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> keycloak.getAuthServerUrl() + "/realms/porturl-local");
        registry.add("porturl.keycloak.admin.server-url", keycloak::getAuthServerUrl);
        registry.add("porturl.keycloak.admin.realm", () -> "porturl-local");
        registry.add("porturl.keycloak.admin.client-id", () -> "porturl-management-client");
        registry.add("porturl.keycloak.admin.client-secret", () -> "local-management-secret");
        
        registry.add("porturl.security.validate-issuer", () -> "false");
    }

    @Test
    void contextLoads() {
        assertTrue(keycloak.isRunning());
    }
}
