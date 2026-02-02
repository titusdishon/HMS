package com.devdishon.controller;

import com.devdishon.AbstractIntegrationTest;
import com.devdishon.dto.DoctorRequest;
import com.devdishon.dto.auth.RegisterRequest;
import com.devdishon.entity.Role;
import com.devdishon.entity.RoleName;
import com.devdishon.entity.Specialization;
import com.devdishon.entity.User;
import com.devdishon.repository.DoctorRepository;
import com.devdishon.repository.RefreshTokenRepository;
import com.devdishon.repository.RoleRepository;
import com.devdishon.repository.UserRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class DoctorControllerTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private String adminToken;
    private String superAdminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        doctorRepository.deleteAll();
        // Delete refresh tokens first to avoid FK constraint violations
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        // Ensure roles exist
        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.USER)));
        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ADMIN)));
        Role superAdminRole = roleRepository.findByName(RoleName.SUPER_ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.SUPER_ADMIN)));

        // Create users with different roles
        userToken = registerAndGetToken("user", "user@test.com", "password123");

        adminToken = registerAndGetToken("admin", "admin@test.com", "password123");
        User adminUser = userRepository.findByEmail("admin@test.com").orElseThrow();
        adminUser.setRoles(Set.of(adminRole, userRole));
        userRepository.save(adminUser);
        adminToken = loginAndGetToken("admin@test.com", "password123");

        superAdminToken = registerAndGetToken("superadmin", "superadmin@test.com", "password123");
        User superAdminUser = userRepository.findByEmail("superadmin@test.com").orElseThrow();
        superAdminUser.setRoles(Set.of(superAdminRole, adminRole, userRole));
        userRepository.save(superAdminUser);
        superAdminToken = loginAndGetToken("superadmin@test.com", "password123");
    }

    private String registerAndGetToken(String firstName, String email, String password) {
        RegisterRequest request = new RegisterRequest(firstName, "User", email, password);
        return given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(201)
                .extract()
                .path("accessToken");
    }

    private String loginAndGetToken(String email, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body("{\"email\": \"" + email + "\", \"password\": \"" + password + "\"}")
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .path("accessToken");
    }

    @Test
    @DisplayName("Should get all doctors with USER role")
    void shouldGetAllDoctorsWithUserRole() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get("/api/v1/doctors")
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
    }

    @Test
    @DisplayName("Should create doctor with ADMIN role")
    void shouldCreateDoctorWithAdminRole() {
        DoctorRequest request = new DoctorRequest(
                "Dr. John",
                "Smith",
                "dr.smith@hospital.com",
                "1234567890",
                "LIC123456",
                Specialization.CARDIOLOGY,
                "Cardiology Department",
                10
        );

        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/doctors")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("firstName", equalTo("Dr. John"))
                .body("lastName", equalTo("Smith"))
                .body("specialization", equalTo("CARDIOLOGY"));
    }

    @Test
    @DisplayName("Should fail to create doctor with USER role")
    void shouldFailToCreateDoctorWithUserRole() {
        DoctorRequest request = new DoctorRequest(
                "Dr. Jane",
                "Doe",
                "dr.doe@hospital.com",
                "0987654321",
                "LIC654321",
                Specialization.DERMATOLOGY,
                "Dermatology Department",
                5
        );

        given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/doctors")
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("Should delete doctor with SUPER_ADMIN role only")
    void shouldDeleteDoctorWithSuperAdminRole() {
        // Create a doctor first
        DoctorRequest request = new DoctorRequest(
                "Dr. Delete",
                "Me",
                "dr.delete@hospital.com",
                "5555555555",
                "LIC999999",
                Specialization.NEUROLOGY,
                "Neurology Department",
                15
        );

        Integer doctorId = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/doctors")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        // Admin should not be able to delete
        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .delete("/api/v1/doctors/" + doctorId)
                .then()
                .statusCode(403);

        // Super admin should be able to delete
        given()
                .header("Authorization", "Bearer " + superAdminToken)
                .when()
                .delete("/api/v1/doctors/" + doctorId)
                .then()
                .statusCode(204);

        // Verify deletion
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get("/api/v1/doctors/" + doctorId)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("Should get doctors by specialization")
    void shouldGetDoctorsBySpecialization() {
        // Create doctors with different specializations
        DoctorRequest cardiologist = new DoctorRequest(
                "Dr. Heart",
                "Expert",
                "dr.heart@hospital.com",
                "1111111111",
                "LIC111111",
                Specialization.CARDIOLOGY,
                "Cardiology",
                20
        );

        DoctorRequest dermatologist = new DoctorRequest(
                "Dr. Skin",
                "Expert",
                "dr.skin@hospital.com",
                "2222222222",
                "LIC222222",
                Specialization.DERMATOLOGY,
                "Dermatology",
                15
        );

        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(cardiologist)
                .when()
                .post("/api/v1/doctors");

        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(dermatologist)
                .when()
                .post("/api/v1/doctors");

        // Get cardiologists
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get("/api/v1/doctors/specialization/CARDIOLOGY")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].specialization", equalTo("CARDIOLOGY"));
    }

    @Test
    @DisplayName("Should update doctor availability")
    void shouldUpdateDoctorAvailability() {
        // Create a doctor
        DoctorRequest request = new DoctorRequest(
                "Dr. Available",
                "Doctor",
                "dr.available@hospital.com",
                "3333333333",
                "LIC333333",
                Specialization.GENERAL_PRACTICE,
                "General",
                8
        );

        Integer doctorId = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/doctors")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        // Update availability to false
        given()
                .header("Authorization", "Bearer " + adminToken)
                .param("isAvailable", false)
                .when()
                .patch("/api/v1/doctors/" + doctorId + "/availability")
                .then()
                .statusCode(200)
                .body("available", equalTo(false));

        // Verify the doctor is not in available list
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get("/api/v1/doctors/available")
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
    }
}
