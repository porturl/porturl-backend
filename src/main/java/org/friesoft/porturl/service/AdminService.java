package org.friesoft.porturl.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.friesoft.porturl.config.PorturlProperties;
import org.friesoft.porturl.dto.ApplicationExport;
import org.friesoft.porturl.dto.CategoryExport;
import org.friesoft.porturl.dto.ExportData;
import org.friesoft.porturl.entities.Application;
import org.friesoft.porturl.entities.Category;
import org.friesoft.porturl.entities.User;
import org.friesoft.porturl.repositories.ApplicationRepository;
import org.friesoft.porturl.repositories.CategoryRepository;
import org.friesoft.porturl.repositories.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AdminService {
    private final ApplicationRepository applicationRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ApplicationService applicationService;
    private final FileStorageService fileStorageService;
    private final PorturlProperties properties;
    private final ObjectMapper yamlMapper;
    private String lastSyncedHash = "";
    private boolean isSyncing = false;

    public AdminService(ApplicationRepository applicationRepository,
                        CategoryRepository categoryRepository,
                        UserRepository userRepository,
                        ApplicationService applicationService,
                        FileStorageService fileStorageService,
                        PorturlProperties properties) {
        this.applicationRepository = applicationRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.applicationService = applicationService;
        this.fileStorageService = fileStorageService;
        this.properties = properties;
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    public boolean isSyncing() {
        return isSyncing;
    }

    public synchronized void exportToFile() {
        if (properties.getStorage().getType() != PorturlProperties.StorageType.YAML) {
            return;
        }
        try {
            ExportData data = exportData();
            String yaml = yamlMapper.writeValueAsString(data);
            
            String currentHash = Integer.toHexString(yaml.hashCode());
            if (currentHash.equals(lastSyncedHash)) {
                return;
            }
            
            Path path = Paths.get(properties.getStorage().getYamlPath());
            Files.writeString(path, yaml);
            this.lastSyncedHash = currentHash;
            log.debug("Database state exported to YAML.");
        } catch (IOException e) {
            log.error("Failed to export data to YAML file", e);
        }
    }

    public ExportData exportData() {
        ExportData exportData = new ExportData();
        exportData.setCategories(categoryRepository.findAll().stream().map(this::mapCategoryToExport).collect(Collectors.toList()));
        exportData.setApplications(applicationRepository.findAll().stream()
                .map(this::mapApplicationToExport)
                .collect(Collectors.toList()));
        return exportData;
    }

    public List<org.friesoft.porturl.dto.KeycloakClientDto> scanRealmForClients(String targetRealm) {
        // Fallback to configured realm if not specified, but for scanning we usually want explicit
        String realmToUse = (targetRealm != null && !targetRealm.isBlank()) ? targetRealm : properties.getKeycloak().getRealm();
        if (realmToUse == null || realmToUse.isBlank()) {
            realmToUse = properties.getKeycloak().getAdmin().getRealm();
        }

        try {
            return applicationService.getKeycloakAdminClient(realmToUse).realm(realmToUse).clients().findAll().stream()
                    .map(client -> {
                        org.friesoft.porturl.dto.KeycloakClientDto dto = new org.friesoft.porturl.dto.KeycloakClientDto();
                        dto.setId(client.getId());
                        dto.setClientId(client.getClientId());
                        dto.setName(client.getName());
                        return dto;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to scan clients in realm {}", realmToUse, e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not scan realm: " + e.getMessage());
        }
    }

    private CategoryExport mapCategoryToExport(Category category) {
        CategoryExport export = new CategoryExport();
        export.setName(category.getName());
        export.setSortOrder(category.getSortOrder());
        export.setDescription(category.getDescription());
        export.setApplicationSortMode(CategoryExport.ApplicationSortModeEnum.valueOf(category.getApplicationSortMode().name()));
        return export;
    }

    private ApplicationExport mapApplicationToExport(Application app) {
        ApplicationExport export = new ApplicationExport();
        export.setName(app.getName());
        export.setUrl(app.getUrl());
        export.setCategories(app.getCategories().stream().map(Category::getName).collect(Collectors.toList()));
        export.setClientId(app.getClientId());
        export.setRealm(app.getRealm());

        List<String> roles;
        if (app.getClientId() != null && !app.getClientId().isBlank()) {
             // For linked apps, roles are fetched from the client if needed.
             // But for export, we usually want to export what PortUrl considers as available roles.
             // Since we only store them in Keycloak now, we can fetch them here.
             roles = applicationService.getRolesForApplication(app.getId());
        } else {
            roles = new ArrayList<>();
        }
        export.setRoles(roles);

        if (app.getIcon() != null && !app.getIcon().isBlank()) {
            try {
                Path path = fileStorageService.load(app.getIcon());
                if (Files.exists(path)) {
                    byte[] bytes = Files.readAllBytes(path);
                    export.setIcon(Base64.getEncoder().encodeToString(bytes));
                }
            } catch (IOException e) {
                // Log error or ignore
            }
        }

        return export;
    }

    @Transactional
    public void importData(ExportData data, Jwt principal) {
        User creator = userRepository.findByProviderUserId(principal.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        syncData(data, creator);
    }

    @Transactional
    public void syncData(ExportData data, User creator) {
        log.info("Starting differential sync...");
        this.isSyncing = true;
        try {
            // --- 1. Load and Group Existing Data ---
            Map<String, List<Category>> existingCategoriesGrouped = categoryRepository.findAll().stream()
                    .collect(Collectors.groupingBy(Category::getName));
            Map<String, List<Application>> existingAppsGrouped = applicationRepository.findAll().stream()
                    .collect(Collectors.groupingBy(Application::getName));

            Map<String, Category> synchronizedCategories = new HashMap<>();
            Map<String, Application> synchronizedApps = new HashMap<>();

            // --- 2. Sync Categories (Metadata only) ---
            log.debug("Syncing categories...");
            Set<String> categoriesInYaml = new HashSet<>();
            if (data.getCategories() != null) {
                for (CategoryExport catExport : data.getCategories()) {
                    String name = catExport.getName();
                    categoriesInYaml.add(name);
                    List<Category> existing = existingCategoriesGrouped.getOrDefault(name, List.of());
                    
                    Category category;
                    if (existing.isEmpty()) {
                        category = new Category();
                    } else {
                        category = existing.get(0);
                        // Clean up duplicates
                        for (int i = 1; i < existing.size(); i++) {
                            categoryRepository.delete(existing.get(i));
                        }
                    }

                    category.setName(name);
                    category.setSortOrder(catExport.getSortOrder());
                    category.setDescription(catExport.getDescription());
                    if (catExport.getApplicationSortMode() != null) {
                        category.setApplicationSortMode(Category.SortMode.valueOf(catExport.getApplicationSortMode().name()));
                    }
                    // Clear links to be rebuilt later
                    category.getApplications().clear();
                    
                    synchronizedCategories.put(name, categoryRepository.save(category));
                }
            }

            // Remove categories not in YAML
            existingCategoriesGrouped.forEach((name, list) -> {
                if (!categoriesInYaml.contains(name)) {
                    categoryRepository.deleteAll(list);
                }
            });

            // --- 3. Sync Applications (Metadata only) ---
            log.debug("Syncing applications...");
            Set<String> appsInYaml = new HashSet<>();
            if (data.getApplications() != null) {
                for (ApplicationExport appExport : data.getApplications()) {
                    String name = appExport.getName();
                    appsInYaml.add(name);
                    List<Application> existing = existingAppsGrouped.getOrDefault(name, List.of());

                    Application app;
                    if (existing.isEmpty()) {
                        app = new Application();
                    } else {
                        app = existing.get(0);
                        // Clean up duplicates
                        for (int i = 1; i < existing.size(); i++) {
                            applicationRepository.delete(existing.get(i));
                        }
                    }

                    app.setName(name);
                    app.setUrl(appExport.getUrl());
                    app.setClientId(appExport.getClientId());
                    app.setRealm(appExport.getRealm());

                    if (app.getCreatedBy() == null) {
                        app.setCreatedBy(creator);
                    }
                    if (appExport.getIcon() != null) {
                        updateIconIfChanged(app, appExport.getIcon());
                    }
                    // Clear links to be rebuilt
                    app.getCategories().clear();
                    
                    Application savedApp = applicationRepository.save(app);
                    synchronizedApps.put(name, savedApp);

                    // Sync roles
                    applicationService.createAccessRole(savedApp);
                    if (savedApp.getClientId() != null && !savedApp.getClientId().isBlank()) {
                        applicationService.createClientRoles(savedApp, appExport.getRoles());
                    }
                }
            }

            // Remove applications not in YAML
            existingAppsGrouped.forEach((name, list) -> {
                if (!appsInYaml.contains(name)) {
                    applicationRepository.deleteAll(list);
                }
            });

            // Flush metadata changes before linking to avoid transient reference issues
            categoryRepository.flush();
            applicationRepository.flush();

            // --- 4. Rebuild Links ---
            log.debug("Linking applications and categories...");
            if (data.getApplications() != null) {
                for (ApplicationExport appExport : data.getApplications()) {
                    Application app = synchronizedApps.get(appExport.getName());
                    if (app != null && appExport.getCategories() != null) {
                        for (String catName : appExport.getCategories()) {
                            Category category = synchronizedCategories.get(catName);
                            if (category != null) {
                                // Add to both sides, though Category is the owner
                                if (!category.getApplications().contains(app)) {
                                    category.getApplications().add(app);
                                }
                                if (!app.getCategories().contains(category)) {
                                    app.getCategories().add(category);
                                }
                            }
                        }
                    }
                }
            }

            // Final save for categories (owning side)
            categoryRepository.saveAll(synchronizedCategories.values());
            categoryRepository.flush();
            applicationRepository.flush();

            log.info("Differential sync logic completed.");

            // Update hash after successful sync to avoid circular export
            try {
                this.lastSyncedHash = Integer.toHexString(yamlMapper.writeValueAsString(data).hashCode());
            } catch (Exception e) {
                log.warn("Failed to calculate sync hash", e);
            }
        } finally {
            this.isSyncing = false;
        }
    }

    private void updateIconIfChanged(Application app, String iconBase64) {
        app.setIcon(syncBase64Image(app.getIcon(), iconBase64));
    }

    private String syncBase64Image(String existingFilename, String base64) {
        if (base64 == null || base64.isBlank()) return null;
        
        // Simple optimization: if it already has an image, we could compare hashes here.
        // For now, we just save it as new to be safe, but we could improve this.
        return saveBase64Image(base64);
    }

    public void updateSyncHash(ExportData data) {
        try {
            this.lastSyncedHash = Integer.toHexString(yamlMapper.writeValueAsString(data).hashCode());
        } catch (Exception e) {
            log.warn("Failed to calculate sync hash", e);
        }
    }

    private String saveBase64Image(String base64) {
        if (base64 == null || base64.isBlank()) return null;
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            String filename = UUID.randomUUID().toString() + ".png";
            return fileStorageService.storeBytes(bytes, filename);
        } catch (Exception e) {
            return null;
        }
    }
}
