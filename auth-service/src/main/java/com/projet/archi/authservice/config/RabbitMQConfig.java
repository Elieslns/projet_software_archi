package com.projet.archi.authservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "authExchange";
    public static final String QUEUE_NAME = "notificationQueue";
    public static final String ROUTING_KEY = "user.registered";

    @Bean
    public TopicExchange authExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue notificationQueue() {
        // durable = true
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    public Binding binding(Queue notificationQueue, TopicExchange authExchange) {
        return BindingBuilder.bind(notificationQueue).to(authExchange).with(ROUTING_KEY);
    }
}
