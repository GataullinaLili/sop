package edu.rutmiit.demo.demorest.storage;

import edu.rutmiit.demo.medicinescontract.dto.*;
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
        // Производители
        ManufacturerResponse pfizer = new ManufacturerResponse(
                manufacturerSequence.incrementAndGet(),
                "Pfizer", "USA", "FDA-12345", "contact@pfizer.com");

        ManufacturerResponse novartis = new ManufacturerResponse(
                manufacturerSequence.incrementAndGet(),
                "Novartis", "Switzerland", "EMA-67890", "contact@novartis.com");

        ManufacturerResponse bayer = new ManufacturerResponse(
                manufacturerSequence.incrementAndGet(),
                "Bayer", "Germany", "EMA-54321", "contact@bayer.com");

        manufacturers.put(pfizer.getId(), pfizer);
        manufacturers.put(novartis.getId(), novartis);
        manufacturers.put(bayer.getId(), bayer);

        // Лекарства
        medications.put(medicationSequence.incrementAndGet(),
                new MedicationResponse(
                        1L, "Аспирин", "Acetylsalicylic acid", "B01AC06",
                        "Таблетки", new BigDecimal("500"), "мг", bayer,
                        false, "Хранить при температуре до 25°C", 36, LocalDateTime.now()
                ));

        medications.put(medicationSequence.incrementAndGet(),
                new MedicationResponse(
                        2L, "Амоксиклав", "Amoxicillin/Clavulanic acid", "J01CR02",
                        "Таблетки", new BigDecimal("625"), "мг", pfizer,
                        true, "Хранить в сухом месте", 24, LocalDateTime.now()
                ));

        medications.put(medicationSequence.incrementAndGet(),
                new MedicationResponse(
                        3L, "Лозартан", "Losartan", "C09CA01",
                        "Таблетки", new BigDecimal("50"), "мг", novartis,
                        true, "Хранить в оригинальной упаковке", 48, LocalDateTime.now()
                ));
    }
}