package com.projet.archi.notificationservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender javaMailSender;

    public void sendVerificationEmail(String toEmail, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@projetarchi.com");
            message.setTo(toEmail);
            message.setSubject("Vérification de votre compte");
            
            // Build the verification link pointing to the Auth Service
            String verificationLink = "http://localhost:8081/verify?token=" + token;
            
            message.setText("Bienvenue sur notre plateforme !\n\n" +
                    "Veuillez cliquer sur le lien ci-dessous pour vérifier votre compte :\n" +
                    verificationLink + "\n\n" +
                    "Merci !");

            javaMailSender.send(message);
            logger.info("E-mail de vérification envoyé avec succès à {}", toEmail);

        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de l'e-mail à {}", toEmail, e);
        }
    }
}
