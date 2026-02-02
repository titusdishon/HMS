package com.devdishon.service;

import com.devdishon.entity.Doctor;
import com.devdishon.entity.Specialization;
import com.devdishon.repository.DoctorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DoctorService {

    private final DoctorRepository doctorRepository;

    public DoctorService(DoctorRepository doctorRepository) {
        this.doctorRepository = doctorRepository;
    }

    public List<Doctor> getAllDoctors() {
        return doctorRepository.findAll();
    }

    public Doctor getDoctorById(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Doctor with id " + id + " not found"));
    }

    public List<Doctor> getDoctorsBySpecialization(Specialization specialization) {
        return doctorRepository.findBySpecialization(specialization);
    }

    public List<Doctor> getAvailableDoctors() {
        return doctorRepository.findByIsAvailableTrue();
    }

    public List<Doctor> getAvailableDoctorsBySpecialization(Specialization specialization) {
        return doctorRepository.findBySpecializationAndIsAvailableTrue(specialization);
    }

    public Doctor createDoctor(Doctor doctor) {
        if (doctorRepository.existsByEmail(doctor.getEmail())) {
            throw new IllegalStateException("Doctor with email " + doctor.getEmail() + " already exists");
        }
        if (doctorRepository.existsByLicenseNumber(doctor.getLicenseNumber())) {
            throw new IllegalStateException("Doctor with license number " + doctor.getLicenseNumber() + " already exists");
        }
        return doctorRepository.save(doctor);
    }

    public Doctor updateDoctor(Long id, Doctor updatedDoctor) {
        Doctor existingDoctor = getDoctorById(id);

        existingDoctor.setFirstName(updatedDoctor.getFirstName());
        existingDoctor.setLastName(updatedDoctor.getLastName());
        existingDoctor.setPhoneNumber(updatedDoctor.getPhoneNumber());
        existingDoctor.setSpecialization(updatedDoctor.getSpecialization());
        existingDoctor.setDepartment(updatedDoctor.getDepartment());
        existingDoctor.setYearsOfExperience(updatedDoctor.getYearsOfExperience());
        existingDoctor.setIsAvailable(updatedDoctor.getIsAvailable());

        return doctorRepository.save(existingDoctor);
    }

    public Doctor updateAvailability(Long id, Boolean isAvailable) {
        Doctor doctor = getDoctorById(id);
        doctor.setIsAvailable(isAvailable);
        return doctorRepository.save(doctor);
    }

    public void deleteDoctor(Long id) {
        if (!doctorRepository.existsById(id)) {
            throw new IllegalStateException("Doctor with id " + id + " does not exist");
        }
        doctorRepository.deleteById(id);
    }
}
