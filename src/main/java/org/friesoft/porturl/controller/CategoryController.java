package org.friesoft.porturl.controller;

import org.friesoft.porturl.entities.Category;
import org.friesoft.porturl.repositories.CategoryRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

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
     * @param category The category object from the request body.
     * @return A 201 Created response with the location of the new resource,
     * or a 409 Conflict if the name already exists.
     */
    @PostMapping
    public ResponseEntity<?> addCategory(@RequestBody Category category) {
        try {
            // Ensure the ID is null so it's treated as a new entity
            category.setId(null);
            Category savedCategory = this.repository.save(category);

            // Return a 201 Created status with a Location header
            URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(savedCategory.getId())
                    .toUri();

            return ResponseEntity.created(location).body(savedCategory);
        } catch (DataIntegrityViolationException e) {
            // This will be caught when the unique constraint on the name is violated.
            return ResponseEntity
                    .status(HttpStatus.CONFLICT) // 409 Conflict is a good status for this.
                    .body("A category with this name already exists.");
        }
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
     * Handles batch updates for reordering categories.
     * This is more efficient than sending individual update requests for each move.
     */
    @PostMapping("/reorder")
    @Transactional // Ensures all updates are part of a single database transaction
    public ResponseEntity<Void> reorderCategories(@RequestBody List<Category> categories) {
        // Create a map for quick lookups
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
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}

