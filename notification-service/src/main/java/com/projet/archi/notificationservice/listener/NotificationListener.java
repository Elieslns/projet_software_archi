package com.projet.archi.notificationservice.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projet.archi.notificationservice.config.RabbitMQConfig;
import com.projet.archi.notificationservice.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NotificationListener {

    private static final Logger logger = LoggerFactory.getLogger(NotificationListener.class);

    @Autowired
    private EmailService emailService;

    @Autowired
    private ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void receiveMessage(String message) {
        logger.info("Message reçu depuis RabbitMQ : {}", message);
        
        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            String email = jsonNode.get("email").asText();
            String token = jsonNode.get("token").asText();
            
            // Send email
            emailService.sendVerificationEmail(email, token);
            
        } catch (Exception e) {
            logger.error("Erreur lors du traitement du message RabbitMQ", e);
        }
    }
}
