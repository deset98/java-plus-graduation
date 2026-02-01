package ru.practicum.ewm.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.dto.event.UserEventSearchParams;
import ru.practicum.ewm.event.service.EventService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
public class PublicEventController {

    private final EventService eventService;

    @GetMapping("/{eventId}")
    public ResponseEntity<EventFullDto> getEvent(@PathVariable @Positive Long eventId,
                                                 @RequestHeader("X-EWM-USER-ID") Long userId) {
        log.debug("Метод publicSearchOne(); eventId={}", eventId);

        EventFullDto event = eventService.getEventBy(eventId, userId);
        return ResponseEntity.ok(event);
    }

    @GetMapping
    public ResponseEntity<List<EventFullDto>> getListOfEvents(@Valid @ModelAttribute UserEventSearchParams params,
                                                              HttpServletRequest request) {
        log.debug("Метод publicSearchMany(); {}", params);

        List<EventFullDto> events = eventService.getListOfEventsBy(params, request);
        return ResponseEntity.ok(events);
    }


    @GetMapping("/recommendations")
    public ResponseEntity<List<EventShortDto>> getRecommendations(
            @RequestHeader("X-EWM-USER-ID") Long userId,
            @RequestParam(defaultValue = "10") @Positive int size
    ) {
        log.debug("Метод getRecommendations();  userId={}, size={}", userId, size);

        List<EventShortDto> result = eventService.getRecommendations(userId, size);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{eventId}/like")
    public ResponseEntity<Void> like(@PathVariable @PositiveOrZero @NotNull Long eventId,
                                     @RequestHeader("X-EWM-USER-ID") Long userId) {
        log.debug("Метод like(); eventId={}, userId={}", eventId, userId);

        eventService.like(eventId, userId);
        return ResponseEntity.ok().build();
    }
}