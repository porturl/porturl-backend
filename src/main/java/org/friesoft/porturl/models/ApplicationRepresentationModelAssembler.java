package org.friesoft.porturl.models;

import org.friesoft.porturl.controller.ApplicationController;
import org.friesoft.porturl.entities.Application;
import org.springframework.stereotype.Component;

@Component
public class ApplicationRepresentationModelAssembler extends SimpleIdentifiableRepresentationModelAssembler<Application> {

    ApplicationRepresentationModelAssembler() {
        super(ApplicationController.class);
    }
}
