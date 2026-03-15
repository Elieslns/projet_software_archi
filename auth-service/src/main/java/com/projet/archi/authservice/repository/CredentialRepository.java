package com.projet.archi.authservice.repository;

import com.projet.archi.authservice.model.Credential;
import com.projet.archi.authservice.model.Identity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CredentialRepository extends JpaRepository<Credential, Long> {
    Optional<Credential> findByIdentity(Identity identity);
}
