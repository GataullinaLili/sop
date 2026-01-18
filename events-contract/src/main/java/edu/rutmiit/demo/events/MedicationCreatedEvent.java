package edu.rutmiit.demo.events;

import java.io.Serializable;

public record MedicationCreatedEvent(
        Long medicationId,
        String medicationName,
        String inn,
        String manufacturerName,
        boolean prescriptionRequired
) implements Serializable {}