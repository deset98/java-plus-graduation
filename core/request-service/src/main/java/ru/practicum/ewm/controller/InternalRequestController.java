package ru.practicum.ewm.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.enums.request.RequestStatus;
import ru.practicum.ewm.service.RequestService;

import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/internal/requests")
@RequiredArgsConstructor
public class InternalRequestController {

    private final RequestService requestService;

    @GetMapping("/list/event/{eventId}")
    List<ParticipationRequestDto> findAllByEventId(@PathVariable Long eventId) {
        log.debug("Метод findAllByEventId(); eventId={}", eventId);

        return requestService.findAllByEventId(eventId);
    }

    @GetMapping("/list")
    List<ParticipationRequestDto> findAllByIdIn(@RequestParam Set<Long> requestIds) {
        log.debug("Метод findAllByIdIn(); requestIds={}", requestIds);

        return requestService.findAllByIdIn(requestIds);
    }

    @PostMapping("/user/status/{status}")
    void updateRequestStatus(@PathVariable RequestStatus status, @RequestParam Set<Long> ids) {
        log.debug("Метод updateRequestStatus(); status={}, ids={}", status, ids);

        requestService.updateRequestStatus(status, ids);
    }
}