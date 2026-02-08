package org.friesoft.porturl.controller;

import org.friesoft.porturl.api.UserApi;
import org.friesoft.porturl.entities.User;
import org.friesoft.porturl.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class UserController implements UserApi {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Override
    public ResponseEntity<org.friesoft.porturl.dto.User> getCurrentUser() {
        return ResponseEntity.ok(mapToDto(userService.getCurrentUser()));
    }

    @Override
    public ResponseEntity<org.friesoft.porturl.dto.User> updateCurrentUser(@RequestBody org.friesoft.porturl.dto.UserUpdateRequest request) {
        return ResponseEntity.ok(mapToDto(userService.updateCurrentUser(request)));
    }

    @Override
    public ResponseEntity<List<String>> getCurrentUserRoles() {
        return ResponseEntity.ok(userService.getCurrentUserRoles().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList()));
    }

    @Override
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<org.friesoft.porturl.dto.User>> getAllUsers() {
        List<org.friesoft.porturl.dto.User> dtos = userService.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @Override
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<String>> getUserRoles(Long id) {
        return ResponseEntity.ok(userService.getUserRoles(id));
    }

    private org.friesoft.porturl.dto.User mapToDto(User user) {
        org.friesoft.porturl.dto.User dto = new org.friesoft.porturl.dto.User();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setProviderUserId(user.getProviderUserId());
        dto.setImage(user.getImage());
        dto.setImageUrl(user.getImageUrl());
        return dto;
    }
}
