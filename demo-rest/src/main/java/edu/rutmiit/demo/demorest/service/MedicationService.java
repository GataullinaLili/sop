package edu.rutmiit.demo.demorest.service;

import edu.rutmiit.demo.medicinescontract.dto.*;
import edu.rutmiit.demo.medicinescontract.exception.InnAlreadyExistsException;
import edu.rutmiit.demo.medicinescontract.exception.ResourceNotFoundException;
import edu.rutmiit.demo.demorest.config.RabbitMQConfig;
import edu.rutmiit.demo.demorest.storage.InMemoryStorage;
import edu.rutmiit.demo.events.MedicationCreatedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class MedicationService {

    private final InMemoryStorage storage;
    private final ManufacturerService manufacturerService;
    private final RabbitTemplate rabbitTemplate;

    public MedicationService(InMemoryStorage storage, @Lazy ManufacturerService manufacturerService, RabbitTemplate rabbitTemplate) {
        this.storage = storage;
        this.manufacturerService = manufacturerService;
        this.rabbitTemplate = rabbitTemplate;
    }

    public MedicationResponse findMedicationById(Long id) {
        return Optional.ofNullable(storage.medications.get(id))
                .orElseThrow(() -> new ResourceNotFoundException("Medication", id));
    }

    public PagedResponse<MedicationResponse> findAllMedications(Long manufacturerId, String atcCode, String search, int page, int size) {
        Stream<MedicationResponse> medicationsStream = storage.medications.values().stream()
                .sorted((m1, m2) -> m1.getId().compareTo(m2.getId()));

        // Фильтруем по manufacturerId
        if (manufacturerId != null) {
            medicationsStream = medicationsStream.filter(med ->
                    med.getManufacturer() != null && med.getManufacturer().getId().equals(manufacturerId));
        }

        // Фильтруем по ATC коду
        if (atcCode != null && !atcCode.isEmpty()) {
            medicationsStream = medicationsStream.filter(med ->
                    med.getAtcCode() != null && med.getAtcCode().equalsIgnoreCase(atcCode));
        }

        // Поиск по названию или INN
        if (search != null && !search.isEmpty()) {
            String searchLower = search.toLowerCase();
            medicationsStream = medicationsStream.filter(med ->
                    med.getName().toLowerCase().contains(searchLower) ||
                            med.getInn().toLowerCase().contains(searchLower));
        }

        List<MedicationResponse> allMedications = medicationsStream.toList();

        // Пагинация
        int totalElements = allMedications.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalElements);

        List<MedicationResponse> pageContent = (fromIndex > toIndex) ? List.of() : allMedications.subList(fromIndex, toIndex);

        return new PagedResponse<>(pageContent, page, size, totalElements, totalPages, page >= totalPages - 1);
    }

    // Перегруженный метод для GraphQL
    public PagedResponse<MedicationResponse> findAllMedications(Long manufacturerId, int page, int size) {
        return findAllMedications(manufacturerId, null, null, page, size);
    }

    public PagedResponse<MedicationResponse> findPrescriptionMedications(int page, int size) {
        return findAllMedications(null, null, null, page, size)
                .content().stream()
                .filter(MedicationResponse::getPrescriptionRequired)
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toList(),
                        list -> createPagedResponse(list, page, size)
                ));
    }

    public PagedResponse<MedicationResponse> findOverTheCounterMedications(int page, int size) {
        return findAllMedications(null, null, null, page, size)
                .content().stream()
                .filter(med -> !med.getPrescriptionRequired())
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toList(),
                        list -> createPagedResponse(list, page, size)
                ));
    }

    private PagedResponse<MedicationResponse> createPagedResponse(List<MedicationResponse> list, int page, int size) {
        int totalElements = list.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalElements);

        List<MedicationResponse> pageContent = (fromIndex > toIndex) ? List.of() : list.subList(fromIndex, toIndex);

        return new PagedResponse<>(pageContent, page, size, totalElements, totalPages, page >= totalPages - 1);
    }

    public MedicationResponse createMedication(MedicationRequest request) {
        // Проверка на существующий INN
        validateInn(request.inn(), null);

        // Находим производителя
        ManufacturerResponse manufacturer = manufacturerService.findById(request.manufacturerId());

        long id = storage.medicationSequence.incrementAndGet();
        var medication = new MedicationResponse(
                id,
                request.name(),
                request.inn(),
                request.atcCode(),
                request.dosageForm(),
                request.dosage(),
                request.unit(),
                manufacturer,
                request.prescriptionRequired(),
                request.storageConditions(),
                request.shelfLifeMonths(),
                LocalDateTime.now()
        );
        storage.medications.put(id, medication);

        // Публикуем событие создания лекарства
        MedicationCreatedEvent event = new MedicationCreatedEvent(
                medication.getId(),
                medication.getName(),
                medication.getInn(),
                manufacturer.getName(),
                medication.getPrescriptionRequired()
        );
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY_MEDICATION_CREATED,
                event);

        return medication;
    }

    public MedicationResponse updateMedication(Long id, UpdateMedicationRequest request) {
        MedicationResponse existingMedication = findMedicationById(id);
        validateInn(request.inn(), id);

        var updatedMedication = new MedicationResponse(
                id,
                request.name(),
                request.inn(),
                request.atcCode(),
                request.dosageForm(),
                request.dosage(),
                request.unit(),
                existingMedication.getManufacturer(),
                request.prescriptionRequired(),
                request.storageConditions(),
                request.shelfLifeMonths(),
                existingMedication.getCreatedAt()
        );
        storage.medications.put(id, updatedMedication);
        return updatedMedication;
    }

    public MedicationResponse updatePrescriptionStatus(Long id, boolean prescriptionRequired) {
        MedicationResponse existingMedication = findMedicationById(id);

        // Создаем UpdateMedicationRequest для обновления только статуса
        UpdateMedicationRequest updateRequest = new UpdateMedicationRequest(
                existingMedication.getName(),
                existingMedication.getInn(),
                existingMedication.getAtcCode(),
                existingMedication.getDosageForm(),
                existingMedication.getDosage(),
                existingMedication.getUnit(),
                prescriptionRequired,
                existingMedication.getStorageConditions(),
                existingMedication.getShelfLifeMonths()
        );

        return updateMedication(id, updateRequest);
    }

    public void deleteMedication(Long id) {
        findMedicationById(id);
        storage.medications.remove(id);
    }

    public void deleteMedicationsByManufacturerId(Long manufacturerId) {
        List<Long> medicationIdsToDelete = storage.medications.values().stream()
                .filter(med -> med.getManufacturer() != null && med.getManufacturer().getId().equals(manufacturerId))
                .map(MedicationResponse::getId)
                .toList();

        medicationIdsToDelete.forEach(storage.medications::remove);
    }

    private void validateInn(String inn, Long currentMedicationId) {
        storage.medications.values().stream()
                .filter(med -> med.getInn().equalsIgnoreCase(inn))
                .filter(med -> !med.getId().equals(currentMedicationId))
                .findAny()
                .ifPresent(med -> {
                    throw new InnAlreadyExistsException(inn);
                });
    }
}