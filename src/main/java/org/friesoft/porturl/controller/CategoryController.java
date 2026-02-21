package org.friesoft.porturl.controller;

import org.friesoft.porturl.api.CategoryApi;
import org.friesoft.porturl.entities.Category;
import org.friesoft.porturl.repositories.CategoryRepository;
import org.friesoft.porturl.service.CategoryService;
import org.friesoft.porturl.service.ApplicationService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class CategoryController implements CategoryApi {

    private final CategoryRepository repository;
    private final CategoryService categoryService;
    private final ApplicationService applicationService;

    public CategoryController(CategoryRepository repository, CategoryService categoryService, ApplicationService applicationService) {
        this.repository = repository;
        this.categoryService = categoryService;
        this.applicationService = applicationService;
    }

    @Override
    public ResponseEntity<List<org.friesoft.porturl.dto.Category>> findAllCategories() {
        List<org.friesoft.porturl.dto.Category> dtos = this.categoryService.getVisibleCategories().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @Override
    public ResponseEntity<org.friesoft.porturl.dto.Category> findCategoryById(Long id) {
        return repository.findById(id)
                .map(this::mapToDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<org.friesoft.porturl.dto.Category> addCategory(@RequestBody org.friesoft.porturl.dto.Category categoryDto) {
        try {
            Category category = new Category();
            category.setName(categoryDto.getName());
            category.setSortOrder(categoryDto.getSortOrder());
            if (categoryDto.getApplicationSortMode() != null) {
                category.setApplicationSortMode(Category.SortMode.valueOf(categoryDto.getApplicationSortMode().getValue()));
            }
            category.setDescription(categoryDto.getDescription());

            Category savedCategory = this.repository.save(category);
            return ResponseEntity.status(201).body(mapToDto(savedCategory));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @Override
    public ResponseEntity<org.friesoft.porturl.dto.Category> updateCategory(Long id, @RequestBody org.friesoft.porturl.dto.Category updatedCategoryDto) {
        return repository.findById(id)
                .map(category -> {
                    category.setName(updatedCategoryDto.getName());
                    category.setSortOrder(updatedCategoryDto.getSortOrder());
                    if (updatedCategoryDto.getApplicationSortMode() != null) {
                        category.setApplicationSortMode(Category.SortMode.valueOf(updatedCategoryDto.getApplicationSortMode().getValue()));
                        applicationService.enforceApplicationSortOrder(category);
                    }
                    category.setDescription(updatedCategoryDto.getDescription());
                    return ResponseEntity.ok(mapToDto(repository.save(category)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    @Transactional
    public ResponseEntity<Void> reorderCategories(@RequestBody List<org.friesoft.porturl.dto.Category> categories) {
        var categoryMap = repository.findAllById(categories.stream().map(org.friesoft.porturl.dto.Category::getId).toList());
        var mappedCategories = new java.util.HashMap<Long, Category>();
        categoryMap.forEach(c -> mappedCategories.put(c.getId(), c));

        for (org.friesoft.porturl.dto.Category cat : categories) {
            if (mappedCategories.containsKey(cat.getId())) {
                Category category = mappedCategories.get(cat.getId());
                category.setSortOrder(cat.getSortOrder());
                if (cat.getApplicationSortMode() != null) {
                    category.setApplicationSortMode(Category.SortMode.valueOf(cat.getApplicationSortMode().getValue()));
                }
            }
        }

        repository.saveAll(mappedCategories.values());
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> deleteCategory(Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    private org.friesoft.porturl.dto.Category mapToDto(Category category) {
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
