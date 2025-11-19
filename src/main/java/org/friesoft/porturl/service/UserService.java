package org.friesoft.porturl.service;

import org.friesoft.porturl.entities.User;
import org.friesoft.porturl.repositories.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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

    public List<User> findAll() {
        return userRepository.findAll();
    }
}
