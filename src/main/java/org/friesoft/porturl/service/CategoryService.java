package org.friesoft.porturl.service;

import org.friesoft.porturl.entities.Application;
import org.friesoft.porturl.entities.Category;
import org.friesoft.porturl.repositories.CategoryRepository;
import org.friesoft.porturl.util.NaturalOrderComparator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    public CategoryService(CategoryRepository categoryRepository, ApplicationService applicationService) {
        this.categoryRepository = categoryRepository;
        this.applicationService = applicationService;
    }

    @Transactional(readOnly = true)
    public Page<org.friesoft.porturl.dto.Category> getVisibleCategories(Pageable pageable) {
        // 1. Get ALL applications the current user is allowed to see
        List<org.friesoft.porturl.dto.ApplicationWithRolesDto> visibleAppDtos = applicationService.getApplicationsForCurrentUser(PageRequest.of(0, Integer.MAX_VALUE)).getContent();
        Set<Long> visibleAppIds = visibleAppDtos.stream()
                .map(dto -> dto.getApplication().getId())
                .collect(Collectors.toSet());

        // 2. Fetch all categories.
        List<Category> allCategories = categoryRepository.findAllByOrderBySortOrderAsc();

        // 3. Filter applications list within EACH category and map to DTOs
        List<org.friesoft.porturl.dto.Category> filteredCategoryDtos = allCategories.stream()
                .peek(cat -> {
                    if (cat.getApplications() != null) {
                        List<Application> filteredApps = cat.getApplications().stream()
                                .filter(Objects::nonNull)
                                .filter(app -> visibleAppIds.contains(app.getId()))
                                .collect(Collectors.toCollection(ArrayList::new));

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
                })
                .map(this::mapToDto)
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredCategoryDtos.size());

        if (start > filteredCategoryDtos.size()) {
            return new PageImpl<>(List.of(), pageable, filteredCategoryDtos.size());
        }

        return new PageImpl<>(filteredCategoryDtos.subList(start, end), pageable, filteredCategoryDtos.size());
    }

    public org.friesoft.porturl.dto.Category mapToDto(Category category) {
        org.friesoft.porturl.dto.Category dto = new org.friesoft.porturl.dto.Category();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setSortOrder(category.getSortOrder());
        dto.setApplicationSortMode(org.friesoft.porturl.dto.Category.ApplicationSortModeEnum.fromValue(category.getApplicationSortMode().name()));
        dto.setDescription(category.getDescription());
        
        if (category.getApplications() != null) {
            dto.setApplications(category.getApplications().stream()
                .map(applicationService::mapToDto)
                .collect(Collectors.toList()));
        }
        
        return dto;
    }
}
