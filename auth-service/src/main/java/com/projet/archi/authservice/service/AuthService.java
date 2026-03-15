package com.projet.archi.authservice.service;

import com.projet.archi.authservice.dto.RegisterRequest;
import com.projet.archi.authservice.dto.LoginRequest;
import com.projet.archi.authservice.model.Authority;
import com.projet.archi.authservice.model.Credential;
import com.projet.archi.authservice.model.Identity;
import com.projet.archi.authservice.model.Token;
import com.projet.archi.authservice.repository.AuthorityRepository;
import com.projet.archi.authservice.repository.CredentialRepository;
import com.projet.archi.authservice.repository.IdentityRepository;
import com.projet.archi.authservice.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.Optional;
import java.util.Collections;

@Service
public class AuthService {

    @Autowired
    private IdentityRepository identityRepository;

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private JwtUtil jwtUtil;

    @Transactional
    public Identity registerUser(RegisterRequest request) {
        if (identityRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Cet email est déjà utilisé");
        }

        // 1. Create Identity
        Identity identity = new Identity(request.getEmail());
        String verificationToken = UUID.randomUUID().toString();
        identity.setVerificationToken(verificationToken);

        // 2. Assign default Authority (ROLE_USER)
        Authority userRole = authorityRepository.findByName("ROLE_USER")
                .orElseGet(() -> authorityRepository.save(new Authority("ROLE_USER")));
        identity.setAuthorities(Collections.singleton(userRole));

        // 3. Create Credential linked to Identity and set it in Identity for Cascade
        Credential credential = new Credential(
                passwordEncoder.encode(request.getPassword()),
                identity
        );
        identity.setCredential(credential);

        // 4. Save Identity (Cascade will save Credential)
        Identity savedIdentity = identityRepository.save(identity);

        // Publish event to RabbitMQ
        String message = "{\"email\":\"" + savedIdentity.getEmail() + "\", \"token\":\"" + verificationToken + "\"}";
        rabbitTemplate.convertAndSend("authExchange", "user.registered", message);

        return savedIdentity;
    }

    public boolean verifyToken(String token) {
        return identityRepository.findByVerificationToken(token).map(identity -> {
            identity.setVerified(true);
            identity.setVerificationToken(null);
            identityRepository.save(identity);
            return true;
        }).orElse(false);
    }

    public Token login(LoginRequest request) {
        Identity identity = identityRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        
        Credential credential = credentialRepository.findByIdentity(identity)
                .orElseThrow(() -> new RuntimeException("Credentials non trouvés"));

        if (!passwordEncoder.matches(request.getPassword(), credential.getPassword())) {
            throw new RuntimeException("Mot de passe incorrect");
        }
        
        if (!identity.isVerified()) {
            throw new RuntimeException("Veuillez d'abord vérifier votre compte par e-mail");
        }
        
        return jwtUtil.generateToken(identity);
    }
}
