package org.friesoft.porturl.controller;

import org.friesoft.porturl.entities.Application;
import org.friesoft.porturl.models.ApplicationRepresentationModelAssembler;
import org.friesoft.porturl.repositories.ApplicationRepository;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@CrossOrigin(origins = "http://localhost:22080")
public class ApplicationController {

    private final ApplicationRepository repository;
    private final ApplicationRepresentationModelAssembler assembler;

    public ApplicationController(ApplicationRepository repository, ApplicationRepresentationModelAssembler assembler) {

        this.repository = repository;
        this.assembler = assembler;
    }

    @GetMapping("/applications")
    public ResponseEntity<CollectionModel<EntityModel<Application>>> findAll() {

        return ResponseEntity.ok( //
                this.assembler.toCollectionModel(this.repository.findAll()));

    }

    @GetMapping("/applications/{id}")
    public ResponseEntity<EntityModel<Application>> findOne(@PathVariable Long id) {

        return this.repository.findById(id) //
                .map(this.assembler::toModel) //
                .map(ResponseEntity::ok) //
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/applications")
    ResponseEntity<?> newApplication(@RequestBody Application application) {

        try {
            Application savedApplication = this.repository.save(application);

            EntityModel<Application> applicationResource = EntityModel.of(savedApplication, linkTo(methodOn(ApplicationController.class).findOne(savedApplication.getId())).withSelfRel());

            return ResponseEntity //
                    .created(new URI(applicationResource.getRequiredLink(IanaLinkRelations.SELF).getHref()))
                    .body(applicationResource);
        } catch (URISyntaxException e) {
            return ResponseEntity.badRequest().body("Unable to create " + application);
        }
    }

}