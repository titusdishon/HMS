package com.devdishon.repository;

import com.devdishon.entity.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {

    List<MedicalRecord> findByPatientId(Long patientId);

    List<MedicalRecord> findByDoctorId(Long doctorId);

    Optional<MedicalRecord> findByAppointmentId(Long appointmentId);

    @Query("SELECT m FROM MedicalRecord m WHERE m.patient.id = :patientId ORDER BY m.recordDate DESC")
    List<MedicalRecord> findByPatientIdOrderByRecordDateDesc(@Param("patientId") Long patientId);

    @Query("SELECT m FROM MedicalRecord m WHERE m.patient.id = :patientId AND m.recordDate BETWEEN :start AND :end")
    List<MedicalRecord> findByPatientIdAndDateRange(@Param("patientId") Long patientId,
                                                     @Param("start") LocalDateTime start,
                                                     @Param("end") LocalDateTime end);

    @Query("SELECT m FROM MedicalRecord m WHERE m.followUpDate IS NOT NULL AND m.followUpDate BETWEEN :start AND :end")
    List<MedicalRecord> findPendingFollowUps(@Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end);
}
