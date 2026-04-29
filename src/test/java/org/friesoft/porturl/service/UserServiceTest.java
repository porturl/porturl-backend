package org.friesoft.porturl.service;

import org.friesoft.porturl.config.PorturlProperties;
import org.friesoft.porturl.entities.User;
import org.friesoft.porturl.repositories.ApplicationRepository;
import org.friesoft.porturl.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.security.core.GrantedAuthority;
import org.keycloak.representations.idm.RoleRepresentation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private Keycloak keycloakAdminClient;
    @Mock
    private Keycloak masterKeycloakAdminClient;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PorturlProperties properties;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository,
                applicationRepository,
                keycloakAdminClient,
                masterKeycloakAdminClient,
                properties);
        lenient().when(properties.getKeycloak().getRealm()).thenReturn("test-realm");
    }

    private void mockSecurityContext(String name) {
        Authentication auth = mock(Authentication.class);
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        when(auth.getName()).thenReturn(name);
        SecurityContextHolder.setContext(ctx);
    }

    private void mockSecurityContextWithRoles(String name, List<String> roles) {
        Authentication auth = mock(Authentication.class);
        SecurityContext ctx = mock(SecurityContext.class);
        lenient().when(ctx.getAuthentication()).thenReturn(auth);
        lenient().when(auth.getName()).thenReturn(name);

        List<GrantedAuthority> authorities = roles.stream()
                .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
                .collect(java.util.stream.Collectors.toList());

        lenient().doReturn(authorities).when(auth).getAuthorities();
        SecurityContextHolder.setContext(ctx);
    }

    @Test
    void getCurrentUser_returnsUser() {
        // Arrange
        mockSecurityContext("user-123");
        User user = new User();
        user.setProviderUserId("user-123");
        when(userRepository.findByProviderUserId("user-123")).thenReturn(Optional.of(user));

        // Act
        User result = userService.getCurrentUser();

        // Assert
        assertEquals("user-123", result.getProviderUserId());
    }

    @Test
    void getCurrentUser_throwsWhenNotAuthenticated() {
        // Arrange
        SecurityContextHolder.clearContext();

        // Act & Assert
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.getCurrentUser());
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void save_createsUserInKeycloakAndDb() {
        // Arrange
        org.friesoft.porturl.dto.User dto = new org.friesoft.porturl.dto.User()
                .username("newuser")
                .email("new@example.com");

        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        when(keycloakAdminClient.realm("test-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);

        jakarta.ws.rs.core.Response response = mock(jakarta.ws.rs.core.Response.class);
        when(response.getStatus()).thenReturn(201);
        when(response.getLocation()).thenReturn(URI.create("http://keycloak/users/uuid-123"));
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);

        org.keycloak.admin.client.resource.UserResource userResource = mock(
                org.keycloak.admin.client.resource.UserResource.class);
        when(usersResource.get("uuid-123")).thenReturn(userResource);
        UserRepresentation kcUser = new UserRepresentation();
        kcUser.setId("uuid-123");
        when(userResource.toRepresentation()).thenReturn(kcUser);

        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        User result = userService.save(dto);

        // Assert
        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        assertEquals("uuid-123", result.getProviderUserId());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void delete_removesFromKeycloakAndDb() {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setProviderUserId("uuid-123");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        org.keycloak.admin.client.resource.UserResource kcUserResource = mock(
                org.keycloak.admin.client.resource.UserResource.class);

        when(keycloakAdminClient.realm("test-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get("uuid-123")).thenReturn(kcUserResource);

        // Act
        userService.delete(1L);

        // Assert
        verify(kcUserResource).remove();
        verify(userRepository).delete(user);
    }

    @Test
    void save_handlesConflict() {
        org.friesoft.porturl.dto.User dto = new org.friesoft.porturl.dto.User()
                .username("existing");

        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        when(keycloakAdminClient.realm("test-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);

        jakarta.ws.rs.core.Response response = mock(jakarta.ws.rs.core.Response.class);
        when(response.getStatus()).thenReturn(409);
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);

        UserRepresentation kcUser = new UserRepresentation();
        kcUser.setId("kc-existing");
        when(usersResource.search("existing", true)).thenReturn(List.of(kcUser));

        when(userRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        User result = userService.save(dto);

        assertEquals("kc-existing", result.getProviderUserId());
    }

    @Test
    void save_throwsWhenKeycloakCreateFails() {
        org.friesoft.porturl.dto.User dto = new org.friesoft.porturl.dto.User();
        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        when(keycloakAdminClient.realm("test-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);

        jakarta.ws.rs.core.Response response = mock(jakarta.ws.rs.core.Response.class);
        when(response.getStatus()).thenReturn(500);
        when(response.readEntity(String.class)).thenReturn("Internal Server Error");
        when(usersResource.create(any())).thenReturn(response);

        assertThrows(ResponseStatusException.class, () -> userService.save(dto));
    }

    @Test
    void updateCurrentUser_updatesAndSaves() {
        mockSecurityContext("user-123");
        User user = new User();
        user.setProviderUserId("user-123");
        when(userRepository.findByProviderUserId("user-123")).thenReturn(Optional.of(user));

        org.friesoft.porturl.dto.UserUpdateRequest request = new org.friesoft.porturl.dto.UserUpdateRequest();
        request.setImage("new-image");

        when(userRepository.save(any())).thenReturn(user);

        User result = userService.updateCurrentUser(request);

        assertEquals("new-image", result.getImage());
        verify(userRepository).save(user);
    }

    @Test
    void update_updatesLocalAndKeycloak() {
        User user = new User();
        user.setId(1L);
        user.setProviderUserId("kc-123");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        org.keycloak.admin.client.resource.UserResource kcUserResource = mock(
                org.keycloak.admin.client.resource.UserResource.class);
        when(keycloakAdminClient.realm("test-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get("kc-123")).thenReturn(kcUserResource);
        when(kcUserResource.toRepresentation()).thenReturn(new UserRepresentation());

        org.friesoft.porturl.dto.User dto = new org.friesoft.porturl.dto.User();
        dto.setUsername("new-username");
        dto.setEmail("new@test.com");

        User result = userService.update(1L, dto);

        assertEquals("new-username", result.getUsername());
        verify(kcUserResource).update(any());
    }

    @Test
    void findAll_filtersAndPaginates() {
        User user1 = new User();
        user1.setId(1L);
        user1.setUsername("abc");
        user1.setEmail("test@abc.com");

        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("xyz");
        user2.setEmail("test@xyz.com");

        when(userRepository.findAll(any(Sort.class))).thenReturn(List.of(user1, user2));

        Page<org.friesoft.porturl.dto.User> result = userService.findAll(PageRequest.of(0, 10), "abc");

        assertEquals(1, result.getTotalElements());
        assertEquals("abc", result.getContent().get(0).getUsername());
    }

    @Test
    void getCurrentUserRoles_filtersInternal() {
        mockSecurityContextWithRoles("user-123", List.of("ROLE_USER", "offline_access", "default-roles-test"));

        java.util.Collection<? extends GrantedAuthority> roles = userService.getCurrentUserRoles();

        assertEquals(1, roles.size());
        assertEquals("ROLE_USER", roles.iterator().next().getAuthority());
    }

    @Test
    void getUserRoles_success() {
        User user = new User();
        user.setProviderUserId("kc-123");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        org.keycloak.admin.client.resource.UserResource kcUserResource = mock(
                org.keycloak.admin.client.resource.UserResource.class);
        org.keycloak.admin.client.resource.RoleMappingResource roleMappingResource = mock(
                org.keycloak.admin.client.resource.RoleMappingResource.class);
        org.keycloak.admin.client.resource.RoleScopeResource roleScopeResource = mock(
                org.keycloak.admin.client.resource.RoleScopeResource.class);

        when(keycloakAdminClient.realm("test-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get("kc-123")).thenReturn(kcUserResource);
        when(kcUserResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);

        RoleRepresentation r1 = new RoleRepresentation();
        r1.setName("ROLE_USER");
        RoleRepresentation r2 = new RoleRepresentation();
        r2.setName("offline_access");

        when(roleScopeResource.listEffective()).thenReturn(List.of(r1, r2));

        UserRepresentation kcUser = new UserRepresentation();
        kcUser.setUsername("testuser");
        when(kcUserResource.toRepresentation()).thenReturn(kcUser);

        when(applicationRepository.findAll()).thenReturn(List.of());

        List<String> roles = userService.getUserRoles(1L);

        assertEquals(1, roles.size());
        assertEquals("ROLE_USER", roles.get(0));
    }

    @Test
    void getUserRoles_withClientRoles_succeeds() {
        User user = new User();
        user.setId(1L);
        user.setProviderUserId("kc-123");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        org.keycloak.admin.client.resource.UserResource kcUserResource = mock(org.keycloak.admin.client.resource.UserResource.class);
        org.keycloak.admin.client.resource.RoleMappingResource roleMappingResource = mock(org.keycloak.admin.client.resource.RoleMappingResource.class);
        org.keycloak.admin.client.resource.RoleScopeResource roleScopeResource = mock(org.keycloak.admin.client.resource.RoleScopeResource.class);

        when(keycloakAdminClient.realm("test-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get("kc-123")).thenReturn(kcUserResource);
        when(kcUserResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);

        RoleRepresentation r1 = new RoleRepresentation();
        r1.setName("ROLE_USER");
        when(roleScopeResource.listEffective()).thenReturn(List.of(r1));

        UserRepresentation kcUser = new UserRepresentation();
        kcUser.setUsername("testuser");
        kcUser.setId("kc-123");
        when(kcUserResource.toRepresentation()).thenReturn(kcUser);

        org.friesoft.porturl.entities.Application app = new org.friesoft.porturl.entities.Application();
        app.setId(10L);
        app.setClientId("test-client");
        app.setRealm("test-realm");
        when(applicationRepository.findAll()).thenReturn(List.of(app));

        when(usersResource.search("testuser")).thenReturn(List.of(kcUser));
        
        org.keycloak.admin.client.resource.ClientsResource clientsResource = mock(org.keycloak.admin.client.resource.ClientsResource.class);
        when(realmResource.clients()).thenReturn(clientsResource);
        
        org.keycloak.representations.idm.ClientRepresentation clientRep = new org.keycloak.representations.idm.ClientRepresentation();
        clientRep.setId("client-uuid-123");
        when(clientsResource.findByClientId("test-client")).thenReturn(List.of(clientRep));
        
        org.keycloak.admin.client.resource.RoleScopeResource clientRoleScope = mock(org.keycloak.admin.client.resource.RoleScopeResource.class);
        when(roleMappingResource.clientLevel("client-uuid-123")).thenReturn(clientRoleScope);
        
        RoleRepresentation clientRole = new RoleRepresentation();
        clientRole.setName("editor");
        when(clientRoleScope.listAll()).thenReturn(List.of(clientRole));

        List<String> roles = userService.getUserRoles(1L);

        assertEquals(2, roles.size());
        assertTrue(roles.contains("ROLE_USER"));
        assertTrue(roles.contains("APP_10_editor"));
    }
}
