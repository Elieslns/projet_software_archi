package com.projet.archi.authservice.service;

import com.projet.archi.authservice.dto.RegisterRequest;
import com.projet.archi.authservice.model.User;
import com.projet.archi.authservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public User registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Cet email est déjà utilisé");
        }

        User user = new User(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword())
        );

        User savedUser = userRepository.save(user);

        // Generate a simple verification token to send over RabbitMQ
        String verificationToken = UUID.randomUUID().toString();
        
        // Publish event to RabbitMQ
        String message = "{\"email\":\"" + savedUser.getEmail() + "\", \"token\":\"" + verificationToken + "\"}";
        rabbitTemplate.convertAndSend("authExchange", "user.registered", message);

        return savedUser;
    }
}
