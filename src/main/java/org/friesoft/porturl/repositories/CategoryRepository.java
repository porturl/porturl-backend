package org.friesoft.porturl.repositories;

import org.friesoft.porturl.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    /**
     * Finds all categories and sorts them by their 'sortOrder' field in ascending order.
     * This might be useful for an admin panel that needs to show all categories, including disabled ones.
     */
    List<Category> findAllByOrderBySortOrderAsc();

    /**
     * Finds all ENABLED categories and sorts them by their 'sortOrder' field.
     * This is the primary method for fetching categories for the main dashboard display.
     *
     * @return A sorted list of all enabled categories.
     */
    List<Category> findByEnabledTrueOrderBySortOrderAsc();
}

