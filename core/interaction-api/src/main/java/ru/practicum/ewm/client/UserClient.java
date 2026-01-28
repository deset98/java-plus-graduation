package ru.practicum.ewm.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.ewm.dto.user.UserFullDto;
import ru.practicum.ewm.dto.user.UserShortDto;

import java.util.List;

@FeignClient(name = "user-service", path = "/internal/users")
public interface UserClient {

    @GetMapping("/check/{userId}")
    void validateUserExists(@PathVariable Long userId);

    @GetMapping("/{userId}")
    UserFullDto getUserBy(@PathVariable Long userId);

    @GetMapping("/list")
    List<UserShortDto> getUsersBy(@RequestParam List<Long> ids);
}