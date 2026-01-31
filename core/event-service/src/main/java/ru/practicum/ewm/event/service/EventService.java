package ru.practicum.ewm.event.service;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.ewm.dto.event.*;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;

import java.util.List;

public interface EventService {

    // Private API:
    EventFullDto create(Long userId, NewEventDto newEventDto);

    EventFullDto getByUser(Long userId, Long eventId);

    List<EventShortDto> getAllByUser(Long userId, int from, int size);

    EventFullDto updateByUser(Long userId, Long eventId, UpdEventUserRequest updEventUserRequest);

    // Admin API:
    EventFullDto updateByAdmin(Long eventId, UpdEventAdminRequest updEventAdminRequest);

    List<EventFullDto> searchForAdmin(AdminEventSearchParams params);

    // Public API:
    EventFullDto getEventBy(Long eventId, HttpServletRequest request);

    List<EventFullDto> getListOfEventsBy(UserEventSearchParams params, HttpServletRequest request);

    List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId);

    UpdRequestsStatusResult updateRequests(Long userId, Long eventId, EventRequestStatusUpdateRequest updDto);

    // Internal API:
    EventFullDto getEventBy(Long eventId);

    boolean existsByIdAndInitiatorId(Long eventId, Long userId);

    void incrementConfirmedRequests(Long eventId);
}