package org.friesoft.porturl.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.friesoft.porturl.entities.User;
import org.friesoft.porturl.repositories.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CustomGrantedAuthoritiesExtractor {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public CustomGrantedAuthoritiesExtractor(UserRepository userRepository, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    public Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        String providerUserId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");

        userRepository.findByProviderUserId(providerUserId)
            .orElseGet(() -> {
                User newUser = new User();
                newUser.setProviderUserId(providerUserId);
                newUser.setEmail(email);
                return userRepository.save(newUser);
            });

        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            List<String> roles = objectMapper.convertValue(realmAccess.get("roles"), new TypeReference<>() {});
            return roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
