package org.friesoft.porturl.service;

import jakarta.persistence.EntityManager;
import jakarta.ws.rs.NotFoundException;
import org.friesoft.porturl.config.PorturlProperties;
import org.friesoft.porturl.controller.exceptions.ApplicationNotFoundException;
import org.friesoft.porturl.dto.MoveApplicationRequest;
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
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final PorturlProperties properties;
    private final Keycloak keycloakAdminClient;
    private final Keycloak masterKeycloakAdminClient;
    private final EntityManager entityManager;

    public ApplicationService(ApplicationRepository applicationRepository,
                              CategoryRepository categoryRepository,
                              UserRepository userRepository,
                              PorturlProperties properties,
                              @org.springframework.beans.factory.annotation.Qualifier("keycloakAdmin") Keycloak keycloakAdminClient,
                              @org.springframework.beans.factory.annotation.Qualifier("masterKeycloakAdmin") Keycloak masterKeycloakAdminClient,
                              EntityManager entityManager) {
        this.applicationRepository = applicationRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.properties = properties;
        this.keycloakAdminClient = keycloakAdminClient;
        this.masterKeycloakAdminClient = masterKeycloakAdminClient;
        this.entityManager = entityManager;
    }

    public Page<org.friesoft.porturl.dto.ApplicationWithRolesDto> getApplicationsForCurrentUser(Pageable pageable, String q) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        List<Application> allApps = applicationRepository.findAll();
        List<org.friesoft.porturl.dto.ApplicationWithRolesDto> dtos = allApps.stream()
                .filter(app -> q == null || q.isEmpty() || app.getName().toLowerCase().contains(q.toLowerCase()))
                .filter(app -> {
                    if (isAdmin) return true;
                    String accessRole = "APP_" + app.getName().toUpperCase().replaceAll("\\s+", "_") + "_ACCESS";
                    return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(accessRole));
                })
                .map(this::mapToApplicationWithRolesDto)
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), dtos.size());

        if (start > dtos.size()) {
            return new PageImpl<>(List.of(), pageable, dtos.size());
        }

        return new org.springframework.data.domain.PageImpl<>(dtos.subList(start, end), pageable, dtos.size());
    }

    public Application findOne(Long id) {
        return applicationRepository.findById(id).orElseThrow(() -> new ApplicationNotFoundException(id));
    }

    @Transactional
    public Application createApplication(org.friesoft.porturl.dto.ApplicationCreateRequest request, Jwt principal) {
        Application app = new Application();
        app.setName(request.getName());
        app.setUrl(request.getUrl());
        app.setIcon(request.getIcon());
        app.setClientId(request.getClientId());
        app.setRealm(request.getRealm());

        if (request.getCategories() != null) {
            List<Category> categories = categoryRepository.findAllById(request.getCategories().stream().map(org.friesoft.porturl.dto.Category::getId).collect(Collectors.toList()));
            app.setCategories(categories);
            for (Category category : categories) {
                category.getApplications().add(app);
                enforceApplicationSortOrder(category);
            }
        }

        Application savedApp = applicationRepository.save(app);
        createAccessRole(savedApp);
        if (request.getClientId() != null && !request.getClientId().isBlank() && request.getRoles() != null) {
            createClientRoles(savedApp, request.getRoles());
        }
        return savedApp;
    }

    @Transactional
    public Application updateApplication(Long id, org.friesoft.porturl.dto.ApplicationUpdateRequest request) {
        Application updatedApplication = applicationRepository.findById(id)
                .map(application -> {
                    application.setName(request.getName());
                    application.setUrl(request.getUrl());
                    application.setIcon(request.getIcon());
                    application.setClientId(request.getClientId());
                    application.setRealm(request.getRealm());

                    if (request.getCategories() != null) {
                        Set<Long> incomingCatIds = request.getCategories().stream()
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
    public void deleteApplication(Long id) {
        applicationRepository.findById(id).ifPresent(app -> {
            app.getCategories().forEach(cat -> cat.getApplications().remove(app));
            applicationRepository.delete(app);
        });
    }

    @Transactional
    public void moveApplication(Long id, MoveApplicationRequest request) {
        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new ApplicationNotFoundException(id));

        Category fromCat = categoryRepository.findById(request.getFromCategoryId())
                .orElseThrow(() -> new RuntimeException("Source category not found"));
        Category toCat = categoryRepository.findById(request.getToCategoryId())
                .orElseThrow(() -> new RuntimeException("Target category not found"));

        // Remove from source if it was there
        fromCat.getApplications().remove(app);
        app.getCategories().remove(fromCat);

        // Add to target at specific index
        if (!toCat.getApplications().contains(app)) {
            if (request.getTargetIndex() != null && request.getTargetIndex() >= 0 && request.getTargetIndex() < toCat.getApplications().size()) {
                toCat.getApplications().add(request.getTargetIndex(), app);
            } else {
                toCat.getApplications().add(app);
            }
            app.getCategories().add(toCat);
            enforceApplicationSortOrder(toCat);
        }

        categoryRepository.save(fromCat);
        categoryRepository.save(toCat);
        applicationRepository.save(app);
    }

    @Transactional
    public void createAccessRole(Application app) {
        RolesResource rolesResource = keycloakAdminClient.realm(getRealm()).roles();
        String appNameUpper = app.getName().toUpperCase().replaceAll("\\s+", "_");
        String accessRoleName = "APP_" + appNameUpper + "_ACCESS";
        createRoleIfNotExists(rolesResource, accessRoleName, "Grants basic access to " + app.getName());
    }

    @Transactional
    public void createClientRoles(Application app, List<String> roles) {
        if (app.getClientId() == null || app.getClientId().isBlank()) return;
        String realm = (app.getRealm() != null && !app.getRealm().isBlank()) ? app.getRealm() : getRealm();
        
        var clientResource = getClientResource(realm, app.getClientId());
        
        for (String roleName : roles) {
            RoleRepresentation role = new RoleRepresentation();
            role.setName(roleName);
            role.setClientRole(true);
            try {
                clientResource.roles().create(role);
            } catch (Exception e) {
                // Already exists or other error
            }
        }
    }

    public List<String> getRolesForApplication(Long id) {
        Application app = findOne(id);
        if (app.getClientId() == null || app.getClientId().isBlank()) return List.of();
        
        String realm = (app.getRealm() != null && !app.getRealm().isBlank()) ? app.getRealm() : getRealm();
        try {
            return getClientResource(realm, app.getClientId()).roles().list().stream()
                    .map(RoleRepresentation::getName)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }

    @Transactional
    public void assignRoleToUser(Long applicationId, Long userId, String roleName) {
        Application app = findOne(applicationId);
        User user = userRepository.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String realm = (app.getRealm() != null && !app.getRealm().isBlank()) ? app.getRealm() : getRealm();

        if (roleName.startsWith("ROLE_") || roleName.startsWith("APP_")) {
            // Assign realm-level role
            RolesResource rolesResource = keycloakAdminClient.realm(getRealm()).roles();
            RoleRepresentation role = rolesResource.get(roleName).toRepresentation();
            keycloakAdminClient.realm(getRealm()).users().get(user.getProviderUserId()).roles().realmLevel().add(List.of(role));
        } else if (app.getClientId() != null && !app.getClientId().isBlank()) {
            // Assign client role
            var clientResource = getClientResource(realm, app.getClientId());
            RoleRepresentation role = clientResource.roles().get(roleName).toRepresentation();

            // Find user in target realm
            String username = keycloakAdminClient.realm(getRealm()).users().get(user.getProviderUserId()).toRepresentation().getUsername();
            UserRepresentation targetUser = getKeycloakAdminClient(realm).realm(realm).users().search(username).stream()
                    .filter(u -> u.getUsername().equalsIgnoreCase(username))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found in target realm"));

            getKeycloakAdminClient(realm).realm(realm).users().get(targetUser.getId()).roles().clientLevel(getClientUuid(realm, app.getClientId())).add(List.of(role));
        }
    }

    @Transactional
    public void removeRoleFromUser(Long applicationId, Long userId, String roleName) {
        Application app = findOne(applicationId);
        User user = userRepository.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String realm = (app.getRealm() != null && !app.getRealm().isBlank()) ? app.getRealm() : getRealm();

        if (roleName.startsWith("ROLE_") || roleName.startsWith("APP_")) {
            RolesResource rolesResource = keycloakAdminClient.realm(getRealm()).roles();
            RoleRepresentation role = rolesResource.get(roleName).toRepresentation();
            keycloakAdminClient.realm(getRealm()).users().get(user.getProviderUserId()).roles().realmLevel().remove(List.of(role));
        } else if (app.getClientId() != null && !app.getClientId().isBlank()) {
            var clientResource = getClientResource(realm, app.getClientId());
            RoleRepresentation role = clientResource.roles().get(roleName).toRepresentation();

            String username = keycloakAdminClient.realm(getRealm()).users().get(user.getProviderUserId()).toRepresentation().getUsername();
            UserRepresentation targetUser = getKeycloakAdminClient(realm).realm(realm).users().search(username).stream()
                    .filter(u -> u.getUsername().equalsIgnoreCase(username))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found in target realm"));

            getKeycloakAdminClient(realm).realm(realm).users().get(targetUser.getId()).roles().clientLevel(getClientUuid(realm, app.getClientId())).remove(List.of(role));
        }
    }

    private org.keycloak.admin.client.resource.ClientResource getClientResource(String realm, String clientId) {
        return getKeycloakAdminClient(realm).realm(realm).clients().get(getClientUuid(realm, clientId));
    }

    private String getClientUuid(String realm, String clientId) {
        List<org.keycloak.representations.idm.ClientRepresentation> clients = getKeycloakAdminClient(realm).realm(realm).clients().findByClientId(clientId);
        if (clients.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found: " + clientId);
        }
        return clients.get(0).getId();
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
        dto.setIsLinked(app.getClientId() != null && !app.getClientId().isBlank());
        
        if (app.getCategories() != null) {
            dto.setCategories(app.getCategories().stream()
                .map(cat -> {
                    org.friesoft.porturl.dto.Category catDto = new org.friesoft.porturl.dto.Category();
                    catDto.setId(cat.getId());
                    catDto.setName(cat.getName());
                    catDto.setSortOrder(cat.getSortOrder());
                    catDto.setApplicationSortMode(org.friesoft.porturl.dto.Category.ApplicationSortModeEnum.fromValue(cat.getApplicationSortMode().name()));
                    return catDto;
                })
                .collect(Collectors.toList()));
        }
        return dto;
    }

    private org.friesoft.porturl.dto.ApplicationWithRolesDto mapToApplicationWithRolesDto(Application app) {
        org.friesoft.porturl.dto.ApplicationWithRolesDto dto = new org.friesoft.porturl.dto.ApplicationWithRolesDto();
        dto.setApplication(mapToDto(app));
        dto.setAvailableRoles(getRolesForApplication(app.getId()));
        return dto;
    }

    public void enforceApplicationSortOrder(Category category) {
        if (category.getApplicationSortMode() == Category.SortMode.ALPHABETICAL) {
            NaturalOrderComparator comparator = new NaturalOrderComparator();
            category.getApplications().sort((a, b) -> {
                String nameA = a.getName() != null ? a.getName() : "";
                String nameB = b.getName() != null ? b.getName() : "";
                return comparator.compare(nameA, nameB);
            });
        }
    }

    public Keycloak getKeycloakAdminClient(String realm) {
        String adminRealm = properties.getKeycloak().getAdmin().getRealm();
        if (realm != null && !realm.equalsIgnoreCase(adminRealm)) {
            return masterKeycloakAdminClient;
        }
        return keycloakAdminClient;
    }

    private String getRealm() {
        return properties.getKeycloak().getRealm();
    }

    private void createRoleIfNotExists(RolesResource rolesResource, String name, String description) {
        try {
            rolesResource.get(name).toRepresentation();
        } catch (NotFoundException e) {
            RoleRepresentation role = new RoleRepresentation();
            role.setName(name);
            role.setDescription(description);
            rolesResource.create(role);
        }
    }
}
