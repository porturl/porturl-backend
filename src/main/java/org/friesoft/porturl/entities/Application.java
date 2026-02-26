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

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String url;

    @ManyToMany(mappedBy = "applications", fetch = FetchType.EAGER)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private java.util.List<Category> categories = new java.util.ArrayList<>();

    @Column(nullable = true)
    private String icon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = true)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private User createdBy;

    @Column(nullable = true)
    private String clientId;

    @Column(nullable = true)
    private String realm;

    @Transient
    public String getIconUrl() {
        return buildIconUrl(this.icon);
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