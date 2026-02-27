package org.friesoft.porturl.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@NoArgsConstructor
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
    private SortMode applicationSortMode = SortMode.ALPHABETICAL;

    /**
     * An optional description for the category, which can be displayed as a tooltip.
     */
    @Column(nullable = true, length = 512)
    private String description;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "application_category_link",
        joinColumns = @JoinColumn(name = "category_id"),
        inverseJoinColumns = @JoinColumn(name = "application_id")
    )
    @OrderColumn(name = "sort_order")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private java.util.List<Application> applications = new java.util.ArrayList<>();
}

