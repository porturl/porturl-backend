package org.friesoft.porturl.dto;

import lombok.Data;
import org.friesoft.porturl.entities.ApplicationCategory;

import java.util.List;
import java.util.Set;

@Data
public class ApplicationUpdateRequest {
    private String name;
    private String url;
    private String iconLarge;
    private String iconMedium;
    private String iconThumbnail;
    private Set<ApplicationCategory> applicationCategories;
    private List<String> availableRoles;
}