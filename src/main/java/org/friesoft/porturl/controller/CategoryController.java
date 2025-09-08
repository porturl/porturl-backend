package org.friesoft.porturl.controller;

import org.friesoft.porturl.entities.Category;
import org.friesoft.porturl.repositories.CategoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

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
     * @return A 201 Created response with the location of the new resource.
     */
    @PostMapping
    public ResponseEntity<Category> addCategory(@RequestBody Category category) {
        // Ensure the ID is null so it's treated as a new entity
        category.setId(null);
        Category savedCategory = this.repository.save(category);

        // Return a 201 Created status with a Location header
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedCategory.getId())
                .toUri();

        return ResponseEntity.created(location).body(savedCategory);
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

