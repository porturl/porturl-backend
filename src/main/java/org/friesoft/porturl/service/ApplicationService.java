package org.friesoft.porturl.service;

import jakarta.persistence.EntityManager;
import jakarta.ws.rs.NotFoundException;
import org.friesoft.porturl.controller.exceptions.ApplicationNotFoundException;
import org.friesoft.porturl.dto.ApplicationCreateRequest;
import org.friesoft.porturl.dto.ApplicationUpdateRequest;
import org.friesoft.porturl.dto.ApplicationWithRolesDto;
import org.friesoft.porturl.entities.Application;
import org.friesoft.porturl.entities.ApplicationCategory;
import org.friesoft.porturl.entities.Category;
import org.friesoft.porturl.entities.User;
import org.friesoft.porturl.repositories.ApplicationRepository;
import org.friesoft.porturl.repositories.CategoryRepository;
import org.friesoft.porturl.repositories.UserRepository;
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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
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
    public Application createApplication(ApplicationCreateRequest request, Jwt principal) {
        User creator = userRepository.findByProviderUserId(principal.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found in local database"));

        Application newApp = new Application();
        newApp.setName(request.getName());
        newApp.setUrl(request.getUrl());
        newApp.setCreatedBy(creator);

        if (request.getApplicationCategories() != null) {
            Set<ApplicationCategory> managedAppCategories = new HashSet<>();
            for (ApplicationCategory ac : request.getApplicationCategories()) {
                Category managedCategory = categoryRepository.findById(ac.getCategory().getId())
                        .orElseThrow(() -> new RuntimeException("Category not found with id: " + ac.getCategory().getId()));
                ApplicationCategory newLink = new ApplicationCategory();
                newLink.setApplication(newApp);
                newLink.setCategory(managedCategory);
                newLink.setSortOrder(ac.getSortOrder());
                managedAppCategories.add(newLink);
            }
            newApp.setApplicationCategories(managedAppCategories);
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
    public Application updateApplication(Long id, ApplicationUpdateRequest newApplicationData) {
        Application updatedApplication = applicationRepository.findById(id)
                .map(application -> {
                    application.setName(newApplicationData.getName());
                    application.setUrl(newApplicationData.getUrl());
                    application.setIconLarge(newApplicationData.getIconLarge());
                    application.setIconMedium(newApplicationData.getIconMedium());
                    application.setIconThumbnail(newApplicationData.getIconThumbnail());

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


                    if (newApplicationData.getApplicationCategories() != null) {
                        Map<Long, ApplicationCategory> incomingLinks = newApplicationData.getApplicationCategories().stream()
                                .collect(Collectors.toMap(ac -> ac.getCategory().getId(), Function.identity()));

                        application.getApplicationCategories().removeIf(
                                existingLink -> !incomingLinks.containsKey(existingLink.getCategory().getId())
                        );

                        incomingLinks.forEach((catId, incomingLink) -> {
                            Category managedCategory = categoryRepository.findById(catId)
                                    .orElseThrow(() -> new RuntimeException("Category not found"));

                            application.getApplicationCategories().stream()
                                    .filter(link -> link.getCategory().getId().equals(catId))
                                    .findFirst()
                                    .ifPresentOrElse(
                                            existingLink -> existingLink.setSortOrder(incomingLink.getSortOrder()),
                                            () -> {
                                                ApplicationCategory newLink = new ApplicationCategory();
                                                newLink.setApplication(application);
                                                newLink.setCategory(managedCategory);
                                                newLink.setSortOrder(incomingLink.getSortOrder());
                                                application.getApplicationCategories().add(newLink);
                                            }
                                    );
                        });
                    }
                    return applicationRepository.save(application);
                })
                .orElseThrow(() -> new ApplicationNotFoundException(id));
        entityManager.flush();
        return applicationRepository.findById(updatedApplication.getId()).orElseThrow(() -> new ApplicationNotFoundException(updatedApplication.getId()));
    }


    @Transactional
    public void reorderApplications(List<Application> applications) {
        for (Application app : applications) {
            applicationRepository.findById(app.getId()).ifPresent(existingApp -> {
                app.getApplicationCategories().forEach(incomingLink ->
                    existingApp.getApplicationCategories().stream()
                            .filter(existingLink -> existingLink.getCategory().getId().equals(incomingLink.getCategory().getId()))
                            .findFirst()
                            .ifPresent(existingLink -> existingLink.setSortOrder(incomingLink.getSortOrder()))
                );
                applicationRepository.save(existingApp);
            });
        }
    }

    private void createApplicationRoles(Application app, List<String> roleNames) {
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

    private void deleteApplicationRoles(Application app, List<String> roleNames) {
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
        String compositeRoleName = "ROLE_" + appNameUpper + "_" + roleNameUpper;
        RoleRepresentation roleToRemove = keycloakAdminClient.realm(realm).roles().get(compositeRoleName).toRepresentation();
        keycloakAdminClient.realm(realm).users().get(user.getProviderUserId()).roles().realmLevel().remove(List.of(roleToRemove));
    }

    public List<ApplicationWithRolesDto> getApplicationsForCurrentUser() {
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
                        return new ApplicationWithRolesDto(app, availableRoles);
                    })
                    .collect(Collectors.toList());
        }

        return allApps.stream()
                .filter(app -> {
                    String requiredRole = "APP_" + app.getName().toUpperCase().replaceAll("\s+", "_") + "_ACCESS";
                    return userRoles.contains(requiredRole);
                })
                .map(app -> new ApplicationWithRolesDto(app, List.of()))
                .collect(Collectors.toList());
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
        List<RoleRepresentation> allRealmRoles = keycloakAdminClient.realm(realm).roles().list();
        String appPrefix = "ROLE_" + app.getName().toUpperCase().replaceAll("\s+", "_") + "_";
        return allRealmRoles.stream()
                .map(RoleRepresentation::getName)
                .filter(name -> name.startsWith(appPrefix))
                .map(name -> name.substring(appPrefix.length()).toLowerCase())
                .collect(Collectors.toList());
    }
}