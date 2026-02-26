package org.friesoft.porturl.config;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;

@Configuration
@ImportRuntimeHints(NativeBinaryHints.class)
@Slf4j
public class KeycloakAdminConfig {

    private final PorturlProperties properties;

    public KeycloakAdminConfig(PorturlProperties properties) {
        this.properties = properties;
    }

    @Bean
    public Keycloak keycloakAdmin() {
        return createKeycloakClient(properties.getKeycloak().getAdmin());
    }

    @Bean
    public Keycloak masterKeycloakAdmin() {
        if (properties.getKeycloak().getCrossRealm() == null || properties.getKeycloak().getCrossRealm().getServerUrl() == null) {
            return keycloakAdmin(); // Fallback to local if cross-realm not configured
        }
        return createKeycloakClient(properties.getKeycloak().getCrossRealm());
    }

    private Keycloak createKeycloakClient(PorturlProperties.Keycloak.Admin admin) {
        if (admin == null || admin.getServerUrl() == null || admin.getRealm() == null) {
            return null;
        }

        KeycloakBuilder builder = KeycloakBuilder.builder()
                .serverUrl(admin.getServerUrl())
                .realm(admin.getRealm())
                .grantType("client_credentials")
                .clientId(admin.getClientId())
                .clientSecret(admin.getClientSecret());

        String trustStorePathStr = properties.getKeycloak().getTruststorePath();
        if (trustStorePathStr != null) {
            try {
                Path path = Paths.get(trustStorePathStr);
                if (Files.exists(path)) {
                    log.info("Configuring Keycloak Admin Client for realm {} with truststore: {}", admin.getRealm(), path.toAbsolutePath());
                    KeyStore trustStore = KeyStore.getInstance("JKS");
                    try (FileInputStream fis = new FileInputStream(path.toFile())) {
                        trustStore.load(fis, properties.getKeycloak().getTruststorePassword().toCharArray());
                    }

                    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                    tmf.init(trustStore);

                    SSLContext sslContext = SSLContext.getInstance("TLS");
                    sslContext.init(null, tmf.getTrustManagers(), null);

                    // Configure Resteasy client specifically for the Keycloak Admin Client
                    ResteasyClientBuilder clientBuilder = (ResteasyClientBuilder) ResteasyClientBuilder.newBuilder();
                    clientBuilder.sslContext(sslContext);
                    clientBuilder.hostnameVerifier((hostname, session) -> 
                        hostname.equals("localhost") || hostname.equals("127.0.0.1") || hostname.equals("10.0.2.2")
                    );
                    
                    builder.resteasyClient(clientBuilder.build());
                    log.info("Keycloak Admin Client SSL trust established for realm {}.", admin.getRealm());
                }
            } catch (Exception e) {
                log.error("Failed to configure Keycloak Admin Client SSL trust for realm {}", admin.getRealm(), e);
            }
        }

        return builder.build();
    }
}
