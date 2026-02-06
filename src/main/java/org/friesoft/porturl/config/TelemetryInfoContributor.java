package org.friesoft.porturl.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Component
public class TelemetryInfoContributor implements InfoContributor {

    @Value("${management.otlp.metrics.export.url:http://localhost:4318/v1/metrics}")
    private String otlpEndpoint;

    @Override
    public void contribute(Info.Builder builder) {
        Map<String, Object> telemetryDetails = new HashMap<>();
        boolean healthy = checkAlloyHealth();
        
        telemetryDetails.put("enabled", true);
        telemetryDetails.put("healthy", healthy);
        
        builder.withDetail("telemetry", telemetryDetails);
    }

    private boolean checkAlloyHealth() {
        try {
            // Extract the base URL from the metrics endpoint (e.g., http://localhost:4318)
            String baseUrl = otlpEndpoint.substring(0, otlpEndpoint.indexOf("/v1/"));
            URL url = new URL(baseUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(1000);
            connection.setReadTimeout(1000);
            int responseCode = connection.getResponseCode();
            // Alloy usually returns 404 for GET on the root or OTLP endpoints if not configured for it, 
            // but if we can connect, it's "working" at the networking level.
            return responseCode < 500;
        } catch (Exception e) {
            return false;
        }
    }
}
