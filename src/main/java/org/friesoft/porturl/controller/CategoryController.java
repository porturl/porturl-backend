package org.friesoft.porturl.controller;

import org.friesoft.porturl.entities.Category;
import org.friesoft.porturl.repositories.CategoryRepository;
import org.friesoft.porturl.service.CategoryService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.lang.NonNull;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryRepository repository;
    private final CategoryService categoryService;

    public CategoryController(CategoryRepository repository, CategoryService categoryService) {
        this.repository = repository;
        this.categoryService = categoryService;
    }

    /**
     * Finds and returns all categories that are visible to the current user.
     * A category is visible if it is enabled and contains at least one application
     * that the user has permission to see.
     */
    @GetMapping
    public Iterable<Category> findAll() {
        return this.categoryService.getVisibleCategories();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Category> findById(@PathVariable @NonNull Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Creates a new category.
     *
     * @param category The category object from the request body.
     * @return A 201 Created response with the location of the new resource,
     *         or a 409 Conflict if the name already exists.
     */
    @PostMapping
    public ResponseEntity<?> addCategory(@RequestBody Category category) {
        try {
            category.setId(null);
            Category savedCategory = this.repository.save(category);

            URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(savedCategory.getId())
                    .toUri();

            return ResponseEntity.created(location).body(savedCategory);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body("A category with this name already exists.");
        }
    }

    /**
     * Updates an existing category's properties.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(@PathVariable @NonNull Long id,
            @RequestBody Category updatedCategory) {
        return repository.findById(id)
                .map(category -> {
                    category.setName(updatedCategory.getName());
                    category.setSortOrder(updatedCategory.getSortOrder());
                    category.setApplicationSortMode(updatedCategory.getApplicationSortMode());
                    category.setIcon(updatedCategory.getIcon());
                    category.setDescription(updatedCategory.getDescription());
                    category.setEnabled(updatedCategory.isEnabled());
                    return ResponseEntity.ok(repository.save(category));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Handles batch updates for reordering categories.
     */
    @PostMapping("/reorder")
    @Transactional
    public ResponseEntity<Void> reorderCategories(@RequestBody List<Category> categories) {
        var categoryMap = repository.findAllById(categories.stream().map(Category::getId).toList());
        var mappedCategories = new java.util.HashMap<Long, Category>();
        categoryMap.forEach(c -> mappedCategories.put(c.getId(), c));

        for (Category cat : categories) {
            mappedCategories.get(cat.getId()).setSortOrder(cat.getSortOrder());
        }

        repository.saveAll(mappedCategories.values());

        return ResponseEntity.ok().build();
    }

    /**
     * Deletes a category by its ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable @NonNull Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
