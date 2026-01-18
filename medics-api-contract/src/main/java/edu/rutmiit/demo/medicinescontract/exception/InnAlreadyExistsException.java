package edu.rutmiit.demo.medicinescontract.exception;

public class InnAlreadyExistsException extends RuntimeException {
    public InnAlreadyExistsException(String inn) {
        super(String.format("Лекарство с МНН=%s уже существует", inn));
    }
}