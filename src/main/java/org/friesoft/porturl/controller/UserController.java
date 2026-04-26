package org.friesoft.porturl.controller;

import org.friesoft.porturl.api.UserApi;
import org.friesoft.porturl.dto.CreateAuthTicket200Response;
import org.friesoft.porturl.entities.User;
import org.friesoft.porturl.service.UserService;
import org.friesoft.porturl.service.AdminService;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
public class UserController extends BaseController implements UserApi {

    private final UserService userService;
    private final CacheManager cacheManager;
    private final AdminService adminService;

    public UserController(UserService userService, CacheManager cacheManager, AdminService adminService) {
        this.userService = userService;
        this.cacheManager = cacheManager;
        this.adminService = adminService;
    }

    @Override
    public ResponseEntity<CreateAuthTicket200Response> createAuthTicket() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID ticket = UUID.randomUUID();
        Cache cache = cacheManager.getCache("authTickets");
        if (cache != null) {
            cache.put(ticket.toString(), authentication);
        }
        CreateAuthTicket200Response response = new CreateAuthTicket200Response();
        response.setTicket(ticket);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> bridgeAuth(UUID ticket, URI next) {
        Cache cache = cacheManager.getCache("authTickets");
        if (cache != null) {
            Authentication authentication = cache.get(ticket.toString(), Authentication.class);
            if (authentication != null) {
                ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
                HttpServletRequest request = attr.getRequest();
                
                HttpSession session = request.getSession(true);
                SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
                securityContext.setAuthentication(authentication);
                session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
                cache.evict(ticket.toString());
            }
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(next);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
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
    public ResponseEntity<List<org.friesoft.porturl.dto.User>> getAllUsers(Integer page, Integer size, List<String> sort, String filter, String range) {
        return ok(userService.findAll(getPageable(page, size, sort, range), getQuery(filter)), "users");
    }

    @Override
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional
    public ResponseEntity<org.friesoft.porturl.dto.User> createUser(@RequestBody org.friesoft.porturl.dto.User user) {
        ResponseEntity<org.friesoft.porturl.dto.User> response = ResponseEntity.status(HttpStatus.CREATED).body(mapToDto(userService.save(user)));
        adminService.exportToFile();
        return response;
    }

    @Override
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<org.friesoft.porturl.dto.User> getUserById(Long id) {
        return ResponseEntity.ok(mapToDto(userService.findById(id)));
    }

    @Override
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional
    public ResponseEntity<org.friesoft.porturl.dto.User> updateUser(Long id, @RequestBody org.friesoft.porturl.dto.User user) {
        ResponseEntity<org.friesoft.porturl.dto.User> response = ResponseEntity.ok(mapToDto(userService.update(id, user)));
        adminService.exportToFile();
        return response;
    }

    @Override
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional
    public ResponseEntity<Void> deleteUser(Long id) {
        boolean isAdmin = userService.getUserRoles(id).stream()
            .anyMatch(r -> r.equalsIgnoreCase("admin") || r.equalsIgnoreCase("role_admin") || r.startsWith("ROLE_ADMIN"));
        
        if (isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin users cannot be deleted");
        }

        userService.delete(id);
        adminService.exportToFile();
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<String>> getUserRoles(Long id) {
        return ResponseEntity.ok(userService.getUserRoles(id));
    }

    private org.friesoft.porturl.dto.User mapToDto(User user) {
        System.out.println("Mapping user: " + user.getId() + ", username: " + user.getUsername());
        org.friesoft.porturl.dto.User dto = new org.friesoft.porturl.dto.User();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setProviderUserId(user.getProviderUserId());
        dto.setImage(user.getImage());
        dto.setImageUrl(user.getImageUrl());
        return dto;
    }
}
