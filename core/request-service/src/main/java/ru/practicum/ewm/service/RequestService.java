package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.request.ParticipationRequestDto;

import java.util.List;

public interface RequestService {

    ParticipationRequestDto create(Long userId, Long eventId);

    List<ParticipationRequestDto> getAllBy(Long userId);

    ParticipationRequestDto cancel(Long userId, Long requestId);
}