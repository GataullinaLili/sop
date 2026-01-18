package edu.rutmiit.demo.events;

import java.io.Serializable;
import java.util.List;

public record DrugInteractionCheckedEvent(
        Long medicationId,
        String medicationName,
        Integer riskLevel,
        String severity,
        List<String> contraindications,
        String recommendation
) implements Serializable {}