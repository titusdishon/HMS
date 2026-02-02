package com.devdishon.service;

import com.devdishon.entity.Patient;
import com.devdishon.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PatientService {

    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public List<Patient> getAllPatients() {
        return patientRepository.findAll();
    }

    public Patient getPatientById(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Patient with id " + id + " not found"));
    }

    public Patient getPatientByEmail(String email) {
        return patientRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Patient with email " + email + " not found"));
    }

    public Patient createPatient(Patient patient) {
        if (patientRepository.existsByEmail(patient.getEmail())) {
            throw new IllegalStateException("Patient with email " + patient.getEmail() + " already exists");
        }
        if (patient.getNationalId() != null && patientRepository.existsByNationalId(patient.getNationalId())) {
            throw new IllegalStateException("Patient with national ID " + patient.getNationalId() + " already exists");
        }
        return patientRepository.save(patient);
    }

    public Patient updatePatient(Long id, Patient updatedPatient) {
        Patient existingPatient = getPatientById(id);

        existingPatient.setFirstName(updatedPatient.getFirstName());
        existingPatient.setLastName(updatedPatient.getLastName());
        existingPatient.setPhoneNumber(updatedPatient.getPhoneNumber());
        existingPatient.setDateOfBirth(updatedPatient.getDateOfBirth());
        existingPatient.setGender(updatedPatient.getGender());
        existingPatient.setAddress(updatedPatient.getAddress());
        existingPatient.setBloodType(updatedPatient.getBloodType());
        existingPatient.setEmergencyContactName(updatedPatient.getEmergencyContactName());
        existingPatient.setEmergencyContactPhone(updatedPatient.getEmergencyContactPhone());

        return patientRepository.save(existingPatient);
    }

    public void deletePatient(Long id) {
        if (!patientRepository.existsById(id)) {
            throw new IllegalStateException("Patient with id " + id + " does not exist");
        }
        patientRepository.deleteById(id);
    }
}
