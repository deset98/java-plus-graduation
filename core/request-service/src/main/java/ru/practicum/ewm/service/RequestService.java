package ru.practicum.ewm.service;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.enums.request.RequestStatus;

import java.util.List;
import java.util.Set;

public interface RequestService {

    ParticipationRequestDto create(Long userId, Long eventId);

    List<ParticipationRequestDto> getAllBy(Long userId);

    ParticipationRequestDto cancel(Long userId, Long requestId);

    List<ParticipationRequestDto> findAllByEventId(Long eventId);

    List<ParticipationRequestDto> findAllByIdIn(Set<Long> requestIds);

    void updateRequestStatus(@PathVariable RequestStatus status, @RequestParam Set<Long> ids);
}