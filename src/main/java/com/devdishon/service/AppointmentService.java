package com.devdishon.service;

import com.devdishon.entity.Appointment;
import com.devdishon.entity.AppointmentStatus;
import com.devdishon.entity.Doctor;
import com.devdishon.entity.Patient;
import com.devdishon.repository.AppointmentRepository;
import com.devdishon.repository.DoctorRepository;
import com.devdishon.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              PatientRepository patientRepository,
                              DoctorRepository doctorRepository) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
    }

    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    public Appointment getAppointmentById(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Appointment with id " + id + " not found"));
    }

    public List<Appointment> getAppointmentsByPatientId(Long patientId) {
        return appointmentRepository.findByPatientId(patientId);
    }

    public List<Appointment> getAppointmentsByDoctorId(Long doctorId) {
        return appointmentRepository.findByDoctorId(doctorId);
    }

    public List<Appointment> getAppointmentsByStatus(AppointmentStatus status) {
        return appointmentRepository.findByStatus(status);
    }

    public List<Appointment> getUpcomingAppointmentsByPatientId(Long patientId) {
        return appointmentRepository.findUpcomingAppointmentsByPatientId(patientId, LocalDateTime.now());
    }

    public List<Appointment> getAppointmentsByDateRange(LocalDateTime start, LocalDateTime end) {
        return appointmentRepository.findByDateRange(start, end);
    }

    public Appointment createAppointment(Long patientId, Long doctorId, Appointment appointment) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalStateException("Patient with id " + patientId + " not found"));

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalStateException("Doctor with id " + doctorId + " not found"));

        if (!doctor.getIsAvailable()) {
            throw new IllegalStateException("Doctor is not available for appointments");
        }

        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        return appointmentRepository.save(appointment);
    }

    public Appointment updateAppointmentStatus(Long id, AppointmentStatus status) {
        Appointment appointment = getAppointmentById(id);
        appointment.setStatus(status);
        return appointmentRepository.save(appointment);
    }

    public Appointment rescheduleAppointment(Long id, LocalDateTime newDateTime) {
        Appointment appointment = getAppointmentById(id);
        appointment.setAppointmentDateTime(newDateTime);
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        return appointmentRepository.save(appointment);
    }

    public Appointment cancelAppointment(Long id) {
        return updateAppointmentStatus(id, AppointmentStatus.CANCELLED);
    }

    public Appointment addNotes(Long id, String notes) {
        Appointment appointment = getAppointmentById(id);
        appointment.setNotes(notes);
        return appointmentRepository.save(appointment);
    }

    public void deleteAppointment(Long id) {
        if (!appointmentRepository.existsById(id)) {
            throw new IllegalStateException("Appointment with id " + id + " does not exist");
        }
        appointmentRepository.deleteById(id);
    }
}
