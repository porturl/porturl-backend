package org.friesoft.porturl.controller;

import org.friesoft.porturl.api.ApplicationApi;
import org.friesoft.porturl.entities.Application;
import org.friesoft.porturl.service.ApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ApplicationController implements ApplicationApi {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @Override
    public ResponseEntity<List<org.friesoft.porturl.dto.ApplicationWithRolesDto>> getVisibleApplications() {
        return ResponseEntity.ok(applicationService.getApplicationsForCurrentUser());
    }

    @Override
    public ResponseEntity<org.friesoft.porturl.dto.Application> findOneApplication(Long id) {
        return ResponseEntity.ok(applicationService.mapToDto(applicationService.findOne(id)));
    }

    @Override
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<org.friesoft.porturl.dto.Application> createApplication(@RequestBody org.friesoft.porturl.dto.ApplicationCreateRequest request) {
        Jwt principal = (Jwt) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Application createdApp = applicationService.createApplication(request, principal);
        return ResponseEntity.status(201).body(applicationService.mapToDto(createdApp));
    }

    @Override
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<org.friesoft.porturl.dto.Application> updateApplication(Long id, @RequestBody org.friesoft.porturl.dto.ApplicationUpdateRequest request) {
        return ResponseEntity.ok(applicationService.mapToDto(applicationService.updateApplication(id, request)));
    }

    @Override
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteApplication(Long id) {
        applicationService.deleteApplication(id);
        return ResponseEntity.ok().build();
    }

    @Override
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> reorderApplications(@RequestBody List<org.friesoft.porturl.dto.Category> categories) {
        applicationService.reorderApplications(categories);
        return ResponseEntity.ok().build();
    }

    @Override
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> assignRoleToUser(Long applicationId, Long userId, String role) {
        applicationService.assignRoleToUser(applicationId, userId, role);
        return ResponseEntity.ok().build();
    }

    @Override
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> removeRoleFromUser(Long applicationId, Long userId, String role) {
        applicationService.removeRoleFromUser(applicationId, userId, role);
        return ResponseEntity.ok().build();
    }

    @Override
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<String>> getApplicationRoles(Long id) {
        return ResponseEntity.ok(applicationService.getRolesForApplication(id));
    }
}