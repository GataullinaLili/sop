package edu.rutmiit.demo.medicinescontract.dto;

import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import java.util.Objects;

@Relation(collectionRelation = "manufacturers", itemRelation = "manufacturer")
public class ManufacturerResponse extends RepresentationModel<ManufacturerResponse> {
        private final Long id;
        private final String name;
        private final String country;
        private final String licenseNumber;
        private final String contactEmail;

        public ManufacturerResponse(Long id, String name, String country, String licenseNumber, String contactEmail) {
                this.id = id;
                this.name = name;
                this.country = country;
                this.licenseNumber = licenseNumber;
                this.contactEmail = contactEmail;
        }

        public Long getId() {
                return id;
        }

        public String getName() {
                return name;
        }

        public String getCountry() {
                return country;
        }

        public String getLicenseNumber() {
                return licenseNumber;
        }

        public String getContactEmail() {
                return contactEmail;
        }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                if (!super.equals(o)) return false;
                ManufacturerResponse that = (ManufacturerResponse) o;
                return Objects.equals(id, that.id) &&
                        Objects.equals(name, that.name) &&
                        Objects.equals(country, that.country) &&
                        Objects.equals(licenseNumber, that.licenseNumber) &&
                        Objects.equals(contactEmail, that.contactEmail);
        }

        @Override
        public int hashCode() {
                return Objects.hash(super.hashCode(), id, name, country, licenseNumber, contactEmail);
        }

        @Override
        public String toString() {
                return "ManufacturerResponse{" +
                        "id=" + id +
                        ", name='" + name + '\'' +
                        ", country='" + country + '\'' +
                        ", licenseNumber='" + licenseNumber + '\'' +
                        ", contactEmail='" + contactEmail + '\'' +
                        '}';
        }
}