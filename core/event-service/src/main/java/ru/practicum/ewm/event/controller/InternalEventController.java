package ru.practicum.ewm.event.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.event.service.EventService;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/events")
public class InternalEventController {

    private final EventService eventService;

    @GetMapping("/{eventId}")
    public ResponseEntity<EventFullDto> findEventBy(@PathVariable @Positive Long eventId) {
        log.debug("InternalEventController, Метод findEventBy(); eventId={}", eventId);

        EventFullDto result = eventService.getEventBy(eventId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/check")
    public boolean existsByIdAndInitiatorId(@RequestParam @Positive Long eventId,
                                            @RequestParam @Positive Long userId) {
        log.debug("InternalEventController, метод existsByIdAndInitiatorId(); eventId={}, userId={}", eventId, userId);

        return eventService.existsByIdAndInitiatorId(eventId, userId);
    }

    @PostMapping("/{eventId}")
    public void incrementConfirmedRequests(@PathVariable Long eventId) {
        log.debug("InternalEventController, метод incrementConfirmedRequests(); eventId={}", eventId);

        eventService.incrementConfirmedRequests(eventId);
    }
}