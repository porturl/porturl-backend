package org.friesoft.porturl.dto;

import lombok.Data;
import org.friesoft.porturl.entities.Application;

import java.util.List;

@Data
public class ApplicationWithRolesDto {
    private Application application;
    private List<String> availableRoles;

    public ApplicationWithRolesDto(Application application, List<String> availableRoles) {
        this.application = application;
        this.availableRoles = availableRoles;
    }
}
