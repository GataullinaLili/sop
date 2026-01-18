package edu.rutmiit.demo.audit_service.listeners;

import edu.rutmiit.demo.events.MedicationCreatedEvent;
import edu.rutmiit.demo.events.DrugInteractionCheckedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class MedicationEventListener {

    private static final Logger log = LoggerFactory.getLogger(MedicationEventListener.class);

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "medication-audit-queue", durable = "true"),
            exchange = @Exchange(name = "medications-exchange", type = "topic"),
            key = "medication.created"
    ))
    public void handleMedicationCreatedEvent(MedicationCreatedEvent event) {
        log.info("АУДИТ: Создано новое лекарство: ID={}, Название={}, МНН={}, Рецептурный={}",
                event.medicationId(), event.medicationName(), event.inn(), event.prescriptionRequired());

        // Дополнительная логика: проверка рецептурных препаратов
        if (event.prescriptionRequired()) {
            log.warn("ВНИМАНИЕ: Создан рецептурный препарат {}. Требуется усиленный контроль.",
                    event.medicationName());
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "interaction-audit-queue", durable = "true"),
            exchange = @Exchange(name = "medications-exchange", type = "topic"),
            key = "interaction.checked"
    ))
    public void handleInteractionCheckedEvent(DrugInteractionCheckedEvent event) {
        log.info("АУДИТ: Проверка взаимодействий для препарата ID={}, Название={}, Уровень риска={}, Серьезность={}",
                event.medicationId(), event.medicationName(), event.riskLevel(), event.severity());

        // Логирование критических взаимодействий
        if ("HIGH".equals(event.severity()) || event.riskLevel() > 7) {
            log.error("КРИТИЧЕСКОЕ ВЗАИМОДЕЙСТВИЕ: {} - {}",
                    event.medicationName(), event.recommendation());
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "q.audit.medications", durable = "true"),
            exchange = @Exchange(name = "medications-fanout", type = "fanout")
    ))
    public void handleMedicationFanoutEvents(Object event) {
        log.debug("Получено общее событие от medications-fanout: {}", event);
    }
}