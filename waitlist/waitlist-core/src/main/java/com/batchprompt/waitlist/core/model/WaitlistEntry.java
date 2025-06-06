package com.batchprompt.waitlist.core.model;

import com.batchprompt.waitlist.model.WaitlistStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "waitlist_entry")
public class WaitlistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "name")
    private String name;

    @Column(name = "company")
    private String company;

    @Column(name = "use_case", columnDefinition = "TEXT")
    private String useCase;

    @Column(name = "position")
    private Integer position;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private WaitlistStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "invited_at")
    private LocalDateTime invitedAt;

    @Column(name = "registered_at")
    private LocalDateTime registeredAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = WaitlistStatus.PENDING;
        }
    }
}