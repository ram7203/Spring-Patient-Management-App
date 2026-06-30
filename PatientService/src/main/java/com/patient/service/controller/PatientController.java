package com.patient.service.controller;

import com.patient.service.dto.PatientRequestDTO;
import com.patient.service.dto.PatientResponseDTO;
import com.patient.service.dto.validators.CreatePatientValidationGroup;
import com.patient.service.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Scanner;
import java.util.UUID;

@RestController
@RequestMapping("/patients")
@Tag(name = "Patient", description = "API for managing patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService){
        this.patientService = patientService;
    }

    //1. Get all patients
    @GetMapping
    @Operation(summary = "Get all patients")
    public ResponseEntity<List<PatientResponseDTO>> getPatients(){

        List<PatientResponseDTO> patients = patientService.getPatients();

        return ResponseEntity.ok(patients);

    }

    //2. Create a patient
    @PostMapping
    @Operation(summary = "Create a patient")
    public ResponseEntity<PatientResponseDTO> createPatient(
            @Validated({Default.class, CreatePatientValidationGroup.class}) @RequestBody PatientRequestDTO patientRequestDTO){

        PatientResponseDTO patient = patientService.createPatient(patientRequestDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(patient);

    }

    //3. Update Patient
    @PutMapping("/{id}")
    @Operation(summary = "Update a Patient")
    public ResponseEntity<PatientResponseDTO> updatePatient(
            @PathVariable UUID id,
            @Validated({Default.class}) @RequestBody PatientRequestDTO patientRequestDTO
    ){
        PatientResponseDTO patient = patientService.updatePatient(id, patientRequestDTO);

        return ResponseEntity.ok(patient);
    }

    //4. Delete patient
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a patient")
    public  ResponseEntity<Void> deletePatient(@PathVariable UUID id){
        patientService.deletePatient(id);

        return ResponseEntity.noContent().build();
    }

}
