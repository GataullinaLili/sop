package edu.rutmiit.demo.demorest.storage;

import edu.rutmiit.demo.medicinescontract.dto.ManufacturerResponse;
import edu.rutmiit.demo.medicinescontract.dto.MedicationResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class InMemoryStorage {
    public final Map<Long, ManufacturerResponse> manufacturers = new ConcurrentHashMap<>();
    public final Map<Long, MedicationResponse> medications = new ConcurrentHashMap<>();

    public final AtomicLong manufacturerSequence = new AtomicLong(0);
    public final AtomicLong medicationSequence = new AtomicLong(0);

    @PostConstruct
    public void init() {
        // Создаем несколько производителей
        ManufacturerResponse manufacturer1 = new ManufacturerResponse(
                manufacturerSequence.incrementAndGet(),
                "Pfizer",
                "USA",
                "LIC-001",
                "contact@pfizer.com"
        );
        ManufacturerResponse manufacturer2 = new ManufacturerResponse(
                manufacturerSequence.incrementAndGet(),
                "Novartis",
                "Switzerland",
                "LIC-002",
                "contact@novartis.com"
        );
        manufacturers.put(manufacturer1.getId(), manufacturer1);
        manufacturers.put(manufacturer2.getId(), manufacturer2);

        // Создаем несколько лекарств
        long medId1 = medicationSequence.incrementAndGet();
        medications.put(medId1, new MedicationResponse(
                medId1,
                "Аспирин",
                "Acetylsalicylic acid",
                "B01AC06",
                "Таблетки",
                new BigDecimal("500"),
                "мг",
                manufacturer1,
                false,
                "Хранить в сухом месте",
                36,
                LocalDateTime.now()
        ));

        long medId2 = medicationSequence.incrementAndGet();
        medications.put(medId2, new MedicationResponse(
                medId2,
                "Амоксициллин",
                "Amoxicillin",
                "J01CA04",
                "Капсулы",
                new BigDecimal("250"),
                "мг",
                manufacturer2,
                true,
                "Хранить в холодильнике",
                24,
                LocalDateTime.now()
        ));

        long medId3 = medicationSequence.incrementAndGet();
        medications.put(medId3, new MedicationResponse(
                medId3,
                "Ибупрофен",
                "Ibuprofen",
                "M01AE01",
                "Таблетки",
                new BigDecimal("200"),
                "мг",
                manufacturer1,
                false,
                "Хранить при комнатной температуре",
                48,
                LocalDateTime.now()
        ));
    }
}