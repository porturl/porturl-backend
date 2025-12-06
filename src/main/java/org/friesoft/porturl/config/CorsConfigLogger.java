package org.friesoft.porturl.config; // Or any appropriate package

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class CorsConfigLogger implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(CorsConfigLogger.class);

    private final PorturlProperties portUrlProperties;

    // Spring will inject your custom properties class here
    public CorsConfigLogger(PorturlProperties portUrlProperties) {
        this.portUrlProperties = portUrlProperties;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.info("============================================================");
        logger.info("            CORS CONFIGURATION DIAGNOSTICS");
        logger.info("============================================================");
        if (portUrlProperties.getCors() != null && portUrlProperties.getCors().getAllowedOrigins() != null) {
            logger.info("Loaded Allowed Origins: " + portUrlProperties.getCors().getAllowedOrigins());
        } else {
            logger.warn("PortURL CORS properties are not loaded. Allowed Origins is null.");
        }
        logger.info("============================================================");
    }
}