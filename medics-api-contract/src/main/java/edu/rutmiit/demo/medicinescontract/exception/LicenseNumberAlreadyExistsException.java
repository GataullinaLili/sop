package edu.rutmiit.demo.medicinescontract.exception;

public class LicenseNumberAlreadyExistsException extends RuntimeException {
    public LicenseNumberAlreadyExistsException(String licenseNumber) {
        super(String.format("Производитель с лицензионным номером=%s уже существует", licenseNumber));
    }
}