package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.ewm.client.EventClient;
import ru.practicum.ewm.client.UserClient;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.enums.event.EventState;
import ru.practicum.ewm.enums.request.RequestStatus;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.RequestMapper;
import ru.practicum.ewm.model.Request;
import ru.practicum.ewm.repository.RequestRepository;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {

    private final UserClient userClient;
    private final EventClient eventClient;
    private final RequestRepository requestRepository;

    private final RequestMapper requestMapper;

    @Override
    @Transactional
    public ParticipationRequestDto create(Long userId, Long eventId) {
        log.debug("Метод createRequest(); userId={}, eventId={}", userId, eventId);

        userClient.validateUserExists(userId);
        EventFullDto eventDto = eventClient.getEventBy(eventId);

        if (eventClient.existsByIdAndInitiatorId(eventId, userId)) {
            throw new ConflictException("Нельзя участвовать в собственном событии");
        }

        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new ConflictException("Request уже создан ранее");
        }

        if (!eventDto.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }

        long limit = eventDto.getParticipantLimit();
        long confirm = eventDto.getConfirmedRequests();

        if (limit > 0 && confirm >= limit) {
            throw new ConflictException("Достигнут лимит запросов на участие в событии");
        }

        RequestStatus status =
                (!eventDto.getRequestModeration() || limit == 0) ? RequestStatus.CONFIRMED : RequestStatus.PENDING;

        if (status == RequestStatus.CONFIRMED) {
//            eventDto.setConfirmedRequests(eventDto.getConfirmedRequests() + 1);
//            eventClient.updateEvent(eventDto.getId(), eventDto);
            eventClient.incrementConfirmedRequests(eventDto.getId());
        }

        Request request = Request.builder()
                .eventId(eventId)
                .requesterId(userId)
                .status(status)
                .build();
        request = requestRepository.save(request);

        return requestMapper.toDto(request);
    }

    @Override
    public List<ParticipationRequestDto> getAllBy(Long userId) {
        log.debug("Метод getAllBy(); userId={}", userId);

        List<Request> result = requestRepository.findAllByRequesterId(userId);

        return result.stream()
                .map(requestMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancel(Long userId, Long requestId) {
        log.debug("Метод cancel(); userId={}, requestId={}", userId, requestId);

        userClient.validateUserExists(userId);
        Request request = this.findRequestBy(requestId);
        request.setStatus(RequestStatus.CANCELED);

        if (!request.getRequesterId().equals(userId)) {
            throw new ConflictException("User id={} не является автором этого запроса", userId);
        }
        request = requestRepository.save(request);

        return requestMapper.toDto(request);
    }


    // Internal API:

    @Override
    public List<ParticipationRequestDto> findAllByEventId(Long eventId) {
        log.debug("Метод findAllByEventId(); eventId={}", eventId);

        return requestRepository.findAllByEventId(eventId).stream()
                .map(requestMapper::toDto)
                .toList();
    }

    @Override
    public List<ParticipationRequestDto> findAllByIdIn(Set<Long> requestIds) {
        log.debug("Метод findAllByIdIn(); requestIds={}", requestIds);


        return requestRepository.findAllByIdIn(requestIds).stream()
                .map(requestMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void updateRequestStatus(@PathVariable RequestStatus status, @RequestParam Set<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            requestRepository.updateStatus(status, ids);
        }
    }

    @Override
    public boolean isParticipant(Long userId, Long eventId) {
        return requestRepository.existsByEventIdAndRequesterId(userId, eventId);
    }


    private Request findRequestBy(Long requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request id={} не найден", requestId));
    }
}