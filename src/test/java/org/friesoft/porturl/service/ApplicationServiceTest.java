package org.friesoft.porturl.service;

import jakarta.persistence.EntityManager;
import jakarta.ws.rs.NotFoundException;
import org.friesoft.porturl.config.PorturlProperties;
import org.friesoft.porturl.dto.ApplicationCreateRequest;
import org.friesoft.porturl.dto.ApplicationWithRolesDto;
import org.friesoft.porturl.entities.Application;
import org.friesoft.porturl.entities.User;
import org.friesoft.porturl.repositories.ApplicationRepository;
import org.friesoft.porturl.repositories.CategoryRepository;
import org.friesoft.porturl.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private Keycloak keycloakAdminClient;

    @Mock
    private EntityManager entityManager;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PorturlProperties properties;

    // Mocks for the Keycloak client fluent API chain
    @Mock
    private RealmsResource realmsResource;
    @Mock
    private RealmResource realmResource;
    @Mock
    private RolesResource rolesResource;
    @Mock
    private RoleResource roleResource;
    @Mock
    private UsersResource usersResource;
    @Mock
    private UserResource userResource;
    @Mock
    private RoleMappingResource roleMappingResource;
    @Mock
    private RoleScopeResource roleScopeResource;

    @InjectMocks
    private ApplicationService applicationService;

    @BeforeEach
    void setUp() {
        lenient().when(properties.getKeycloak().getAdmin().getRealm()).thenReturn("test-realm");
        lenient().when(properties.getKeycloak().getRealm()).thenReturn("test-realm");

        lenient().when(keycloakAdminClient.realm("test-realm")).thenReturn(realmResource);
        lenient().when(realmResource.roles()).thenReturn(rolesResource);
        lenient().when(rolesResource.list(anyInt(), anyInt())).thenReturn(Collections.emptyList());
        lenient().when(realmResource.users()).thenReturn(usersResource);
        lenient().when(usersResource.get(anyString())).thenReturn(userResource);
        lenient().when(userResource.roles()).thenReturn(roleMappingResource);
        lenient().when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);
        lenient().when(rolesResource.get(anyString())).thenReturn(roleResource);
        
        // Mock findByClientId for getClientUuid
        ClientsResource clientsResource = mock(ClientsResource.class);
        lenient().when(realmResource.clients()).thenReturn(clientsResource);
        org.keycloak.representations.idm.ClientRepresentation clientRep = new org.keycloak.representations.idm.ClientRepresentation();
        clientRep.setId("test-client-uuid");
        clientRep.setClientId("test-client-id");
        lenient().when(clientsResource.findByClientId(anyString())).thenReturn(List.of(clientRep));
        ClientResource clientResource = mock(ClientResource.class, RETURNS_DEEP_STUBS);
        lenient().when(clientsResource.get(anyString())).thenReturn(clientResource);
    }

    private void mockSecurityContext(Set<SimpleGrantedAuthority> authorities) {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void getApplicationsForCurrentUser_asAdmin_returnsAllWithRoles() {
        // Arrange
        mockSecurityContext(Set.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        Application app1 = new Application();
        app1.setName("Grafana");
        app1.setId(1L);
        app1.setClientId("grafana-client");
        lenient().when(applicationRepository.findAll()).thenReturn(List.of(app1));
        lenient().when(applicationRepository.findById(1L)).thenReturn(Optional.of(app1));

        // Mock client resource chain
        ClientResource clientResource = mock(ClientResource.class, RETURNS_DEEP_STUBS);
        ClientsResource clientsResource = mock(ClientsResource.class);
        when(realmResource.clients()).thenReturn(clientsResource);
        org.keycloak.representations.idm.ClientRepresentation clientRep = new org.keycloak.representations.idm.ClientRepresentation();
        clientRep.setId("grafana-uuid");
        clientRep.setClientId("grafana-client");
        when(clientsResource.findByClientId("grafana-client")).thenReturn(List.of(clientRep));
        when(clientsResource.get("grafana-uuid")).thenReturn(clientResource);

        RoleRepresentation role1 = new RoleRepresentation("ROLE_GRAFANA_ADMIN", "", false);
        RoleRepresentation role2 = new RoleRepresentation("ROLE_GRAFANA_VIEWER", "", false);
        lenient().when(clientResource.roles().list()).thenReturn(List.of(role1, role2));

        // Act
        Page<ApplicationWithRolesDto> result = applicationService.getApplicationsForCurrentUser(PageRequest.of(0, 10), null);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals("Grafana", result.getContent().get(0).getApplication().getName());
        assertEquals(2, result.getContent().get(0).getAvailableRoles().size());
        assertEquals("ROLE_GRAFANA_ADMIN", result.getContent().get(0).getAvailableRoles().get(0));
    }

    @Test
    void getApplicationsForCurrentUser_asUser_returnsOnlyVisible() {
        // Arrange
        mockSecurityContext(Set.of(new SimpleGrantedAuthority("APP_GRAFANA_ACCESS")));
        Application app1 = new Application();
        app1.setName("Grafana");
        app1.setId(1L);
        Application app2 = new Application();
        app2.setName("Other App");
        app2.setId(2L);
        lenient().when(applicationRepository.findAll()).thenReturn(List.of(app1, app2));
        lenient().when(applicationRepository.findById(1L)).thenReturn(Optional.of(app1));
        lenient().when(applicationRepository.findById(2L)).thenReturn(Optional.of(app2));

        // Act
        Page<ApplicationWithRolesDto> result = applicationService.getApplicationsForCurrentUser(PageRequest.of(0, 10), null);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals("Grafana", result.getContent().get(0).getApplication().getName());
        assertEquals(0, result.getContent().get(0).getAvailableRoles().size()); // Users don't see available roles
    }
    
    @Test
    void createApplication_succeeds() {
        // Arrange
        ApplicationCreateRequest request = new ApplicationCreateRequest();
        request.setName("New App");
        request.setUrl("http://new.app");
        request.setClientId("new-app-client");
        request.setRoles(List.of("admin", "viewer"));
        request.setCategories(Collections.emptyList());

        Jwt jwt = mock(Jwt.class);
        lenient().when(jwt.getSubject()).thenReturn("user-provider-id");

        Application appToSave = new Application();
        appToSave.setName(request.getName());
        appToSave.setUrl(request.getUrl());
        appToSave.setClientId(request.getClientId());
        appToSave.setId(100L);
        when(applicationRepository.save(any(Application.class))).thenReturn(appToSave);

        // Mock client resource chain
        ClientResource clientResource = mock(ClientResource.class, RETURNS_DEEP_STUBS);
        ClientsResource clientsResource = mock(ClientsResource.class);
        lenient().when(realmResource.clients()).thenReturn(clientsResource);
        org.keycloak.representations.idm.ClientRepresentation clientRep = new org.keycloak.representations.idm.ClientRepresentation();
        clientRep.setId("new-app-uuid");
        clientRep.setClientId("new-app-client");
        lenient().when(clientsResource.findByClientId("new-app-client")).thenReturn(List.of(clientRep));
        lenient().when(clientsResource.get("new-app-uuid")).thenReturn(clientResource);
        
        RolesResource clientRolesResource = mock(RolesResource.class);
        lenient().when(clientResource.roles()).thenReturn(clientRolesResource);

        // createAccessRole mocks
        lenient().when(rolesResource.get(anyString())).thenThrow(new NotFoundException());
        
        // Act
        Application result = applicationService.createApplication(request, jwt);

        // Assert
        assertNotNull(result);
        assertEquals("New App", result.getName());
        verify(applicationRepository, times(1)).save(any(Application.class));
        
        // 1 for access role
        verify(rolesResource, times(1)).create(any(RoleRepresentation.class));
        // 2 for admin/viewer
        verify(clientRolesResource, times(2)).create(any(RoleRepresentation.class));
    }

    @Test
    void assignRoleToUser_succeeds() {
        // Arrange
        Application app = new Application();
        app.setName("Test App");
        app.setClientId("test-client");
        User user = new User();
        user.setProviderUserId("test-user-id");
        user.setUsername("testuser");
        
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Keycloak mocks for assignRoleToUser (Client Role path)
        ClientsResource clientsResource = mock(ClientsResource.class);
        lenient().when(realmResource.clients()).thenReturn(clientsResource);
        org.keycloak.representations.idm.ClientRepresentation clientRep = new org.keycloak.representations.idm.ClientRepresentation();
        clientRep.setId("test-client-uuid");
        clientRep.setClientId("test-client");
        lenient().when(clientsResource.findByClientId("test-client")).thenReturn(List.of(clientRep));
        
        ClientResource clientResource = mock(ClientResource.class, RETURNS_DEEP_STUBS);
        lenient().when(clientsResource.get("test-client-uuid")).thenReturn(clientResource);
        lenient().when(clientResource.toRepresentation()).thenReturn(clientRep);

        RoleRepresentation mockRole = mock(RoleRepresentation.class);
        lenient().when(mockRole.getName()).thenReturn("admin");
        
        RolesResource clientRolesResource = mock(RolesResource.class);
        lenient().when(clientResource.roles()).thenReturn(clientRolesResource);
        RoleResource mockRoleResource = mock(RoleResource.class);
        lenient().when(clientRolesResource.get("admin")).thenReturn(mockRoleResource);
        lenient().when(mockRoleResource.toRepresentation()).thenReturn(mockRole);

        UserRepresentation kcUser = new UserRepresentation();
        kcUser.setId("test-user-id");
        kcUser.setUsername("testuser");
        
        lenient().when(usersResource.get("test-user-id")).thenReturn(userResource);
        lenient().when(userResource.toRepresentation()).thenReturn(kcUser);
        lenient().when(usersResource.search("testuser")).thenReturn(List.of(kcUser));
        
        lenient().when(userResource.roles()).thenReturn(roleMappingResource);
        lenient().when(roleMappingResource.clientLevel("test-client-uuid")).thenReturn(roleScopeResource);

        // Act
        applicationService.assignRoleToUser(1L, 1L, "admin");

        // Assert
        verify(roleScopeResource, times(1)).add(List.of(mockRole));
    }
}