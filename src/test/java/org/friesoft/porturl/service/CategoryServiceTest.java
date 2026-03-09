package org.friesoft.porturl.service;

import org.friesoft.porturl.dto.ApplicationWithRolesDto;
import org.friesoft.porturl.entities.Application;
import org.friesoft.porturl.entities.Category;
import org.friesoft.porturl.repositories.ApplicationRepository;
import org.friesoft.porturl.repositories.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ApplicationService applicationService;

    @Mock
    private ApplicationRepository applicationRepository;

    @InjectMocks
    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void getVisibleCategories_includesEmptyCategories() {
        // Arrange
        Category emptyCategory = new Category();
        emptyCategory.setId(1L);
        emptyCategory.setName("Empty");
        emptyCategory.setApplications(new ArrayList<>());

        Category categoryWithApps = new Category();
        categoryWithApps.setId(2L);
        categoryWithApps.setName("With Apps");
        Application app = new Application();
        app.setId(10L);
        app.setName("App");
        categoryWithApps.setApplications(new ArrayList<>(List.of(app)));

        when(categoryRepository.findAllByOrderBySortOrderAsc()).thenReturn(List.of(emptyCategory, categoryWithApps));

        org.friesoft.porturl.dto.Application visibleAppDto = new org.friesoft.porturl.dto.Application();
        visibleAppDto.setId(10L);
        ApplicationWithRolesDto dto = new ApplicationWithRolesDto();
        dto.setApplication(visibleAppDto);

        when(applicationService.getApplicationsForCurrentUser(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(dto)));

        // Act
        Page<org.friesoft.porturl.dto.Category> result = categoryService.getVisibleCategories(PageRequest.of(0, 10));

        // Assert
        assertEquals(2, result.getTotalElements());
        assertEquals("Empty", result.getContent().get(0).getName());
        assertEquals("With Apps", result.getContent().get(1).getName());
    }

    @Test
    void getVisibleCategories_includesCategoriesWithNoVisibleApps() {
        // Arrange
        Category categoryWithHiddenApps = new Category();
        categoryWithHiddenApps.setId(1L);
        categoryWithHiddenApps.setName("Hidden");
        Application hiddenApp = new Application();
        hiddenApp.setId(99L);
        hiddenApp.setName("Hidden App");
        categoryWithHiddenApps.setApplications(new ArrayList<>(List.of(hiddenApp)));

        when(categoryRepository.findAllByOrderBySortOrderAsc()).thenReturn(List.of(categoryWithHiddenApps));
        when(applicationService.getApplicationsForCurrentUser(any(Pageable.class))).thenReturn(new PageImpl<>(Collections.emptyList()));

        // Act
        Page<org.friesoft.porturl.dto.Category> result = categoryService.getVisibleCategories(PageRequest.of(0, 10));

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals(0, result.getContent().get(0).getApplications().size());
    }
}
