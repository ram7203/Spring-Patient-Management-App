package com.patient.service.mappper;

import com.patient.service.dto.PatientRequestDTO;
import com.patient.service.dto.PatientResponseDTO;
import com.patient.service.model.Patient;

import java.time.LocalDate;

public class PatientMapper {

    public static PatientResponseDTO toDTO(Patient patient){

        PatientResponseDTO patientResponseDTO = PatientResponseDTO.builder()
                .id(patient.getId().toString())
                .name(patient.getName())
                .email(patient.getEmail())
                .address(patient.getAddress())
                .dateOfBirth(patient.getDateOfBirth().toString())
                .build();

        return patientResponseDTO;

    }

    public static Patient toModel(PatientRequestDTO patientRequestDTO){

        Patient patient = Patient.builder()
                .name(patientRequestDTO.getName())
                .email(patientRequestDTO.getEmail())
                .address(patientRequestDTO.getAddress())
                .dateOfBirth(LocalDate.parse(patientRequestDTO.getDateOfBirth()))
                .registeredDate(LocalDate.parse(patientRequestDTO.getRegisteredDate()))
                .build();

        return patient;

    }

}
