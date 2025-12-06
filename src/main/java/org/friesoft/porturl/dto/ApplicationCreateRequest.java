package org.friesoft.porturl.dto;

import lombok.Data;
import org.friesoft.porturl.entities.ApplicationCategory;

import java.util.List;
import java.util.Set;

@Data
public class ApplicationCreateRequest {
    private String name;
    private String url;
    private List<String> roles; // e.g., ["admin", "viewer"]
    private Set<ApplicationCategory> applicationCategories; // The missing field
}
