package com.patient.service.service;

import com.patient.service.dto.PatientRequestDTO;
import com.patient.service.dto.PatientResponseDTO;
import com.patient.service.mappper.PatientMapper;
import com.patient.service.model.Patient;
import com.patient.service.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PatientService {

    private final PatientRepository patientRepository;

     public PatientService(PatientRepository patientRepository){
         this.patientRepository = patientRepository;
     }

     //Get all patients
    public List<PatientResponseDTO> getPatients(){

         List<Patient> patients = patientRepository.findAll();

         List<PatientResponseDTO> patientResponseDTOS = patients.stream()
                 .map(PatientMapper::toDTO).toList();

         return patientResponseDTOS;
    }

    //Create Patients
    public PatientResponseDTO createPatient(PatientRequestDTO patientRequestDTO){

         Patient patient = patientRepository.save(PatientMapper.toModel(patientRequestDTO));

         return PatientMapper.toDTO(patient);

    }

}
