package com.projet.archi.authservice.service;

import com.projet.archi.authservice.dto.RegisterRequest;
import com.projet.archi.authservice.model.User;
import com.projet.archi.authservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.projet.archi.authservice.dto.LoginRequest;
import com.projet.archi.authservice.security.JwtUtil;
import java.util.UUID;
import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private JwtUtil jwtUtil;

    public User registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Cet email est déjà utilisé");
        }

        User user = new User(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword())
        );

        // Generate a simple verification token to send over RabbitMQ
        String verificationToken = UUID.randomUUID().toString();
        user.setVerificationToken(verificationToken);
        
        User savedUser = userRepository.save(user);

        // Publish event to RabbitMQ
        String message = "{\"email\":\"" + savedUser.getEmail() + "\", \"token\":\"" + verificationToken + "\"}";
        rabbitTemplate.convertAndSend("authExchange", "user.registered", message);

        return savedUser;
    }

    public boolean verifyToken(String token) {
        return userRepository.findByVerificationToken(token).map(user -> {
            user.setVerified(true);
            user.setVerificationToken(null);
            userRepository.save(user);
            return true;
        }).orElse(false);
    }

    public String login(LoginRequest request) {
        Optional<User> optionalUser = userRepository.findByEmail(request.getEmail());
        
        if (optionalUser.isEmpty()) {
            throw new RuntimeException("Utilisateur non trouvé");
        }
        
        User user = optionalUser.get();
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Mot de passe incorrect");
        }
        
        if (!user.isVerified()) {
            throw new RuntimeException("Veuillez d'abord vérifier votre compte par e-mail");
        }
        
        return jwtUtil.generateToken(user.getEmail());
    }
}
