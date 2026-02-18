package org.friesoft.porturl.repositories;

import org.friesoft.porturl.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByProviderUserId(String providerUserId);
    Optional<User> findByEmail(String email);
}
