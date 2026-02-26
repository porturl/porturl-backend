package org.friesoft.porturl.security;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import org.friesoft.porturl.entities.User;
import org.friesoft.porturl.repositories.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CustomGrantedAuthoritiesExtractor {

    private final UserRepository userRepository;
    private final JsonMapper jsonMapper;

    public CustomGrantedAuthoritiesExtractor(UserRepository userRepository, JsonMapper jsonMapper) {
        this.userRepository = userRepository;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    public synchronized Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        String providerUserId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");

        userRepository.findByProviderUserId(providerUserId)
            .or(() -> userRepository.findByEmail(email).map(user -> {
                user.setProviderUserId(providerUserId);
                return userRepository.save(user);
            }))
            .orElseGet(() -> {
                User newUser = new User();
                newUser.setProviderUserId(providerUserId);
                newUser.setEmail(email);
                return userRepository.save(newUser);
            });

        List<GrantedAuthority> authorities = new java.util.ArrayList<>();

        // 1. Extract Realm Roles
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            List<String> roles = jsonMapper.convertValue(realmAccess.get("roles"), new TypeReference<>() {});
            roles.stream()
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);
        }

        // 2. Extract Client Roles (for linked apps)
        // Format in JWT: "resource_access": { "client-id": { "roles": ["role1", "role2"] } }
        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess != null) {
            resourceAccess.forEach((clientId, access) -> {
                Map<String, Object> accessMap = jsonMapper.convertValue(access, new TypeReference<>() {});
                if (accessMap.containsKey("roles")) {
                    List<String> roles = jsonMapper.convertValue(accessMap.get("roles"), new TypeReference<>() {});
                    roles.stream()
                        .map(role -> new SimpleGrantedAuthority("APP_" + clientId + "_" + role))
                        .forEach(authorities::add);
                }
            });
        }

        return authorities;
    }
}
