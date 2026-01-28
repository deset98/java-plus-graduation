package ru.practicum.ewm.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.ewm.dto.event.EventFullDto;

@FeignClient(name = "event-service", path = "/internal/events")
public interface EventClient {

    @GetMapping("/{eventId}")
    EventFullDto getEventBy(@PathVariable Long eventId);

    @GetMapping("/check")
    boolean existsByIdAndInitiatorId(@RequestParam Long eventId,
                                     @RequestParam Long userId);

    @PatchMapping("/{eventId}")
    void incrementConfirmedRequests(@PathVariable Long eventId);
}