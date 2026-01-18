package edu.rutmiit.demo.demorest.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String EXCHANGE_NAME = "medications-exchange";
    public static final String ROUTING_KEY_MEDICATION_CREATED = "medication.created";
    public static final String ROUTING_KEY_INTERACTION_CHECKED = "interaction.checked";

    public static final String FANOUT_EXCHANGE = "medications-fanout";
    public static final String AUDIT_QUEUE = "q.audit.medications";
    public static final String NOTIFICATION_QUEUE = "q.notification.medications";

    @Bean
    public FanoutExchange medicationsFanoutExchange() {
        return new FanoutExchange(FANOUT_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange medicationsTopicExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public Queue auditQueue() {
        return QueueBuilder.durable(AUDIT_QUEUE).build();
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE).build();
    }

    @Bean
    public Binding auditBinding(FanoutExchange medicationsFanoutExchange, Queue auditQueue) {
        return BindingBuilder.bind(auditQueue).to(medicationsFanoutExchange);
    }

    @Bean
    public Binding notificationBinding(FanoutExchange medicationsFanoutExchange, Queue notificationQueue) {
        return BindingBuilder.bind(notificationQueue).to(medicationsFanoutExchange);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}