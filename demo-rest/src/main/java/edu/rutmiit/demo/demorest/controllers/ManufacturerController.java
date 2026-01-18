package edu.rutmiit.demo.demorest.controllers;

import edu.rutmiit.demo.medicinescontract.dto.*;
import edu.rutmiit.demo.medicinescontract.endpoints.ManufacturerApi;
import edu.rutmiit.demo.demorest.assemblers.ManufacturerModelAssembler;
import edu.rutmiit.demo.demorest.service.ManufacturerService;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ManufacturerController implements ManufacturerApi {

    private final ManufacturerService manufacturerService;
    private final ManufacturerModelAssembler manufacturerModelAssembler;

    public ManufacturerController(ManufacturerService manufacturerService,
                                  ManufacturerModelAssembler manufacturerModelAssembler) {
        this.manufacturerService = manufacturerService;
        this.manufacturerModelAssembler = manufacturerModelAssembler;
    }

    @Override
    public CollectionModel<EntityModel<ManufacturerResponse>> getAllManufacturers() {
        List<ManufacturerResponse> manufacturers = manufacturerService.findAll();
        return manufacturerModelAssembler.toCollectionModel(manufacturers);
    }

    @Override
    public EntityModel<ManufacturerResponse> getManufacturerById(Long id) {
        ManufacturerResponse manufacturer = manufacturerService.findById(id);
        return manufacturerModelAssembler.toModel(manufacturer);
    }

    @Override
    public ResponseEntity<EntityModel<ManufacturerResponse>> createManufacturer(ManufacturerRequest request) {
        ManufacturerResponse createdManufacturer = manufacturerService.create(request);
        EntityModel<ManufacturerResponse> entityModel = manufacturerModelAssembler.toModel(createdManufacturer);

        return ResponseEntity
                .created(entityModel.getRequiredLink("self").toUri())
                .body(entityModel);
    }

    @Override
    public EntityModel<ManufacturerResponse> updateManufacturer(Long id, UpdateManufacturerRequest request) {
        // Конвертируем UpdateManufacturerRequest в ManufacturerRequest
        ManufacturerRequest manufacturerRequest = new ManufacturerRequest(
                request.name(),
                request.country(),
                request.licenseNumber(),
                request.contactEmail()
        );

        ManufacturerResponse updatedManufacturer = manufacturerService.update(id, manufacturerRequest);
        return manufacturerModelAssembler.toModel(updatedManufacturer);
    }

    @Override
    public void deleteManufacturer(Long id) {
        manufacturerService.delete(id);
    }
}