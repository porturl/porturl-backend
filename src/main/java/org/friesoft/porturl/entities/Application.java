package org.friesoft.porturl.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@NoArgsConstructor
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

    // an application can have multiple categories (tags).
    // This defines the "one" side of the One-to-Many relationship with the join entity.
    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference // Manages the "forward" part of the relationship for JSON serialization.
    @EqualsAndHashCode.Exclude // Excludes this collection from Lombok's generated methods to prevent recursion.
    @ToString.Exclude
    private Set<ApplicationCategory> applicationCategories = new HashSet<>();

    @Column(nullable = true)
    private String iconLarge;

    @Column(nullable = true)
    private String iconMedium;

    @Column(nullable = true)
    private String iconThumbnail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private User createdBy;

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
