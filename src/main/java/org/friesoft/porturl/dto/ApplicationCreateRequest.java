package org.friesoft.porturl.dto;

import lombok.Data;
import java.util.List;

@Data
public class ApplicationCreateRequest {
    private String name;
    private String url;
    private List<String> roles; // e.g., ["admin", "viewer"]
}
