package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.user.NewUserRequest;
import ru.practicum.ewm.dto.user.UserFullDto;

import java.util.List;

public interface UserService {

    // Admin API:

    List<UserFullDto> findAllBy(List<Long> ids, Integer from, Integer size);

    UserFullDto add(NewUserRequest newDto);

    void delete(Long userId);

    void validateUserExists(Long userId);

    // Internal API:

    UserFullDto findById(Long userId);
}