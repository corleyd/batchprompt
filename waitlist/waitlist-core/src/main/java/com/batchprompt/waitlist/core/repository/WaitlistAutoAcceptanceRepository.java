package com.batchprompt.waitlist.core.repository;

import com.batchprompt.waitlist.core.model.WaitlistAutoAcceptance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WaitlistAutoAcceptanceRepository extends JpaRepository<WaitlistAutoAcceptance, UUID> {
    
    /**
     * Get the current auto-acceptance configuration (there should only be one row)
     */
    @Query("SELECT w FROM WaitlistAutoAcceptance w ORDER BY w.updatedAt DESC")
    Optional<WaitlistAutoAcceptance> findCurrentConfiguration();
    
    /**
     * Atomically decrement the remaining auto-accept count if it's greater than 0
     * @param id The ID of the configuration to update
     * @return The number of rows affected (1 if successful, 0 if count was already 0)
     */
    @Modifying
    @Query("UPDATE WaitlistAutoAcceptance w SET w.remainingAutoAcceptCount = w.remainingAutoAcceptCount - 1, w.updatedAt = CURRENT_TIMESTAMP WHERE w.id = :id AND w.remainingAutoAcceptCount > 0")
    int decrementAutoAcceptCount(@Param("id") UUID id);
}
