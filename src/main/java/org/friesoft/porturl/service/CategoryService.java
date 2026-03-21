package org.friesoft.porturl.service;

import org.friesoft.porturl.entities.Application;
import org.friesoft.porturl.entities.Category;
import org.friesoft.porturl.repositories.ApplicationRepository;
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
import java.util.Map;
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
    public Page<org.friesoft.porturl.dto.Category> getVisibleCategories(Pageable pageable, Map<String, Object> filter) {
        String q = null;
        List<Long> ids = null;

        if (filter.containsKey("filter")) {
            q = filter.get("filter").toString();
        } else if (filter.containsKey("q")) {
            q = filter.get("q").toString();
        }

        if (filter.containsKey("id")) {
            Object idVal = filter.get("id");
            if (idVal instanceof List) {
                ids = ((List<?>) idVal).stream()
                        .map(o -> {
                            if (o instanceof Map) {
                                return Long.valueOf(((Map<?, ?>) o).get("id").toString());
                            }
                            String s = o.toString();
                            if (s.startsWith("{") && s.contains("id=")) {
                                try {
                                    String[] parts = s.substring(1, s.length() - 1).split(",");
                                    for (String part : parts) {
                                        String[] pair = part.trim().split("=");
                                        if (pair[0].equals("id")) {
                                            return Long.valueOf(pair[1]);
                                        }
                                    }
                                } catch (Exception e) {
                                    // Fallback
                                }
                            }
                            return Long.valueOf(s);
                        })
                        .collect(Collectors.toList());
            } else if (idVal instanceof Map) {
                ids = List.of(Long.valueOf(((Map<?, ?>) idVal).get("id").toString()));
            } else {
                ids = List.of(Long.valueOf(idVal.toString()));
            }
        }

        final String query = q;
        final List<Long> filterIds = ids;

        // Security filtering
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        List<Category> categoriesToProcess;
        if (isAdmin) {
            categoriesToProcess = categoryRepository.findAllByOrderBySortOrderAsc();
        } else {
            // For regular users, only show categories that have visible applications
            List<org.friesoft.porturl.dto.ApplicationWithRolesDto> visibleApps = applicationService.getApplicationsForCurrentUser(PageRequest.of(0, Integer.MAX_VALUE), null).getContent();
            Set<Long> visibleAppIds = visibleApps.stream()
                    .map(dto -> dto.getApplication().getId())
                    .collect(Collectors.toSet());

            categoriesToProcess = categoryRepository.findAllByOrderBySortOrderAsc().stream()
                    .filter(cat -> cat.getApplications().stream().anyMatch(app -> visibleAppIds.contains(app.getId())))
                    .collect(Collectors.toList());
        }

        // Filter categories and map to DTOs (thin)
        List<org.friesoft.porturl.dto.Category> filteredCategoryDtos = categoriesToProcess.stream()
                .filter(cat -> {
                    boolean matchesId = filterIds == null || filterIds.contains(cat.getId());
                    boolean matchesQuery = query == null || query.isEmpty() || (cat.getName() != null && cat.getName().toLowerCase().contains(query.toLowerCase()));
                    return matchesId && matchesQuery;
                })
                .map(this::mapToDto)
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = (int) Math.min((pageable.getOffset() + pageable.getPageSize()), filteredCategoryDtos.size());

        if (start > filteredCategoryDtos.size()) {
            return new PageImpl<>(List.of(), pageable, filteredCategoryDtos.size());
        }

        return new PageImpl<>(filteredCategoryDtos.subList(start, end), pageable, filteredCategoryDtos.size());
    }

    @Transactional(readOnly = true)
    public List<org.friesoft.porturl.dto.Application> getApplicationsByCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // Get visible application IDs for current user
        List<org.friesoft.porturl.dto.ApplicationWithRolesDto> visibleAppDtos = applicationService.getApplicationsForCurrentUser(PageRequest.of(0, Integer.MAX_VALUE), null).getContent();
        Set<Long> visibleAppIds = visibleAppDtos.stream()
                .map(dto -> dto.getApplication().getId())
                .collect(Collectors.toSet());

        if (category.getApplications() == null) {
            return List.of();
        }

        List<Application> filteredApps = category.getApplications().stream()
                .filter(Objects::nonNull)
                .filter(app -> visibleAppIds.contains(app.getId()))
                .collect(Collectors.toCollection(ArrayList::new));

        if (category.getApplicationSortMode() == Category.SortMode.ALPHABETICAL) {
            NaturalOrderComparator comparator = new NaturalOrderComparator();
            filteredApps.sort((a, b) -> {
                String nameA = a.getName() != null ? a.getName() : "";
                String nameB = b.getName() != null ? b.getName() : "";
                return comparator.compare(nameA, nameB);
            });
        }

        return filteredApps.stream()
                .map(applicationService::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void reorderApplicationsInCategory(Long categoryId, List<Long> applicationIds) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        List<Application> reorderedApps = applicationRepository.findAllById(applicationIds);
        // Ensure the order matches the incoming list
        Map<Long, Application> appMap = reorderedApps.stream().collect(Collectors.toMap(Application::getId, a -> a));
        List<Application> sortedApps = applicationIds.stream()
                .map(appMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        category.setApplications(sortedApps);
        categoryRepository.save(category);
    }

    public org.friesoft.porturl.dto.Category mapToDto(Category category) {
        org.friesoft.porturl.dto.Category dto = new org.friesoft.porturl.dto.Category();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setSortOrder(category.getSortOrder());
        dto.setApplicationSortMode(org.friesoft.porturl.dto.Category.ApplicationSortModeEnum.fromValue(category.getApplicationSortMode().name()));
        dto.setDescription(category.getDescription());

        return dto;
    }
}
