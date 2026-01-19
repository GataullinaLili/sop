package edu.rutmiit.demo.audit_service.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQConfig.class);

    // Exchange'ы, как в demo-rest
    public static final String MEDICATIONS_EXCHANGE = "medications-exchange";
    public static final String INTERACTIONS_EXCHANGE = "interactions-exchange";

    // Очереди
    public static final String MEDICATION_AUDIT_QUEUE = "medication-audit-queue";
    public static final String INTERACTION_AUDIT_QUEUE = "interaction-audit-queue";

    // === Exchange'ы ===
    @Bean
    public TopicExchange medicationsExchange() {
        return new TopicExchange(MEDICATIONS_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange interactionsExchange() {
        return new TopicExchange(INTERACTIONS_EXCHANGE, true, false);
    }

    // === Очереди ===
    @Bean
    public Queue medicationAuditQueue() {
        return QueueBuilder.durable(MEDICATION_AUDIT_QUEUE).build();
    }

    @Bean
    public Queue interactionAuditQueue() {
        return QueueBuilder.durable(INTERACTION_AUDIT_QUEUE).build();
    }

    // === Биндинги ===
    @Bean
    public Binding medicationCreatedBinding(
            TopicExchange medicationsExchange,
            Queue medicationAuditQueue) {
        return BindingBuilder.bind(medicationAuditQueue)
                .to(medicationsExchange)
                .with("medication.created");
    }

    @Bean
    public Binding interactionCheckedBinding(
            TopicExchange interactionsExchange,  // ← КЛЮЧЕВОЕ ИСПРАВЛЕНИЕ
            Queue interactionAuditQueue) {
        return BindingBuilder.bind(interactionAuditQueue)
                .to(interactionsExchange)       // ← СЛУШАЕМ ПРАВИЛЬНЫЙ EXCHANGE
                .with("interaction.checked");
    }

    // === Конвертер и шаблон ===
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("Сообщение не подтверждено брокером: {}", cause);
            }
        });
        return rabbitTemplate;
    }
}