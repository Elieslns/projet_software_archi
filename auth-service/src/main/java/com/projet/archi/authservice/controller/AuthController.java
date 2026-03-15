package com.projet.archi.authservice.controller;

import com.projet.archi.authservice.dto.RegisterRequest;
import com.projet.archi.authservice.dto.LoginRequest;
import com.projet.archi.authservice.model.Identity;
import com.projet.archi.authservice.model.Token;
import com.projet.archi.authservice.service.AuthService;
import com.projet.archi.authservice.security.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            Identity registeredIdentity = authService.registerUser(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Utilisateur créé avec succès. Un e-mail de vérification a été envoyé.");
            response.put("identityId", registeredIdentity.getId());
            
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verify(@RequestParam String token) {
        boolean isVerified = authService.verifyToken(token);
        
        if (isVerified) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Compte validé avec succès.");
            return ResponseEntity.ok(response);
        } else {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Jeton de validation invalide ou expiré.");
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            Token token = authService.login(request);
            return ResponseEntity.ok(token);
        } catch (RuntimeException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token manquant ou mal formaté");
        }

        String token = authHeader.substring(7); // Remove "Bearer " prefix

        if (jwtUtil.isTokenValid(token)) {
            return ResponseEntity.ok("Token valide");
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token invalide ou expiré");
        }
    }
}
