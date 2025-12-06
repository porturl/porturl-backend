package org.friesoft.porturl.controller;

import org.friesoft.porturl.security.CustomGrantedAuthoritiesExtractor;
import org.friesoft.porturl.service.ApplicationService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

/**
 * This is a @WebMvcTest, which focuses only on the web layer (the controller).
 * It does not load the full application context. The ApplicationService is mocked
 * to isolate the controller's behavior.
 */
@WebMvcTest(ApplicationController.class)
class ApplicationControllerTest {

    /**
     * This static nested class is the modern, AOT-compatible replacement for @MockBean.
     * It explicitly defines how to create the mock bean for the test context.
     */
    @TestConfiguration
    static class ControllerTestConfig {
        @Bean
        public ApplicationService applicationService() {
            return Mockito.mock(ApplicationService.class);
        }

        @Bean
        public CustomGrantedAuthoritiesExtractor customGrantedAuthoritiesExtractor() {
            return Mockito.mock(CustomGrantedAuthoritiesExtractor.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationService applicationService; // This is the mock from our TestConfig

    @Test
    void getVisibleApplications_returnsOk() throws Exception {
        // Arrange: Configure the mock service to return an empty list
        when(applicationService.getApplicationsForCurrentUser()).thenReturn(List.of());

        // Act & Assert: Perform the GET request and expect a 200 OK status
        mockMvc.perform(get("/api/applications").with(jwt()))
                .andExpect(status().isOk());
    }

    @Test
    void deleteApplication_asAdmin_returnsOk() throws Exception {
        // Arrange (no service interaction needed for a simple 200 OK check)

        // Act & Assert
        mockMvc.perform(delete("/api/applications/1").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }
}