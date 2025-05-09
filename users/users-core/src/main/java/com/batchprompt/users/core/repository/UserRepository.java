package com.batchprompt.users.core.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.batchprompt.users.core.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    
    Optional<User> findByEmail(String email);
    
    Page<User> findByNameContainingIgnoreCase(String name, Pageable pageable);
    
    Page<User> findAll(Pageable pageable);
}