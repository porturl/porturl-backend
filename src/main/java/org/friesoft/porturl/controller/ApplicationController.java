package org.friesoft.porturl.controller;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.friesoft.porturl.controller.exceptions.ApplicationNotFoundException;
import org.friesoft.porturl.entities.Application;
import org.friesoft.porturl.entities.ApplicationCategory;
import org.friesoft.porturl.entities.Category;
import org.friesoft.porturl.repositories.ApplicationRepository;
import org.friesoft.porturl.repositories.CategoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationRepository repository;
    private final CategoryRepository categoryRepository;
    private final EntityManager entityManager;

    public ApplicationController(ApplicationRepository repository, CategoryRepository categoryRepository, EntityManager entityManager) {
        this.repository = repository;
        this.categoryRepository = categoryRepository;
        this.entityManager = entityManager;
    }

    /**
     * Finds and returns all Application entities.
     * The client is responsible for sorting and grouping.
     * @return An iterable collection of all applications.
     */
    @GetMapping
    public Iterable<Application> findAll() {
        return this.repository.findAll();
    }

    /**
     * Finds a single Application by its ID.
     * @param id The ID of the application to find.
     * @return The found Application.
     * @throws ApplicationNotFoundException if no application with the given ID exists.
     */
    @GetMapping("/{id}")
    public Application findOne(@PathVariable Long id) {
        return this.repository.findById(id)
                .orElseThrow(() -> new ApplicationNotFoundException(id));
    }

    /**
     * Creates a new Application. It correctly handles linking to existing categories.
     */
    @PostMapping
    @Transactional
    public Application addApplication(@RequestBody Application newApplication) {
        // A temporary set to hold the correctly managed relationship objects.
        Set<ApplicationCategory> managedAppCategories = new HashSet<>();

        // Iterate over the incoming, detached category relationships from the JSON payload.
        for (ApplicationCategory ac : newApplication.getApplicationCategories()) {
            // Fetch the managed instance of the Category from the database using its ID.
            Category managedCategory = categoryRepository.findById(ac.getCategory().getId())
                    .orElseThrow(() -> new RuntimeException("Category not found with id: " + ac.getCategory().getId()));

            // Create a new link entity.
            ApplicationCategory newLink = new ApplicationCategory();
            newLink.setApplication(newApplication); // Link to the new application being created.
            newLink.setCategory(managedCategory);  // Link to the existing, managed category.
            newLink.setSortOrder(ac.getSortOrder()); // Copy the sort order from the request.

            managedAppCategories.add(newLink);
        }

        // Replace the detached collection on our new Application with the managed one.
        newApplication.setApplicationCategories(managedAppCategories);

        Application savedApplication = this.repository.save(newApplication);

        // Explicitly flush the changes to the database to ensure they are visible
        // to subsequent transactions/requests immediately.
        entityManager.flush();

        // After saving, re-fetch the entity to ensure transient properties are loaded correctly
        Application freshApplication = repository.findById(savedApplication.getId())
                .orElseThrow(() -> new ApplicationNotFoundException(savedApplication.getId()));

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(freshApplication.getId())
                .toUri();
        return ResponseEntity.created(location).body(freshApplication).getBody();

    }

    /**
     * Deletes an Application by its ID.
     * @param id The ID of the application to delete.
     * @return An OK response if the deletion was successful.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(@PathVariable Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Updates an existing Application. This method handles updating simple properties as well as
     * the complex many-to-many relationship with Categories.
     * @param newApplication The updated Application object from the request body.
     * @param id The ID of the application to update.
     * @return The updated Application.
     */
    @PutMapping("/{id}")
    @Transactional
    public Application replaceApplication(@RequestBody Application newApplication, @PathVariable Long id) {
        Application updatedApplication = repository.findById(id)
                .map(application -> {
                    // Update simple properties
                    application.setName(newApplication.getName());
                    application.setUrl(newApplication.getUrl());
                    application.setIconLarge(newApplication.getIconLarge());
                    application.setIconMedium(newApplication.getIconMedium());
                    application.setIconThumbnail(newApplication.getIconThumbnail());

                    // 1. Create a map of the incoming category links for easy lookup.
                    Map<Long, ApplicationCategory> incomingLinks = newApplication.getApplicationCategories().stream()
                            .collect(Collectors.toMap(ac -> ac.getCategory().getId(), Function.identity()));

                    // 2. Remove links that are no longer present in the incoming data.
                    application.getApplicationCategories().removeIf(
                            existingLink -> !incomingLinks.containsKey(existingLink.getCategory().getId())
                    );

                    // 3. Update existing links and add new ones.
                    incomingLinks.forEach((catId, incomingLink) -> {
                        Category managedCategory = categoryRepository.findById(catId)
                                .orElseThrow(() -> new RuntimeException("Category not found"));

                        application.getApplicationCategories().stream()
                                .filter(link -> link.getCategory().getId().equals(catId))
                                .findFirst()
                                .ifPresentOrElse(
                                        // If a link for this category already exists, just update its sort order.
                                        existingLink -> existingLink.setSortOrder(incomingLink.getSortOrder()),
                                        // If it's a new link, create and add it.
                                        () -> {
                                            ApplicationCategory newLink = new ApplicationCategory();
                                            newLink.setApplication(application);
                                            newLink.setCategory(managedCategory);
                                            newLink.setSortOrder(incomingLink.getSortOrder());
                                            application.getApplicationCategories().add(newLink);
                                        }
                                );
                    });

                    // The save operation will now correctly handle the merged collection.
                    return repository.save(application);
                })
                .orElseThrow(() -> new ApplicationNotFoundException(id));

        // Explicitly flush the changes to the database.
        entityManager.flush();

        // After saving, re-fetch the entity to ensure transient properties are loaded correctly
        return repository.findById(updatedApplication.getId())
                .orElseThrow(() -> new ApplicationNotFoundException(updatedApplication.getId()));

    }

    /**
     * An efficient endpoint for batch-updating the sort order and category
     * memberships of multiple applications at once.
     * @param applications A list of Application objects with their new state.
     * @return An OK response if the update was successful.
     */
    @PostMapping("/reorder")
    @Transactional
    public ResponseEntity<Void> reorderApplications(@RequestBody List<Application> applications) {
        // A more robust implementation would fetch all entities first and then update them,
        // but for simplicity, we'll iterate and call the existing update logic.
        for (Application app : applications) {
            replaceApplication(app, app.getId());
        }
        return ResponseEntity.ok().build();
    }
}

