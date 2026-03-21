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
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class CategoryController extends BaseController implements CategoryApi {

    private final CategoryRepository repository;
    private final CategoryService categoryService;
    private final ApplicationService applicationService;

    public CategoryController(CategoryRepository repository, CategoryService categoryService, ApplicationService applicationService) {
        this.repository = repository;
        this.categoryService = categoryService;
        this.applicationService = applicationService;
    }

    @Override
    public ResponseEntity<List<org.friesoft.porturl.dto.Category>> findAllCategories(Integer page, Integer size, List<String> sort, String filter, String range) {
        return ok(categoryService.getVisibleCategories(getPageable(page, size, sort, range), getFilterMap(filter)), "categories");
    }

    @Override
    public ResponseEntity<org.friesoft.porturl.dto.Category> findCategoryById(Long id) {
        return repository.findById(id)
                .map(categoryService::mapToDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<org.friesoft.porturl.dto.Category> addCategory(@RequestBody org.friesoft.porturl.dto.Category categoryDto) {
        try {
            Category category = new Category();
            category.setName(categoryDto.getName());
            
            if (categoryDto.getSortOrder() != null) {
                category.setSortOrder(categoryDto.getSortOrder());
            } else {
                Integer maxOrder = repository.findMaxSortOrder();
                category.setSortOrder(maxOrder != null ? maxOrder + 1 : 0);
            }
            
            if (categoryDto.getApplicationSortMode() != null) {
                category.setApplicationSortMode(Category.SortMode.valueOf(categoryDto.getApplicationSortMode().getValue()));
            }
            category.setDescription(categoryDto.getDescription());

            Category savedCategory = this.repository.save(category);
            return ResponseEntity.status(201).body(categoryService.mapToDto(savedCategory));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @Override
    @Transactional
    public ResponseEntity<org.friesoft.porturl.dto.Category> updateCategory(Long id, @RequestBody org.friesoft.porturl.dto.Category updatedCategoryDto) {
        Category category = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        category.setName(updatedCategoryDto.getName());
        if (updatedCategoryDto.getSortOrder() != null) {
            category.setSortOrder(updatedCategoryDto.getSortOrder());
        }
        if (updatedCategoryDto.getApplicationSortMode() != null) {
            category.setApplicationSortMode(Category.SortMode.valueOf(updatedCategoryDto.getApplicationSortMode().getValue()));
            applicationService.enforceApplicationSortOrder(category);
        }
        category.setDescription(updatedCategoryDto.getDescription());

        Category savedCategory = repository.saveAndFlush(category);
        return ResponseEntity.ok(categoryService.mapToDto(savedCategory));
    }

    @Override
    @Transactional
    public ResponseEntity<Void> reorderCategories(@RequestBody List<org.friesoft.porturl.dto.CategoryReorderRequest> requests) {
        for (org.friesoft.porturl.dto.CategoryReorderRequest req : requests) {
            repository.findById(req.getId()).ifPresent(category -> {
                category.setSortOrder(req.getSortOrder());
                repository.save(category);
            });
        }
        return ResponseEntity.ok().build();
    }

    @Override
    @Transactional
    public ResponseEntity<Void> deleteCategory(Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @Override
    public ResponseEntity<List<org.friesoft.porturl.dto.Application>> findApplicationsByCategory(Long id) {
        return ResponseEntity.ok(categoryService.getApplicationsByCategory(id));
    }

    @Override
    public ResponseEntity<Void> reorderApplicationsInCategory(Long id, @RequestBody List<Long> applicationIds) {
        categoryService.reorderApplicationsInCategory(id, applicationIds);
        return ResponseEntity.ok().build();
    }
}
