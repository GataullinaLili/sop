package edu.rutmiit.demo.demorest.config;

import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String EXCHANGE_NAME = "medications-exchange";
    public static final String ROUTING_KEY_MEDICATION_CREATED = "medication.created";

    public static final String INTERACTIONS_EXCHANGE = "interactions-exchange";
    public static final String ROUTING_KEY_INTERACTION_CHECKED = "interaction.checked";

    public static final String FANOUT_EXCHANGE = "analytics-fanout";

    @Bean
    public FanoutExchange analyticsExchange() {
        return new FanoutExchange(FANOUT_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange medicationsExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public TopicExchange interactionsExchange() {
        return new TopicExchange(INTERACTIONS_EXCHANGE);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}