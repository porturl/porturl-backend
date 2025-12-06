package org.friesoft.porturl.service;

import org.friesoft.porturl.dto.ApplicationWithRolesDto;
import org.friesoft.porturl.entities.Application;
import org.friesoft.porturl.entities.Category;
import org.friesoft.porturl.repositories.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ApplicationService applicationService;

    public CategoryService(CategoryRepository categoryRepository, ApplicationService applicationService) {
        this.categoryRepository = categoryRepository;
        this.applicationService = applicationService;
    }

    public List<Category> getVisibleCategories() {
        // 1. Get the applications the current user is allowed to see.
        List<ApplicationWithRolesDto> visibleAppDtos = applicationService.getApplicationsForCurrentUser();

        // 2. If the user can't see any apps, they can't see any categories.
        if (visibleAppDtos.isEmpty()) {
            return List.of();
        }

        List<Application> visibleApps = visibleAppDtos.stream()
                .map(ApplicationWithRolesDto::getApplication)
                .collect(Collectors.toList());

        // 3. Find all categories that are associated with at least one of the visible apps.
        return categoryRepository.findDistinctByApplicationInAndEnabledTrueOrderBySortOrderAsc(visibleApps);
    }
}
