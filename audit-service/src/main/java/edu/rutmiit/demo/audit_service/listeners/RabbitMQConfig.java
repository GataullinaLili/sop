package edu.rutmiit.demo.audit_service.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.*;

@Configuration
public class RabbitMQConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQConfig.class);

    public static final String FANOUT_EXCHANGE = "medications-fanout";
    public static final String TOPIC_EXCHANGE = "medications-exchange";
    public static final String AUDIT_QUEUE = "q.audit.medications";
    public static final String MEDICATION_AUDIT_QUEUE = "medication-audit-queue";
    public static final String INTERACTION_AUDIT_QUEUE = "interaction-audit-queue";

    @Bean
    public FanoutExchange medicationsFanoutExchange() {
        return new FanoutExchange(FANOUT_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange medicationsTopicExchange() {
        return new TopicExchange(TOPIC_EXCHANGE, true, false);
    }

    @Bean
    public Queue auditQueue() {
        return QueueBuilder.durable(AUDIT_QUEUE).build();
    }

    @Bean
    public Queue medicationAuditQueue() {
        return QueueBuilder.durable(MEDICATION_AUDIT_QUEUE).build();
    }

    @Bean
    public Queue interactionAuditQueue() {
        return QueueBuilder.durable(INTERACTION_AUDIT_QUEUE).build();
    }

    @Bean
    public Binding fanoutBinding(FanoutExchange medicationsFanoutExchange, Queue auditQueue) {
        return BindingBuilder.bind(auditQueue).to(medicationsFanoutExchange);
    }

    @Bean
    public Binding medicationCreatedBinding(TopicExchange medicationsTopicExchange,
                                            Queue medicationAuditQueue) {
        return BindingBuilder.bind(medicationAuditQueue)
                .to(medicationsTopicExchange)
                .with("medication.created");
    }

    @Bean
    public Binding interactionCheckedBinding(TopicExchange medicationsTopicExchange,
                                             Queue interactionAuditQueue) {
        return BindingBuilder.bind(interactionAuditQueue)
                .to(medicationsTopicExchange)
                .with("interaction.checked");
    }

    @Bean
    public Binding allMedicationsBinding(TopicExchange medicationsTopicExchange,
                                         Queue auditQueue) {
        return BindingBuilder.bind(auditQueue)
                .to(medicationsTopicExchange)
                .with("medication.*");
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);

        // Включение подтверждений доставки
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("Сообщение не доставлено: {}", cause);
            }
        });

        return rabbitTemplate;
    }
}