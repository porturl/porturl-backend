package org.friesoft.porturl.service;

import lombok.extern.slf4j.Slf4j;
import org.friesoft.porturl.config.PorturlProperties;
import org.friesoft.porturl.entities.User;
import org.friesoft.porturl.repositories.UserRepository;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class KeycloakUserSyncService {

    private final UserRepository userRepository;
    private final Keycloak keycloakAdminClient;
    private final PorturlProperties properties;

    public KeycloakUserSyncService(UserRepository userRepository, Keycloak keycloakAdminClient, PorturlProperties properties) {
        this.userRepository = userRepository;
        this.keycloakAdminClient = keycloakAdminClient;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void syncUsers() {
        if (properties.getStorage().getType() != PorturlProperties.StorageType.SQL) {
            log.info("Skipping Keycloak user synchronization as storage type is not SQL.");
            return;
        }
        log.info("Starting user synchronization from Keycloak...");
        try {
            String realm = getRealm();
            List<UserRepresentation> kcUsers = keycloakAdminClient.realm(realm).users().list();
            java.util.Set<String> kcUserIds = new java.util.HashSet<>();
            
            for (UserRepresentation kcUser : kcUsers) {
                kcUserIds.add(kcUser.getId());
                userRepository.findByProviderUserId(kcUser.getId())
                    .or(() -> userRepository.findByUsername(kcUser.getUsername()))
                    .ifPresentOrElse(
                        user -> {
                            // Update existing user if needed
                            boolean changed = false;
                            if (user.getProviderUserId() == null) {
                                user.setProviderUserId(kcUser.getId());
                                changed = true;
                            }
                            if (!kcUser.getUsername().equals(user.getUsername())) {
                                user.setUsername(kcUser.getUsername());
                                changed = true;
                            }
                            if (kcUser.getEmail() != null && !kcUser.getEmail().equals(user.getEmail())) {
                                user.setEmail(kcUser.getEmail());
                                changed = true;
                            }
                            if (changed) {
                                userRepository.save(user);
                            }
                        },
                        () -> {
                            // Create new local user
                            log.info("Creating local user from Keycloak: {}", kcUser.getUsername());
                            User newUser = new User();
                            newUser.setProviderUserId(kcUser.getId());
                            newUser.setUsername(kcUser.getUsername());
                            newUser.setEmail(kcUser.getEmail());
                            userRepository.save(newUser);
                        }
                    );
            }

            // Remove local users that are no longer in Keycloak
            List<User> allLocalUsers = userRepository.findAll();
            for (User localUser : allLocalUsers) {
                if (localUser.getProviderUserId() != null && !kcUserIds.contains(localUser.getProviderUserId())) {
                    // Safety check: Don't delete admins even if not in the current KC user list (might be from another realm or temporary issue)
                    boolean isAdmin = keycloakAdminClient.realm(realm).users().get(localUser.getProviderUserId()).roles().realmLevel().listEffective().stream()
                        .anyMatch(r -> r.getName().equalsIgnoreCase("admin") || r.getName().equalsIgnoreCase("role_admin"));
                    
                    if (!isAdmin) {
                        log.info("Removing local user no longer in Keycloak: {}", localUser.getUsername());
                        userRepository.delete(localUser);
                    } else {
                        log.warn("Skipping removal of local admin user not found in Keycloak: {}", localUser.getUsername());
                    }
                }
            }

            log.info("User synchronization completed. Total Keycloak users: {}", kcUsers.size());
        } catch (Exception e) {
            log.error("Failed to sync users from Keycloak", e);
        }
    }

    private String getRealm() {
        if (properties.getKeycloak().getRealm() != null && !properties.getKeycloak().getRealm().isBlank()) {
            return properties.getKeycloak().getRealm();
        }
        return properties.getKeycloak().getAdmin().getRealm();
    }
}
