package ru.practicum.ewm.controller;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.dto.user.UserFullDto;
import ru.practicum.ewm.service.UserService;

import java.util.List;

@RestController
@Slf4j
@Validated
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;

    public ResponseEntity<Void> validateUserExists(@RequestParam @Positive Long userId) {
        log.debug("IntUserController, метод validateUserExists: userId={}", userId);

        userService.validateUserExists(userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<UserFullDto> getUserBy(@RequestParam @Positive Long userId) {
        log.debug("IntUserController, метод getUsers: userId={}", userId);

        UserFullDto result = userService.findById(userId);
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<List<UserFullDto>> getUsersBy(@RequestParam List<Long> ids,
                                                        @RequestParam(defaultValue = "0") @Min(0) Integer from,
                                                        @RequestParam(defaultValue = "10") @Min(1) Integer size) {
        log.debug("InternalUserController, метод getUsers: ids={}, from={}, size={}", ids, from, size);

        List<UserFullDto> result = userService.findAllBy(ids, from, size);
        return ResponseEntity.ok(result);
    }
}