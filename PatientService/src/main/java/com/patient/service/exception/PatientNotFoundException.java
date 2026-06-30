package com.patient.service.exception;

public class PatientNotFoundException extends RuntimeException {

    public PatientNotFoundException(String s) {
        super(s);
    }
}
