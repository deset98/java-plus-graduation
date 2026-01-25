package ru.practicum.ewm.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.ewm.dto.user.*;
import ru.practicum.ewm.model.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserFullDto toFullDto(User user);

    @Mapping(target = "id", ignore = true)
    User toEntity(NewUserRequest newDto);

    UserShortDto toShortDto(User user);
}