package edu.rutmiit.demo.demorest.service;

import edu.rutmiit.demo.medicinescontract.dto.*;
import edu.rutmiit.demo.medicinescontract.exception.ResourceNotFoundException;
import edu.rutmiit.demo.medicinescontract.exception.InnAlreadyExistsException;
import edu.rutmiit.demo.medicinescontract.exception.MedicationValidationException;
import edu.rutmiit.demo.demorest.config.RabbitMQConfig;
import edu.rutmiit.demo.demorest.storage.InMemoryStorage;
import edu.rutmiit.demo.medicinescontract.events.MedicationCreatedEvent;
import edu.rutmiit.demo.medicinescontract.events.MedicationUpdatedEvent;
import edu.rutmiit.demo.medicinescontract.events.MedicationDeletedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

@Service
public class MedicationService {

    private final InMemoryStorage storage;
    private final ManufacturerService manufacturerService;
    private final RabbitTemplate rabbitTemplate;

    // Статические данные для валидации и проверок
    private static final Set<String> VALID_DOSAGE_FORMS = Set.of(
            "Таблетки", "Капсулы", "Сироп", "Раствор", "Мазь", "Крем", "Гель",
            "Суппозитории", "Капли", "Инъекционный раствор", "Аэрозоль", "Порошок"
    );

    private static final Set<String> VALID_UNITS = Set.of(
            "мг", "г", "мл", "л", "МЕ", "%", "мкг"
    );

    private static final Map<String, List<String>> ATC_CATEGORIES = Map.of(
            "A", List.of("Пищеварительный тракт и обмен веществ"),
            "B", List.of("Кровь и кроветворные органы"),
            "C", List.of("Сердечно-сосудистая система"),
            "D", List.of("Дерматологические препараты"),
            "G", List.of("Мочеполовая система и половые гормоны"),
            "H", List.of("Гормональные препараты (искл. половые гормоны)"),
            "J", List.of("Противомикробные препараты для системного применения"),
            "L", List.of("Противоопухолевые препараты и иммуномодуляторы"),
            "M", List.of("Костно-мышечная система"),
            "N", List.of("Нервная система"),
            "P", List.of("Противопаразитарные препараты"),
            "R", List.of("Дыхательная система"),
            "S", List.of("Органы чувств"),
            "V", List.of("Прочие")
    );

    public MedicationService(InMemoryStorage storage,
                             ManufacturerService manufacturerService,
                             RabbitTemplate rabbitTemplate) {
        this.storage = storage;
        this.manufacturerService = manufacturerService;
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Найти лекарство по ID
     */
    public MedicationResponse findMedicationById(Long id) {
        MedicationResponse medication = storage.medications.get(id);
        if (medication == null) {
            throw new ResourceNotFoundException("Medication", id);
        }
        return medication;
    }

    /**
     * Найти лекарства по названию (поиск с учетом регистра)
     */
    public List<MedicationResponse> findMedicationsByName(String name) {
        String searchName = name.toLowerCase();
        return storage.medications.values().stream()
                .filter(med -> med.getName().toLowerCase().contains(searchName))
                .sorted(Comparator.comparing(MedicationResponse::getName))
                .toList();
    }

    /**
     * Найти лекарства по МНН (международное непатентованное наименование)
     */
    public List<MedicationResponse> findMedicationsByInn(String inn) {
        String searchInn = inn.toLowerCase();
        return storage.medications.values().stream()
                .filter(med -> med.getInn().toLowerCase().contains(searchInn))
                .sorted(Comparator.comparing(MedicationResponse::getInn))
                .toList();
    }

    /**
     * Получить все лекарства с пагинацией и фильтрацией
     */
    public PagedResponse<MedicationResponse> findAllMedications(
            Long manufacturerId, String atcCode, String searchTerm, int page, int size) {

        Stream<MedicationResponse> medicationsStream = storage.medications.values().stream()
                .sorted(Comparator.comparing(MedicationResponse::getId));

        // Фильтрация по производителю
        if (manufacturerId != null) {
            medicationsStream = medicationsStream.filter(
                    med -> med.getManufacturer() != null &&
                            med.getManufacturer().getId().equals(manufacturerId));
        }

        // Фильтрация по коду АТХ
        if (atcCode != null && !atcCode.isEmpty()) {
            medicationsStream = medicationsStream.filter(
                    med -> atcCode.equalsIgnoreCase(med.getAtcCode()));
        }

        // Поиск по названию или МНН
        if (StringUtils.hasText(searchTerm)) {
            String term = searchTerm.toLowerCase();
            medicationsStream = medicationsStream.filter(med ->
                    med.getName().toLowerCase().contains(term) ||
                            med.getInn().toLowerCase().contains(term) ||
                            (med.getAtcCode() != null && med.getAtcCode().toLowerCase().contains(term))
            );
        }

        List<MedicationResponse> allMedications = medicationsStream.toList();

        // Пагинация
        int totalElements = allMedications.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        if (page < 0) page = 0;
        if (size <= 0) size = 10;

        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalElements);

        List<MedicationResponse> pageContent = (fromIndex >= totalElements || fromIndex > toIndex)
                ? List.of()
                : allMedications.subList(fromIndex, toIndex);

        return new PagedResponse<>(pageContent, page, size, totalElements, totalPages, page >= totalPages - 1);
    }

    /**
     * Получить рецептурные лекарства
     */
    public List<MedicationResponse> getPrescriptionMedications() {
        return storage.medications.values().stream()
                .filter(MedicationResponse::getPrescriptionRequired)
                .sorted(Comparator.comparing(MedicationResponse::getName))
                .toList();
    }

    /**
     * Получить безрецептурные лекарства
     */
    public List<MedicationResponse> getOverTheCounterMedications() {
        return storage.medications.values().stream()
                .filter(med -> !med.getPrescriptionRequired())
                .sorted(Comparator.comparing(MedicationResponse::getName))
                .toList();
    }

    /**
     * Получить лекарства по сроку годности
     */
    public List<MedicationResponse> getMedicationsByShelfLife(int minMonths, int maxMonths) {
        return storage.medications.values().stream()
                .filter(med -> med.getShelfLifeMonths() != null)
                .filter(med -> med.getShelfLifeMonths() >= minMonths && med.getShelfLifeMonths() <= maxMonths)
                .sorted(Comparator.comparing(MedicationResponse::getShelfLifeMonths))
                .toList();
    }

    /**
     * Создать новое лекарство
     */
    public MedicationResponse createMedication(MedicationRequest request) {
        // Валидация входных данных
        validateMedicationRequest(request);

        // Проверка уникальности МНН
        validateInn(request.inn(), null);

        // Найти производителя
        ManufacturerResponse manufacturer = manufacturerService.findById(request.manufacturerId());

        // Создание лекарства
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

        // Сохранение
        storage.medications.put(id, medication);

        // Публикация события
        MedicationCreatedEvent event = new MedicationCreatedEvent(
                medication.getId(),
                medication.getName(),
                medication.getInn(),
                manufacturer.getName(),
                medication.getPrescriptionRequired()
        );

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY_MEDICATION_CREATED,
                event
        );

        return medication;
    }

    /**
     * Обновить лекарство
     */
    public MedicationResponse updateMedication(Long id, UpdateMedicationRequest request) {
        // Проверка существования лекарства
        MedicationResponse existingMedication = findMedicationById(id);

        // Валидация
        validateUpdateMedicationRequest(request);

        // Проверка уникальности МНН (исключая текущее лекарство)
        validateInn(request.inn(), id);

        // Создание обновленного лекарства
        var updatedMedication = new MedicationResponse(
                id,
                request.name(),
                request.inn(),
                request.atcCode(),
                existingMedication.getDosageForm(),
                existingMedication.getDosage(),
                existingMedication.getUnit(),
                existingMedication.getManufacturer(),
                existingMedication.getPrescriptionRequired(),
                request.storageConditions(),
                request.shelfLifeMonths(),
                existingMedication.getCreatedAt()
        );

        // Сохранение
        storage.medications.put(id, updatedMedication);

        // Публикация события обновления
        MedicationUpdatedEvent event = new MedicationUpdatedEvent(
                id,
                updatedMedication.getName(),
                updatedMedication.getInn(),
                existingMedication.getName(),
                existingMedication.getInn()
        );

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                "medication.updated",
                event
        );

        return updatedMedication;
    }

    /**
     * Обновить производителя лекарства
     */
    public MedicationResponse updateMedicationManufacturer(Long medicationId, Long manufacturerId) {
        MedicationResponse medication = findMedicationById(medicationId);
        ManufacturerResponse newManufacturer = manufacturerService.findById(manufacturerId);

        var updatedMedication = new MedicationResponse(
                medicationId,
                medication.getName(),
                medication.getInn(),
                medication.getAtcCode(),
                medication.getDosageForm(),
                medication.getDosage(),
                medication.getUnit(),
                newManufacturer,
                medication.getPrescriptionRequired(),
                medication.getStorageConditions(),
                medication.getShelfLifeMonths(),
                medication.getCreatedAt()
        );

        storage.medications.put(medicationId, updatedMedication);
        return updatedMedication;
    }

    /**
     * Обновить рецептурный статус
     */
    public MedicationResponse updatePrescriptionStatus(Long id, boolean prescriptionRequired) {
        MedicationResponse medication = findMedicationById(id);

        var updatedMedication = new MedicationResponse(
                id,
                medication.getName(),
                medication.getInn(),
                medication.getAtcCode(),
                medication.getDosageForm(),
                medication.getDosage(),
                medication.getUnit(),
                medication.getManufacturer(),
                prescriptionRequired,
                medication.getStorageConditions(),
                medication.getShelfLifeMonths(),
                medication.getCreatedAt()
        );

        storage.medications.put(id, updatedMedication);
        return updatedMedication;
    }

    /**
     * Удалить лекарство
     */
    public void deleteMedication(Long id) {
        MedicationResponse medication = findMedicationById(id);

        // Публикация события удаления перед удалением
        MedicationDeletedEvent event = new MedicationDeletedEvent(
                id,
                medication.getName(),
                medication.getInn(),
                medication.getManufacturer().getName()
        );

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                "medication.deleted",
                event
        );

        // Удаление
        storage.medications.remove(id);
    }

    /**
     * Удалить все лекарства производителя
     */
    public void deleteMedicationsByManufacturerId(Long manufacturerId) {
        List<Long> medicationIdsToDelete = storage.medications.values().stream()
                .filter(med -> med.getManufacturer() != null &&
                        med.getManufacturer().getId().equals(manufacturerId))
                .map(MedicationResponse::getId)
                .toList();

        // Публикация событий для каждого удаляемого лекарства
        medicationIdsToDelete.forEach(medId -> {
            MedicationResponse med = storage.medications.get(medId);
            if (med != null) {
                MedicationDeletedEvent event = new MedicationDeletedEvent(
                        medId,
                        med.getName(),
                        med.getInn(),
                        med.getManufacturer().getName()
                );

                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.EXCHANGE_NAME,
                        "medication.deleted.batch",
                        event
                );
            }
        });

        // Удаление
        medicationIdsToDelete.forEach(storage.medications::remove);
    }

    /**
     * Получить статистику по лекарствам
     */
    public Map<String, Object> getMedicationsStatistics() {
        Map<String, Object> stats = new HashMap<>();

        long totalMedications = storage.medications.size();
        long prescriptionCount = storage.medications.values().stream()
                .filter(MedicationResponse::getPrescriptionRequired)
                .count();
        long otcCount = totalMedications - prescriptionCount;

        // Статистика по производителям
        Map<String, Long> manufacturerStats = new HashMap<>();
        storage.medications.values().forEach(med -> {
            String manufacturerName = med.getManufacturer().getName();
            manufacturerStats.put(manufacturerName,
                    manufacturerStats.getOrDefault(manufacturerName, 0L) + 1);
        });

        // Статистика по формам выпуска
        Map<String, Long> dosageFormStats = new HashMap<>();
        storage.medications.values().forEach(med -> {
            String form = med.getDosageForm();
            dosageFormStats.put(form, dosageFormStats.getOrDefault(form, 0L) + 1);
        });

        stats.put("totalMedications", totalMedications);
        stats.put("prescriptionMedications", prescriptionCount);
        stats.put("overTheCounterMedications", otcCount);
        stats.put("manufacturerDistribution", manufacturerStats);
        stats.put("dosageFormDistribution", dosageFormStats);
        stats.put("calculatedAt", LocalDateTime.now());

        return stats;
    }

    /**
     * Проверить лекарственные взаимодействия (имитация)
     */
    public List<String> checkDrugInteractions(Long medicationId, List<String> otherDrugs) {
        MedicationResponse medication = findMedicationById(medicationId);
        List<String> interactions = new ArrayList<>();

        // Имитация проверки взаимодействий
        if (medication.getInn().toLowerCase().contains("warfarin")) {
            interactions.add("Взаимодействие с антикоагулянтами: повышенный риск кровотечений");
        }
        if (medication.getInn().toLowerCase().contains("stat")) {
            interactions.add("Взаимодействие с ингибиторами CYP3A4: риск миопатии");
        }
        if (otherDrugs != null) {
            otherDrugs.forEach(drug -> {
                interactions.add("Потенциальное взаимодействие с: " + drug);
            });
        }

        return interactions;
    }

    /**
     * Валидация запроса на создание лекарства
     */
    private void validateMedicationRequest(MedicationRequest request) {
        List<String> errors = new ArrayList<>();

        if (!StringUtils.hasText(request.name())) {
            errors.add("Название лекарства не может быть пустым");
        }

        if (!StringUtils.hasText(request.inn())) {
            errors.add("МНН (международное непатентованное наименование) не может быть пустым");
        }

        if (!VALID_DOSAGE_FORMS.contains(request.dosageForm())) {
            errors.add("Недопустимая форма выпуска: " + request.dosageForm() +
                    ". Допустимые значения: " + String.join(", ", VALID_DOSAGE_FORMS));
        }

        if (!VALID_UNITS.contains(request.unit())) {
            errors.add("Недопустимая единица измерения: " + request.unit() +
                    ". Допустимые значения: " + String.join(", ", VALID_UNITS));
        }

        if (request.dosage() == null || request.dosage().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Дозировка должна быть положительным числом");
        }

        if (request.atcCode() != null && request.atcCode().length() > 0) {
            String firstLetter = request.atcCode().substring(0, 1).toUpperCase();
            if (!ATC_CATEGORIES.containsKey(firstLetter)) {
                errors.add("Некорректный код АТХ. Первая буква должна быть одной из: " +
                        String.join(", ", ATC_CATEGORIES.keySet()));
            }
        }

        if (request.shelfLifeMonths() != null && request.shelfLifeMonths() <= 0) {
            errors.add("Срок годности должен быть положительным числом месяцев");
        }

        if (!errors.isEmpty()) {
            throw new MedicationValidationException("Ошибка валидации лекарства: " +
                    String.join("; ", errors));
        }
    }

    /**
     * Валидация запроса на обновление лекарства
     */
    private void validateUpdateMedicationRequest(UpdateMedicationRequest request) {
        List<String> errors = new ArrayList<>();

        if (!StringUtils.hasText(request.name())) {
            errors.add("Название лекарства не может быть пустым");
        }

        if (!StringUtils.hasText(request.inn())) {
            errors.add("МНН не может быть пустым");
        }

        if (request.atcCode() != null && request.atcCode().length() > 0) {
            String firstLetter = request.atcCode().substring(0, 1).toUpperCase();
            if (!ATC_CATEGORIES.containsKey(firstLetter)) {
                errors.add("Некорректный код АТХ");
            }
        }

        if (!errors.isEmpty()) {
            throw new MedicationValidationException("Ошибка валидации обновления: " +
                    String.join("; ", errors));
        }
    }

    /**
     * Проверка уникальности МНН
     */
    private void validateInn(String inn, Long currentMedicationId) {
        storage.medications.values().stream()
                .filter(med -> med.getInn().equalsIgnoreCase(inn))
                .filter(med -> !med.getId().equals(currentMedicationId))
                .findAny()
                .ifPresent(med -> {
                    throw new InnAlreadyExistsException(inn);
                });
    }

    /**
     * Получить лекарства по диапазону дозировки
     */
    public List<MedicationResponse> getMedicationsByDosageRange(BigDecimal minDosage, BigDecimal maxDosage) {
        return storage.medications.values().stream()
                .filter(med -> med.getDosage() != null)
                .filter(med -> med.getDosage().compareTo(minDosage) >= 0 &&
                        med.getDosage().compareTo(maxDosage) <= 0)
                .sorted(Comparator.comparing(MedicationResponse::getDosage))
                .toList();
    }

    /**
     * Поиск лекарств по нескольким критериям
     */
    public List<MedicationResponse> searchMedications(Map<String, Object> criteria) {
        Stream<MedicationResponse> stream = storage.medications.values().stream();

        if (criteria.containsKey("name")) {
            String name = criteria.get("name").toString().toLowerCase();
            stream = stream.filter(med -> med.getName().toLowerCase().contains(name));
        }

        if (criteria.containsKey("inn")) {
            String inn = criteria.get("inn").toString().toLowerCase();
            stream = stream.filter(med -> med.getInn().toLowerCase().contains(inn));
        }

        if (criteria.containsKey("manufacturerId")) {
            Long manufacturerId = Long.parseLong(criteria.get("manufacturerId").toString());
            stream = stream.filter(med -> med.getManufacturer() != null &&
                    med.getManufacturer().getId().equals(manufacturerId));
        }

        if (criteria.containsKey("prescriptionRequired")) {
            boolean prescriptionRequired = Boolean.parseBoolean(criteria.get("prescriptionRequired").toString());
            stream = stream.filter(med -> med.getPrescriptionRequired() == prescriptionRequired);
        }

        if (criteria.containsKey("dosageForm")) {
            String dosageForm = criteria.get("dosageForm").toString();
            stream = stream.filter(med -> dosageForm.equals(med.getDosageForm()));
        }

        return stream.sorted(Comparator.comparing(MedicationResponse::getName)).toList();
    }

    /**
     * Получить количество лекарств по производителю
     */
    public long countMedicationsByManufacturer(Long manufacturerId) {
        return storage.medications.values().stream()
                .filter(med -> med.getManufacturer() != null &&
                        med.getManufacturer().getId().equals(manufacturerId))
                .count();
    }

    /**
     * Получить последние добавленные лекарства
     */
    public List<MedicationResponse> getRecentMedications(int limit) {
        return storage.medications.values().stream()
                .sorted((m1, m2) -> m2.getCreatedAt().compareTo(m1.getCreatedAt()))
                .limit(limit)
                .toList();
    }
}