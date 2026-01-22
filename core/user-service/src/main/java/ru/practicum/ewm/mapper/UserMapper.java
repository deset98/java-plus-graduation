package ru.practicum.ewm.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.ewm.dto.*;
import ru.practicum.ewm.model.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toFullDto(User user);

    @Mapping(target = "id", ignore = true)
    User toEntity(NewUserRequest newDto);
}