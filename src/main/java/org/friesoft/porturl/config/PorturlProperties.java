package org.friesoft.porturl.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "porturl")
@Data
public class PorturlProperties {

    private final Cors cors = new Cors();
    private final Security security = new Security();
    private final Otel otel = new Otel();
    private Storage storage = new Storage();
    private CleanupProperties cleanup = new CleanupProperties();
    
    @NestedConfigurationProperty
    private Keycloak keycloak = new Keycloak();

    @Data
    public static class Cors {
        private List<String> allowedOrigins = Arrays.asList("http://localhost:4200");
    }

    @Data
    public static class Otel {
        private String alloyUrl = "http://localhost:4318";
    }

    @Data
    public static class Security {
        private boolean enabled = true;
        private boolean validateIssuer = true;
    }

    @Data
    public static class Storage {
        private String location;
        private StorageType type = StorageType.SQL;
        private String yamlPath = "config.yaml";
    }

    public enum StorageType {
        SQL,
        YAML
    }

    @Data
    public static class CleanupProperties {
        private boolean enabled = true;
        private String cron = "0 0 3 * * ?"; // Defaults to 3 AM every day
    }

    @Data
    public static class Keycloak {
        private String realm;
        private String truststorePath;
        private String truststorePassword = "changeit";

        @NestedConfigurationProperty
        private Admin admin = new Admin();

        @NestedConfigurationProperty
        private Admin crossRealm = new Admin();

        @Data
        public static class Admin {
            private String serverUrl;
            private String realm;
            private String clientId;
            private String clientSecret;
        }
    }
}
