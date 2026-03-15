package com.projet.archi.authservice.repository;

import com.projet.archi.authservice.model.Identity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface IdentityRepository extends JpaRepository<Identity, Long> {
    Optional<Identity> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<Identity> findByVerificationToken(String token);
}
