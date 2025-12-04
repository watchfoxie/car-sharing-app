package com.usarbcs.authen.service.dto.mapper;


import com.usarbcs.authen.service.dto.UserDto;
import com.usarbcs.authen.service.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserDto toDto(User user) {
        if (user == null) {
            return null;
        }

        UserDto userDto = new UserDto();
        if (user.getId() != null) {
            userDto.setId(user.getId().toString());
        }
        userDto.setFirstName(user.getFirstName());
        userDto.setLastName(user.getLastName());
        userDto.setPhoneNumber(user.getPhoneNumber());
        userDto.setEmail(user.getEmail());
        userDto.setPassword(user.getPassword());
        userDto.setActive(user.isActive());
        userDto.setCreatedAt(user.getCreatedAt());
        userDto.setCreatedBy(user.getCreatedBy());
        userDto.setUpdatedAt(user.getUpdatedAt());
        userDto.setUpdatedBy(user.getUpdatedBy());
        userDto.setDeleted(user.getDeleted());

        return userDto;
    }
}
