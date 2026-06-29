package com.patient.service.controller;

import com.patient.service.dto.PatientResponseDTO;
import com.patient.service.service.PatientService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService){
        this.patientService = patientService;
    }

    //1. Get all patients
    @GetMapping
    public ResponseEntity<List<PatientResponseDTO>> getPatients(){

        List<PatientResponseDTO> patients = patientService.getPatients();

        return ResponseEntity.ok(patients);

    }

}
