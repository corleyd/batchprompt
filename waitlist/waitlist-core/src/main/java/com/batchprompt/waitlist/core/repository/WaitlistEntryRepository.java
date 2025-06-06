package com.batchprompt.waitlist.core.repository;

import com.batchprompt.waitlist.core.model.WaitlistEntry;
import com.batchprompt.waitlist.model.WaitlistStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, UUID> {
    
    Optional<WaitlistEntry> findByEmail(String email);
    
    List<WaitlistEntry> findByStatusOrderByCreatedAtAsc(WaitlistStatus status);
    
    @Query("SELECT COUNT(w) FROM WaitlistEntry w WHERE w.status = :status AND w.createdAt < " +
           "(SELECT we.createdAt FROM WaitlistEntry we WHERE we.email = :email)")
    int countPendingEntriesBeforeEmail(@Param("status") WaitlistStatus status, @Param("email") String email);
    
    @Query("SELECT MAX(w.position) FROM WaitlistEntry w")
    Integer findMaxPosition();
    
    @Query("SELECT w FROM WaitlistEntry w WHERE w.status = :status ORDER BY w.position ASC")
    List<WaitlistEntry> findByStatusOrderByPosition(@Param("status") WaitlistStatus status);
}