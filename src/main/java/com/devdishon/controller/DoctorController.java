package com.devdishon.controller;

import com.devdishon.dto.DoctorRequest;
import com.devdishon.entity.Doctor;
import com.devdishon.entity.Specialization;
import com.devdishon.service.DoctorService;
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
@RequestMapping("/api/v1/doctors")
@Tag(name = "Doctors", description = "Doctor management endpoints")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @GetMapping
    @Operation(summary = "Get all doctors", description = "Returns a list of all doctors")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<Doctor>> getAllDoctors() {
        return ResponseEntity.ok(doctorService.getAllDoctors());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get doctor by ID", description = "Returns a doctor by their ID")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Doctor> getDoctorById(@PathVariable Long id) {
        return ResponseEntity.ok(doctorService.getDoctorById(id));
    }

    @GetMapping("/specialization/{specialization}")
    @Operation(summary = "Get doctors by specialization", description = "Returns doctors filtered by specialization")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<Doctor>> getDoctorsBySpecialization(@PathVariable Specialization specialization) {
        return ResponseEntity.ok(doctorService.getDoctorsBySpecialization(specialization));
    }

    @GetMapping("/available")
    @Operation(summary = "Get available doctors", description = "Returns all available doctors")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<Doctor>> getAvailableDoctors() {
        return ResponseEntity.ok(doctorService.getAvailableDoctors());
    }

    @GetMapping("/available/specialization/{specialization}")
    @Operation(summary = "Get available doctors by specialization", description = "Returns available doctors filtered by specialization")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<Doctor>> getAvailableDoctorsBySpecialization(@PathVariable Specialization specialization) {
        return ResponseEntity.ok(doctorService.getAvailableDoctorsBySpecialization(specialization));
    }

    @PostMapping
    @Operation(summary = "Create a doctor", description = "Creates a new doctor record")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Doctor created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Doctor> createDoctor(@Valid @RequestBody DoctorRequest request) {
        Doctor doctor = new Doctor(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phoneNumber(),
                request.licenseNumber(),
                request.specialization(),
                request.department(),
                request.yearsOfExperience()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(doctorService.createDoctor(doctor));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a doctor", description = "Updates an existing doctor record")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Doctor> updateDoctor(@PathVariable Long id, @Valid @RequestBody DoctorRequest request) {
        Doctor doctor = new Doctor(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phoneNumber(),
                request.licenseNumber(),
                request.specialization(),
                request.department(),
                request.yearsOfExperience()
        );
        return ResponseEntity.ok(doctorService.updateDoctor(id, doctor));
    }

    @PatchMapping("/{id}/availability")
    @Operation(summary = "Update doctor availability", description = "Updates the availability status of a doctor")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Doctor> updateAvailability(@PathVariable Long id, @RequestParam Boolean isAvailable) {
        return ResponseEntity.ok(doctorService.updateAvailability(id, isAvailable));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a doctor", description = "Deletes a doctor record")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Doctor deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SUPER_ADMIN role")
    })
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteDoctor(@PathVariable Long id) {
        doctorService.deleteDoctor(id);
        return ResponseEntity.noContent().build();
    }
}
