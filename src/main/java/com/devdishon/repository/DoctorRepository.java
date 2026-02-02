package com.devdishon.repository;

import com.devdishon.entity.Doctor;
import com.devdishon.entity.Specialization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    Optional<Doctor> findByEmail(String email);

    Optional<Doctor> findByLicenseNumber(String licenseNumber);

    List<Doctor> findBySpecialization(Specialization specialization);

    List<Doctor> findByIsAvailableTrue();

    List<Doctor> findBySpecializationAndIsAvailableTrue(Specialization specialization);

    boolean existsByEmail(String email);

    boolean existsByLicenseNumber(String licenseNumber);
}
