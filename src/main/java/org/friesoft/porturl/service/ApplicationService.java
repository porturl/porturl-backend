package org.friesoft.porturl.service;

import jakarta.persistence.EntityManager;
import jakarta.ws.rs.NotFoundException;
import org.friesoft.porturl.config.PorturlProperties;
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
@lombok.extern.slf4j.Slf4j
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final Keycloak keycloakAdminClient;
    private final Keycloak masterKeycloakAdminClient;
    private final EntityManager entityManager;
    private final PorturlProperties properties;

    public ApplicationService(ApplicationRepository applicationRepository,
                              UserRepository userRepository,
                              CategoryRepository categoryRepository,
                              @org.springframework.beans.factory.annotation.Qualifier("keycloakAdmin") Keycloak keycloakAdminClient,
                              @org.springframework.beans.factory.annotation.Qualifier("masterKeycloakAdmin") Keycloak masterKeycloakAdminClient,
                              EntityManager entityManager,
                              PorturlProperties properties) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.keycloakAdminClient = keycloakAdminClient;
        this.masterKeycloakAdminClient = masterKeycloakAdminClient;
        this.entityManager = entityManager;
        this.properties = properties;
    }

    public Keycloak getKeycloakAdminClient() {
        return keycloakAdminClient;
    }

    public Keycloak getKeycloakAdminClient(String targetRealm) {
        String localRealm = getRealm();
        if (targetRealm == null || targetRealm.isBlank() || targetRealm.equals(localRealm)) {
            return keycloakAdminClient;
        }
        return masterKeycloakAdminClient;
    }

    private String getRealm() {
        if (properties.getKeycloak().getRealm() != null && !properties.getKeycloak().getRealm().isBlank()) {
            return properties.getKeycloak().getRealm();
        }
        return properties.getKeycloak().getAdmin().getRealm();
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
        newApp.setClientId(request.getClientId());
        newApp.setRealm(request.getRealm());

        // Clear cache for this client
        if (newApp.getClientId() != null) {
            String targetRealm = (newApp.getRealm() != null && !newApp.getRealm().isBlank()) ? newApp.getRealm() : getRealm();
            clientExistsCache.remove(targetRealm + ":" + newApp.getClientId());
        }

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

        // Handle roles
        if (savedApp.getClientId() == null || savedApp.getClientId().isBlank()) {
            createAccessRole(savedApp);
        } else {
            createClientRoles(savedApp, request.getRoles());
        }

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
                    application.setClientId(newApplicationData.getClientId());
                    application.setRealm(newApplicationData.getRealm());

                    // Clear cache for this client
                    if (application.getClientId() != null) {
                        String targetRealm = (application.getRealm() != null && !application.getRealm().isBlank()) ? application.getRealm() : getRealm();
                        clientExistsCache.remove(targetRealm + ":" + application.getClientId());
                    }

                    // Handle roles
                    List<String> existingRoles = getRolesForApplication(id);
                    List<String> newRoles = newApplicationData.getAvailableRoles();

                    if (newRoles != null && application.getClientId() != null && !application.getClientId().isBlank()) {
                        List<String> rolesToAdd = newRoles.stream()
                                .filter(role -> !existingRoles.contains(role))
                                .collect(Collectors.toList());

                        List<String> rolesToRemove = existingRoles.stream()
                                .filter(role -> !newRoles.contains(role))
                                .collect(Collectors.toList());

                        if (!rolesToAdd.isEmpty()) {
                            createClientRoles(application, rolesToAdd);
                        }
                        if (!rolesToRemove.isEmpty()) {
                            deleteClientRoles(application, rolesToRemove);
                        }
                    } else if (newRoles != null && !newRoles.isEmpty()) {
                        log.warn("Attempted to add roles to an unlinked application {}. Roles are only supported for linked applications.", application.getName());
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
    public void createAccessRole(Application app) {
        RolesResource rolesResource = keycloakAdminClient.realm(getRealm()).roles();
        String appNameUpper = app.getName().toUpperCase().replaceAll("\s+", "_");
        String accessRoleName = "APP_" + appNameUpper + "_ACCESS";
        createRoleIfNotExists(rolesResource, accessRoleName, "Grants basic access to " + app.getName());
    }

    @Transactional
    public void deleteAccessRole(Application app) {
        RolesResource rolesResource = keycloakAdminClient.realm(getRealm()).roles();
        String appNameUpper = app.getName().toUpperCase().replaceAll("\s+", "_");
        String accessRoleName = "APP_" + appNameUpper + "_ACCESS";
        try {
            rolesResource.get(accessRoleName).remove();
        } catch (NotFoundException e) {
            // Ignore
        }
    }

    private String getAccessRoleName(Application app) {
        String appNameUpper = app.getName().toUpperCase().replaceAll("\s+", "_");
        return "APP_" + appNameUpper + "_ACCESS";
    }

    @Transactional
    public void createClientRoles(Application app, List<String> roleNames) {
        try {
            createAccessRole(app);
            if (roleNames == null || roleNames.isEmpty()) return;
            String targetRealm = (app.getRealm() != null && !app.getRealm().isBlank()) ? app.getRealm() : getRealm();
            String clientUuid = getClientUuid(targetRealm, app.getClientId());
            RolesResource rolesResource = getKeycloakAdminClient(targetRealm).realm(targetRealm).clients().get(clientUuid).roles();

            for (String roleName : roleNames) {
                if (rolesResource.list().stream().noneMatch(r -> r.getName().equals(roleName))) {
                    RoleRepresentation newRole = new RoleRepresentation(roleName, "Created by PortUrl", false);
                    rolesResource.create(newRole);
                }
            }
        } catch (jakarta.ws.rs.WebApplicationException | org.springframework.web.server.ResponseStatusException e) {
            log.warn("Failed to synchronize Keycloak roles for application {}: {}. This is often expected in dev environments if the target realm/client is missing.", app.getName(), e.getMessage());
        }
    }

    @Transactional
    public void deleteClientRoles(Application app, List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) return;
        String targetRealm = (app.getRealm() != null && !app.getRealm().isBlank()) ? app.getRealm() : getRealm();
        String clientUuid = getClientUuid(targetRealm, app.getClientId());
        RolesResource rolesResource = getKeycloakAdminClient(targetRealm).realm(targetRealm).clients().get(clientUuid).roles();

        for (String roleName : roleNames) {
            try {
                rolesResource.get(roleName).remove();
            } catch (NotFoundException e) {
                // Ignore
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

        String realm = getRealm();
        String accessRoleName = getAccessRoleName(app);

        // Always ensure Access Role is assigned in PortUrl Realm
        RoleRepresentation accessRole = keycloakAdminClient.realm(realm).roles().get(accessRoleName).toRepresentation();
        keycloakAdminClient.realm(realm).users().get(user.getProviderUserId()).roles().realmLevel().add(List.of(accessRole));

        if (role.equals(accessRoleName)) {
            return; // Only access role was requested, we are done
        }

        if (app.getClientId() != null && !app.getClientId().isBlank()) {
            // Linked App: Assign Client Role in Target Realm
            String targetRealm = (app.getRealm() != null && !app.getRealm().isBlank()) ? app.getRealm() : realm;

            // 1. Find user in target realm (assuming username/email match)
            // Using user.getProviderUserId() (keycloak ID) only works if it's the SAME Keycloak instance and realm.
            // For cross-realm, we must search by username.
            // But wait, if it's the same Keycloak instance but different realm, IDs are different.
            // If we assume same username:
            String username = user.getEmail(); // or username if available. User entity has email.
            // Wait, User entity has providerUserId which is the ID in the PortUrl realm.
            // We need to fetch the User Representation from PortUrl realm to get the username to search in target realm.
            String portUrlUserId = user.getProviderUserId();
            org.keycloak.representations.idm.UserRepresentation portUrlUser = keycloakAdminClient.realm(realm).users().get(portUrlUserId).toRepresentation();
            String usernameToSearch = portUrlUser.getUsername();

            List<org.keycloak.representations.idm.UserRepresentation> targetUsers = getKeycloakAdminClient(targetRealm).realm(targetRealm).users().search(usernameToSearch);
            // Ensure exact match
            String targetUserId = targetUsers.stream()
                    .filter(u -> u.getUsername().equalsIgnoreCase(usernameToSearch))
                    .map(org.keycloak.representations.idm.UserRepresentation::getId)
                    .findFirst()
                    .orElse(null);

            if (targetUserId == null) {
                log.warn("User {} not found in target realm {}. Client role {} could not be assigned, but local access role was granted.", usernameToSearch, targetRealm, role);
                return;
            }

            // 2. Find Client UUID in target realm
            String clientUuid = getClientUuid(targetRealm, app.getClientId());

            // 3. Find Role
            RoleRepresentation clientRole = getKeycloakAdminClient(targetRealm).realm(targetRealm).clients().get(clientUuid).roles().get(role).toRepresentation();

            // 4. Assign
            getKeycloakAdminClient(targetRealm).realm(targetRealm).users().get(targetUserId).roles().clientLevel(clientUuid).add(List.of(clientRole));

        } else {
             log.warn("Attempted to assign role {} to unlinked application {}. Only access roles are supported for unlinked applications.", role, app.getName());
        }
    }

    @Transactional
    public void removeRoleFromUser(Long applicationId, Long userId, String role) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String realm = getRealm();
        String accessRoleName = getAccessRoleName(app);

        if (role.equals(accessRoleName)) {
            RoleRepresentation accessRole = keycloakAdminClient.realm(realm).roles().get(accessRoleName).toRepresentation();
            keycloakAdminClient.realm(realm).users().get(user.getProviderUserId()).roles().realmLevel().remove(List.of(accessRole));
            return;
        }

        if (app.getClientId() != null && !app.getClientId().isBlank()) {
            // Linked App: Remove Client Role in Target Realm
            String targetRealm = (app.getRealm() != null && !app.getRealm().isBlank()) ? app.getRealm() : realm;

            String portUrlUserId = user.getProviderUserId();
            org.keycloak.representations.idm.UserRepresentation portUrlUser = keycloakAdminClient.realm(realm).users().get(portUrlUserId).toRepresentation();
            String usernameToSearch = portUrlUser.getUsername();

            List<org.keycloak.representations.idm.UserRepresentation> targetUsers = getKeycloakAdminClient(targetRealm).realm(targetRealm).users().search(usernameToSearch);
            // Ensure exact match
            String targetUserId = targetUsers.stream()
                    .filter(u -> u.getUsername().equalsIgnoreCase(usernameToSearch))
                    .map(org.keycloak.representations.idm.UserRepresentation::getId)
                    .findFirst()
                    .orElse(null);

            if (targetUserId == null) {
                // User doesn't exist there, so role is effectively gone
                return;
            }

            String clientUuid = getClientUuid(targetRealm, app.getClientId());

            try {
                RoleRepresentation clientRole = getKeycloakAdminClient(targetRealm).realm(targetRealm).clients().get(clientUuid).roles().get(role).toRepresentation();
                getKeycloakAdminClient(targetRealm).realm(targetRealm).users().get(targetUserId).roles().clientLevel(clientUuid).remove(List.of(clientRole));
            } catch (NotFoundException e) {
                // Role not found, ignore
            }
        } else {
            log.warn("Attempted to remove role {} from unlinked application {}. Roles are not supported for unlinked applications.", role, app.getName());
        }
    }

    private String getClientUuid(String targetRealm, String clientId) {
        List<org.keycloak.representations.idm.ClientRepresentation> clients = getKeycloakAdminClient(targetRealm).realm(targetRealm).clients().findByClientId(clientId);
        if (clients.isEmpty()) {
             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Client " + clientId + " not found in realm " + targetRealm);
        }
        return clients.get(0).getId();
    }

    public List<org.friesoft.porturl.dto.ApplicationWithRolesDto> getApplicationsForCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Set<String> userRoles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        List<Application> allApps = applicationRepository.findAll();

        String realm = getRealm();

        if (userRoles.contains("ROLE_ADMIN")) {
            return allApps.stream()
                    .map(app -> {
                        org.friesoft.porturl.dto.ApplicationWithRolesDto dto = new org.friesoft.porturl.dto.ApplicationWithRolesDto();
                        dto.setApplication(mapToDto(app));
                        dto.setAvailableRoles(getRolesForApplication(app.getId()));
                        return dto;
                    })
                    .collect(Collectors.toList());
        }

        // For regular users, we need to check visibility.
        // Visibility is ALWAYS checked via the centralized access role in the PortUrl realm.

        return allApps.stream()
                .filter(app -> {
                    String requiredRole = getAccessRoleName(app);
                    return userRoles.contains(requiredRole);
                })
                .map(app -> {
                    org.friesoft.porturl.dto.ApplicationWithRolesDto dto = new org.friesoft.porturl.dto.ApplicationWithRolesDto();
                    dto.setApplication(mapToDto(app));
                    dto.setAvailableRoles(List.of()); // Regular users don't see available roles list
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
        dto.setClientId(app.getClientId());
        dto.setRealm(app.getRealm());
        dto.setIsLinked(checkClientExists(app));
        if (app.getCategories() != null) {
            dto.setCategories(app.getCategories().stream().map(this::mapCategoryToDtoSimple).collect(Collectors.toList()));
        }
        return dto;
    }

    private final Map<String, Boolean> clientExistsCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, Long> clientExistsCacheTime = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 60000; // 1 minute

    private boolean checkClientExists(Application app) {
        if (app.getClientId() == null || app.getClientId().isBlank()) {
            return false;
        }
        String targetRealm = (app.getRealm() != null && !app.getRealm().isBlank()) ? app.getRealm() : getRealm();
        String cacheKey = targetRealm + ":" + app.getClientId();

        long now = System.currentTimeMillis();
        if (clientExistsCache.containsKey(cacheKey) && (now - clientExistsCacheTime.get(cacheKey)) < CACHE_TTL_MS) {
            return clientExistsCache.get(cacheKey);
        }

        try {
            boolean exists = !getKeycloakAdminClient(targetRealm).realm(targetRealm).clients().findByClientId(app.getClientId()).isEmpty();
            clientExistsCache.put(cacheKey, exists);
            clientExistsCacheTime.put(cacheKey, now);
            return exists;
        } catch (Exception e) {
            return false;
        }
    }

    private org.friesoft.porturl.dto.Category mapCategoryToDtoSimple(Category category) {
        org.friesoft.porturl.dto.Category dto = new org.friesoft.porturl.dto.Category();
        dto.setId(category.getId());
        dto.setName(category.getName());
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

            String realm = getRealm();

            if (app.getClientId() != null && !app.getClientId().isBlank()) {
                // Linked App: Fetch Client Roles
                String targetRealm = (app.getRealm() != null && !app.getRealm().isBlank()) ? app.getRealm() : realm;
                try {
                    String clientUuid = getClientUuid(targetRealm, app.getClientId());
                    return getKeycloakAdminClient(targetRealm).realm(targetRealm).clients().get(clientUuid).roles().list().stream()
                            .map(RoleRepresentation::getName)
                            .filter(name -> !name.equals(getAccessRoleName(app))) // Exclude access role
                            .collect(Collectors.toList());
                } catch (Exception e) {
                    log.warn("Could not fetch roles for linked application {} in realm {}: {}", app.getName(), targetRealm, e.getMessage());
                    return List.of();
                }
            } else {
                // Unlinked App: only access role which is not in this list.
                return List.of();
            }
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
