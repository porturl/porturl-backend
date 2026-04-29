package org.friesoft.porturl.service;

import org.friesoft.porturl.config.PorturlProperties;
import org.friesoft.porturl.entities.User;
import org.friesoft.porturl.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class KeycloakUserSyncServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private Keycloak keycloakAdminClient;

    @Mock
    private PorturlProperties properties;

    @Mock
    private PorturlProperties.Storage storage;

    @Mock
    private PorturlProperties.Keycloak keycloakProperties;

    @Mock
    private PorturlProperties.Keycloak.Admin adminProperties;

    @Mock
    private RealmResource realmResource;

    @Mock
    private UsersResource usersResource;

    @Mock
    private UserResource userResource;

    @Mock
    private RoleMappingResource roleMappingResource;

    private KeycloakUserSyncService service;

    @BeforeEach
    void setUp() {
        service = new KeycloakUserSyncService(userRepository, keycloakAdminClient, properties);
    }

    @Test
    void syncUsers_skipsIfNotSqlStorage() {
        when(properties.getStorage()).thenReturn(storage);
        when(storage.getType()).thenReturn(PorturlProperties.StorageType.YAML);

        service.syncUsers();

        verify(keycloakAdminClient, never()).realm(any());
        verify(userRepository, never()).findAll();
    }

    @Test
    void syncUsers_syncsUsersFromKeycloak_createNewUser() {
        setupSqlStorage();
        setupKeycloakProperties();

        UserRepresentation kcUser = new UserRepresentation();
        kcUser.setId("kc-id-1");
        kcUser.setUsername("user1");
        kcUser.setEmail("user1@test.com");

        when(keycloakAdminClient.realm("test-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.list()).thenReturn(List.of(kcUser));

        when(userRepository.findByProviderUserId("kc-id-1")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("user1")).thenReturn(Optional.empty());
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        service.syncUsers();

        verify(userRepository).save(argThat(user -> 
            "kc-id-1".equals(user.getProviderUserId()) && 
            "user1".equals(user.getUsername()) && 
            "user1@test.com".equals(user.getEmail())
        ));
    }

    @Test
    void syncUsers_syncsUsersFromKeycloak_updatesExistingUser() {
        setupSqlStorage();
        setupKeycloakProperties();

        UserRepresentation kcUser = new UserRepresentation();
        kcUser.setId("kc-id-1");
        kcUser.setUsername("user1_updated");
        kcUser.setEmail("updated@test.com");

        when(keycloakAdminClient.realm("test-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.list()).thenReturn(List.of(kcUser));

        User existingUser = new User();
        existingUser.setProviderUserId("kc-id-1");
        existingUser.setUsername("user1");
        existingUser.setEmail("old@test.com");

        when(userRepository.findByProviderUserId("kc-id-1")).thenReturn(Optional.of(existingUser));
        when(userRepository.findAll()).thenReturn(List.of(existingUser));

        service.syncUsers();

        verify(userRepository).save(argThat(user -> 
            "kc-id-1".equals(user.getProviderUserId()) && 
            "user1_updated".equals(user.getUsername()) && 
            "updated@test.com".equals(user.getEmail())
        ));
    }

    @Test
    void syncUsers_removesLocalUserIfNotFoundInKeycloak() {
        setupSqlStorage();
        setupKeycloakProperties();

        when(keycloakAdminClient.realm("test-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.list()).thenReturn(Collections.emptyList());

        User localUser = new User();
        localUser.setProviderUserId("kc-id-deleted");
        localUser.setUsername("user2");

        when(userRepository.findAll()).thenReturn(List.of(localUser));
        when(usersResource.get("kc-id-deleted")).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        
        org.keycloak.admin.client.resource.RoleScopeResource roleScopeResource = mock(org.keycloak.admin.client.resource.RoleScopeResource.class);
        when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);
        RoleRepresentation role = new RoleRepresentation();
        role.setName("user");
        when(roleScopeResource.listEffective()).thenReturn(List.of(role));

        service.syncUsers();

        verify(userRepository).delete(localUser);
    }

    @Test
    void syncUsers_handlesExceptionsGracefully() {
        setupSqlStorage();
        setupKeycloakProperties();

        when(keycloakAdminClient.realm("test-realm")).thenThrow(new RuntimeException("KC connection error"));

        service.syncUsers();

        verify(userRepository, never()).save(any());
    }

    private void setupSqlStorage() {
        when(properties.getStorage()).thenReturn(storage);
        when(storage.getType()).thenReturn(PorturlProperties.StorageType.SQL);
    }

    private void setupKeycloakProperties() {
        when(properties.getKeycloak()).thenReturn(keycloakProperties);
        when(keycloakProperties.getRealm()).thenReturn("test-realm");
    }
}
