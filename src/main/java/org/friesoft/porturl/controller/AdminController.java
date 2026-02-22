package org.friesoft.porturl.controller;

import org.friesoft.porturl.api.AdminApi;
import org.friesoft.porturl.dto.ExportData;
import org.friesoft.porturl.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminController implements AdminApi {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @Override
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ExportData> exportData() {
        return ResponseEntity.ok(adminService.exportData());
    }

    @Override
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> importData(ExportData data) {
        Jwt principal = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        adminService.importData(data, principal);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<java.util.List<org.friesoft.porturl.dto.KeycloakClientDto>> scanRealmClients(String realm) {
        return ResponseEntity.ok(adminService.scanRealmForClients(realm));
    }
}
