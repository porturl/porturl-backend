package org.friesoft.porturl.service;

import org.friesoft.porturl.entities.Application;
import org.friesoft.porturl.entities.Category;
import org.friesoft.porturl.repositories.CategoryRepository;
import org.friesoft.porturl.repositories.ApplicationRepository;
import org.friesoft.porturl.util.NaturalOrderComparator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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

    @Transactional(readOnly = true)
    public List<Category> getVisibleCategories() {
        // 1. Get the applications the current user is allowed to see.
        List<org.friesoft.porturl.dto.ApplicationWithRolesDto> visibleAppDtos = applicationService.getApplicationsForCurrentUser();
        Set<Long> visibleAppIds = visibleAppDtos.stream()
                .map(dto -> dto.getApplication().getId())
                .collect(Collectors.toSet());

        // 2. Fetch all enabled categories. 
        List<Category> categories = categoryRepository.findByEnabledTrueOrderBySortOrderAsc();

        // 3. Filter and sort the applications list within EACH category
        for (Category cat : categories) {
            if (cat.getApplications() != null) {
                List<Application> filteredApps = cat.getApplications().stream()
                    .filter(Objects::nonNull)
                    .filter(app -> visibleAppIds.contains(app.getId()))
                    .collect(Collectors.toCollection(ArrayList::new));
                
                // Always enforce sort order based on mode during retrieval
                if (cat.getApplicationSortMode() == Category.SortMode.ALPHABETICAL) {
                    NaturalOrderComparator comparator = new NaturalOrderComparator();
                    filteredApps.sort((a, b) -> {
                        String nameA = a.getName() != null ? a.getName() : "";
                        String nameB = b.getName() != null ? b.getName() : "";
                        return comparator.compare(nameA, nameB);
                    });
                }
                
                cat.setApplications(filteredApps);
            }
        }

        return categories;
    }
}
