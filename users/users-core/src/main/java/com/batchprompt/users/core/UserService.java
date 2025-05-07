package com.batchprompt.users.core;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.batchprompt.users.core.model.User;
import com.batchprompt.users.core.repository.UserRepository;
import com.batchprompt.users.model.UserRole;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final AccountService accountService;

    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public Optional<User> getUserById(UUID userUuid) {
        return userRepository.findById(userUuid);
    }

    public Optional<User> getUserByUserId(String userId) {
        return userRepository.findByuserId(userId);
    }
    
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Page<User> searchUsersByName(String name, Pageable pageable) {
        return userRepository.findByNameContainingIgnoreCase(name, pageable);
    }

    /**
     * Validate and update user information during login
     * This method will create a user if they don't exist, or update their information if they do
     * 
     * @param user The user information from the login process
     * @return The validated and persisted user
     */
    @Transactional
    public User validateUserOnLogin(User user) {
        log.debug("Validating user on login: {}", user.getUserId());
        
        // Check if user already exists by Auth0 ID
        Optional<User> existingUserOpt = userRepository.findByuserId(user.getUserId());
        
        if (existingUserOpt.isPresent()) {
            // Update existing user with latest information
            User existingUser = existingUserOpt.get();
            boolean needsUpdate = false;
            
            // Update name if it has changed
            if (user.getName() != null && !user.getName().equals(existingUser.getName())) {
                existingUser.setName(user.getName());
                needsUpdate = true;
            }
            
            // Update email if it has changed
            if (user.getEmail() != null && !user.getEmail().equals(existingUser.getEmail())) {
                existingUser.setEmail(user.getEmail());
                needsUpdate = true;
            }
            
            // Update picture if it has changed
            if (user.getPicture() != null && !user.getPicture().equals(existingUser.getPicture())) {
                existingUser.setPicture(user.getPicture());
                needsUpdate = true;
            }
            
            // If any fields were updated, update the timestamp
            if (needsUpdate) {
                existingUser.setUpdateTimestamp(LocalDateTime.now());
                log.info("Updating existing user on login: {}", existingUser.getUserId());
                return userRepository.save(existingUser);
            }
            
            return existingUser;
        } else {
            // Create new user
            log.info("Creating new user on login: {}", user.getUserId());
            return registerUser(user);
        }
    }

    @Transactional
    public User registerUser(User user) {
        // Check if user already exists
        if (userRepository.findByuserId(user.getUserId()).isPresent()) {
            throw new IllegalArgumentException("User with this Auth0 ID already exists");
        }
        
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalArgumentException("User with this email already exists");
        }
        
        // Set defaults for a new user
        user.setUserUuid(UUID.randomUUID());
        LocalDateTime now = LocalDateTime.now();
        user.setCreateTimestamp(now);
        user.setUpdateTimestamp(now);
        
        // By default, new users get USER role
        if (user.getRole() == null) {
            user.setRole(UserRole.USER);
        }
        
        // By default, users are enabled
        user.setEnabled(true);
        
        User savedUser = userRepository.save(user);
        
        // Create a default account for the new user using their email as the account name
        try {
            String accountName = user.getEmail().split("@")[0] + "-account";
            accountService.createAccount(accountName, savedUser);
            log.info("Created default account '{}' for user: {}", accountName, user.getUserId());
        } catch (Exception e) {
            log.error("Failed to create default account for user: {}", user.getUserId(), e);
            // We don't want to fail the user registration if account creation fails
        }
        
        return savedUser;
    }

    @Transactional
    public Optional<User> updateUser(UUID userUuid, User userDetails) {
        return userRepository.findById(userUuid)
                .map(existingUser -> {
                    // Don't update userId as it should remain constant
                    existingUser.setName(userDetails.getName());
                    existingUser.setPicture(userDetails.getPicture());
                    
                    // Only update email if it changed and isn't already taken
                    if (!existingUser.getEmail().equals(userDetails.getEmail())) {
                        userRepository.findByEmail(userDetails.getEmail())
                            .ifPresent(user -> {
                                if (!user.getUserUuid().equals(userUuid)) {
                                    throw new IllegalArgumentException("Email already in use");
                                }
                            });
                        existingUser.setEmail(userDetails.getEmail());
                    }
                    
                    // Update role if provided
                    if (userDetails.getRole() != null) {
                        existingUser.setRole(userDetails.getRole());
                    }
                    
                    // Update enabled status
                    existingUser.setEnabled(userDetails.isEnabled());
                    
                    existingUser.setUpdateTimestamp(LocalDateTime.now());
                    return userRepository.save(existingUser);
                });
    }

    @Transactional
    public boolean deleteUser(UUID userUuid) {
        return userRepository.findById(userUuid)
                .map(user -> {
                    userRepository.delete(user);
                    return true;
                })
                .orElse(false);
    }
}