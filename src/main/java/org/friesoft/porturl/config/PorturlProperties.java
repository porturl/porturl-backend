package org.friesoft.porturl.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "porturl")
@Getter
public class PorturlProperties {

    private final Cors cors = new Cors();
    private final Security security = new Security();

    @Getter
    @Setter
    public static class Cors {
        private List<String> allowedOrigins = Arrays.asList("http://localhost:4200");
    }

    @Getter
    @Setter
    public static class Security {
        private boolean enabled = true;
    }
}