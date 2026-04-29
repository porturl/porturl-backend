package org.friesoft.porturl.service;

import org.friesoft.porturl.dto.ApplicationWithRolesDto;
import org.friesoft.porturl.entities.Application;
import org.friesoft.porturl.entities.Category;
import org.friesoft.porturl.repositories.ApplicationRepository;
import org.friesoft.porturl.repositories.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ApplicationService applicationService;
    @Mock
    private ApplicationRepository applicationRepository;

    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(categoryRepository, applicationService, applicationRepository);
    }

    private void mockSecurityContext(String role) {
        Authentication auth = mock(Authentication.class);
        SecurityContext ctx = mock(SecurityContext.class);
        lenient().when(ctx.getAuthentication()).thenReturn(auth);
        lenient().when(auth.getAuthorities()).thenAnswer(invocation -> List.of(new SimpleGrantedAuthority(role)));
        SecurityContextHolder.setContext(ctx);
    }

    @Test
    void getVisibleCategories_asAdmin_returnsAll() {
        // Arrange
        mockSecurityContext("ROLE_ADMIN");
        Category cat = new Category();
        cat.setId(1L);
        cat.setName("Admin Only");
        cat.setApplicationSortMode(Category.SortMode.ALPHABETICAL);
        
        lenient().when(categoryRepository.findAllByOrderBySortOrderAsc()).thenReturn(List.of(cat));

        // Act
        var result = categoryService.getVisibleCategories(PageRequest.of(0, 10), Collections.emptyMap());

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals("Admin Only", result.getContent().get(0).getName());
    }

    @Test
    void getVisibleCategories_asUser_returnsOnlyPopulated() {
        // Arrange
        mockSecurityContext("ROLE_USER");
        
        Category cat = new Category();
        cat.setId(1L);
        cat.setName("Populated");
        cat.setApplicationSortMode(Category.SortMode.ALPHABETICAL);
        Application app = new Application();
        app.setId(10L);
        cat.setApplications(List.of(app));

        lenient().when(categoryRepository.findAllByOrderBySortOrderAsc()).thenReturn(List.of(cat));
        
        org.friesoft.porturl.dto.Application appDto = new org.friesoft.porturl.dto.Application();
        appDto.setId(10L);
        ApplicationWithRolesDto dto = new ApplicationWithRolesDto();
        dto.setApplication(appDto);
        lenient().when(applicationService.getApplicationsForCurrentUser(any(), any())).thenReturn(new PageImpl<>(List.of(dto)));

        // Act
        var result = categoryService.getVisibleCategories(PageRequest.of(0, 10), Collections.emptyMap());

        // Assert
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getApplicationsByCategory_appliesNaturalOrder() {
        // Arrange
        mockSecurityContext("ROLE_ADMIN");
        Category cat = new Category();
        cat.setId(1L);
        cat.setApplicationSortMode(Category.SortMode.ALPHABETICAL);
        
        Application app1 = new Application(); app1.setId(1L); app1.setName("App 10");
        Application app2 = new Application(); app2.setId(2L); app2.setName("App 2");
        cat.setApplications(List.of(app1, app2));

        lenient().when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat));
        
        // Visible apps mock
        org.friesoft.porturl.dto.Application d1 = new org.friesoft.porturl.dto.Application(); d1.setId(1L); d1.setName("App 10");
        org.friesoft.porturl.dto.Application d2 = new org.friesoft.porturl.dto.Application(); d2.setId(2L); d2.setName("App 2");
        ApplicationWithRolesDto wr1 = new ApplicationWithRolesDto(); wr1.setApplication(d1);
        ApplicationWithRolesDto wr2 = new ApplicationWithRolesDto(); wr2.setApplication(d2);
        lenient().when(applicationService.getApplicationsForCurrentUser(any(), any())).thenReturn(new PageImpl<>(List.of(wr1, wr2)));
        
        lenient().when(applicationService.mapToDto(app1)).thenReturn(d1);
        lenient().when(applicationService.mapToDto(app2)).thenReturn(d2);

        // Act
        var result = categoryService.getApplicationsByCategory(1L);

        // Assert
        assertEquals(2, result.size());
        assertEquals("App 2", result.get(0).getName()); // Natural order: 2 before 10
        assertEquals("App 10", result.get(1).getName());
    }

    @Test
    void getVisibleCategories_withFilter_appliesFilter() {
        mockSecurityContext("ROLE_ADMIN");
        Category cat1 = new Category(); cat1.setId(1L); cat1.setName("Alpha");
        Category cat2 = new Category(); cat2.setId(2L); cat2.setName("Beta");
        
        lenient().when(categoryRepository.findAllByOrderBySortOrderAsc()).thenReturn(List.of(cat1, cat2));
        
        var result = categoryService.getVisibleCategories(PageRequest.of(0, 10), Map.of("filter", "alpha"));
        
        assertEquals(1, result.getTotalElements());
        assertEquals("Alpha", result.getContent().get(0).getName());
    }

    @Test
    void getVisibleCategories_withIdFilter_appliesFilter() {
        mockSecurityContext("ROLE_ADMIN");
        Category cat1 = new Category(); cat1.setId(1L); cat1.setName("Alpha");
        Category cat2 = new Category(); cat2.setId(2L); cat2.setName("Beta");
        
        lenient().when(categoryRepository.findAllByOrderBySortOrderAsc()).thenReturn(List.of(cat1, cat2));
        
        var result = categoryService.getVisibleCategories(PageRequest.of(0, 10), Map.of("id", List.of(Map.of("id", "2"))));
        
        assertEquals(1, result.getTotalElements());
        assertEquals("Beta", result.getContent().get(0).getName());
    }

    @Test
    void reorderApplicationsInCategory_succeeds() {
        Category cat = new Category();
        cat.setId(1L);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat));
        
        Application a1 = new Application(); a1.setId(10L);
        Application a2 = new Application(); a2.setId(20L);
        
        when(applicationRepository.findAllById(List.of(20L, 10L))).thenReturn(List.of(a1, a2));
        
        categoryService.reorderApplicationsInCategory(1L, List.of(20L, 10L));
        
        verify(categoryRepository).save(cat);
        assertEquals(20L, cat.getApplications().get(0).getId());
        assertEquals(10L, cat.getApplications().get(1).getId());
    }

    @Test
    void mapToDto_succeeds() {
        Category cat = new Category();
        cat.setId(1L);
        cat.setName("Test");
        cat.setSortOrder(5);
        cat.setApplicationSortMode(Category.SortMode.CUSTOM);
        cat.setDescription("Desc");
        
        var dto = categoryService.mapToDto(cat);
        
        assertEquals(1L, dto.getId());
        assertEquals("Test", dto.getName());
        assertEquals(5, dto.getSortOrder());
        assertEquals("Desc", dto.getDescription());
        assertEquals(org.friesoft.porturl.dto.Category.ApplicationSortModeEnum.CUSTOM, dto.getApplicationSortMode());
    }
}
