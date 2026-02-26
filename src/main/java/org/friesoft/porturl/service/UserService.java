package org.friesoft.porturl.service;

import org.friesoft.porturl.config.PorturlProperties;
import org.friesoft.porturl.entities.User;
import org.friesoft.porturl.repositories.ApplicationRepository;
import org.friesoft.porturl.repositories.UserRepository;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final Keycloak keycloakAdminClient;
    private final Keycloak masterKeycloakAdminClient;
    private final PorturlProperties properties;

    public UserService(UserRepository userRepository,
                       ApplicationRepository applicationRepository,
                       @org.springframework.beans.factory.annotation.Qualifier("keycloakAdmin") Keycloak keycloakAdminClient,
                       @org.springframework.beans.factory.annotation.Qualifier("masterKeycloakAdmin") Keycloak masterKeycloakAdminClient,
                       PorturlProperties properties) {
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
        this.keycloakAdminClient = keycloakAdminClient;
        this.masterKeycloakAdminClient = masterKeycloakAdminClient;
        this.properties = properties;
    }

    private Keycloak getKeycloakAdminClient(String targetRealm) {
        String localRealm = getRealm();
        if (targetRealm == null || targetRealm.isBlank() || targetRealm.equals(localRealm)) {
            return keycloakAdminClient;
        }
        return masterKeycloakAdminClient;
    }

    public Optional<User> findByProviderUserId(String providerUserId) {
        return userRepository.findByProviderUserId(providerUserId);
    }

    public Collection<? extends GrantedAuthority> getCurrentUserRoles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            return authentication.getAuthorities();
        }
        return Collections.emptyList();
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        return findByProviderUserId(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public User updateCurrentUser(org.friesoft.porturl.dto.UserUpdateRequest request) {
        User user = getCurrentUser();
        if (request.getImage() != null) {
            user.setImage(request.getImage());
        }
        return userRepository.save(user);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    private String getRealm() {
        if (properties.getKeycloak().getRealm() != null && !properties.getKeycloak().getRealm().isBlank()) {
            return properties.getKeycloak().getRealm();
        }
        return properties.getKeycloak().getAdmin().getRealm();
    }

    private String getAccessRoleName(org.friesoft.porturl.entities.Application app) {
        String appNameUpper = app.getName().toUpperCase().replaceAll("\s+", "_");
        return "APP_" + appNameUpper + "_ACCESS";
    }

    public List<String> getUserRoles(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String realm = getRealm();

        // 1. Get realm roles (includes unlinked app roles with ROLE_ prefix AND all centralized APP_..._ACCESS roles)
        List<String> userRoles = new ArrayList<>(keycloakAdminClient.realm(realm).users().get(user.getProviderUserId())
                .roles().realmLevel().listEffective().stream()
                .map(RoleRepresentation::getName)
                .toList());

        // 2. Fetch Client Roles for Linked Apps (potentially across realms)
        // Optimization: Cache target user IDs per realm
        Map<String, String> targetUserIdsCache = new HashMap<>();
        String username = keycloakAdminClient.realm(realm).users().get(user.getProviderUserId()).toRepresentation().getUsername();

        applicationRepository.findAll().stream()
                .forEach(app -> {
                    String accessRoleName = getAccessRoleName(app);
                    // Ensure access role is in the list if user has it
                    // (It should already be there from step 1 if it's a realm role, but we can verify/format if needed)

                    if (app.getClientId() != null && !app.getClientId().isBlank()) {
                        try {
                            String targetRealm = (app.getRealm() != null && !app.getRealm().isBlank()) ? app.getRealm() : realm;

                            String targetUserId = targetUserIdsCache.computeIfAbsent(targetRealm, r -> {
                                List<org.keycloak.representations.idm.UserRepresentation> users = getKeycloakAdminClient(r).realm(r).users().search(username);
                                return users.stream()
                                        .filter(u -> u.getUsername().equalsIgnoreCase(username))
                                        .map(org.keycloak.representations.idm.UserRepresentation::getId)
                                        .findFirst()
                                        .orElse(null);
                            });

                            if (targetUserId != null) {
                                String clientUuid = getKeycloakAdminClient(targetRealm).realm(targetRealm).clients().findByClientId(app.getClientId()).get(0).getId();
                                List<RoleRepresentation> clientRoles = getKeycloakAdminClient(targetRealm).realm(targetRealm).users().get(targetUserId).roles().clientLevel(clientUuid).listAll();

                                clientRoles.forEach(r -> {
                                    // Format: APP_{id}_{roleName} to avoid collisions between apps
                                    userRoles.add("APP_" + app.getId() + "_" + r.getName());
                                });
                            }
                        } catch (Exception e) {
                            // Log and ignore
                        }
                    }
                    // For unlinked apps, roles are already in userRoles (with ROLE_ prefix)
                });

        return userRoles;
    }
}
