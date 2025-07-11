package com.batchprompt.users.core.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.batchprompt.users.core.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    
    Optional<User> findByEmail(String email);
    
    Page<User> findByNameContainingIgnoreCase(String name, Pageable pageable);
    
    Page<User> findAll(Pageable pageable);
    
    // Methods that exclude deleted users
    @Query("SELECT u FROM User u WHERE u.deleteTimestamp IS NULL")
    Page<User> findAllActive(Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.deleteTimestamp IS NULL AND u.userId = :userId")
    Optional<User> findActiveById(@Param("userId") String userId);
    
    @Query("SELECT u FROM User u WHERE u.deleteTimestamp IS NULL AND u.email = :email")
    Optional<User> findActiveByEmail(@Param("email") String email);
    
    @Query("SELECT u FROM User u WHERE u.deleteTimestamp IS NULL AND UPPER(u.name) LIKE UPPER(CONCAT('%', :name, '%'))")
    Page<User> findActiveByNameContainingIgnoreCase(@Param("name") String name, Pageable pageable);
}