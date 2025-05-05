package com.batchprompt.users.api.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.batchprompt.common.services.ServiceAuthenticationService;
import com.batchprompt.users.api.service.Auth0ManagementService;
import com.batchprompt.users.core.UserService;
import com.batchprompt.users.core.mapper.UserMapper;
import com.batchprompt.users.core.model.User;
import com.batchprompt.users.model.dto.UserDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final Auth0ManagementService auth0ManagementService;
    private final ServiceAuthenticationService serviceAuthenticationService;

    @GetMapping
    public ResponseEntity<Page<UserDto>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? 
                Sort.Direction.DESC : Sort.Direction.ASC;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<User> users = userService.getAllUsers(pageable);
        
        return ResponseEntity.ok(userMapper.toDtoPage(users));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUserById(
        @PathVariable String userId,
        @AuthenticationPrincipal Jwt jwt) {

        if (serviceAuthenticationService.canAccessUserData(jwt, userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        return userService.getUserByUserId(userId)
                .map(userMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/auth0/{userId}")
    public ResponseEntity<UserDto> getUserByuserId(@PathVariable String userId) {
        return userService.getUserByUserId(userId)
                .map(userMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<Page<UserDto>> searchUsersByName(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? 
                Sort.Direction.DESC : Sort.Direction.ASC;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<User> users = userService.searchUsersByName(name, pageable);
        
        return ResponseEntity.ok(userMapper.toDtoPage(users));
    }

    @PostMapping("/login-validation")
    public ResponseEntity<UserDto> validateUserOnLogin(@AuthenticationPrincipal Jwt jwt, @RequestBody(required = false) UserDto userDto) {
        String userId = jwt.getSubject();
        
        // Fetch user profile from Auth0 Management API
        com.auth0.json.mgmt.users.User auth0User = auth0ManagementService.getUserProfile(userId);
        
        String name = null;
        String email = null;
        
        // Extract user information from Auth0 profile
        if (auth0User != null) {
            name = auth0User.getName();
            email = auth0User.getEmail();
            log.debug("Retrieved user profile from Auth0: name={}, email={}", name, email);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // If userDto is not provided, create one with info from Auth0
        if (userDto == null) {
            userDto = new UserDto();
            userDto.setUserId(userId);
            userDto.setName(name);
            userDto.setEmail(email);
        } else {
            // Ensure userId matches the authenticated user
            userDto.setUserId(userId);
        }
        
        // Check if user exists and create/update as needed
        try {
            User user = userMapper.toEntity(userDto);
            User validatedUser = userService.validateUserOnLogin(user);
            return ResponseEntity.ok(userMapper.toDto(validatedUser));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PostMapping("/register")
    public ResponseEntity<UserDto> registerUser(@RequestBody UserDto userDto) {
        try {
            User user = userMapper.toEntity(userDto);
            User savedUser = userService.registerUser(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(userMapper.toDto(savedUser));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PutMapping("/{userUuid}")
    public ResponseEntity<UserDto> updateUser(@PathVariable UUID userUuid, @RequestBody UserDto userDto) {
        try {
            User user = userMapper.toEntity(userDto);
            return userService.updateUser(userUuid, user)
                    .map(userMapper::toDto)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @DeleteMapping("/{userUuid}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userUuid) {
        boolean deleted = userService.deleteUser(userUuid);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}