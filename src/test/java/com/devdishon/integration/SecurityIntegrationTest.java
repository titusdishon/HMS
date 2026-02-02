package com.devdishon.integration;

import com.devdishon.AbstractIntegrationTest;
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

class SecurityIntegrationTest extends AbstractIntegrationTest {

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
    @DisplayName("Public endpoints should be accessible without authentication")
    void publicEndpointsShouldBeAccessible() {
        // Auth endpoints
        given()
                .contentType(ContentType.JSON)
                .body("{\"email\": \"test@test.com\", \"password\": \"wrong\"}")
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(anyOf(equalTo(401), equalTo(400))); // Either unauthorized or bad request, but not 403

        // Health endpoint
        given()
                .when()
                .get("/actuator/health")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("Protected endpoints should return 401 without token")
    void protectedEndpointsShouldReturn401WithoutToken() {
        given()
                .when()
                .get("/api/v1/patients")
                .then()
                .statusCode(401);

        given()
                .when()
                .get("/api/v1/doctors")
                .then()
                .statusCode(401);

        given()
                .when()
                .get("/api/v1/appointments")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("Should return 401 with invalid token")
    void shouldReturn401WithInvalidToken() {
        given()
                .header("Authorization", "Bearer invalid.token.here")
                .when()
                .get("/api/v1/patients")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("Should return 401 with malformed Authorization header")
    void shouldReturn401WithMalformedAuthHeader() {
        given()
                .header("Authorization", "InvalidFormat token")
                .when()
                .get("/api/v1/patients")
                .then()
                .statusCode(401);

        given()
                .header("Authorization", "Bearer")
                .when()
                .get("/api/v1/patients")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("Should access protected endpoint with valid token")
    void shouldAccessProtectedEndpointWithValidToken() {
        // Register user
        RegisterRequest request = new RegisterRequest(
                "Security",
                "Test",
                "security@test.com",
                "password123"
        );

        String token = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(201) // Register returns 201 CREATED
                .extract()
                .path("accessToken");

        // Access protected endpoint
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/v1/patients")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("Token should work across multiple requests")
    void tokenShouldWorkAcrossMultipleRequests() {
        RegisterRequest request = new RegisterRequest(
                "Multi",
                "Test",
                "multi@test.com",
                "password123"
        );

        String token = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(201) // Register returns 201 CREATED
                .extract()
                .path("accessToken");

        // Make multiple requests with same token
        for (int i = 0; i < 3; i++) {
            given()
                    .header("Authorization", "Bearer " + token)
                    .when()
                    .get("/api/v1/patients")
                    .then()
                    .statusCode(200);
        }
    }

    @Test
    @DisplayName("CORS headers should be present")
    void corsHeadersShouldBePresent() {
        given()
                .header("Origin", "http://localhost:3000")
                .when()
                .options("/api/v1/auth/login")
                .then()
                .header("Access-Control-Allow-Origin", notNullValue());
    }
}
