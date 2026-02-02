package com.devdishon.controller;

import com.devdishon.dto.PatientRequest;
import com.devdishon.entity.Patient;
import com.devdishon.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/patients")
@Tag(name = "Patients", description = "Patient management endpoints")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping
    @Operation(summary = "Get all patients", description = "Returns a list of all patients")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved patients"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<Patient>> getAllPatients() {
        return ResponseEntity.ok(patientService.getAllPatients());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get patient by ID", description = "Returns a patient by their ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved patient"),
            @ApiResponse(responseCode = "404", description = "Patient not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Patient> getPatientById(@PathVariable Long id) {
        return ResponseEntity.ok(patientService.getPatientById(id));
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Get patient by email", description = "Returns a patient by their email address")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Patient> getPatientByEmail(@PathVariable String email) {
        return ResponseEntity.ok(patientService.getPatientByEmail(email));
    }

    @PostMapping
    @Operation(summary = "Create a patient", description = "Creates a new patient record")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Patient created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Patient> createPatient(@Valid @RequestBody PatientRequest request) {
        Patient patient = new Patient(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phoneNumber(),
                request.dateOfBirth(),
                request.gender(),
                request.address(),
                request.nationalId(),
                request.bloodType(),
                request.emergencyContactName(),
                request.emergencyContactPhone()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(patientService.createPatient(patient));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a patient", description = "Updates an existing patient record")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Patient updated successfully"),
            @ApiResponse(responseCode = "404", description = "Patient not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Patient> updatePatient(@PathVariable Long id, @Valid @RequestBody PatientRequest request) {
        Patient patient = new Patient(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phoneNumber(),
                request.dateOfBirth(),
                request.gender(),
                request.address(),
                request.nationalId(),
                request.bloodType(),
                request.emergencyContactName(),
                request.emergencyContactPhone()
        );
        return ResponseEntity.ok(patientService.updatePatient(id, patient));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a patient", description = "Deletes a patient record")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Patient deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Patient not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SUPER_ADMIN role")
    })
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deletePatient(@PathVariable Long id) {
        patientService.deletePatient(id);
        return ResponseEntity.noContent().build();
    }
}
