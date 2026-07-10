package com.patient.service.service;

import com.patient.service.dto.PatientRequestDTO;
import com.patient.service.dto.PatientResponseDTO;
import com.patient.service.exception.EmailAlreadyExistsException;
import com.patient.service.exception.PatientNotFoundException;
import com.patient.service.grpc.BillingServiceGrpcClient;
import com.patient.service.kafka.KafkaProducer;
import com.patient.service.mappper.PatientMapper;
import com.patient.service.model.Patient;
import com.patient.service.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PatientService {

    private final PatientRepository patientRepository;

    private BillingServiceGrpcClient billingServiceGrpcClient;

    private KafkaProducer kafkaProducer;

     public PatientService(PatientRepository patientRepository,
                           BillingServiceGrpcClient billingServiceGrpcClient,
                           KafkaProducer kafkaProducer){
         this.patientRepository = patientRepository;
         this.billingServiceGrpcClient = billingServiceGrpcClient;
         this.kafkaProducer = kafkaProducer;
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

         if(patientRepository.existsByEmail(patientRequestDTO.getEmail())){
             throw new EmailAlreadyExistsException("A patient with this email already exists: "+ patientRequestDTO.getEmail());
         }

         Patient patient = patientRepository.save(PatientMapper.toModel(patientRequestDTO));

         billingServiceGrpcClient.createBillingAccount(String.valueOf(patient.getId()), patient.getName(), patient.getEmail());

         kafkaProducer.sendEvent(patient);

         return PatientMapper.toDTO(patient);

    }

    //Update Patients
    public PatientResponseDTO updatePatient(UUID id, PatientRequestDTO patientRequestDTO){

         Patient patient = patientRepository.findById(id)
                 .orElseThrow(() -> new PatientNotFoundException("Patient not found with id: " + id));

        // Only throw an error if the email exists AND belongs to someone else
        if (patientRepository.existsByEmailAndIdNot(patientRequestDTO.getEmail(), id)) {
            throw new EmailAlreadyExistsException("A patient with this email already exists: " + patientRequestDTO.getEmail());
        }

        patient.setName(patientRequestDTO.getName());
        patient.setEmail(patientRequestDTO.getEmail());
        patient.setAddress(patientRequestDTO.getAddress());
        patient.setDateOfBirth(LocalDate.parse(patientRequestDTO.getDateOfBirth()));

        Patient patient1 = patientRepository.save(patient);

         PatientResponseDTO patientResponseDTO = PatientMapper.toDTO(patient1);

         return patientResponseDTO;
    }

    //Delete patient
    public void deletePatient(UUID id){
         Patient patient = patientRepository.findById(id).orElseThrow(
                 () -> new PatientNotFoundException("Patient not found with id: "+ id)
         );

         patientRepository.deleteById(id);
    }

}
