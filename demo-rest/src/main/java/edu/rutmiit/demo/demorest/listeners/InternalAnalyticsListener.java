package edu.rutmiit.demo.demorest.listeners;

import edu.rutmiit.demo.events.*;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class InternalAnalyticsListener {

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(name = "q.demorest.analytics.log", durable = "true"),
                    exchange = @Exchange(name = "analytics-fanout", type = "fanout")
            )
    )
    public void logRating(UserRatedEvent event) {
        System.out.println("User rated: " + event.userId() + " with rating: " + event.rating());
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(name = "q.demorest.medications.created", durable = "true"),
                    exchange = @Exchange(name = "medications-exchange", type = "topic"),
                    key = "medication.created"
            )
    )
    public void logMedicationCreated(MedicationCreatedEvent event) {
        System.out.println("Medication created: " + event.medicationName() +
                " (INN: " + event.inn() + ", Manufacturer: " +
                event.manufacturerName() + ")");
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(name = "q.demorest.interactions.checked", durable = "true"),
                    exchange = @Exchange(name = "interactions-exchange", type = "topic"),
                    key = "interaction.checked"
            )
    )
    public void logInteractionChecked(DrugInteractionCheckedEvent event) {
        System.out.println("Drug interaction checked: " + event.medicationName() +
                " (Risk Level: " + event.riskLevel() +
                ", Severity: " + event.severity() + ")");
    }
}