package edu.rutmiit.demo.demorest.controllers;

import edu.rutmiit.demo.medicinescontract.dto.*;
import edu.rutmiit.demo.medicinescontract.endpoints.MedicationApi;
import edu.rutmiit.demo.demorest.assemblers.MedicationModelAssembler;
import edu.rutmiit.demo.demorest.service.MedicationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MedicationController implements MedicationApi {

    private final MedicationService medicationService;
    private final MedicationModelAssembler medicationModelAssembler;
    private final PagedResourcesAssembler<MedicationResponse> pagedResourcesAssembler;

    public MedicationController(MedicationService medicationService,
                                MedicationModelAssembler medicationModelAssembler,
                                PagedResourcesAssembler<MedicationResponse> pagedResourcesAssembler) {
        this.medicationService = medicationService;
        this.medicationModelAssembler = medicationModelAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Override
    public EntityModel<MedicationResponse> getMedicationById(Long id) {
        MedicationResponse medication = medicationService.findMedicationById(id);
        return medicationModelAssembler.toModel(medication);
    }

    @Override
    public PagedModel<EntityModel<MedicationResponse>> getAllMedications(Long manufacturerId, String atcCode,
                                                                         String search, int page, int size) {
        PagedResponse<MedicationResponse> pagedResponse = medicationService.findAllMedications(
                manufacturerId, atcCode, search, page, size);

        Page<MedicationResponse> medicationPage = new PageImpl<>(
                pagedResponse.content(),
                PageRequest.of(pagedResponse.pageNumber(), pagedResponse.pageSize()),
                pagedResponse.totalElements()
        );

        return pagedResourcesAssembler.toModel(medicationPage, medicationModelAssembler);
    }

    @Override
    public PagedModel<EntityModel<MedicationResponse>> getPrescriptionMedications(int page, int size) {
        PagedResponse<MedicationResponse> pagedResponse = medicationService.findPrescriptionMedications(page, size);

        Page<MedicationResponse> medicationPage = new PageImpl<>(
                pagedResponse.content(),
                PageRequest.of(pagedResponse.pageNumber(), pagedResponse.pageSize()),
                pagedResponse.totalElements()
        );

        return pagedResourcesAssembler.toModel(medicationPage, medicationModelAssembler);
    }

    @Override
    public PagedModel<EntityModel<MedicationResponse>> getOverTheCounterMedications(int page, int size) {
        PagedResponse<MedicationResponse> pagedResponse = medicationService.findOverTheCounterMedications(page, size);

        Page<MedicationResponse> medicationPage = new PageImpl<>(
                pagedResponse.content(),
                PageRequest.of(pagedResponse.pageNumber(), pagedResponse.pageSize()),
                pagedResponse.totalElements()
        );

        return pagedResourcesAssembler.toModel(medicationPage, medicationModelAssembler);
    }

    @Override
    public ResponseEntity<EntityModel<MedicationResponse>> createMedication(MedicationRequest request) {
        MedicationResponse createdMedication = medicationService.createMedication(request);
        EntityModel<MedicationResponse> entityModel = medicationModelAssembler.toModel(createdMedication);

        return ResponseEntity
                .created(entityModel.getRequiredLink("self").toUri())
                .body(entityModel);
    }

    @Override
    public EntityModel<MedicationResponse> updateMedication(Long id, UpdateMedicationRequest request) {
        MedicationResponse updatedMedication = medicationService.updateMedication(id, request);
        return medicationModelAssembler.toModel(updatedMedication);
    }

    @Override
    public void deleteMedication(Long id) {
        medicationService.deleteMedication(id);
    }

    @Override
    public EntityModel<MedicationResponse> updatePrescriptionStatus(Long id, boolean prescriptionRequired) {
        MedicationResponse updatedMedication = medicationService.updatePrescriptionStatus(id, prescriptionRequired);
        return medicationModelAssembler.toModel(updatedMedication);
    }
}