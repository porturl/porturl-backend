package org.friesoft.porturl.config;

import org.friesoft.porturl.service.AlloyHealthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class TelemetryInfoContributor implements InfoContributor {

    private final Optional<AlloyHealthService> healthService;

    public TelemetryInfoContributor(@Autowired(required = false) AlloyHealthService healthService) {
        this.healthService = Optional.ofNullable(healthService);
    }

    @Override
    public void contribute(Info.Builder builder) {
        Map<String, Object> telemetryDetails = new HashMap<>();
        boolean enabled = healthService.isPresent();
        boolean healthy = healthService.map(AlloyHealthService::isUp).orElse(false);
        
        telemetryDetails.put("enabled", enabled);
        telemetryDetails.put("healthy", healthy);
        
        builder.withDetail("telemetry", telemetryDetails);
    }
}
