package com.devdishon.dto;

import java.time.LocalDateTime;

public record MedicalRecordRequest(
        Long patientId,
        Long doctorId,
        Long appointmentId,
        String diagnosis,
        String symptoms,
        String treatment,
        String prescription,
        String labResults,
        String notes,
        LocalDateTime followUpDate
) {}
