package edu.rutmiit.demo.medicinescontract.dto;

import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Relation(collectionRelation = "medications", itemRelation = "medication")
public class MedicationResponse extends RepresentationModel<MedicationResponse> {
    private final Long id;
    private final String name;
    private final String inn;
    private final String atcCode;
    private final String dosageForm;
    private final BigDecimal dosage;
    private final String unit;
    private final ManufacturerResponse manufacturer;
    private final Boolean prescriptionRequired;
    private final String storageConditions;
    private final Integer shelfLifeMonths;
    private final LocalDateTime createdAt;

    public MedicationResponse(Long id, String name, String inn, String atcCode,
                              String dosageForm, BigDecimal dosage, String unit,
                              ManufacturerResponse manufacturer, Boolean prescriptionRequired,
                              String storageConditions, Integer shelfLifeMonths, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.inn = inn;
        this.atcCode = atcCode;
        this.dosageForm = dosageForm;
        this.dosage = dosage;
        this.unit = unit;
        this.manufacturer = manufacturer;
        this.prescriptionRequired = prescriptionRequired;
        this.storageConditions = storageConditions;
        this.shelfLifeMonths = shelfLifeMonths;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getInn() {
        return inn;
    }

    public String getAtcCode() {
        return atcCode;
    }

    public String getDosageForm() {
        return dosageForm;
    }

    public BigDecimal getDosage() {
        return dosage;
    }

    public String getUnit() {
        return unit;
    }

    public ManufacturerResponse getManufacturer() {
        return manufacturer;
    }

    public Boolean getPrescriptionRequired() {
        return prescriptionRequired;
    }

    public String getStorageConditions() {
        return storageConditions;
    }

    public Integer getShelfLifeMonths() {
        return shelfLifeMonths;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        MedicationResponse that = (MedicationResponse) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(inn, that.inn) &&
                Objects.equals(atcCode, that.atcCode) &&
                Objects.equals(dosageForm, that.dosageForm) &&
                Objects.equals(dosage, that.dosage) &&
                Objects.equals(unit, that.unit) &&
                Objects.equals(manufacturer, that.manufacturer) &&
                Objects.equals(prescriptionRequired, that.prescriptionRequired) &&
                Objects.equals(storageConditions, that.storageConditions) &&
                Objects.equals(shelfLifeMonths, that.shelfLifeMonths) &&
                Objects.equals(createdAt, that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, name, inn, atcCode, dosageForm,
                dosage, unit, manufacturer, prescriptionRequired,
                storageConditions, shelfLifeMonths, createdAt);
    }

    @Override
    public String toString() {
        return "MedicationResponse{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", inn='" + inn + '\'' +
                ", atcCode='" + atcCode + '\'' +
                ", dosageForm='" + dosageForm + '\'' +
                ", dosage=" + dosage +
                ", unit='" + unit + '\'' +
                ", manufacturer=" + manufacturer +
                ", prescriptionRequired=" + prescriptionRequired +
                ", storageConditions='" + storageConditions + '\'' +
                ", shelfLifeMonths=" + shelfLifeMonths +
                ", createdAt=" + createdAt +
                '}';
    }
}