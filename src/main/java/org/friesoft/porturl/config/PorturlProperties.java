package org.friesoft.porturl.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

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
    }

    @Data
    public static class Storage {
        private String location;
    }

    @Data
    public static class CleanupProperties {
        private boolean enabled = true;
        private String cron = "0 0 3 * * ?"; // Defaults to 3 AM every day
    }
}