package org.friesoft.porturl.security;

import tools.jackson.databind.json.JsonMapper;
import org.friesoft.porturl.entities.User;
import org.friesoft.porturl.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomGrantedAuthoritiesExtractorTest {

    @Mock
    private UserRepository userRepository;

    private JsonMapper jsonMapper;

    private CustomGrantedAuthoritiesExtractor extractor;

    @Mock
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        jsonMapper = new JsonMapper();
        extractor = new CustomGrantedAuthoritiesExtractor(userRepository, jsonMapper);
    }

    @Test
    void testExtractAuthorities_UserExists() {
        when(jwt.getSubject()).thenReturn("sub-123");
        when(jwt.getClaimAsString("preferred_username")).thenReturn("testuser");
        when(jwt.getClaimAsString("email")).thenReturn("test@example.com");

        User existingUser = new User();
        existingUser.setUsername("testuser");
        existingUser.setProviderUserId("sub-123");

        when(userRepository.findByProviderUserId("sub-123")).thenReturn(Optional.of(existingUser));

        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", List.of("admin", "APP_SOME_ACCESS"));
        when(jwt.getClaimAsMap("realm_access")).thenReturn(realmAccess);

        Map<String, Object> resourceAccess = new HashMap<>();
        Map<String, Object> clientAccess = new HashMap<>();
        clientAccess.put("roles", List.of("editor"));
        resourceAccess.put("my-client", clientAccess);
        when(jwt.getClaimAsMap("resource_access")).thenReturn(resourceAccess);

        Collection<GrantedAuthority> authorities = extractor.extractAuthorities(jwt);

        assertNotNull(authorities);
        assertEquals(3, authorities.size());

        List<String> authorityStrings = authorities.stream().map(GrantedAuthority::getAuthority).toList();
        assertTrue(authorityStrings.contains("ROLE_ADMIN"));
        assertTrue(authorityStrings.contains("APP_SOME_ACCESS"));
        assertTrue(authorityStrings.contains("APP_my-client_editor"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void testExtractAuthorities_UserDoesNotExist_CreatesUser() {
        when(jwt.getSubject()).thenReturn("sub-new");
        when(jwt.getClaimAsString("preferred_username")).thenReturn(null);
        when(jwt.getClaimAsString("email")).thenReturn(null);

        when(userRepository.findByProviderUserId("sub-new")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("sub-new")).thenReturn(Optional.empty());

        when(jwt.getClaimAsMap("realm_access")).thenReturn(null);
        when(jwt.getClaimAsMap("resource_access")).thenReturn(null);

        extractor.extractAuthorities(jwt);

        verify(userRepository, times(1)).save(any(User.class));
    }
}
