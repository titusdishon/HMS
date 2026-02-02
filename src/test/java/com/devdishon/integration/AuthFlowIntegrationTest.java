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
import static org.assertj.core.api.Assertions.assertThat;

class AuthFlowIntegrationTest extends AbstractIntegrationTest {

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
    @DisplayName("Complete authentication flow: register -> login -> refresh -> logout")
    void completeAuthenticationFlow() {
        // Step 1: Register
        RegisterRequest registerRequest = new RegisterRequest(
                "Flow",
                "Test",
                "flow@test.com",
                "password123"
        );

        var registerResponse = given()
                .contentType(ContentType.JSON)
                .body(registerRequest)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(200)
                .body("accessToken", notNullValue())
                .body("refreshToken", notNullValue())
                .extract();

        String initialAccessToken = registerResponse.path("accessToken");
        String initialRefreshToken = registerResponse.path("refreshToken");

        // Step 2: Access protected resource with registration token
        given()
                .header("Authorization", "Bearer " + initialAccessToken)
                .when()
                .get("/api/v1/patients")
                .then()
                .statusCode(200);

        // Step 3: Login with same credentials
        var loginResponse = given()
                .contentType(ContentType.JSON)
                .body("{\"email\": \"flow@test.com\", \"password\": \"password123\"}")
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .body("accessToken", notNullValue())
                .body("refreshToken", notNullValue())
                .extract();

        String loginAccessToken = loginResponse.path("accessToken");
        String loginRefreshToken = loginResponse.path("refreshToken");

        // Step 4: Refresh token
        var refreshResponse = given()
                .contentType(ContentType.JSON)
                .body("{\"refreshToken\": \"" + loginRefreshToken + "\"}")
                .when()
                .post("/api/v1/auth/refresh")
                .then()
                .statusCode(200)
                .body("accessToken", notNullValue())
                .extract();

        String refreshedAccessToken = refreshResponse.path("accessToken");

        // Step 5: Access protected resource with refreshed token
        given()
                .header("Authorization", "Bearer " + refreshedAccessToken)
                .when()
                .get("/api/v1/patients")
                .then()
                .statusCode(200);

        // Step 6: Logout
        given()
                .header("Authorization", "Bearer " + refreshedAccessToken)
                .contentType(ContentType.JSON)
                .body("{\"refreshToken\": \"" + loginRefreshToken + "\"}")
                .when()
                .post("/api/v1/auth/logout")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("Should not be able to use refresh token after logout")
    void shouldNotUseRefreshTokenAfterLogout() {
        // Register
        RegisterRequest request = new RegisterRequest(
                "Logout",
                "Test",
                "logout@test.com",
                "password123"
        );

        var response = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(200)
                .extract();

        String accessToken = response.path("accessToken");
        String refreshToken = response.path("refreshToken");

        // Logout
        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body("{\"refreshToken\": \"" + refreshToken + "\"}")
                .when()
                .post("/api/v1/auth/logout")
                .then()
                .statusCode(200);

        // Try to use the refresh token after logout
        given()
                .contentType(ContentType.JSON)
                .body("{\"refreshToken\": \"" + refreshToken + "\"}")
                .when()
                .post("/api/v1/auth/refresh")
                .then()
                .statusCode(anyOf(equalTo(401), equalTo(400)));
    }

    @Test
    @DisplayName("User should have USER role by default after registration")
    void userShouldHaveUserRoleByDefault() {
        RegisterRequest request = new RegisterRequest(
                "Role",
                "Test",
                "role@test.com",
                "password123"
        );

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(200);

        // Verify user exists with USER role
        var user = userRepository.findByEmail("role@test.com");
        assertThat(user).isPresent();
        assertThat(user.get().getRoles())
                .extracting(Role::getName)
                .contains(RoleName.USER);
    }

    @Test
    @DisplayName("Invalid refresh token should return error")
    void invalidRefreshTokenShouldReturnError() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"refreshToken\": \"invalid-refresh-token\"}")
                .when()
                .post("/api/v1/auth/refresh")
                .then()
                .statusCode(anyOf(equalTo(401), equalTo(400)));
    }

    @Test
    @DisplayName("Should persist refresh token to database")
    void shouldPersistRefreshTokenToDatabase() {
        RegisterRequest request = new RegisterRequest(
                "Persist",
                "Test",
                "persist@test.com",
                "password123"
        );

        var response = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(200)
                .extract();

        String refreshToken = response.path("refreshToken");

        // Verify token exists in database
        var storedToken = refreshTokenRepository.findByToken(refreshToken);
        assertThat(storedToken).isPresent();
        assertThat(storedToken.get().isRevoked()).isFalse();
    }
}
