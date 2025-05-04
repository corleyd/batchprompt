package com.batchprompt.users.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private UUID userUuid;
    private String userId;
    private String email;
    private String name;
    private String picture;
    private String role;
    private boolean enabled;
    private LocalDateTime createTimestamp;
    private LocalDateTime updateTimestamp;
}