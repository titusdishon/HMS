package com.devdishon.service;

import com.devdishon.dto.RoleAssignmentRequest;
import com.devdishon.dto.UserResponse;
import com.devdishon.entity.Role;
import com.devdishon.entity.RoleName;
import com.devdishon.entity.User;
import com.devdishon.exception.BadRequestException;
import com.devdishon.exception.ResourceNotFoundException;
import com.devdishon.repository.RoleRepository;
import com.devdishon.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public UserService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::fromUser)
                .toList();
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " not found"));
        return UserResponse.fromUser(user);
    }

    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User with email " + email + " not found"));
        return UserResponse.fromUser(user);
    }

    @Transactional
    public UserResponse assignRoles(RoleAssignmentRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + request.userId() + " not found"));

        Set<Role> newRoles = new HashSet<>();
        for (RoleName roleName : request.roles()) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new BadRequestException("Role " + roleName + " not found"));
            newRoles.add(role);
        }

        // Ensure USER role is always present
        roleRepository.findByName(RoleName.USER).ifPresent(newRoles::add);

        user.setRoles(newRoles);
        User savedUser = userRepository.save(user);

        logger.info("Roles updated for user {}: {}", user.getEmail(),
                newRoles.stream().map(r -> r.getName().name()).toList());

        return UserResponse.fromUser(savedUser);
    }

    @Transactional
    public UserResponse addRole(Long userId, RoleName roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + userId + " not found"));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new BadRequestException("Role " + roleName + " not found"));

        user.addRole(role);
        User savedUser = userRepository.save(user);

        logger.info("Added role {} to user {}", roleName, user.getEmail());

        return UserResponse.fromUser(savedUser);
    }

    @Transactional
    public UserResponse removeRole(Long userId, RoleName roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + userId + " not found"));

        // Cannot remove USER role
        if (roleName == RoleName.USER) {
            throw new BadRequestException("Cannot remove the USER role from a user");
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new BadRequestException("Role " + roleName + " not found"));

        user.getRoles().remove(role);
        User savedUser = userRepository.save(user);

        logger.info("Removed role {} from user {}", roleName, user.getEmail());

        return UserResponse.fromUser(savedUser);
    }

    @Transactional
    public UserResponse enableUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + userId + " not found"));

        user.setEnabled(true);
        User savedUser = userRepository.save(user);

        logger.info("Enabled user: {}", user.getEmail());

        return UserResponse.fromUser(savedUser);
    }

    @Transactional
    public UserResponse disableUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + userId + " not found"));

        user.setEnabled(false);
        User savedUser = userRepository.save(user);

        logger.info("Disabled user: {}", user.getEmail());

        return UserResponse.fromUser(savedUser);
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }
}
