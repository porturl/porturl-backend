package org.friesoft.porturl.controller;

import org.friesoft.porturl.controller.exceptions.ApplicationNotFoundException;
import org.friesoft.porturl.entities.Application;
import org.friesoft.porturl.repositories.ApplicationRepository;
import org.springframework.web.bind.annotation.*;

@RestController
public class ApplicationController {

    private final ApplicationRepository repository;

    public ApplicationController(ApplicationRepository repository) {

        this.repository = repository;
    }

    @GetMapping("/applications")
    public Iterable<Application> findAll() {
        return this.repository.findAll();
    }

    @GetMapping("/applications/{id}")
    public Application findOne(@PathVariable Long id) {
        return this.repository.findById(id).orElseThrow(() -> new ApplicationNotFoundException(id));
    }

    @PostMapping("/applications")
    Application addApplication(@RequestBody Application application) {
        return this.repository.save(application);
    }

    @DeleteMapping(value = "/applications/{id}")
    public void deleteApplication(@PathVariable Long id) {
        this.repository.deleteById(id);
    }

    @PutMapping("/applications/{id}")
    Application replaceApplication(@RequestBody Application newApplication, @PathVariable Long id) {
        return repository.findById(id)
                .map(application -> {
                    application.setName(newApplication.getName());
                    application.setUrl(newApplication.getUrl());
                    return repository.save(application);
                })
                .orElseGet(() -> {
                    newApplication.setId(id);
                    return repository.save(newApplication);
                });
    }

}