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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

        when(applicationService.getApplicationsForCurrentUser()).thenReturn(List.of(dto));

        // Act
        List<Category> result = categoryService.getVisibleCategories();

        // Assert
        assertEquals(2, result.size());
        assertEquals("Empty", result.get(0).getName());
        assertEquals("With Apps", result.get(1).getName());
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
        when(applicationService.getApplicationsForCurrentUser()).thenReturn(Collections.emptyList());

        // Act
        List<Category> result = categoryService.getVisibleCategories();

        // Assert
        assertEquals(1, result.size());
        assertEquals(0, result.get(0).getApplications().size());
    }
}
