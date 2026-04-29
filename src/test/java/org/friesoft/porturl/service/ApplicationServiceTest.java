package org.friesoft.porturl.service;

import jakarta.persistence.EntityManager;
import jakarta.ws.rs.NotFoundException;
import org.friesoft.porturl.config.PorturlProperties;
import org.friesoft.porturl.dto.ApplicationCreateRequest;
import org.friesoft.porturl.dto.ApplicationWithRolesDto;
import org.friesoft.porturl.entities.Application;
import org.friesoft.porturl.entities.Category;
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
import org.springframework.data.domain.PageRequest;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        Page<ApplicationWithRolesDto> result = applicationService.getApplicationsForCurrentUser(PageRequest.of(0, 10),
                null);

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
        Page<ApplicationWithRolesDto> result = applicationService.getApplicationsForCurrentUser(PageRequest.of(0, 10),
                null);

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

    @Test
    void updateApplication_succeeds() {
        Application app = new Application();
        app.setId(1L);
        app.setName("Old Name");

        Application savedApp = new Application();
        savedApp.setId(1L);
        savedApp.setName("New Name");

        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(app))
                .thenReturn(Optional.of(savedApp));

        org.friesoft.porturl.dto.ApplicationUpdateRequest request = new org.friesoft.porturl.dto.ApplicationUpdateRequest();
        request.setName("New Name");

        when(applicationRepository.save(any())).thenReturn(app);

        Application result = applicationService.updateApplication(1L, request);

        assertEquals("New Name", result.getName());
        verify(applicationRepository).save(app);
    }

    @Test
    void deleteApplication_succeeds() {
        Application app = new Application();
        app.setId(1L);
        app.setName("App to Delete");
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));

        applicationService.deleteApplication(1L);

        verify(applicationRepository).delete(app);
    }

    @Test
    void getRolesForApplication_succeeds() {
        Application app = new Application();
        app.setId(1L);
        app.setClientId("client-123");
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));

        ClientsResource clientsResource = mock(ClientsResource.class);
        lenient().when(realmResource.clients()).thenReturn(clientsResource);
        org.keycloak.representations.idm.ClientRepresentation clientRep = new org.keycloak.representations.idm.ClientRepresentation();
        clientRep.setId("client-uuid");
        lenient().when(clientsResource.findByClientId("client-123")).thenReturn(List.of(clientRep));

        ClientResource clientResource = mock(ClientResource.class, RETURNS_DEEP_STUBS);
        lenient().when(clientsResource.get("client-uuid")).thenReturn(clientResource);

        RoleRepresentation role1 = new RoleRepresentation("admin", "", false);
        lenient().when(clientResource.roles().list()).thenReturn(List.of(role1));

        List<String> roles = applicationService.getRolesForApplication(1L);

        assertEquals(1, roles.size());
        assertEquals("admin", roles.get(0));
    }

    @Test
    void removeRoleFromUser_succeeds() {
        Application app = new Application();
        app.setId(1L);
        app.setClientId("test-client");
        User user = new User();
        user.setId(1L);
        user.setProviderUserId("test-user-id");
        user.setUsername("testuser");

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

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

        applicationService.removeRoleFromUser(1L, 1L, "admin");

        verify(roleScopeResource, times(1)).remove(List.of(mockRole));
    }

    @Test
    void enforceApplicationSortOrder_alphabetical() {
        org.friesoft.porturl.entities.Category cat = new org.friesoft.porturl.entities.Category();
        cat.setApplicationSortMode(org.friesoft.porturl.entities.Category.SortMode.ALPHABETICAL);

        Application a1 = new Application();
        a1.setName("Z");
        Application a2 = new Application();
        a2.setName("A");

        cat.setApplications(new java.util.ArrayList<>(List.of(a1, a2)));

        applicationService.enforceApplicationSortOrder(cat);

        assertEquals("A", cat.getApplications().get(0).getName());
        assertEquals("Z", cat.getApplications().get(1).getName());
    }

    @Test
    void updateApplication_withCategories_succeeds() {
        Application app = new Application();
        app.setId(1L);
        app.setName("Old Name");
        Category oldCat = new Category(); oldCat.setId(10L);
        app.setCategories(new java.util.ArrayList<Category>(List.of(oldCat)));
        oldCat.setApplications(new java.util.ArrayList<Application>(List.of(app)));
        
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any())).thenReturn(app);
        
        Category newCat = new Category(); newCat.setId(20L);
        newCat.setApplications(new java.util.ArrayList<Application>());
        when(categoryRepository.findById(20L)).thenReturn(Optional.of(newCat));
        
        org.friesoft.porturl.dto.ApplicationUpdateRequest request = new org.friesoft.porturl.dto.ApplicationUpdateRequest();
        request.setName("New Name");
        org.friesoft.porturl.dto.Category dtoCat = new org.friesoft.porturl.dto.Category();
        dtoCat.setId(20L);
        request.setCategories(List.of(dtoCat));
        
        applicationService.updateApplication(1L, request);
        
        assertTrue(app.getCategories().contains(newCat));
        assertFalse(app.getCategories().contains(oldCat));
        assertFalse(oldCat.getApplications().contains(app));
        assertTrue(newCat.getApplications().contains(app));
    }

    @Test
    void deleteApplication_withCategories_succeeds() {
        Application app = new Application();
        app.setId(1L);
        Category cat = new Category(); cat.setId(10L);
        app.setCategories(new java.util.ArrayList<Category>(List.of(cat)));
        cat.setApplications(new java.util.ArrayList<Application>(List.of(app)));
        
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
        
        applicationService.deleteApplication(1L);
        
        verify(applicationRepository).delete(app);
        assertFalse(cat.getApplications().contains(app));
    }

    @Test
    void moveApplication_succeeds() {
        Application app = new Application(); app.setId(1L);
        Category fromCat = new Category(); fromCat.setId(10L);
        Category toCat = new Category(); toCat.setId(20L);
        
        fromCat.setApplications(new java.util.ArrayList<Application>(List.of(app)));
        toCat.setApplications(new java.util.ArrayList<Application>());
        app.setCategories(new java.util.ArrayList<Category>(List.of(fromCat)));
        
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(fromCat));
        when(categoryRepository.findById(20L)).thenReturn(Optional.of(toCat));
        
        org.friesoft.porturl.dto.MoveApplicationRequest req = new org.friesoft.porturl.dto.MoveApplicationRequest();
        req.setFromCategoryId(10L);
        req.setToCategoryId(20L);
        req.setTargetIndex(0);
        
        applicationService.moveApplication(1L, req);
        
        assertFalse(fromCat.getApplications().contains(app));
        assertTrue(toCat.getApplications().contains(app));
        assertTrue(app.getCategories().contains(toCat));
        verify(categoryRepository).save(fromCat);
        verify(categoryRepository).save(toCat);
        verify(applicationRepository).save(app);
    }

    @Test
    void assignRoleToUser_realmRole_succeeds() {
        Application app = new Application(); app.setId(1L);
        User user = new User(); user.setId(1L); user.setProviderUserId("prov-id");
        
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        
        RolesResource rolesResource = mock(RolesResource.class);
        RoleResource roleResource = mock(RoleResource.class);
        RoleRepresentation roleRep = new RoleRepresentation();
        
        lenient().when(realmResource.roles()).thenReturn(rolesResource);
        lenient().when(rolesResource.get("ROLE_TEST")).thenReturn(roleResource);
        lenient().when(roleResource.toRepresentation()).thenReturn(roleRep);
        
        UserRepresentation kcUser = new UserRepresentation();
        kcUser.setUsername("testuser");
        lenient().when(usersResource.get("prov-id")).thenReturn(userResource);
        lenient().when(userResource.roles()).thenReturn(roleMappingResource);
        
        org.keycloak.admin.client.resource.RoleScopeResource realmRoleScope = mock(org.keycloak.admin.client.resource.RoleScopeResource.class);
        lenient().when(roleMappingResource.realmLevel()).thenReturn(realmRoleScope);
        
        applicationService.assignRoleToUser(1L, 1L, "ROLE_TEST");
        
        verify(realmRoleScope).add(List.of(roleRep));
    }
}