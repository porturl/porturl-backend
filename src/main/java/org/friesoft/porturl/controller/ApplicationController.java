package org.friesoft.porturl.controller;

import org.friesoft.porturl.dto.ApplicationCreateRequest;
import org.friesoft.porturl.dto.ApplicationWithRolesDto;
import org.friesoft.porturl.entities.Application;
import org.friesoft.porturl.service.ApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Application> createApplication(@RequestBody ApplicationCreateRequest request, @AuthenticationPrincipal Jwt principal) {
        Application createdApp = applicationService.createApplication(request, principal);
        return new ResponseEntity<>(createdApp, HttpStatus.CREATED);
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
}
