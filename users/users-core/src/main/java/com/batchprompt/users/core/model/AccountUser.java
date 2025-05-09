package com.batchprompt.users.core.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "account_user")
@IdClass(AccountUserPK.class)
public class AccountUser {

    @Id
    @Column(name = "account_uuid")
    private UUID accountUuid;

    @Id
    @Column(name = "user_id")
    private String userId;

    @ManyToOne
    @JoinColumn(name = "account_uuid", insertable = false, updatable = false)
    private Account account;

    @ManyToOne
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Column(name = "is_owner", nullable = false)
    private boolean owner;

    @Column(name = "create_timestamp", nullable = false)
    private LocalDateTime createTimestamp;

    @Column(name = "update_timestamp", nullable = false)
    private LocalDateTime updateTimestamp;
}