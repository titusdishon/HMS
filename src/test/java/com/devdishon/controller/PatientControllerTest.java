package com.devdishon.controller;

import com.devdishon.AbstractIntegrationTest;
import com.devdishon.dto.PatientRequest;
import com.devdishon.dto.auth.AuthResponse;
import com.devdishon.dto.auth.RegisterRequest;
import com.devdishon.entity.BloodType;
import com.devdishon.entity.Gender;
import com.devdishon.entity.Role;
import com.devdishon.entity.RoleName;
import com.devdishon.entity.User;
import com.devdishon.repository.PatientRepository;
import com.devdishon.repository.RoleRepository;
import com.devdishon.repository.UserRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class PatientControllerTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        patientRepository.deleteAll();
        userRepository.deleteAll();

        // Ensure roles exist
        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.USER)));
        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ADMIN)));
        Role superAdminRole = roleRepository.findByName(RoleName.SUPER_ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.SUPER_ADMIN)));

        // Get admin token
        adminToken = registerAndGetToken("admin", "admin@test.com", "password123");

        // Update user to have admin role
        User adminUser = userRepository.findByEmail("admin@test.com").orElseThrow();
        adminUser.setRoles(Set.of(adminRole, userRole));
        userRepository.save(adminUser);

        // Re-login to get token with updated roles
        adminToken = loginAndGetToken("admin@test.com", "password123");

        // Get user token
        userToken = registerAndGetToken("user", "user@test.com", "password123");
    }

    private String registerAndGetToken(String username, String email, String password) {
        RegisterRequest request = new RegisterRequest(username, email, password, "Test", "User");
        return given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(200)
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
    @DisplayName("Should get all patients with USER role")
    void shouldGetAllPatientsWithUserRole() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get("/api/v1/patients")
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
    }

    @Test
    @DisplayName("Should create patient with ADMIN role")
    void shouldCreatePatientWithAdminRole() {
        PatientRequest request = new PatientRequest(
                "John",
                "Doe",
                "john.doe@example.com",
                "1234567890",
                LocalDate.of(1990, 1, 15),
                Gender.MALE,
                BloodType.O_POSITIVE,
                "123 Main St",
                "Contact Person",
                "0987654321"
        );

        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/patients")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("firstName", equalTo("John"))
                .body("lastName", equalTo("Doe"))
                .body("email", equalTo("john.doe@example.com"));
    }

    @Test
    @DisplayName("Should fail to create patient with USER role")
    void shouldFailToCreatePatientWithUserRole() {
        PatientRequest request = new PatientRequest(
                "Jane",
                "Doe",
                "jane.doe@example.com",
                "1234567890",
                LocalDate.of(1990, 1, 15),
                Gender.FEMALE,
                BloodType.A_NEGATIVE,
                "456 Main St",
                "Contact",
                "0987654321"
        );

        given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/patients")
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("Should return 401 for unauthenticated request")
    void shouldReturn401ForUnauthenticatedRequest() {
        given()
                .when()
                .get("/api/v1/patients")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("Should get patient by ID")
    void shouldGetPatientById() {
        // First create a patient
        PatientRequest request = new PatientRequest(
                "Get",
                "ById",
                "getbyid@example.com",
                "1234567890",
                LocalDate.of(1985, 5, 20),
                Gender.MALE,
                BloodType.B_POSITIVE,
                "789 Main St",
                "Emergency Contact",
                "1112223333"
        );

        Integer patientId = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/patients")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        // Then get the patient
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get("/api/v1/patients/" + patientId)
                .then()
                .statusCode(200)
                .body("id", equalTo(patientId))
                .body("firstName", equalTo("Get"))
                .body("lastName", equalTo("ById"));
    }

    @Test
    @DisplayName("Should return 404 for non-existent patient")
    void shouldReturn404ForNonExistentPatient() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get("/api/v1/patients/99999")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("Should update patient with ADMIN role")
    void shouldUpdatePatientWithAdminRole() {
        // Create a patient first
        PatientRequest createRequest = new PatientRequest(
                "Update",
                "Me",
                "updateme@example.com",
                "1234567890",
                LocalDate.of(1992, 8, 10),
                Gender.FEMALE,
                BloodType.AB_POSITIVE,
                "Update St",
                "Contact",
                "4445556666"
        );

        Integer patientId = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(createRequest)
                .when()
                .post("/api/v1/patients")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        // Update the patient
        PatientRequest updateRequest = new PatientRequest(
                "Updated",
                "Name",
                "updateme@example.com",
                "1234567890",
                LocalDate.of(1992, 8, 10),
                Gender.FEMALE,
                BloodType.AB_POSITIVE,
                "New Address",
                "New Contact",
                "7778889999"
        );

        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(updateRequest)
                .when()
                .put("/api/v1/patients/" + patientId)
                .then()
                .statusCode(200)
                .body("firstName", equalTo("Updated"))
                .body("lastName", equalTo("Name"))
                .body("address", equalTo("New Address"));
    }
}
