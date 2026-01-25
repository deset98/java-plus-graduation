package ru.practicum.ewm.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.event.EventFullDto;

@FeignClient(name = "event-service", path = "/internal/events")
public interface EventClient {

    @GetMapping("/{id}")
    EventFullDto getEventBy(@PathVariable Long id);

    @GetMapping("/check")
    boolean existsByIdAndInitiatorId(@RequestParam Long eventId,
                                     @RequestParam Long userId);

    @PatchMapping("/{eventId}")
    void incrementConfirmedRequests(@PathVariable Long eventId);
}