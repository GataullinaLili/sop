package edu.rutmiit.demo.demorest.service;

import edu.rutmiit.demo.medicinescontract.dto.ManufacturerRequest;
import edu.rutmiit.demo.medicinescontract.dto.ManufacturerResponse;
import edu.rutmiit.demo.medicinescontract.exception.ResourceNotFoundException;
import edu.rutmiit.demo.demorest.storage.InMemoryStorage;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ManufacturerService {
    private final InMemoryStorage storage;
    private final MedicationService medicationService;

    public ManufacturerService(InMemoryStorage storage, @Lazy MedicationService medicationService) {
        this.storage = storage;
        this.medicationService = medicationService;
    }

    public List<ManufacturerResponse> findAll() {
        return storage.manufacturers.values().stream().toList();
    }

    public ManufacturerResponse findById(Long id) {
        return Optional.ofNullable(storage.manufacturers.get(id))
                .orElseThrow(() -> new ResourceNotFoundException("Manufacturer", id));
    }

    public ManufacturerResponse create(ManufacturerRequest request) {
        long id = storage.manufacturerSequence.incrementAndGet();
        ManufacturerResponse manufacturer = new ManufacturerResponse(
                id,
                request.name(),
                request.country(),
                request.licenseNumber(),
                request.contactEmail()
        );
        storage.manufacturers.put(id, manufacturer);
        return manufacturer;
    }

    public ManufacturerResponse update(Long id, ManufacturerRequest request) {
        findById(id); // Проверяем, что производитель существует
        ManufacturerResponse updatedManufacturer = new ManufacturerResponse(
                id,
                request.name(),
                request.country(),
                request.licenseNumber(),
                request.contactEmail()
        );
        storage.manufacturers.put(id, updatedManufacturer);
        return updatedManufacturer;
    }

    public void delete(Long id) {
        findById(id); // Проверяем, что производитель существует

        // Перед удалением производителя удаляем все связанные с ним лекарства
        medicationService.deleteMedicationsByManufacturerId(id);

        // Удаляем самого производителя
        storage.manufacturers.remove(id);
    }
}