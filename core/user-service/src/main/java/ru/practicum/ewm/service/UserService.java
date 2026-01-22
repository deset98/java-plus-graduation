package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.NewUserRequest;
import ru.practicum.ewm.dto.UserDto;

import java.util.List;

public interface UserService {

    List<UserDto> findAllBy(List<Long> ids, Integer from, Integer size);

    UserDto add(NewUserRequest newDto);

    void delete(Long userId);
}