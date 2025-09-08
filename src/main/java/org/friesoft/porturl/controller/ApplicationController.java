package org.friesoft.porturl.controller;

import org.friesoft.porturl.controller.exceptions.ApplicationNotFoundException;
import org.friesoft.porturl.entities.Application;
import org.friesoft.porturl.repositories.ApplicationRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationRepository repository;

    public ApplicationController(ApplicationRepository repository) {

        this.repository = repository;
    }

    @GetMapping
    public Iterable<Application> findAll() {
        // TODO: find a more sophisticated sorting method, but for now, we sort by the application's own sortOrder.
        return this.repository.findAllByOrderBySortOrderAsc();
    }

    @GetMapping("/{id}")
    public Application findOne(@PathVariable Long id) {
        return this.repository.findById(id).orElseThrow(() -> new ApplicationNotFoundException(id));
    }

    @PostMapping
    Application addApplication(@RequestBody Application application) {
        return this.repository.save(application);
    }

    @DeleteMapping(value = "/{id}")
    public void deleteApplication(@PathVariable Long id) {
        this.repository.deleteById(id);
    }

    @PutMapping("/{id}")
    Application replaceApplication(@RequestBody Application newApplication, @PathVariable Long id) {
        return repository.findById(id)
                .map(application -> {
                    application.setName(newApplication.getName());
                    application.setUrl(newApplication.getUrl());
                    application.setSortOrder(newApplication.getSortOrder());
                    application.setCategories(newApplication.getCategories());
                    application.setIconLarge(newApplication.getIconLarge());
                    application.setIconMedium(newApplication.getIconMedium());
                    application.setIconThumbnail(newApplication.getIconThumbnail());

                    return repository.save(application);
                })
                .orElseGet(() -> {
                    newApplication.setId(id);
                    return repository.save(newApplication);
                });
    }

}