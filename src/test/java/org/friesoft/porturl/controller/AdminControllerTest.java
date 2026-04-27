package org.friesoft.porturl.controller;

import org.friesoft.porturl.dto.ExportData;
import org.friesoft.porturl.security.CustomGrantedAuthoritiesExtractor;
import org.friesoft.porturl.service.AdminService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @TestConfiguration
    static class ControllerTestConfig {
        @Bean
        public AdminService adminService() {
            return Mockito.mock(AdminService.class);
        }

        @Bean
        public CustomGrantedAuthoritiesExtractor customGrantedAuthoritiesExtractor() {
            return Mockito.mock(CustomGrantedAuthoritiesExtractor.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminService adminService;

    @Test
    void exportData_returnsOk() throws Exception {
        when(adminService.exportData()).thenReturn(new ExportData());

        mockMvc.perform(get("/api/admin/export")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void importData_returnsOk() throws Exception {
        mockMvc.perform(post("/api/admin/import")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.parseMediaType("application/x-yaml"))
                .content("{}"))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void scanRealmClients_returnsOk() throws Exception {
        mockMvc.perform(get("/api/admin/realms/test/clients")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void listRealms_returnsOk() throws Exception {
        mockMvc.perform(get("/api/admin/realms")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }
}
