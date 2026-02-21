package org.friesoft.porturl.repositories;

import org.friesoft.porturl.entities.Application;
import org.friesoft.porturl.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    /**
     * Finds all categories and sorts them by their 'sortOrder' field in ascending order.
     */
    List<Category> findAllByOrderBySortOrderAsc();

    /**
     * Finds all distinct categories that are associated with a given list of applications.
     * This is the core method for security-trimmed category filtering.
     *
     * @param applications The list of applications the user is allowed to see.
     * @return A sorted list of visible and relevant categories.
     */
    @Query("SELECT DISTINCT c FROM Category c JOIN c.applications a WHERE a IN :applications ORDER BY c.sortOrder ASC")
    List<Category> findDistinctByApplicationInOrderBySortOrderAsc(@Param("applications") java.util.Collection<Application> applications);
}
