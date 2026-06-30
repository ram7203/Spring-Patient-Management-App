package com.patient.service.dto;


import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PatientResponseDTO {

    private String id;

    private String name;

    private String email;

    private String address;

    private String dateOfBirth;

}
