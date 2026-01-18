package edu.rutmiit.demo.demorest.assemblers;

import edu.rutmiit.demo.medicinescontract.dto.ManufacturerResponse;
import edu.rutmiit.demo.demorest.controllers.ManufacturerController;
import edu.rutmiit.demo.demorest.controllers.MedicationController;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class ManufacturerModelAssembler implements RepresentationModelAssembler<ManufacturerResponse, EntityModel<ManufacturerResponse>> {

    @Override
    public EntityModel<ManufacturerResponse> toModel(ManufacturerResponse manufacturer) {
        return EntityModel.of(manufacturer,
                linkTo(methodOn(ManufacturerController.class).getManufacturerById(manufacturer.getId())).withSelfRel(),
                linkTo(methodOn(MedicationController.class).getAllMedications(manufacturer.getId(), null, null, 0, 10)).withRel("medications"),
                linkTo(methodOn(ManufacturerController.class).getAllManufacturers()).withRel("collection")
        );
    }

    @Override
    public CollectionModel<EntityModel<ManufacturerResponse>> toCollectionModel(Iterable<? extends ManufacturerResponse> entities) {
        return RepresentationModelAssembler.super.toCollectionModel(entities)
                .add(linkTo(methodOn(ManufacturerController.class).getAllManufacturers()).withSelfRel());
    }
}