package ru.practicum.ewm.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.ewm.dto.user.UserFullDto;
import ru.practicum.ewm.dto.user.UserShortDto;

import java.util.List;

@FeignClient(name = "user-service", path = "/internal/users")
public interface UserClient {

    @GetMapping
    void validateUserExists(@RequestParam Long userId);

    @GetMapping
    UserFullDto getUserBy(@RequestParam Long userId);

    @GetMapping
    List<UserShortDto> getUsersBy(@RequestParam List<Long> ids);
}