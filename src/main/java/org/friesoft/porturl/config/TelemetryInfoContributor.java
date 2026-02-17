package org.friesoft.porturl.config;

import org.friesoft.porturl.service.AlloyHealthService;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class TelemetryInfoContributor implements InfoContributor {

    private final AlloyHealthService healthService;

    public TelemetryInfoContributor(AlloyHealthService healthService) {
        this.healthService = healthService;
    }

    @Override
    public void contribute(Info.Builder builder) {
        Map<String, Object> telemetryDetails = new HashMap<>();
        boolean healthy = healthService.isUp();
        
        telemetryDetails.put("enabled", true);
        telemetryDetails.put("healthy", healthy);
        
        builder.withDetail("telemetry", telemetryDetails);
    }
}
