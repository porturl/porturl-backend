package org.friesoft.porturl.service;

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
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminService {
    private final ApplicationRepository applicationRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ApplicationService applicationService;
    private final FileStorageService fileStorageService;

    public AdminService(ApplicationRepository applicationRepository,
                        CategoryRepository categoryRepository,
                        UserRepository userRepository,
                        ApplicationService applicationService,
                        FileStorageService fileStorageService) {
        this.applicationRepository = applicationRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.applicationService = applicationService;
        this.fileStorageService = fileStorageService;
    }

    public ExportData exportData() {
        List<org.keycloak.representations.idm.RoleRepresentation> allRoles = applicationService.getAllRealmRoles();
        
        ExportData exportData = new ExportData();
        exportData.setCategories(categoryRepository.findAll().stream().map(this::mapCategoryToExport).collect(Collectors.toList()));
        exportData.setApplications(applicationRepository.findAll().stream()
                .map(app -> mapApplicationToExport(app, allRoles))
                .collect(Collectors.toList()));
        return exportData;
    }

    private CategoryExport mapCategoryToExport(Category category) {
        CategoryExport export = new CategoryExport();
        export.setName(category.getName());
        export.setSortOrder(category.getSortOrder());
        export.setIcon(category.getIcon());
        export.setDescription(category.getDescription());
        export.setEnabled(category.isEnabled());
        export.setApplicationSortMode(CategoryExport.ApplicationSortModeEnum.valueOf(category.getApplicationSortMode().name()));
        return export;
    }

    private ApplicationExport mapApplicationToExport(Application app, List<org.keycloak.representations.idm.RoleRepresentation> allRoles) {
        ApplicationExport export = new ApplicationExport();
        export.setName(app.getName());
        export.setUrl(app.getUrl());
        export.setCategories(app.getCategories().stream().map(Category::getName).collect(Collectors.toList()));
        
        String appPrefix = "ROLE_" + app.getName().toUpperCase().replaceAll("\\s+", "_") + "_";
        List<String> roles = allRoles.stream()
                .map(org.keycloak.representations.idm.RoleRepresentation::getName)
                .filter(name -> name.startsWith(appPrefix))
                .map(name -> name.substring(appPrefix.length()).toLowerCase())
                .collect(Collectors.toList());
        export.setRoles(roles);

        Map<String, String> images = new HashMap<>();
        addImageIfPresent(images, "large", app.getIconLarge());
        addImageIfPresent(images, "medium", app.getIconMedium());
        addImageIfPresent(images, "thumbnail", app.getIconThumbnail());
        export.setImages(images);

        return export;
    }

    private void addImageIfPresent(Map<String, String> images, String key, String filename) {
        if (filename != null && !filename.isBlank()) {
            try {
                Path path = fileStorageService.load(filename);
                if (Files.exists(path)) {
                    byte[] bytes = Files.readAllBytes(path);
                    images.put(key, Base64.getEncoder().encodeToString(bytes));
                }
            } catch (IOException e) {
                // Log error or ignore
            }
        }
    }

    @Transactional
    public void importData(ExportData data, Jwt principal) {
        User creator = userRepository.findByProviderUserId(principal.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Purge existing data
        applicationRepository.deleteAll();
        categoryRepository.deleteAll();
        categoryRepository.flush();
        applicationRepository.flush();

        // Restore Categories
        Map<String, Category> categoryMap = new HashMap<>();
        if (data.getCategories() != null) {
            for (CategoryExport catExport : data.getCategories()) {
                Category category = new Category();
                category.setName(catExport.getName());
                category.setSortOrder(catExport.getSortOrder());
                category.setIcon(catExport.getIcon());
                category.setDescription(catExport.getDescription());
                category.setEnabled(catExport.getEnabled() != null ? catExport.getEnabled() : true);
                category.setApplicationSortMode(Category.SortMode.valueOf(catExport.getApplicationSortMode().name()));
                categoryMap.put(category.getName(), categoryRepository.save(category));
            }
        }

        // Restore Applications
        if (data.getApplications() != null) {
            for (ApplicationExport appExport : data.getApplications()) {
                Application app = new Application();
                app.setName(appExport.getName());
                app.setUrl(appExport.getUrl());
                app.setCreatedBy(creator);

                // Handle images
                if (appExport.getImages() != null) {
                    app.setIconLarge(saveBase64Image(appExport.getImages().get("large")));
                    app.setIconMedium(saveBase64Image(appExport.getImages().get("medium")));
                    app.setIconThumbnail(saveBase64Image(appExport.getImages().get("thumbnail")));
                }

                Application savedApp = applicationRepository.save(app);

                if (appExport.getCategories() != null) {
                    for (String catName : appExport.getCategories()) {
                        Category category = categoryMap.get(catName);
                        if (category != null) {
                            category.getApplications().add(savedApp);
                            savedApp.getCategories().add(category);
                        }
                    }
                }
                applicationRepository.save(savedApp);

                // Create Keycloak roles
                applicationService.createApplicationRoles(savedApp, appExport.getRoles());
            }
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
