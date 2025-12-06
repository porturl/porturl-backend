package org.friesoft.porturl;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "keycloak.admin.server-url=http://localhost:9999",
    "keycloak.admin.realm=test",
    "keycloak.admin.client-id=test",
    "keycloak.admin.client-secret=test"
})
class PortUrlApplicationApplicationTests {

    @Test
    void contextLoads() {
        // This test will now pass. @TestPropertySource provides the necessary
        // properties, allowing the KeycloakAdminConfig bean to be created
        // and the application context to load successfully.
    }

}
