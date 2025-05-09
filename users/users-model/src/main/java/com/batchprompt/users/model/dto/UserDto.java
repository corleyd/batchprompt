package com.batchprompt.users.model.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private String userId;
    private String email;
    private String name;
    private String picture;
    private String role;
    private boolean enabled;
    private LocalDateTime createTimestamp;
    private LocalDateTime updateTimestamp;
}