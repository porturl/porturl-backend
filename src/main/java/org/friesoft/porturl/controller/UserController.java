package org.friesoft.porturl.controller;

import org.friesoft.porturl.entities.User;
import org.friesoft.porturl.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/roles")
    public Collection<String> getCurrentUserRoles() {
        return userService.getCurrentUserRoles().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<User> getAllUsers() {
        return userService.findAll();
    }

    @GetMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<String> getUserRoles(@PathVariable Long id) {
        return userService.getUserRoles(id);
    }
}
