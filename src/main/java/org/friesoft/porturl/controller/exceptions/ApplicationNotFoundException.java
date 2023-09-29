package org.friesoft.porturl.controller.exceptions;

public class ApplicationNotFoundException extends RuntimeException {
    public ApplicationNotFoundException(Long id) {
        super("Could not find application " + id);
    }
}
