package org.friesoft.porturl.controller;

import org.friesoft.porturl.entities.Category;
import org.friesoft.porturl.repositories.CategoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryRepository repository;

    public CategoryController(CategoryRepository repository) {
        this.repository = repository;
    }

    /**
     * Finds and returns all ENABLED categories, sorted by their defined sort order.
     */
    @GetMapping
    public Iterable<Category> findAll() {
        return this.repository.findByEnabledTrueOrderBySortOrderAsc();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Category> findById(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Creates a new category.
     */
    @PostMapping
    public Category addCategory(@RequestBody Category category) {
        return this.repository.save(category);
    }

    /**
     * Updates an existing category's properties.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(@PathVariable Long id, @RequestBody Category updatedCategory) {
        return repository.findById(id)
                .map(category -> {
                    // Update all mutable fields, including the new ones.
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
     * Deletes a category by its ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}

