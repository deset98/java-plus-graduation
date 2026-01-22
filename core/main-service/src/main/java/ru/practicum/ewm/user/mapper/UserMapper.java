package ru.practicum.ewm.user.mapper;

import org.mapstruct.Mapper;
import ru.practicum.ewm.user.dto.UserShortDto;
import ru.practicum.ewm.user.model.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

//    UserDto toFullDto(User user);
//
//    @Mapping(target = "id", ignore = true)
//    User toEntity(NewUserRequest newDto);

    UserShortDto toShortDto(User user);
}