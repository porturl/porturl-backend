package org.friesoft.porturl.repositories;

import org.friesoft.porturl.entities.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository interface for managing Application entities.
 * Extends JpaRepository to provide standard CRUD operations.
 */
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    /**
     * Finds all Application entities and sorts them by their 'sortOrder' field
     * in ascending order. This provides a default, predictable ordering for the applications.
     *
     * @return A list of all applications, sorted by their display order.
     */
    List<Application> findAllByOrderBySortOrderAsc();
}
