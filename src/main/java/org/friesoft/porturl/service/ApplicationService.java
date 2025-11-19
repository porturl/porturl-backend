package org.friesoft.porturl.service;

import org.friesoft.porturl.dto.ApplicationCreateRequest;
import org.friesoft.porturl.entities.Application;
import org.friesoft.porturl.entities.User;
import org.friesoft.porturl.repositories.ApplicationRepository;
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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final Keycloak keycloakAdminClient;

    @Value("${keycloak.admin.realm}")
    private String realm;

    public ApplicationService(ApplicationRepository applicationRepository, UserRepository userRepository, Keycloak keycloakAdminClient) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.keycloakAdminClient = keycloakAdminClient;
    }

    @Transactional
    public Application createApplication(ApplicationCreateRequest request, Jwt principal) {
        User creator = userRepository.findByProviderUserId(principal.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found in local database"));

        Application newApp = new Application();
        newApp.setName(request.getName());
        newApp.setUrl(request.getUrl());
        newApp.setCreatedBy(creator);
        Application savedApp = applicationRepository.save(newApp);

        createApplicationRoles(savedApp, request.getRoles());

        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            String highestRole = request.getRoles().getFirst();
            assignRoleToUser(savedApp.getId(), creator.getId(), highestRole);
        }

        return savedApp;
    }

    private void createApplicationRoles(Application app, List<String> roleNames) {
        RolesResource rolesResource = keycloakAdminClient.realm(realm).roles();
        String appNameUpper = app.getName().toUpperCase().replaceAll("\\s+", "_");

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

    private void createRoleIfNotExists(RolesResource rolesResource, String roleName, String description) {
        if (rolesResource.list().stream().noneMatch(r -> r.getName().equals(roleName))) {
            RoleRepresentation newRole = new RoleRepresentation(roleName, description, false);
            rolesResource.create(newRole);
        }
    }

    @Transactional
    public void assignRoleToUser(Long applicationId, Long userId, String role) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String appNameUpper = app.getName().toUpperCase().replaceAll("\\s+", "_");
        String roleNameUpper = role.toUpperCase();
        String compositeRoleName = "ROLE_" + appNameUpper + "_" + roleNameUpper;

        RoleRepresentation roleToAssign = keycloakAdminClient.realm(realm).roles().get(compositeRoleName).toRepresentation();
        keycloakAdminClient.realm(realm).users().get(user.getProviderUserId()).roles().realmLevel().add(List.of(roleToAssign));
    }

    @Transactional
    public void removeRoleFromUser(Long applicationId, Long userId, String role) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String appNameUpper = app.getName().toUpperCase().replaceAll("\\s+", "_");
        String roleNameUpper = role.toUpperCase();
        String compositeRoleName = "ROLE_" + appNameUpper + "_" + roleNameUpper;

        RoleRepresentation roleToRemove = keycloakAdminClient.realm(realm).roles().get(compositeRoleName).toRepresentation();
        keycloakAdminClient.realm(realm).users().get(user.getProviderUserId()).roles().realmLevel().remove(List.of(roleToRemove));
    }

    public List<Application> getVisibleApplications() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Set<String> userRoles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        // Admins see all applications
        if (userRoles.contains("ROLE_ADMIN")) {
            return applicationRepository.findAll();
        }

        // Regular users see only the apps they have access to
        List<Application> allApps = applicationRepository.findAll();
        return allApps.stream()
                .filter(app -> {
                    String requiredRole = "APP_" + app.getName().toUpperCase().replaceAll("\\s+", "_") + "_ACCESS";
                    return userRoles.contains(requiredRole);
                })
                .collect(Collectors.toList());
    }
}
