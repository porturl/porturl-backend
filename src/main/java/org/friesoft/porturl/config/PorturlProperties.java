package org.friesoft.porturl.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "porturl")
@Data
public class PorturlProperties {

    private final Cors cors = new Cors();
    private final Security security = new Security();
    private Storage storage = new Storage();


    @Data
    public static class Cors {
        private List<String> allowedOrigins = Arrays.asList("http://localhost:4200");
    }

    @Data
    public static class Security {
        private boolean enabled = true;
    }

    @Data
    public static class Storage {
        private String location;
    }
}