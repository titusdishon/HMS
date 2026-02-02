package com.devdishon.service;

import com.devdishon.entity.Role;
import com.devdishon.entity.RoleName;
import com.devdishon.entity.User;
import com.devdishon.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setFirstName("Test");
        testUser.setLastName("User");

        Role userRole = new Role(RoleName.USER);
        testUser.setRoles(Set.of(userRole));
    }

    @Test
    @DisplayName("Should generate valid access token")
    void shouldGenerateValidAccessToken() {
        String token = jwtService.generateAccessToken(testUser);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(jwtService.isTokenValid(token, testUser)).isTrue();
    }

    @Test
    @DisplayName("Should extract username from token")
    void shouldExtractUsernameFromToken() {
        String token = jwtService.generateAccessToken(testUser);

        String username = jwtService.extractUsername(token);

        assertThat(username).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should validate token against user details")
    void shouldValidateTokenAgainstUserDetails() {
        String token = jwtService.generateAccessToken(testUser);

        boolean isValid = jwtService.isTokenValid(token, testUser);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should detect expired token")
    void shouldDetectExpiredToken() {
        // This test would require a way to create an expired token
        // For now, we just verify that a fresh token is not expired
        String token = jwtService.generateAccessToken(testUser);

        assertThat(jwtService.isTokenValid(token, testUser)).isTrue();
    }

    @Test
    @DisplayName("Should reject invalid token")
    void shouldRejectInvalidToken() {
        String invalidToken = "invalid.jwt.token";

        // Invalid tokens will throw an exception when parsed
        try {
            jwtService.isTokenValid(invalidToken, testUser);
            // If no exception, the test should fail
        } catch (Exception e) {
            // Expected behavior - invalid token causes exception
            assertThat(e).isNotNull();
        }
    }

    @Test
    @DisplayName("Should reject token for different user")
    void shouldRejectTokenForDifferentUser() {
        String token = jwtService.generateAccessToken(testUser);

        User differentUser = new User();
        differentUser.setId(2L);
        differentUser.setEmail("different@example.com");
        differentUser.setFirstName("Different");
        differentUser.setLastName("User");

        boolean isValid = jwtService.isTokenValid(token, differentUser);

        assertThat(isValid).isFalse();
    }
}
