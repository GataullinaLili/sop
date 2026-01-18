package edu.rutmiit.demo.demorest.graphql;

import com.netflix.graphql.dgs.*;
import edu.rutmiit.demo.medicinescontract.dto.*;
import edu.rutmiit.demo.demorest.service.MedicationService;
import graphql.schema.DataFetchingEnvironment;

import java.math.BigDecimal;
import java.util.Map;

@DgsComponent
public class MedicationDataFetcher {

    private final MedicationService medicationService;

    public MedicationDataFetcher(MedicationService medicationService) {
        this.medicationService = medicationService;
    }

    @DgsQuery
    public MedicationResponse medicationById(@InputArgument Long id) {
        return medicationService.findMedicationById(id);
    }

    @DgsQuery
    public PagedResponse<MedicationResponse> medications(
            @InputArgument Long manufacturerId,
            @InputArgument String atcCode,
            @InputArgument int page,
            @InputArgument int size) {
        return medicationService.findAllMedications(manufacturerId, atcCode, page, size);
    }

    @DgsData(parentType = "Medication", field = "manufacturer")
    public ManufacturerResponse manufacturer(DataFetchingEnvironment dfe) {
        MedicationResponse medication = dfe.getSource();
        return medication.getManufacturer();
    }

    @DgsMutation
    public MedicationResponse createMedication(@InputArgument("input") Map<String, Object> input) {
        MedicationRequest request = new MedicationRequest(
                (String) input.get("name"),
                (String) input.get("inn"),
                (String) input.get("atcCode"),
                (String) input.get("dosageForm"),
                new BigDecimal(input.get("dosage").toString()),
                (String) input.get("unit"),
                Long.parseLong(input.get("manufacturerId").toString()),
                Boolean.parseBoolean(input.get("prescriptionRequired").toString()),
                (String) input.get("storageConditions"),
                input.get("shelfLifeMonths") != null ? Integer.parseInt(input.get("shelfLifeMonths").toString()) : null
        );
        return medicationService.createMedication(request);
    }

    @DgsMutation
    public MedicationResponse updateMedication(@InputArgument Long id,
                                               @InputArgument("input") Map<String, String> input) {
        UpdateMedicationRequest request = new UpdateMedicationRequest(
                input.get("name"),
                input.get("inn"),
                input.get("atcCode"),
                input.get("storageConditions"),
                input.get("shelfLifeMonths") != null ? Integer.parseInt(input.get("shelfLifeMonths")) : null
        );
        return medicationService.updateMedication(id, request);
    }

    @DgsMutation
    public Long deleteMedication(@InputArgument Long id) {
        medicationService.deleteMedication(id);
        return id;
    }
}