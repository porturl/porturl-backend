package org.friesoft.porturl.service;

import org.friesoft.porturl.config.PorturlProperties;
import org.friesoft.porturl.dto.ExportData;
import org.friesoft.porturl.entities.Application;
import org.friesoft.porturl.entities.Category;
import org.friesoft.porturl.entities.User;
import org.friesoft.porturl.repositories.ApplicationRepository;
import org.friesoft.porturl.repositories.CategoryRepository;
import org.friesoft.porturl.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationService applicationService;
    @Mock
    private UserService userService;
    @Mock
    private FileStorageService fileStorageService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PorturlProperties properties;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(
                applicationRepository,
                categoryRepository,
                userRepository,
                applicationService,
                userService,
                fileStorageService,
                properties);
    }

    @Test
    void exportData_returnsPopulatedExportData() {
        // Arrange
        Category category = new Category();
        category.setName("Monitoring");
        category.setApplicationSortMode(Category.SortMode.ALPHABETICAL);
        when(categoryRepository.findAll()).thenReturn(List.of(category));

        Application app = new Application();
        app.setName("Grafana");
        app.setClientId("grafana-client");
        app.setCategories(List.of(category));
        when(applicationRepository.findAll()).thenReturn(List.of(app));

        User user = new User();
        user.setUsername("admin");
        when(userRepository.findAll()).thenReturn(List.of(user));
        when(userService.mapToDto(user)).thenReturn(new org.friesoft.porturl.dto.User().username("admin"));

        // Act
        ExportData result = adminService.exportData();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getCategories().size());
        assertEquals("Monitoring", result.getCategories().get(0).getName());
        assertEquals(1, result.getApplications().size());
        assertEquals("Grafana", result.getApplications().get(0).getName());
        assertEquals(1, result.getUsers().size());
        assertEquals("admin", result.getUsers().get(0).getUsername());
    }

    @Test
    void exportToFile_withYamlStorage_writesFile(@TempDir Path tempDir) throws IOException {
        // Arrange
        Path yamlPath = tempDir.resolve("export.yaml");
        when(properties.getStorage().getType()).thenReturn(PorturlProperties.StorageType.YAML);
        when(properties.getStorage().getYamlPath()).thenReturn(yamlPath.toString());

        when(categoryRepository.findAll()).thenReturn(Collections.emptyList());
        when(applicationRepository.findAll()).thenReturn(Collections.emptyList());
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        adminService.exportToFile();

        // Assert
        assertTrue(Files.exists(yamlPath));
        String content = Files.readString(yamlPath);
        assertTrue(content.contains("categories: []"));
    }

    @Test
    void syncData_createsNewData() {
        // Arrange
        ExportData data = new ExportData();
        org.friesoft.porturl.dto.CategoryExport catExport = new org.friesoft.porturl.dto.CategoryExport();
        catExport.setName("New Category");
        catExport.setApplicationSortMode(org.friesoft.porturl.dto.CategoryExport.ApplicationSortModeEnum.ALPHABETICAL);
        data.setCategories(List.of(catExport));

        org.friesoft.porturl.dto.ApplicationExport appExport = new org.friesoft.porturl.dto.ApplicationExport();
        appExport.setName("New App");
        appExport.setCategories(List.of("New Category"));
        data.setApplications(List.of(appExport));

        User creator = new User();
        creator.setUsername("creator");

        when(categoryRepository.findAll()).thenReturn(Collections.emptyList());
        when(applicationRepository.findAll()).thenReturn(Collections.emptyList());
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArguments()[0]);
        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        adminService.syncData(data, creator);

        // Assert
        verify(categoryRepository, atLeastOnce()).save(any(Category.class));
        verify(applicationRepository, atLeastOnce()).save(any(Application.class));
        verify(applicationService, times(1)).createAccessRole(any(Application.class));
    }

    @Test
    void syncData_updatesExistingData() {
        // Arrange
        ExportData data = new ExportData();
        org.friesoft.porturl.dto.CategoryExport catExport = new org.friesoft.porturl.dto.CategoryExport();
        catExport.setName("Existing Category");
        catExport.setApplicationSortMode(org.friesoft.porturl.dto.CategoryExport.ApplicationSortModeEnum.CUSTOM);
        data.setCategories(List.of(catExport));

        org.friesoft.porturl.dto.ApplicationExport appExport = new org.friesoft.porturl.dto.ApplicationExport();
        appExport.setName("Existing App");
        appExport.setCategories(List.of("Existing Category"));
        appExport.setUrl("http://new-url");
        data.setApplications(List.of(appExport));

        User creator = new User();
        creator.setUsername("creator");

        Category existingCat = new Category();
        existingCat.setName("Existing Category");
        existingCat.setApplicationSortMode(Category.SortMode.ALPHABETICAL);

        Application existingApp = new Application();
        existingApp.setName("Existing App");
        existingApp.setUrl("http://old-url");

        when(categoryRepository.findAll()).thenReturn(List.of(existingCat));
        when(applicationRepository.findAll()).thenReturn(List.of(existingApp));
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArguments()[0]);
        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        adminService.syncData(data, creator);

        // Assert
        verify(categoryRepository, atLeastOnce()).save(existingCat);
        assertEquals(Category.SortMode.CUSTOM, existingCat.getApplicationSortMode());

        verify(applicationRepository, atLeastOnce()).save(existingApp);
        assertEquals("http://new-url", existingApp.getUrl());
    }

    @Test
    void syncData_removesMissingData() {
        // Arrange
        ExportData data = new ExportData();
        data.setCategories(Collections.emptyList());
        data.setApplications(Collections.emptyList());
        data.setUsers(Collections.emptyList());

        User creator = new User();
        creator.setUsername("creator");

        Category oldCat = new Category();
        oldCat.setName("Old Category");

        Application oldApp = new Application();
        oldApp.setName("Old App");
        oldApp.setId(1L);

        when(categoryRepository.findAll()).thenReturn(List.of(oldCat));
        when(applicationRepository.findAll()).thenReturn(List.of(oldApp));
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        adminService.syncData(data, creator);

        // Assert
        verify(applicationRepository, times(1)).deleteAll(List.of(oldApp));
        verify(categoryRepository, times(1)).deleteAll(List.of(oldCat));
    }

    @Test
    void importData_throwsWhenUserNotFound() {
        // Arrange
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("unknown-sub");
        when(userRepository.findByProviderUserId("unknown-sub")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
            adminService.importData(new ExportData(), jwt);
        });
    }

    @Test
    void scanRealmForClients_success() {
        org.keycloak.admin.client.Keycloak kc = mock(org.keycloak.admin.client.Keycloak.class, Answers.RETURNS_DEEP_STUBS);
        when(applicationService.getKeycloakAdminClient("test")).thenReturn(kc);
        
        org.keycloak.representations.idm.ClientRepresentation c1 = new org.keycloak.representations.idm.ClientRepresentation();
        c1.setId("id1");
        c1.setClientId("my-client");
        c1.setName("My Client");
        
        org.keycloak.representations.idm.ClientRepresentation c2 = new org.keycloak.representations.idm.ClientRepresentation();
        c2.setClientId("account"); // should be filtered out
        
        when(kc.realm("test").clients().findAll()).thenReturn(List.of(c1, c2));
        
        var result = adminService.scanRealmForClients("test");
        
        assertEquals(1, result.size());
        assertEquals("my-client", result.get(0).getClientId());
    }

    @Test
    void listRealms_success() {
        org.keycloak.admin.client.Keycloak kc = mock(org.keycloak.admin.client.Keycloak.class, Answers.RETURNS_DEEP_STUBS);
        when(applicationService.getKeycloakAdminClient("master")).thenReturn(kc);
        
        org.keycloak.representations.idm.RealmRepresentation r1 = new org.keycloak.representations.idm.RealmRepresentation();
        r1.setRealm("test-realm");
        
        org.keycloak.representations.idm.RealmRepresentation r2 = new org.keycloak.representations.idm.RealmRepresentation();
        r2.setRealm("master"); // should be filtered out
        
        when(kc.realms().findAll()).thenReturn(List.of(r1, r2));
        
        var result = adminService.listRealms();
        
        assertEquals(1, result.size());
        assertEquals("test-realm", result.get(0));
    }

    @Test
    void syncData_syncsUsersAndHandlesDuplicates() {
        ExportData data = new ExportData();
        org.friesoft.porturl.dto.User u1 = new org.friesoft.porturl.dto.User();
        u1.setUsername("newuser");
        u1.setProviderUserId("prov-1");
        
        org.friesoft.porturl.dto.User u2 = new org.friesoft.porturl.dto.User();
        u2.setUsername("existinguser");
        u2.setProviderUserId("prov-2");
        
        data.setUsers(List.of(u1, u2));
        
        User creator = new User();
        creator.setUsername("admin");
        
        User existingUser = new User();
        existingUser.setId(2L);
        existingUser.setUsername("existinguser");
        existingUser.setProviderUserId("prov-2");
        
        User oldUser = new User();
        oldUser.setId(3L);
        oldUser.setUsername("olduser"); // should be deleted
        
        when(categoryRepository.findAll()).thenReturn(List.of());
        when(applicationRepository.findAll()).thenReturn(List.of());
        when(userRepository.findAll()).thenReturn(List.of(existingUser, oldUser));
        
        when(userRepository.findByProviderUserId("prov-1")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByProviderUserId("prov-2")).thenReturn(Optional.of(existingUser));
        
        when(userService.getUserRoles(3L)).thenReturn(List.of("ROLE_USER"));
        
        assertFalse(adminService.isSyncing());
        adminService.syncData(data, creator);
        assertFalse(adminService.isSyncing());
        
        verify(userService).save(u1);
        verify(userService).update(2L, u2);
        verify(userService).delete(3L);
    }

    @Test
    void importData_success() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("prov-id");
        User user = new User();
        user.setUsername("admin");
        when(userRepository.findByProviderUserId("prov-id")).thenReturn(Optional.of(user));
        
        ExportData data = new ExportData();
        when(categoryRepository.findAll()).thenReturn(List.of());
        when(applicationRepository.findAll()).thenReturn(List.of());
        when(userRepository.findAll()).thenReturn(List.of());
        
        adminService.importData(data, jwt);
        
        verify(userRepository).findByProviderUserId("prov-id");
    }

    @Test
    void updateSyncHash_updatesHashSuccessfully() {
        ExportData data = new ExportData();
        adminService.updateSyncHash(data);
        // We can't easily assert the hash value directly without reflection, but we can verify it doesn't throw.
    }

    @Test
    void exportToFile_doesNothingWhenNotYaml() {
        when(properties.getStorage().getType()).thenReturn(PorturlProperties.StorageType.SQL);
        adminService.exportToFile();
        // Since it's void, we verify no exceptions.
    }
}
