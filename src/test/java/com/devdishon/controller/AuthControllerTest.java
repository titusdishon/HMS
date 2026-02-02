package com.devdishon.controller;

import com.devdishon.AbstractIntegrationTest;
import com.devdishon.dto.auth.LoginRequest;
import com.devdishon.dto.auth.RegisterRequest;
import com.devdishon.entity.Role;
import com.devdishon.entity.RoleName;
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

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class AuthControllerTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        // Delete refresh tokens first to avoid FK constraint violations
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        // Ensure roles exist
        if (roleRepository.findByName(RoleName.USER).isEmpty()) {
            roleRepository.save(new Role(RoleName.USER));
        }
        if (roleRepository.findByName(RoleName.ADMIN).isEmpty()) {
            roleRepository.save(new Role(RoleName.ADMIN));
        }
        if (roleRepository.findByName(RoleName.SUPER_ADMIN).isEmpty()) {
            roleRepository.save(new Role(RoleName.SUPER_ADMIN));
        }
    }

    @Test
    @DisplayName("Should register a new user successfully")
    void shouldRegisterUserSuccessfully() {
        RegisterRequest request = new RegisterRequest(
                "Test",
                "User",
                "test@example.com",
                "password123"
        );

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(201)
                .body("accessToken", notNullValue())
                .body("refreshToken", notNullValue())
                .body("tokenType", equalTo("Bearer"));
    }

    @Test
    @DisplayName("Should fail registration with duplicate email")
    void shouldFailRegistrationWithDuplicateEmail() {
        RegisterRequest request = new RegisterRequest(
                "Test",
                "User",
                "duplicate@example.com",
                "password123"
        );

        // First registration
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(201);

        // Duplicate registration
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(409);
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void shouldLoginSuccessfully() {
        // First register a user
        RegisterRequest registerRequest = new RegisterRequest(
                "Login",
                "User",
                "login@example.com",
                "password123"
        );

        given()
                .contentType(ContentType.JSON)
                .body(registerRequest)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(201);

        // Then login
        LoginRequest loginRequest = new LoginRequest("login@example.com", "password123");

        given()
                .contentType(ContentType.JSON)
                .body(loginRequest)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .body("accessToken", notNullValue())
                .body("refreshToken", notNullValue());
    }

    @Test
    @DisplayName("Should fail login with invalid credentials")
    void shouldFailLoginWithInvalidCredentials() {
        LoginRequest loginRequest = new LoginRequest("nonexistent@example.com", "wrongpassword");

        given()
                .contentType(ContentType.JSON)
                .body(loginRequest)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("Should refresh token successfully")
    void shouldRefreshTokenSuccessfully() {
        // Register a user
        RegisterRequest registerRequest = new RegisterRequest(
                "Refresh",
                "User",
                "refresh@example.com",
                "password123"
        );

        String refreshToken = given()
                .contentType(ContentType.JSON)
                .body(registerRequest)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(201)
                .extract()
                .path("refreshToken");

        // Refresh token
        given()
                .contentType(ContentType.JSON)
                .body("{\"refreshToken\": \"" + refreshToken + "\"}")
                .when()
                .post("/api/v1/auth/refresh")
                .then()
                .statusCode(200)
                .body("accessToken", notNullValue());
    }

    @Test
    @DisplayName("Should validate registration input")
    void shouldValidateRegistrationInput() {
        RegisterRequest invalidRequest = new RegisterRequest(
                "", // Empty first name
                "", // Empty last name
                "invalid-email", // Invalid email
                "123" // Too short password
        );

        given()
                .contentType(ContentType.JSON)
                .body(invalidRequest)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(400);
    }
}
