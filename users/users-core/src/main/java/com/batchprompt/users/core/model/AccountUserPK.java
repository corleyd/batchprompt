package com.batchprompt.users.core.model;

import java.io.Serializable;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountUserPK implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private UUID accountUuid;
    private UUID userUuid;
}