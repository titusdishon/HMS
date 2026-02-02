package com.devdishon.controller;

import com.devdishon.dto.MedicalRecordRequest;
import com.devdishon.entity.MedicalRecord;
import com.devdishon.service.MedicalRecordService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/medical-records")
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    public MedicalRecordController(MedicalRecordService medicalRecordService) {
        this.medicalRecordService = medicalRecordService;
    }

    @GetMapping
    public ResponseEntity<List<MedicalRecord>> getAllMedicalRecords() {
        return ResponseEntity.ok(medicalRecordService.getAllMedicalRecords());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicalRecord> getMedicalRecordById(@PathVariable Long id) {
        return ResponseEntity.ok(medicalRecordService.getMedicalRecordById(id));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<MedicalRecord>> getMedicalRecordsByPatientId(@PathVariable Long patientId) {
        return ResponseEntity.ok(medicalRecordService.getMedicalRecordsByPatientId(patientId));
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<MedicalRecord>> getMedicalRecordsByDoctorId(@PathVariable Long doctorId) {
        return ResponseEntity.ok(medicalRecordService.getMedicalRecordsByDoctorId(doctorId));
    }

    @GetMapping("/appointment/{appointmentId}")
    public ResponseEntity<MedicalRecord> getMedicalRecordByAppointmentId(@PathVariable Long appointmentId) {
        return ResponseEntity.ok(medicalRecordService.getMedicalRecordByAppointmentId(appointmentId));
    }

    @GetMapping("/follow-ups")
    public ResponseEntity<List<MedicalRecord>> getPendingFollowUps(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(medicalRecordService.getPendingFollowUps(start, end));
    }

    @PostMapping
    public ResponseEntity<MedicalRecord> createMedicalRecord(@RequestBody MedicalRecordRequest request) {
        MedicalRecord record = new MedicalRecord();
        record.setDiagnosis(request.diagnosis());
        record.setSymptoms(request.symptoms());
        record.setTreatment(request.treatment());
        record.setPrescription(request.prescription());
        record.setLabResults(request.labResults());
        record.setNotes(request.notes());
        record.setFollowUpDate(request.followUpDate());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(medicalRecordService.createMedicalRecord(
                        request.patientId(),
                        request.doctorId(),
                        request.appointmentId(),
                        record));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MedicalRecord> updateMedicalRecord(@PathVariable Long id, @RequestBody MedicalRecordRequest request) {
        MedicalRecord record = new MedicalRecord();
        record.setDiagnosis(request.diagnosis());
        record.setSymptoms(request.symptoms());
        record.setTreatment(request.treatment());
        record.setPrescription(request.prescription());
        record.setLabResults(request.labResults());
        record.setNotes(request.notes());
        record.setFollowUpDate(request.followUpDate());

        return ResponseEntity.ok(medicalRecordService.updateMedicalRecord(id, record));
    }

    @PatchMapping("/{id}/lab-results")
    public ResponseEntity<MedicalRecord> addLabResults(@PathVariable Long id, @RequestBody String labResults) {
        return ResponseEntity.ok(medicalRecordService.addLabResults(id, labResults));
    }

    @PatchMapping("/{id}/follow-up")
    public ResponseEntity<MedicalRecord> setFollowUpDate(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime followUpDate) {
        return ResponseEntity.ok(medicalRecordService.setFollowUpDate(id, followUpDate));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMedicalRecord(@PathVariable Long id) {
        medicalRecordService.deleteMedicalRecord(id);
        return ResponseEntity.noContent().build();
    }
}
