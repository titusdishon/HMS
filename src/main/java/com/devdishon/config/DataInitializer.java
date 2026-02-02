package com.devdishon.config;

import com.devdishon.entity.Role;
import com.devdishon.entity.RoleName;
import com.devdishon.entity.User;
import com.devdishon.repository.RoleRepository;
import com.devdishon.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Initializes default data on application startup.
 * Creates default roles and a super admin user if they don't exist.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@hms.com}")
    private String adminEmail;

    @Value("${app.admin.password:Admin@123456}")
    private String adminPassword;

    @Value("${app.admin.firstName:System}")
    private String adminFirstName;

    @Value("${app.admin.lastName:Administrator}")
    private String adminLastName;

    public DataInitializer(RoleRepository roleRepository,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        initializeRoles();
        initializeSuperAdmin();
    }

    private void initializeRoles() {
        Arrays.stream(RoleName.values()).forEach(roleName -> {
            if (roleRepository.findByName(roleName).isEmpty()) {
                Role role = new Role(roleName);
                roleRepository.save(role);
                logger.info("Created role: {}", roleName);
            }
        });
    }

    private void initializeSuperAdmin() {
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            // Get all roles for super admin
            Set<Role> allRoles = new HashSet<>();
            roleRepository.findByName(RoleName.USER).ifPresent(allRoles::add);
            roleRepository.findByName(RoleName.ADMIN).ifPresent(allRoles::add);
            roleRepository.findByName(RoleName.SUPER_ADMIN).ifPresent(allRoles::add);

            User superAdmin = new User(
                    adminFirstName,
                    adminLastName,
                    adminEmail,
                    passwordEncoder.encode(adminPassword)
            );
            superAdmin.setRoles(allRoles);

            userRepository.save(superAdmin);

            logger.info("=".repeat(60));
            logger.info("SUPER ADMIN USER CREATED");
            logger.info("Email: {}", adminEmail);
            logger.info("Password: {} (CHANGE THIS IMMEDIATELY IN PRODUCTION!)", adminPassword);
            logger.info("Roles: {}", allRoles.stream().map(r -> r.getName().name()).toList());
            logger.info("=".repeat(60));
        } else {
            logger.info("Super admin user already exists: {}", adminEmail);
        }
    }
}
