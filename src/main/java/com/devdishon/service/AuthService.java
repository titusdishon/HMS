package com.devdishon.service;

import com.devdishon.dto.auth.AuthResponse;
import com.devdishon.dto.auth.LoginRequest;
import com.devdishon.dto.auth.RefreshTokenRequest;
import com.devdishon.dto.auth.RegisterRequest;
import com.devdishon.entity.RefreshToken;
import com.devdishon.entity.Role;
import com.devdishon.entity.RoleName;
import com.devdishon.entity.User;
import com.devdishon.exception.BadRequestException;
import com.devdishon.exception.DuplicateResourceException;
import com.devdishon.exception.ResourceNotFoundException;
import com.devdishon.repository.RefreshTokenRepository;
import com.devdishon.repository.RoleRepository;
import com.devdishon.repository.UserRepository;
import com.devdishon.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("User", "email", request.email());
        }

        User user = new User(
                request.firstName(),
                request.lastName(),
                request.email(),
                passwordEncoder.encode(request.password())
        );

        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.USER)));
        user.addRole(userRole);

        user = userRepository.save(user);
        logger.info("User registered successfully: {}", user.getEmail());

        return generateAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.email()));

        // Revoke existing refresh tokens
        refreshTokenRepository.revokeAllByUser(user);

        logger.info("User logged in successfully: {}", user.getEmail());
        return generateAuthResponse(user);
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));

        if (!refreshToken.isValid()) {
            throw new BadRequestException("Refresh token is expired or revoked");
        }

        User user = refreshToken.getUser();

        // Revoke the old refresh token
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        logger.info("Token refreshed for user: {}", user.getEmail());
        return generateAuthResponse(user);
    }

    public void logout(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        refreshTokenRepository.revokeAllByUser(user);
        logger.info("User logged out: {}", email);
    }

    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = createRefreshToken(user);

        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList());

        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                roles
        );

        return new AuthResponse(
                accessToken,
                refreshToken,
                jwtService.getAccessTokenExpiration() / 1000,
                userInfo
        );
    }

    private String createRefreshToken(User user) {
        String token = UUID.randomUUID().toString();
        Instant expiryDate = Instant.now().plusMillis(jwtService.getRefreshTokenExpiration());

        RefreshToken refreshToken = new RefreshToken(token, user, expiryDate);
        refreshTokenRepository.save(refreshToken);

        return token;
    }
}
