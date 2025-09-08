package org.friesoft.porturl.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Category {

    public enum SortMode {
        CUSTOM, // Sort by the application's 'sortOrder' field
        ALPHABETICAL // Sort alphabetically by the application's 'name'
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, name = "sort_order")
    private Integer sortOrder = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SortMode applicationSortMode = SortMode.CUSTOM;

    /**
     * An identifier for an icon to be displayed next to the category title.
     * e.g., "fas fa-server" for a Font Awesome icon.
     */
    @Column(nullable = true)
    private String icon;

    /**
     * An optional description for the category, which can be displayed as a tooltip.
     */
    @Column(nullable = true, length = 512)
    private String description;

    /**
     * A flag to easily enable or disable the display of the entire category.
     */
    @Column(nullable = false)
    private boolean enabled = true;

    // These annotations break the recursive loop
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToMany(mappedBy = "categories")
    @JsonIgnore
    private Set<Application> applications = new HashSet<>();
}

