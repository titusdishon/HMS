package com.devdishon.dto;

import com.devdishon.entity.BloodType;
import com.devdishon.entity.Gender;

import java.time.LocalDate;

public record PatientRequest(
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        LocalDate dateOfBirth,
        Gender gender,
        String address,
        String nationalId,
        BloodType bloodType,
        String emergencyContactName,
        String emergencyContactPhone
) {}
