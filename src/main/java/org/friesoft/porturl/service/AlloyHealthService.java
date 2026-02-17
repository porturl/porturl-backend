package org.friesoft.porturl.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

@Slf4j
@Service
@ConditionalOnProperty(name = "porturl.otel.enabled", havingValue = "true")
public class AlloyHealthService {

    @Value("${management.otlp.metrics.export.url:http://localhost:4318/v1/metrics}")
    private String otlpEndpoint;

    @Getter
    private volatile boolean isUp = true;

    @Scheduled(fixedDelayString = "${porturl.otel.health-check-interval:30s}")
    public void checkHealth() {
        boolean currentlyUp = performHealthCheck();
        if (currentlyUp != isUp) {
            if (currentlyUp) {
                log.info("Alloy is UP. Resuming telemetry export.");
            } else {
                log.warn("Alloy is DOWN. Suspending telemetry export.");
            }
            isUp = currentlyUp;
        }
    }

    private boolean performHealthCheck() {
        try {
            // Extract the base URL (e.g., http://localhost:4318)
            String baseUrl;
            if (otlpEndpoint.contains("/v1/")) {
                baseUrl = otlpEndpoint.substring(0, otlpEndpoint.indexOf("/v1/"));
            } else {
                baseUrl = otlpEndpoint;
            }
            
            URL url = URI.create(baseUrl).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(2000);
            int responseCode = connection.getResponseCode();
            // Any non-server error means the service is reachable and responding
            return responseCode < 500;
        } catch (Exception e) {
            return false;
        }
    }
}
