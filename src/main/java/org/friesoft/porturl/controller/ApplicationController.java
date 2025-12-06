package org.friesoft.porturl.controller;

import org.friesoft.porturl.dto.ApplicationCreateRequest;
import org.friesoft.porturl.dto.ApplicationUpdateRequest;
import org.friesoft.porturl.dto.ApplicationWithRolesDto;
import org.friesoft.porturl.entities.Application;
import org.friesoft.porturl.service.ApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping
    public List<ApplicationWithRolesDto> getVisibleApplications() {
        return applicationService.getApplicationsForCurrentUser();
    }

    @GetMapping("/{id}")
    public Application findOne(@PathVariable Long id) {
        return applicationService.findOne(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Application> createApplication(@RequestBody ApplicationCreateRequest request, @AuthenticationPrincipal Jwt principal) {
        Application createdApp = applicationService.createApplication(request, principal);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdApp.getId())
                .toUri();
        return ResponseEntity.created(location).body(createdApp);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Application updateApplication(@RequestBody ApplicationUpdateRequest newApplicationData, @PathVariable Long id) {
        return applicationService.updateApplication(id, newApplicationData);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteApplication(@PathVariable Long id) {
        applicationService.deleteApplication(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reorder")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> reorderApplications(@RequestBody List<Application> applications) {
        applicationService.reorderApplications(applications);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{applicationId}/assign/{userId}/{role}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> assignRoleToUser(@PathVariable Long applicationId, @PathVariable Long userId, @PathVariable String role) {
        applicationService.assignRoleToUser(applicationId, userId, role);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{applicationId}/unassign/{userId}/{role}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> removeRoleFromUser(@PathVariable Long applicationId, @PathVariable Long userId, @PathVariable String role) {
        applicationService.removeRoleFromUser(applicationId, userId, role);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<String> getApplicationRoles(@PathVariable Long id) {
        return applicationService.getRolesForApplication(id);
    }
}