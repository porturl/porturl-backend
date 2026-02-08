package org.friesoft.porturl.service;

import org.friesoft.porturl.entities.Application;
import org.friesoft.porturl.entities.Category;
import org.friesoft.porturl.repositories.CategoryRepository;
import org.friesoft.porturl.repositories.ApplicationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ApplicationService applicationService;
    private final ApplicationRepository applicationRepository;

    public CategoryService(CategoryRepository categoryRepository, ApplicationService applicationService, ApplicationRepository applicationRepository) {
        this.categoryRepository = categoryRepository;
        this.applicationService = applicationService;
        this.applicationRepository = applicationRepository;
    }

    public List<Category> getVisibleCategories() {
        // 1. Get the applications the current user is allowed to see.
        List<org.friesoft.porturl.dto.ApplicationWithRolesDto> visibleAppDtos = applicationService.getApplicationsForCurrentUser();
        Set<Long> visibleAppIds = visibleAppDtos.stream()
                .map(dto -> dto.getApplication().getId())
                .collect(Collectors.toSet());

        // 2. Fetch all enabled categories. 
        // Note: For an admin, you might want to return ALL categories, but usually 'enabled' is a display flag.
        List<Category> categories = categoryRepository.findByEnabledTrueOrderBySortOrderAsc();

        // 3. Filter the applications list within EACH category to only include those the user can see.
        for (Category cat : categories) {
            if (cat.getApplications() != null) {
                cat.setApplications(cat.getApplications().stream()
                    .filter(Objects::nonNull)
                    .filter(app -> visibleAppIds.contains(app.getId()))
                    .collect(Collectors.toList()));
            }
        }

        return categories;
    }
}
