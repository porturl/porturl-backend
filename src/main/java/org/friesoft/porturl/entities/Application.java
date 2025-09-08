package org.friesoft.porturl.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String url;

    // default display order of applications.
    @Column(nullable = false, name = "sort_order")
    private Integer sortOrder = 0;

    // an application can have multiple categories (tags).
    @ManyToMany(fetch = FetchType.EAGER)
    // These annotations break the recursive loop in equals/hashCode/toString
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JoinTable(
            name = "application_categories",
            joinColumns = @JoinColumn(name = "application_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories = new HashSet<>();

    @Column(nullable = true)
    private String iconLarge;

    @Column(nullable = true)
    private String iconMedium;

    @Column(nullable = true)
    private String iconThumbnail;

    // --- Transient fields to provide full image URLs to the client ---

    @Transient
    public String getIconUrlLarge() {
        return buildIconUrl(this.iconLarge);
    }

    @Transient
    public String getIconUrlMedium() {
        return buildIconUrl(this.iconMedium);
    }

    @Transient
    public String getIconUrlThumbnail() {
        return buildIconUrl(this.iconThumbnail);
    }

    private String buildIconUrl(String filename) {
        if (filename == null || filename.isBlank()) {
            return null;
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/images/")
                .path(filename)
                .toUriString();
    }
}

