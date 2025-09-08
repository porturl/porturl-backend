package org.friesoft.porturl.repositories;

import org.friesoft.porturl.entities.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository interface for managing Application entities.
 * Extends JpaRepository to provide standard CRUD operations.
 */
public interface ApplicationRepository extends JpaRepository<Application, Long> {

}
