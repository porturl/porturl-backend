package org.friesoft.porturl.service;

import jakarta.persistence.EntityManager;
import jakarta.ws.rs.NotFoundException;
import org.friesoft.porturl.controller.exceptions.ApplicationNotFoundException;
import org.friesoft.porturl.entities.Application;
import org.friesoft.porturl.entities.Category;
import org.friesoft.porturl.entities.User;
import org.friesoft.porturl.repositories.ApplicationRepository;
import org.friesoft.porturl.repositories.CategoryRepository;
import org.friesoft.porturl.repositories.UserRepository;
import org.friesoft.porturl.util.NaturalOrderComparator;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final Keycloak keycloakAdminClient;
    private final EntityManager entityManager;

    @Value("${keycloak.admin.realm}")
    private String realm;

    public ApplicationService(ApplicationRepository applicationRepository, UserRepository userRepository, CategoryRepository categoryRepository, Keycloak keycloakAdminClient, EntityManager entityManager) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.keycloakAdminClient = keycloakAdminClient;
        this.entityManager = entityManager;
    }

    @Transactional
    public Application createApplication(org.friesoft.porturl.dto.ApplicationCreateRequest request, Jwt principal) {
        User creator = userRepository.findByProviderUserId(principal.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found in local database"));

        Application newApp = new Application();
        newApp.setName(request.getName());
        newApp.setUrl(request.getUrl());
        newApp.setIcon(request.getIcon());
        newApp.setCreatedBy(creator);

        if (request.getCategories() != null) {
            for (org.friesoft.porturl.dto.Category catDto : request.getCategories()) {
                Category managedCategory = categoryRepository.findById(catDto.getId())
                        .orElseThrow(() -> new RuntimeException("Category not found with id: " + catDto.getId()));
                managedCategory.getApplications().add(newApp);
                newApp.getCategories().add(managedCategory);
                enforceApplicationSortOrder(managedCategory);
            }
        }

        Application savedApp = applicationRepository.save(newApp);
        entityManager.flush();

        createApplicationRoles(savedApp, request.getRoles());

        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            String highestRole = request.getRoles().get(0);
            assignRoleToUser(savedApp.getId(), creator.getId(), highestRole);
        }

        return applicationRepository.findById(savedApp.getId()).orElseThrow(() -> new ApplicationNotFoundException(savedApp.getId()));
    }

    @Transactional
    public Application updateApplication(Long id, org.friesoft.porturl.dto.ApplicationUpdateRequest newApplicationData) {
        Application updatedApplication = applicationRepository.findById(id)
                .map(application -> {
                    application.setName(newApplicationData.getName());
                    application.setUrl(newApplicationData.getUrl());
                    application.setIcon(newApplicationData.getIcon());

                    // Handle roles
                    List<String> existingRoles = getRolesForApplication(id);
                    List<String> newRoles = newApplicationData.getAvailableRoles();

                    if (newRoles != null) {
                        List<String> rolesToAdd = newRoles.stream()
                                .filter(role -> !existingRoles.contains(role))
                                .collect(Collectors.toList());

                        List<String> rolesToRemove = existingRoles.stream()
                                .filter(role -> !newRoles.contains(role))
                                .collect(Collectors.toList());

                        if (!rolesToAdd.isEmpty()) {
                            createApplicationRoles(application, rolesToAdd);
                        }
                        if (!rolesToRemove.isEmpty()) {
                            deleteApplicationRoles(application, rolesToRemove);
                        }
                    }


                    if (newApplicationData.getCategories() != null) {
                        Set<Long> incomingCatIds = newApplicationData.getCategories().stream()
                                .map(org.friesoft.porturl.dto.Category::getId)
                                .collect(Collectors.toSet());

                        // Remove from old categories
                        application.getCategories().forEach(cat -> {
                            if (!incomingCatIds.contains(cat.getId())) {
                                cat.getApplications().remove(application);
                            }
                        });
                        application.getCategories().removeIf(cat -> !incomingCatIds.contains(cat.getId()));

                        // Add to new categories
                        incomingCatIds.forEach(catId -> {
                            if (application.getCategories().stream().noneMatch(c -> c.getId().equals(catId))) {
                                Category managedCategory = categoryRepository.findById(catId)
                                        .orElseThrow(() -> new RuntimeException("Category not found"));
                                managedCategory.getApplications().add(application);
                                application.getCategories().add(managedCategory);
                                enforceApplicationSortOrder(managedCategory);
                            }
                        });
                    }
                    return applicationRepository.save(application);
                })
                .orElseThrow(() -> new ApplicationNotFoundException(id));
        entityManager.flush();
        return applicationRepository.findById(updatedApplication.getId()).orElseThrow(() -> new ApplicationNotFoundException(updatedApplication.getId()));
    }


    @Transactional
    public void reorderApplications(List<org.friesoft.porturl.dto.Category> categories) {
        for (org.friesoft.porturl.dto.Category catDto : categories) {
            categoryRepository.findById(catDto.getId()).ifPresent(category -> {
                if (catDto.getApplicationSortMode() != null) {
                    category.setApplicationSortMode(Category.SortMode.valueOf(catDto.getApplicationSortMode().getValue()));
                }
                
                if (catDto.getApplications() != null) {
                    if (category.getApplicationSortMode() == Category.SortMode.ALPHABETICAL) {
                        enforceApplicationSortOrder(category);
                    } else {
                        List<Application> reorderedApps = new ArrayList<>();
                        for (org.friesoft.porturl.dto.Application appDto : catDto.getApplications()) {
                            applicationRepository.findById(appDto.getId()).ifPresent(reorderedApps::add);
                        }
                        category.setApplications(reorderedApps);
                    }
                }
                categoryRepository.save(category);
            });
        }
    }

    @Transactional
    public void createApplicationRoles(Application app, List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) return;
        RolesResource rolesResource = keycloakAdminClient.realm(realm).roles();
        String appNameUpper = app.getName().toUpperCase().replaceAll("\s+", "_");
        String accessRoleName = "APP_" + appNameUpper + "_ACCESS";
        createRoleIfNotExists(rolesResource, accessRoleName, "Grants basic access to " + app.getName());
        RoleRepresentation accessRole = rolesResource.get(accessRoleName).toRepresentation();
        for (String roleName : roleNames) {
            String roleNameUpper = roleName.toUpperCase();
            String permRoleName = "PERM_" + appNameUpper + "_" + roleNameUpper;
            createRoleIfNotExists(rolesResource, permRoleName, "Grants " + roleName + " permissions for " + app.getName());
            RoleRepresentation permRole = rolesResource.get(permRoleName).toRepresentation();
            String compositeRoleName = "ROLE_" + appNameUpper + "_" + roleNameUpper;
            createRoleIfNotExists(rolesResource, compositeRoleName, "User role for " + roleName + " in " + app.getName());
            rolesResource.get(compositeRoleName).addComposites(List.of(accessRole, permRole));
        }
    }

    @Transactional
    public void deleteApplicationRoles(Application app, List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) return;
        RolesResource rolesResource = keycloakAdminClient.realm(realm).roles();
        String appNameUpper = app.getName().toUpperCase().replaceAll("\s+", "_");

        for (String roleName : roleNames) {
            String roleNameUpper = roleName.toUpperCase();
            String compositeRoleName = "ROLE_" + appNameUpper + "_" + roleNameUpper;
            String permRoleName = "PERM_" + appNameUpper + "_" + roleNameUpper;

            try {
                rolesResource.get(compositeRoleName).remove();
            } catch (NotFoundException e) {
                // Role might not exist, which is fine
            }
            try {
                rolesResource.get(permRoleName).remove();
            } catch (NotFoundException e) {
                // Role might not exist, which is fine
            }
        }
    }

    private void createRoleIfNotExists(RolesResource rolesResource, String roleName, String description) {
        if (rolesResource.list().stream().noneMatch(r -> r.getName().equals(roleName))) {
            RoleRepresentation newRole = new RoleRepresentation(roleName, description, false);
            rolesResource.create(newRole);
        }
    }

    @Transactional
    public void assignRoleToUser(Long applicationId, Long userId, String role) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        String appNameUpper = app.getName().toUpperCase().replaceAll("\s+", "_");
        String roleNameUpper = role.toUpperCase();
        String compositeRoleName = "ROLE_" + appNameUpper + "_" + roleNameUpper;
        RoleRepresentation roleToAssign = keycloakAdminClient.realm(realm).roles().get(compositeRoleName).toRepresentation();
        keycloakAdminClient.realm(realm).users().get(user.getProviderUserId()).roles().realmLevel().add(List.of(roleToAssign));
    }

    @Transactional
    public void removeRoleFromUser(Long applicationId, Long userId, String role) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        String appNameUpper = app.getName().toUpperCase().replaceAll("\s+", "_");
        String roleNameUpper = role.toUpperCase();
        String compositeRoleName = "ROLE_" + appNameUpper + "_"+ roleNameUpper;
        RoleRepresentation roleToRemove = keycloakAdminClient.realm(realm).roles().get(compositeRoleName).toRepresentation();
        keycloakAdminClient.realm(realm).users().get(user.getProviderUserId()).roles().realmLevel().remove(List.of(roleToRemove));
    }

    public List<org.friesoft.porturl.dto.ApplicationWithRolesDto> getApplicationsForCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Set<String> userRoles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        List<Application> allApps = applicationRepository.findAll();

        if (userRoles.contains("ROLE_ADMIN")) {
            List<RoleRepresentation> allRealmRoles = keycloakAdminClient.realm(realm).roles().list();
            return allApps.stream()
                    .map(app -> {
                        String appPrefix = "ROLE_" + app.getName().toUpperCase().replaceAll("\s+", "_") + "_";
                        List<String> availableRoles = allRealmRoles.stream()
                                .map(RoleRepresentation::getName)
                                .filter(name -> name.startsWith(appPrefix))
                                .map(name -> name.substring(appPrefix.length()).toLowerCase())
                                .collect(Collectors.toList());
                        org.friesoft.porturl.dto.ApplicationWithRolesDto dto = new org.friesoft.porturl.dto.ApplicationWithRolesDto();
                        dto.setApplication(mapToDto(app));
                        dto.setAvailableRoles(availableRoles);
                        return dto;
                    })
                    .collect(Collectors.toList());
        }

        return allApps.stream()
                .filter(app -> {
                    String requiredRole = "APP_" + app.getName().toUpperCase().replaceAll("\s+", "_") + "_ACCESS";
                    return userRoles.contains(requiredRole);
                })
                .map(app -> {
                    org.friesoft.porturl.dto.ApplicationWithRolesDto dto = new org.friesoft.porturl.dto.ApplicationWithRolesDto();
                    dto.setApplication(mapToDto(app));
                    dto.setAvailableRoles(List.of());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public org.friesoft.porturl.dto.Application mapToDto(Application app) {
        org.friesoft.porturl.dto.Application dto = new org.friesoft.porturl.dto.Application();
        dto.setId(app.getId());
        dto.setName(app.getName());
        dto.setUrl(app.getUrl());
        dto.setIcon(app.getIcon());
        dto.setIconUrl(app.getIconUrl());
        if (app.getCategories() != null) {
            dto.setCategories(app.getCategories().stream().map(this::mapCategoryToDtoSimple).collect(Collectors.toList()));
        }
        return dto;
    }

    private org.friesoft.porturl.dto.Category mapCategoryToDtoSimple(Category category) {
        org.friesoft.porturl.dto.Category dto = new org.friesoft.porturl.dto.Category();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setIcon(category.getIcon());
        dto.setEnabled(category.isEnabled());
        dto.setSortOrder(category.getSortOrder());
        dto.setApplicationSortMode(org.friesoft.porturl.dto.Category.ApplicationSortModeEnum.fromValue(category.getApplicationSortMode().name()));
        return dto;
    }

    public Application findOne(Long id) {
        return applicationRepository.findById(id).orElseThrow(() -> new ApplicationNotFoundException(id));
    }

    public void deleteApplication(Long id) {
        if (applicationRepository.existsById(id)) {
            applicationRepository.deleteById(id);
        } else {
            throw new ApplicationNotFoundException(id);
        }
    }

        public List<String> getRolesForApplication(Long applicationId) {
            Application app = applicationRepository.findById(applicationId)
                    .orElseThrow(() -> new ApplicationNotFoundException(applicationId));
            List<RoleRepresentation> allRealmRoles = getAllRealmRoles();
            String appPrefix = "ROLE_" + app.getName().toUpperCase().replaceAll("\\s+", "_") + "_";
            return allRealmRoles.stream()
                    .map(RoleRepresentation::getName)
                    .filter(name -> name.startsWith(appPrefix))
                    .map(name -> name.substring(appPrefix.length()).toLowerCase())
                    .collect(Collectors.toList());
        }

        public List<RoleRepresentation> getAllRealmRoles() {
        List<RoleRepresentation> allRoles = new ArrayList<>();
        int first = 0;
        int max = 100;
        List<RoleRepresentation> page;
        do {
            page = keycloakAdminClient.realm(realm).roles().list(first, max);
            allRoles.addAll(page);
            first += max;
        } while (page.size() == max);
        return allRoles;
    }

    public void enforceApplicationSortOrder(Category category) {
        if (category.getApplicationSortMode() == Category.SortMode.ALPHABETICAL && category.getApplications() != null) {
            List<Application> sortedApps = new ArrayList<>(category.getApplications());
            NaturalOrderComparator comparator = new NaturalOrderComparator();
            sortedApps.sort((a, b) -> {
                String nameA = a.getName() != null ? a.getName() : "";
                String nameB = b.getName() != null ? b.getName() : "";
                return comparator.compare(nameA, nameB);
            });
            category.setApplications(sortedApps);
        }
    }
}

    