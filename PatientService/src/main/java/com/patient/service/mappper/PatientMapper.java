package com.patient.service.mappper;

import com.patient.service.dto.PatientResponseDTO;
import com.patient.service.model.Patient;

public class PatientMapper {

    public static PatientResponseDTO toDTO(Patient patient){

        PatientResponseDTO patientResponseDTO = PatientResponseDTO.builder()
                .Id(patient.getId().toString())
                .Name(patient.getName())
                .email(patient.getEmail())
                .address(patient.getAddress())
                .dateOfBirth(patient.getDateOfBirth().toString())
                .build();

        return patientResponseDTO;

    }

}
