package org.friesoft.porturl.controller;

import org.friesoft.porturl.entities.User;
import org.friesoft.porturl.security.CustomGrantedAuthoritiesExtractor;
import org.friesoft.porturl.service.UserService;
import org.friesoft.porturl.service.AdminService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @TestConfiguration
    static class ControllerTestConfig {
        @Bean
        public UserService userService() {
            return Mockito.mock(UserService.class);
        }

        @Bean
        public AdminService adminService() {
            return Mockito.mock(AdminService.class);
        }

        @Bean
        public CacheManager cacheManager() {
            return Mockito.mock(CacheManager.class);
        }

        @Bean
        public CustomGrantedAuthoritiesExtractor customGrantedAuthoritiesExtractor() {
            return Mockito.mock(CustomGrantedAuthoritiesExtractor.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Test
    void getCurrentUser_returnsOk() throws Exception {
        User u = new User();
        u.setUsername("testuser");
        when(userService.getCurrentUser()).thenReturn(u);
        when(userService.getUserRoles(any())).thenReturn(List.of("ROLE_USER"));

        mockMvc.perform(get("/api/users/current")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk());
    }
}
