package ru.practicum.ewm.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.enums.request.RequestStatus;

import java.util.List;
import java.util.Set;

@FeignClient(name = "request-service", path = "/internal/requests")
public interface RequestClient {

    @GetMapping("/list/event/{eventId}")
    List<ParticipationRequestDto> findAllByEventId(@PathVariable Long eventId);

    @GetMapping("/list")
    List<ParticipationRequestDto> findAllByIdIn(@RequestParam Set<Long> requestIds);

    @PostMapping("/user/status/{status}")
    void updateRequestStatus(@PathVariable RequestStatus status, @RequestParam Set<Long> ids);
}