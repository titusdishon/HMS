package com.devdishon.dto;

import com.devdishon.entity.AppointmentType;

import java.time.LocalDateTime;

public record AppointmentRequest(
        Long patientId,
        Long doctorId,
        LocalDateTime appointmentDateTime,
        AppointmentType appointmentType,
        String reasonForVisit
) {}
