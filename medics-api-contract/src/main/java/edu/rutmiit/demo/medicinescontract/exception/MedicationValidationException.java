package edu.rutmiit.demo.medicinescontract.exception;

public class MedicationValidationException extends RuntimeException {
    public MedicationValidationException(String message) {
        super(message);
    }

    public MedicationValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}