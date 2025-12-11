package org.friesoft.porturl.service;

import org.friesoft.porturl.entities.User;
import org.friesoft.porturl.repositories.UserRepository;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final Keycloak keycloakAdminClient;

    @Value("${keycloak.admin.realm}")
    private String realm;

    public UserService(UserRepository userRepository, Keycloak keycloakAdminClient) {
        this.userRepository = userRepository;
        this.keycloakAdminClient = keycloakAdminClient;
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

    public List<String> getUserRoles(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<RoleRepresentation> roles = keycloakAdminClient.realm(realm).users().get(user.getProviderUserId())
                .roles().realmLevel().listEffective();

        return roles.stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toList());
    }
}
