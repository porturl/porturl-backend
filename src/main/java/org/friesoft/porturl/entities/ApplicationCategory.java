package org.friesoft.porturl.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * A custom join entity that represents the relationship between an Application and a Category.
 * This entity is crucial for storing metadata about the relationship, such as the per-category sort order.
 */
@Entity
@Data
public class ApplicationCategory {

    // A composite primary key uniquely identifies the relationship.
    @EmbeddedId
    private ApplicationCategoryId id = new ApplicationCategoryId();

    // The "many" side of the relationship back to the Application.
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("applicationId")
    @JsonBackReference // Prevents infinite recursion during JSON serialization.
    private Application application;

    // The "many" side of the relationship to the Category.
    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("categoryId")
    private Category category;

    // The sort order for this specific application within this specific category.
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    /**
     * A simple class that defines the composite primary key.
     */
    @Embeddable
    @Data
    @EqualsAndHashCode
    public static class ApplicationCategoryId implements Serializable {
        private Long applicationId;
        private Long categoryId;
    }
}

