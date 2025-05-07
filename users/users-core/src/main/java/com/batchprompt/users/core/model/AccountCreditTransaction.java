package com.batchprompt.users.core.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
@Table(name = "account_credit_transaction")
public class AccountCreditTransaction {

    @Id
    @Column(name = "transaction_uuid")
    private UUID transactionUuid;

    @Column(name = "account_uuid", nullable = false)
    private UUID accountUuid;
    
    @ManyToOne
    @JoinColumn(name = "account_uuid", insertable = false, updatable = false)
    private Account account;

    @Column(name = "change_amount", nullable = false)
    private Double changeAmount;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "reference_id")
    private String referenceId;

    @Column(name = "create_timestamp", nullable = false)
    private LocalDateTime createTimestamp;
}