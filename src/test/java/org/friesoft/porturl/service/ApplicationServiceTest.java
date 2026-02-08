package org.friesoft.porturl.service;

import jakarta.persistence.EntityManager;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

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
        ReflectionTestUtils.setField(applicationService, "realm", "test-realm");

        lenient().when(keycloakAdminClient.realm("test-realm")).thenReturn(realmResource);
        lenient().when(realmResource.roles()).thenReturn(rolesResource);
        lenient().when(realmResource.users()).thenReturn(usersResource);
        lenient().when(usersResource.get(anyString())).thenReturn(userResource);
        lenient().when(userResource.roles()).thenReturn(roleMappingResource);
        lenient().when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);
        lenient().when(rolesResource.get(anyString())).thenReturn(roleResource);
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
        when(applicationRepository.findAll()).thenReturn(List.of(app1));

        RoleRepresentation role1 = new RoleRepresentation("ROLE_GRAFANA_ADMIN", "", false);
        RoleRepresentation role2 = new RoleRepresentation("ROLE_GRAFANA_VIEWER", "", false);
        when(rolesResource.list()).thenReturn(List.of(role1, role2));

        // Act
        List<ApplicationWithRolesDto> result = applicationService.getApplicationsForCurrentUser();

        // Assert
        assertEquals(1, result.size());
        assertEquals("Grafana", result.get(0).getApplication().getName());
        assertEquals(2, result.get(0).getAvailableRoles().size());
        assertEquals("admin", result.get(0).getAvailableRoles().get(0));
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
        when(applicationRepository.findAll()).thenReturn(List.of(app1, app2));

        // Act
        List<ApplicationWithRolesDto> result = applicationService.getApplicationsForCurrentUser();

        // Assert
        assertEquals(1, result.size());
        assertEquals("Grafana", result.get(0).getApplication().getName());
        assertEquals(0, result.get(0).getAvailableRoles().size()); // Users don't see available roles
    }
    
    @Test
    void createApplication_succeeds() {
        // Arrange
        ApplicationCreateRequest request = new ApplicationCreateRequest();
        request.setName("New App");
        request.setUrl("http://new.app");
        request.setRoles(List.of("admin", "viewer"));
        request.setCategories(Collections.emptyList()); // Assume no categories for simplicity

        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("user-provider-id");

        User creator = new User();
        creator.setId(1L);
        creator.setProviderUserId("user-provider-id");
        when(userRepository.findByProviderUserId("user-provider-id")).thenReturn(Optional.of(creator));
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));

        Application appToSave = new Application();
        appToSave.setName(request.getName());
        appToSave.setUrl(request.getUrl());
        appToSave.setId(100L);
        when(applicationRepository.save(any(Application.class))).thenReturn(appToSave);
        when(applicationRepository.findById(any())).thenReturn(Optional.of(appToSave));

        when(rolesResource.list()).thenReturn(Collections.emptyList());
        when(roleResource.toRepresentation()).thenReturn(new RoleRepresentation());

        // Act
        Application result = applicationService.createApplication(request, jwt);

        // Assert
        assertNotNull(result);
        assertEquals("New App", result.getName());
        verify(applicationRepository, times(1)).save(any(Application.class));
        verify(entityManager, times(1)).flush();
        
        verify(rolesResource, times(5)).create(any(RoleRepresentation.class));
        verify(rolesResource, times(3)).get(argThat(s -> s.startsWith("ROLE_")));
        verify(roleResource, times(2)).addComposites(any());
        verify(roleScopeResource, times(1)).add(any());
    }

    @Test
    void assignRoleToUser_succeeds() {
        // Arrange
        Application app = new Application();
        app.setName("Test App");
        User user = new User();
        user.setProviderUserId("test-user-id");
        
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        RoleRepresentation mockRole = new RoleRepresentation();
        when(roleResource.toRepresentation()).thenReturn(mockRole);

        // Act
        applicationService.assignRoleToUser(1L, 1L, "admin");

        // Assert
        verify(roleScopeResource, times(1)).add(List.of(mockRole));
    }
}