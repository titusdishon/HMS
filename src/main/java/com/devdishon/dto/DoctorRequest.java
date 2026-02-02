package com.devdishon.dto;

import com.devdishon.entity.Specialization;

public record DoctorRequest(
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        String licenseNumber,
        Specialization specialization,
        String department,
        Integer yearsOfExperience
) {}
