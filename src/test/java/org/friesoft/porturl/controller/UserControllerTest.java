package org.friesoft.porturl.controller;

import org.friesoft.porturl.entities.User;
import org.friesoft.porturl.security.CustomGrantedAuthoritiesExtractor;
import org.friesoft.porturl.service.UserService;
import org.friesoft.porturl.service.AdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.mockito.Mockito.*;
import org.springframework.http.MediaType;
import org.springframework.cache.Cache;
import java.util.UUID;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private UserService userService;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private AdminService adminService;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private CacheManager cacheManager;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private CustomGrantedAuthoritiesExtractor customGrantedAuthoritiesExtractor;

    @Autowired
    private MockMvc mockMvc;

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

    @Test
    void createAuthTicket_success() throws Exception {
        Cache mockCache = mock(Cache.class);
        when(cacheManager.getCache("authTickets")).thenReturn(mockCache);

        mockMvc.perform(post("/api/auth/ticket")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk());

        verify(mockCache).put(anyString(), any());
    }

    @Test
    void bridgeAuth_success() throws Exception {
        UUID ticket = UUID.randomUUID();
        Cache mockCache = mock(Cache.class);
        when(cacheManager.getCache("authTickets")).thenReturn(mockCache);

        org.springframework.security.core.Authentication auth = mock(
                org.springframework.security.core.Authentication.class);
        when(mockCache.get(ticket.toString(), org.springframework.security.core.Authentication.class)).thenReturn(auth);

        mockMvc.perform(get("/auth/bridge")
                .param("ticket", ticket.toString())
                .param("next", "http://example.com"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "http://example.com"));

        verify(mockCache).evict(ticket.toString());
    }

    @Test
    void updateCurrentUser_success() throws Exception {
        org.friesoft.porturl.dto.UserUpdateRequest req = new org.friesoft.porturl.dto.UserUpdateRequest();
        req.setImage("new-image");

        User u = new User();
        u.setUsername("testuser");
        when(userService.updateCurrentUser(any())).thenReturn(u);

        mockMvc.perform(patch("/api/users/current")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(req))
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk());
    }

    @Test
    void getCurrentUserRoles_success() throws Exception {
        when(userService.getCurrentUserRoles()).thenAnswer(inv -> List.of(new SimpleGrantedAuthority("ROLE_USER")));

        mockMvc.perform(get("/api/users/roles")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk());
    }

    @Test
    void getAllUsers_success() throws Exception {
        when(userService.findAll(any(), any())).thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

        mockMvc.perform(get("/api/users")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void createUser_success() throws Exception {
        org.friesoft.porturl.dto.User req = new org.friesoft.porturl.dto.User();
        req.setUsername("newuser");

        User savedUser = new User();
        savedUser.setUsername("newuser");

        when(userService.save(any())).thenReturn(savedUser);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(req))
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isCreated());

        verify(adminService).exportToFile();
    }

    @Test
    void getUserById_success() throws Exception {
        User u = new User();
        u.setUsername("testuser");
        when(userService.findById(1L)).thenReturn(u);

        mockMvc.perform(get("/api/users/1")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void updateUser_success() throws Exception {
        org.friesoft.porturl.dto.User req = new org.friesoft.porturl.dto.User();
        req.setUsername("updated");

        User updatedUser = new User();
        updatedUser.setUsername("updated");

        when(userService.update(eq(1L), any())).thenReturn(updatedUser);

        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(req))
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());

        verify(adminService).exportToFile();
    }

    @Test
    void deleteUser_success() throws Exception {
        when(userService.getUserRoles(1L)).thenReturn(List.of("ROLE_USER"));

        mockMvc.perform(delete("/api/users/1")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());

        verify(userService).delete(1L);
        verify(adminService).exportToFile();
    }

    @Test
    void deleteUser_admin_forbidden() throws Exception {
        when(userService.getUserRoles(1L)).thenReturn(List.of("ROLE_ADMIN"));

        mockMvc.perform(delete("/api/users/1")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserRoles_success() throws Exception {
        when(userService.getUserRoles(1L)).thenReturn(List.of("ROLE_USER"));

        mockMvc.perform(get("/api/users/1/roles")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }
}
