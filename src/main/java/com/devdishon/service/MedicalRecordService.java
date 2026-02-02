package com.devdishon.service;

import com.devdishon.entity.Appointment;
import com.devdishon.entity.Doctor;
import com.devdishon.entity.MedicalRecord;
import com.devdishon.entity.Patient;
import com.devdishon.repository.AppointmentRepository;
import com.devdishon.repository.DoctorRepository;
import com.devdishon.repository.MedicalRecordRepository;
import com.devdishon.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;

    public MedicalRecordService(MedicalRecordRepository medicalRecordRepository,
                                PatientRepository patientRepository,
                                DoctorRepository doctorRepository,
                                AppointmentRepository appointmentRepository) {
        this.medicalRecordRepository = medicalRecordRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
    }

    public List<MedicalRecord> getAllMedicalRecords() {
        return medicalRecordRepository.findAll();
    }

    public MedicalRecord getMedicalRecordById(Long id) {
        return medicalRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Medical record with id " + id + " not found"));
    }

    public List<MedicalRecord> getMedicalRecordsByPatientId(Long patientId) {
        return medicalRecordRepository.findByPatientIdOrderByRecordDateDesc(patientId);
    }

    public List<MedicalRecord> getMedicalRecordsByDoctorId(Long doctorId) {
        return medicalRecordRepository.findByDoctorId(doctorId);
    }

    public MedicalRecord getMedicalRecordByAppointmentId(Long appointmentId) {
        return medicalRecordRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new IllegalStateException("Medical record for appointment " + appointmentId + " not found"));
    }

    public List<MedicalRecord> getPendingFollowUps(LocalDateTime start, LocalDateTime end) {
        return medicalRecordRepository.findPendingFollowUps(start, end);
    }

    public MedicalRecord createMedicalRecord(Long patientId, Long doctorId, Long appointmentId, MedicalRecord record) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalStateException("Patient with id " + patientId + " not found"));

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalStateException("Doctor with id " + doctorId + " not found"));

        record.setPatient(patient);
        record.setDoctor(doctor);

        if (appointmentId != null) {
            Appointment appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new IllegalStateException("Appointment with id " + appointmentId + " not found"));
            record.setAppointment(appointment);
        }

        return medicalRecordRepository.save(record);
    }

    public MedicalRecord updateMedicalRecord(Long id, MedicalRecord updatedRecord) {
        MedicalRecord existingRecord = getMedicalRecordById(id);

        existingRecord.setDiagnosis(updatedRecord.getDiagnosis());
        existingRecord.setSymptoms(updatedRecord.getSymptoms());
        existingRecord.setTreatment(updatedRecord.getTreatment());
        existingRecord.setPrescription(updatedRecord.getPrescription());
        existingRecord.setLabResults(updatedRecord.getLabResults());
        existingRecord.setNotes(updatedRecord.getNotes());
        existingRecord.setFollowUpDate(updatedRecord.getFollowUpDate());

        return medicalRecordRepository.save(existingRecord);
    }

    public MedicalRecord addLabResults(Long id, String labResults) {
        MedicalRecord record = getMedicalRecordById(id);
        record.setLabResults(labResults);
        return medicalRecordRepository.save(record);
    }

    public MedicalRecord setFollowUpDate(Long id, LocalDateTime followUpDate) {
        MedicalRecord record = getMedicalRecordById(id);
        record.setFollowUpDate(followUpDate);
        return medicalRecordRepository.save(record);
    }

    public void deleteMedicalRecord(Long id) {
        if (!medicalRecordRepository.existsById(id)) {
            throw new IllegalStateException("Medical record with id " + id + " does not exist");
        }
        medicalRecordRepository.deleteById(id);
    }
}
