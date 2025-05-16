package com.batchprompt.users.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.batchprompt.users.client.config.FeignClientConfig;
import com.batchprompt.users.model.dto.UserDto;

@FeignClient(
    name = "users-api", 
    url = "${users-api.url}", 
    path = "/api/users",
    configuration = FeignClientConfig.class
)
public interface UserClient {

    @GetMapping("/{userId}")
    ResponseEntity<UserDto> getUserById(@PathVariable("userId") String userId);
    
    @GetMapping("/auth0/{userId}")
    ResponseEntity<UserDto> getUserByUserId(@PathVariable("userId") String userId);
    
    @GetMapping("/search")
    ResponseEntity<Page<UserDto>> searchUsersByName(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size);
    
    @GetMapping
    ResponseEntity<Page<UserDto>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir);
    
    @PostMapping("/register")
    ResponseEntity<UserDto> registerUser(@RequestBody UserDto userDto);
    
    @PutMapping("/{userId}")
    ResponseEntity<UserDto> updateUser(@PathVariable("userId") String userId, @RequestBody UserDto userDto);
}