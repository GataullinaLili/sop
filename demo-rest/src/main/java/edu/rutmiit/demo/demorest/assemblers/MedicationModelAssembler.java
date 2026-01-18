package edu.rutmiit.demo.demorest.assemblers;

import edu.rutmiit.demo.medicinescontract.dto.MedicationResponse;
import edu.rutmiit.demo.demorest.controllers.ManufacturerController;
import edu.rutmiit.demo.demorest.controllers.MedicationController;
import edu.rutmiit.demo.demorest.controllers.DrugInteractionController;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class MedicationModelAssembler implements RepresentationModelAssembler<MedicationResponse, EntityModel<MedicationResponse>> {

    @Override
    public EntityModel<MedicationResponse> toModel(MedicationResponse medication) {
        return EntityModel.of(medication,
                linkTo(methodOn(MedicationController.class).getMedicationById(medication.getId())).withSelfRel(),
                linkTo(methodOn(ManufacturerController.class).getManufacturerById(
                        medication.getManufacturer().getId())).withRel("manufacturer"),
                linkTo(methodOn(DrugInteractionController.class).checkDrugInteraction(
                        medication.getId(), medication.getName(), null, 30)).withRel("checkInteraction"),
                linkTo(methodOn(MedicationController.class).getAllMedications(null, null, 0, 10)).withRel("collection")
        );
    }
}