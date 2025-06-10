package com.batchprompt.users.core.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.batchprompt.users.core.model.User;
import com.batchprompt.users.model.UserRole;
import com.batchprompt.users.model.dto.UserDto;

@Component
public class UserMapper {

    public UserDto toDto(User user) {
        return UserDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .picture(user.getPicture())
                .role(user.getRole().toString())
                .enabled(user.isEnabled())
                .createTimestamp(user.getCreateTimestamp())
                .updateTimestamp(user.getUpdateTimestamp())
                .deleteTimestamp(user.getDeleteTimestamp())
                .build();
    }

    public User toEntity(UserDto userDto) {
        return User.builder()
                .userId(userDto.getUserId())
                .email(userDto.getEmail())
                .name(userDto.getName())
                .picture(userDto.getPicture())
                .role(userDto.getRole() != null ? UserRole.valueOf(userDto.getRole()) : UserRole.USER)
                .enabled(userDto.isEnabled())
                .createTimestamp(userDto.getCreateTimestamp())
                .updateTimestamp(userDto.getUpdateTimestamp())
                .deleteTimestamp(userDto.getDeleteTimestamp())
                .build();
    }

    public List<UserDto> toDtoList(List<User> users) {
        return users.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    public Page<UserDto> toDtoPage(Page<User> users) {
        return users.map(this::toDto);
    }
}