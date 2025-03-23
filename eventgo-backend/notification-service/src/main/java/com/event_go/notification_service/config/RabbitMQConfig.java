package com.event_go.notification_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;

@Configuration
public class RabbitMQConfig {

    public static final String NOTIFICATION_QUEUE = "notification.queue";

    @Bean
    public Queue notificationQueue() {
        return new Queue(NOTIFICATION_QUEUE, true);
    }
    
    @Bean
    public SimpleMessageConverter messageConverter() {
        SimpleMessageConverter converter = new SimpleMessageConverter();
        // Add specific allowed classes
        converter.addAllowedListPatterns("ticketBookingSystem.dto.notification.*");
        return converter;
    }
    
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            SimpleRabbitListenerContainerFactoryConfigurer configurer) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        configurer.configure(factory, connectionFactory);
        return factory;
    }
}
