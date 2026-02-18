package org.friesoft.porturl.config;

import org.friesoft.porturl.service.AlloyHealthService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.info.Info;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TelemetryInfoContributorTest {

    @Test
    void contribute_whenServiceMissing_shouldIncludeEnabledFalseAndHealthyFalse() {
        TelemetryInfoContributor contributor = new TelemetryInfoContributor(null);
        Info.Builder builder = new Info.Builder();
        
        contributor.contribute(builder);
        Info info = builder.build();
        
        Map<String, Object> telemetry = (Map<String, Object>) info.getDetails().get("telemetry");
        assertNotNull(telemetry);
        assertEquals(false, telemetry.get("enabled"));
        assertEquals(false, telemetry.get("healthy"));
    }

    @Test
    void contribute_whenServicePresentAndUp_shouldIncludeEnabledTrueAndHealthyTrue() {
        AlloyHealthService healthService = mock(AlloyHealthService.class);
        when(healthService.isUp()).thenReturn(true);
        
        TelemetryInfoContributor contributor = new TelemetryInfoContributor(healthService);
        Info.Builder builder = new Info.Builder();
        
        contributor.contribute(builder);
        Info info = builder.build();
        
        Map<String, Object> telemetry = (Map<String, Object>) info.getDetails().get("telemetry");
        assertNotNull(telemetry);
        assertEquals(true, telemetry.get("enabled"));
        assertEquals(true, telemetry.get("healthy"));
    }

    @Test
    void contribute_whenServicePresentAndDown_shouldIncludeEnabledTrueAndHealthyFalse() {
        AlloyHealthService healthService = mock(AlloyHealthService.class);
        when(healthService.isUp()).thenReturn(false);
        
        TelemetryInfoContributor contributor = new TelemetryInfoContributor(healthService);
        Info.Builder builder = new Info.Builder();
        
        contributor.contribute(builder);
        Info info = builder.build();
        
        Map<String, Object> telemetry = (Map<String, Object>) info.getDetails().get("telemetry");
        assertNotNull(telemetry);
        assertEquals(true, telemetry.get("enabled"));
        assertEquals(false, telemetry.get("healthy"));
    }
}
