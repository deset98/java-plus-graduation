package ru.practicum.ewm.event.service;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.client.GrpcAnalyzerClient;
import ru.practicum.ewm.client.GrpcCollectorClient;
import ru.practicum.ewm.client.RequestClient;
import ru.practicum.ewm.client.UserClient;
import ru.practicum.ewm.dto.event.*;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.dto.user.UserShortDto;
import ru.practicum.ewm.enums.event.EventState;
import ru.practicum.ewm.enums.event.UpdRequestStatus;
import ru.practicum.ewm.enums.request.RequestStatus;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.event.mapper.LocationMapper;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.QEvent;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.BadRequestException;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.UserActionProto;
import ru.practicum.ewm.stats.proto.UserPredictionsRequestProto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.time.ZoneOffset.UTC;
import static ru.practicum.ewm.enums.event.EventState.CANCELED;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final UserClient userClient;
    private final EventRepository eventRepository;
    private final RequestClient requestClient;
    private final CategoryRepository categoryRepository;

    private final EventMapper eventMapper;
    private final LocationMapper locationMapper;

    private final GrpcCollectorClient grpcCollectorClient;
    private final GrpcAnalyzerClient grpcAnalyzerClient;

    // Private API:
    @Override
    @Transactional
    public EventFullDto create(Long userId, final NewEventDto newDto) {
        log.debug("Метод create(); userId={}, newDto={}", userId, newDto);

        this.checkStartDate(newDto.getEventDate());
        userClient.validateUserExists(userId);
        Category category = this.findCategoryBy(newDto.getCategory());

        Event event = eventMapper.toEntity(newDto);
        event.setLocationEntity(locationMapper.toEntity(newDto.getLocation()));
        event.setInitiatorId(userId);
        event.setCategory(category);
        event = eventRepository.save(event);

        log.debug("Создан event={}", event);

        return eventMapper.toFullDto(event);
    }

    @Override
    public List<EventShortDto> getAllByUser(Long userId, int from, int size) {
        log.debug("Метод getAllByUser(); userId={}", userId);

        int page = from / size;
        Pageable pageable = PageRequest.of(page, size, Sort.by("eventDate").descending());
        Page<Event> events = eventRepository.findAllByInitiatorId(userId, pageable);

        return events.map(eventMapper::toShortDto).getContent();
    }

    @Override
    public EventFullDto getByUser(Long userId, Long eventId) {
        log.debug("Метод getByUser(); eventId={}, userId={}", eventId, userId);

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event id={} у user id={} не найдено", eventId, userId));

        return eventMapper.toFullDto(event);
    }

    @Override
    @Transactional
    public EventFullDto updateByUser(Long userId, Long eventId, UpdEventUserRequest updDto) {
        log.debug("Метод userUpdate(); userId={}, eventId: {}, dto={}",
                userId, eventId, updDto);

        this.checkEventDateForUpdate(updDto);

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event id={} не найдено; User id={} ", eventId, userId));

        if (event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Event id={} нельзя изменить; его status={}", eventId, event.getState());
        }

        log.debug("Найден Event в репозитории; event={}", event);

        if (!(event.getState().equals(CANCELED) || event.getState().equals(EventState.PENDING))) {
            throw new ConflictException("Event id={} нельзя обновить пока оно опубликовано", eventId);
        }
        if (updDto.getCategory() != null) {
            event.setCategory(this.findCategoryBy(updDto.getCategory()));
        }

        if (updDto.getStateAction() != null) {
            switch (updDto.getStateAction()) {
                case SEND_TO_REVIEW -> event.setState(EventState.PENDING);
                case CANCEL_REVIEW -> event.setState(CANCELED);
            }
        }

        eventMapper.updateFromDto(updDto, event);
        event = eventRepository.save(event);

        log.debug("Метод userUpdate(); Event обновлен в репозитории event={}", event);

        return eventMapper.toFullDto(event);
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        log.debug("Метод getUserRequests(); userId={}, eventId={}", userId, eventId);

        List<ParticipationRequestDto> requests = requestClient.findAllByEventId(eventId);
        return requests;
    }

    @Override
    @Transactional
    public UpdRequestsStatusResult updateRequests(Long userId, Long eventId, EventRequestStatusUpdateRequest updDto) {
        log.debug("Метод updateRequests(), userId={}, eventId={}", userId, eventId);

        Event event = this.findEventBy(eventId);
        List<ParticipationRequestDto> requests = requestClient.findAllByIdIn(updDto.getRequestIds());

        if (requests.isEmpty()) {
            return UpdRequestsStatusResult.builder()
                    .confirmedRequests(List.of())
                    .rejectedRequests(List.of())
                    .build();
        }

        UpdRequestsStatusResult result;

        switch (updDto.getStatus()) {
            case UpdRequestStatus.CONFIRMED -> {
                if (event.getConfirmedRequests() == event.getParticipantLimit().longValue()) {
                    throw new ConflictException("На Event id={} больше нет мест", eventId);
                }

                int availableSlots = event.getParticipantLimit() == 0
                        ? requests.size()
                        : event.getParticipantLimit().intValue() - event.getConfirmedRequests().intValue();

                List<ParticipationRequestDto> toConfirm = requests.size() <= availableSlots
                        ? requests
                        : requests.subList(0, availableSlots);

                List<ParticipationRequestDto> toReject = requests.size() <= availableSlots
                        ? List.of()
                        : requests.subList(availableSlots, requests.size());

                toConfirm.forEach(requestDto -> requestDto.setStatus(RequestStatus.CONFIRMED));
                toReject.forEach(requestDto -> requestDto.setStatus(RequestStatus.REJECTED));

                event.setConfirmedRequests(event.getConfirmedRequests() + toConfirm.size());
                eventRepository.save(event);

                Set<Long> confirmedIds = toConfirm.stream()
                        .map(ParticipationRequestDto::getId)
                        .collect(Collectors.toSet());

                Set<Long> rejectedIds = toReject.stream()
                        .map(ParticipationRequestDto::getId)
                        .collect(Collectors.toSet());

                requestClient.updateRequestStatus(RequestStatus.CONFIRMED, confirmedIds);
                requestClient.updateRequestStatus(RequestStatus.REJECTED, rejectedIds);

                result = UpdRequestsStatusResult.builder()
                        .confirmedRequests(toConfirm)
                        .rejectedRequests(toReject)
                        .build();
            }

            case UpdRequestStatus.REJECTED -> {
                boolean hasConfirmed = requests.stream().anyMatch(r -> r.getStatus() == RequestStatus.CONFIRMED);

                if (hasConfirmed) {
                    throw new ConflictException("Нельзя отклонить подтвержденный Request");
                }

                requests.forEach(r -> r.setStatus(RequestStatus.REJECTED));
                requestClient.updateRequestStatus(RequestStatus.REJECTED, updDto.getRequestIds());

                result = UpdRequestsStatusResult.builder()
                        .confirmedRequests(List.of())
                        .rejectedRequests(requests)
                        .build();
            }

            default -> throw new IllegalArgumentException("Неизвестный статус: " + updDto.getStatus());
        }

        return result;
    }


    // Admin API:
    @Override
    @Transactional
    public EventFullDto updateByAdmin(Long eventId, UpdEventAdminRequest updDto) {
        log.debug("Метод adminUpdateEvent(); eventId: {}, dto={}", eventId, updDto);

        Event event = this.findEventBy(eventId);

        eventMapper.updateFromDto(updDto, event);

        this.checkEventDateForPublish(updDto.getEventDate());

        if (updDto.getStateAction() != null) {
            switch (updDto.getStateAction()) {
                case PUBLISH_EVENT -> {
                    if (event.getState().equals(EventState.PENDING)) {
                        event.setState(EventState.PUBLISHED);
                        event.setPublishedOn(Instant.now());
                    } else if (event.getState().equals(CANCELED) ||
                            event.getState().equals(EventState.PUBLISHED)) {
                        throw new ConflictException("Event id={} нельзя опубликовать; его status={}",
                                eventId, event.getState());
                    }

                    log.debug("Для Event назначен статус={}, время публикации publishedOn={}",
                            event.getState(), event.getPublishedOn());
                }
                case REJECT_EVENT -> {
                    if (event.getState().equals(EventState.PENDING)) {
                        event.setState(CANCELED);
                    } else if (event.getState().equals(EventState.PUBLISHED)) {
                        throw new ConflictException("Опубликованные Event не могут быть отклонены");
                    }

                    log.debug("Для Event назначен статус={}", event.getState());
                }
            }
        }

        event = eventRepository.save(event);

        log.debug("Метод adminUpdate(); Event обновлен в репозитории event={}", event);

        return eventMapper.toFullDto(event);
    }

    @Override
    public List<EventFullDto> searchForAdmin(AdminEventSearchParams params) {
        log.debug("Метод adminSearchEvents; {}", params);

        QEvent event = QEvent.event;
        List<BooleanExpression> conditions = new ArrayList<>();

        if (params.getUsers() != null && !params.getUsers().isEmpty()) {
            conditions.add(event.initiatorId.in(params.getUsers()));
        }

        if (params.getCategories() != null && !params.getCategories().isEmpty()) {
            conditions.add(event.category.id.in(params.getCategories()));
        }

        if (params.getStates() != null && !params.getStates().isEmpty()) {
            conditions.add(event.state.in(params.getStates()));
        }

        if (params.getRangeStart() != null) {
            Instant rangeStart = params.getRangeStart().atZone(UTC).toInstant();
            conditions.add(event.eventDate.after(rangeStart));
        }

        if (params.getRangeEnd() != null) {
            Instant rangeEnd = params.getRangeEnd().atZone(UTC).toInstant();
            conditions.add(event.eventDate.before(rangeEnd));
        }

        BooleanExpression finalCondition = conditions.stream()
                .reduce(BooleanExpression::and)
                .orElse(Expressions.TRUE);

        log.debug("{}", finalCondition);

        int page = params.getFrom() / params.getSize();
        Pageable pageable = PageRequest.of(page, params.getSize());


        Page<Event> eventsPage = eventRepository.findAll(finalCondition, pageable);

        List<Long> initiatorIds = eventsPage.stream()
                .map(Event::getInitiatorId)
                .toList();

        Map<Long, UserShortDto> usersMap = userClient.getUsersBy(initiatorIds)
                .stream().collect(Collectors.toMap(UserShortDto::getId, u -> u));

        List<EventFullDto> dtos = eventsPage.stream()
                .map(e -> {
                    EventFullDto dto = eventMapper.toFullDto(e);
                    dto.setInitiator(usersMap.get(e.getInitiatorId()));
                    return dto;
                })
                .toList();

        return dtos;
    }


    // Public API:
    @Override
    public EventFullDto getEventBy(Long eventId, Long userId) {
        log.debug("Метод getPublicById(); eventId={}", eventId);

        Event event = eventRepository.findByIdAndState(eventId, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Опубликованного Event id={} нет", eventId));

//        grpcCollectorClient.collectUserAction(
//                UserActionProto.newBuilder()
//                        .setEventId(eventId)
//                        .setUserId(userId)
//                        .setActionType(ActionTypeProto.ACTION_VIEW)
//                        .build());

        return eventMapper.toFullDto(event);
    }

    @Override
    public List<EventFullDto> getListOfEventsBy(UserEventSearchParams params, HttpServletRequest request) {
        log.debug("Метод publicSearchMany; {}", params);

        QEvent event = QEvent.event;
        List<BooleanExpression> conditions = new ArrayList<>();

        conditions.add(event.state.eq(EventState.PUBLISHED));

        if (params.getText() != null && !params.getText().isEmpty()) {
            conditions.add(
                    event.annotation.containsIgnoreCase(params.getText())
                            .or(event.description.containsIgnoreCase(params.getText())));
        }

        if (params.getCategories() != null && !params.getCategories().isEmpty()) {
            conditions.add(event.category.id.in(params.getCategories()));
        }

        if (params.getPaid() != null) {
            conditions.add(event.paid.eq(params.getPaid()));
        }

        if (params.getRangeStart() != null) {
            Instant rangeStart = params.getRangeStart().atZone(UTC).toInstant();
            conditions.add(event.eventDate.after(rangeStart));
        }

        if (params.getRangeEnd() != null) {
            Instant rangeEnd = params.getRangeEnd().atZone(UTC).toInstant();
            conditions.add(event.eventDate.before(rangeEnd));
        }

        if (params.getRangeStart() == null && params.getRangeEnd() == null) {
            conditions.add(event.eventDate.after(Instant.now()));
        }

        if (params.getOnlyAvailable() != null) {
            conditions.add(event.confirmedRequests.lt(event.participantLimit.longValue()));
        }

        BooleanExpression finalCondition = conditions.stream()
                .reduce(BooleanExpression::and)
                .orElse(Expressions.TRUE);

        log.debug("{}", finalCondition);

        int page = params.getFrom() / params.getSize();

        Pageable pageable = null;

        switch (params.getSort()) {
            case EVENT_DATE -> pageable =
                    PageRequest.of(page, params.getSize(), Sort.by(Sort.Direction.ASC, "eventDate"));
            case RATING -> pageable =
                    PageRequest.of(page, params.getSize(), Sort.by(Sort.Direction.DESC, "views"));
        }

        Page<Event> events = eventRepository.findAll(finalCondition, pageable);

        return events.map(eventMapper::toFullDto).getContent();
    }

    @Override
    public List<EventShortDto> getRecommendations(Long userId, int size) {

        UserPredictionsRequestProto requestProto =
                UserPredictionsRequestProto.newBuilder()
                        .setUserId(userId)
                        .setMaxResults(20)
                        .setMaxResults(size)
                        .build();

        List<RecommendedEventProto> events = grpcAnalyzerClient.getRecommendationsForUser(requestProto).toList();

        Map<Long, Double> scoreByEvent =
                events.stream()
                        .collect(Collectors.toMap(RecommendedEventProto::getEventId, RecommendedEventProto::getScore));

        Set<Long> ids =
                events.stream()
                        .map(RecommendedEventProto::getEventId)
                        .collect(Collectors.toSet());

        return eventRepository.findAllByIdIn(ids).stream()
                .map(eventMapper::toShortDto)
                .peek(dto -> dto.setRating(scoreByEvent.get(dto.getId())))
                .toList();
    }

    @Override
    public void like(Long userId, Long eventId) {
        if (!requestClient.isParticipant(userId, eventId)) {
            throw new BadRequestException("User id={} не является участником event eventId={}", userId, eventId);
        }

        if (!eventRepository.existsByIdAndEventDateBefore(eventId, Instant.now())) {
            throw new BadRequestException("Event eventId={} еще не прошло", eventId);
        }

        UserActionProto actionProto =
                UserActionProto.newBuilder()
                        .setEventId(eventId)
                        .setUserId(userId)
                        .setActionType(ActionTypeProto.ACTION_LIKE)
                        .build();

        grpcCollectorClient.collectUserAction(actionProto);
    }


    // Internal API:
    @Override
    public EventFullDto getEventBy(Long eventId) {
        return eventMapper.toFullDto(this.findEventBy(eventId));
    }

    @Override
    public boolean existsByIdAndInitiatorId(Long eventId, Long userId) {
        return eventRepository.existsByIdAndInitiatorId(eventId, userId);
    }

    @Override
    @Transactional
    public void incrementConfirmedRequests(Long eventId) {
        eventRepository.incrementConfirmedRequests(eventId);
    }

    private Category findCategoryBy(Long categoryId) {
        log.debug("Поиск Category id={} в репозитории", categoryId);

        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Объект Category id={} не найден", categoryId));
    }

    private Event findEventBy(Long eventId) {
        log.debug("Поиск Event id={} в репозитории", eventId);

        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Объект Event id={} не найден", eventId));
    }

    private void checkStartDate(LocalDateTime eventDate) {
        log.debug("Проверка даты при СОЗДАНИИ");

        if (eventDate != null && eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ConflictException("Дата Event при СОЗДАНИИ должна быть в будущем, мин. через 2 часа");
        }
    }

    private void checkEventDateForUpdate(UpdEventUserRequest updDto) {
        log.debug("Проверка даты Event при ОБНОВЛЕНИИ");

        if (updDto.getEventDate() != null) {
            this.checkStartDate(updDto.getEventDate());
        }
    }

    private void checkEventDateForPublish(LocalDateTime eventDate) {
        log.debug("Проверка даты Event при ПУБЛИКАЦИИ");

        if (eventDate != null && eventDate.isBefore(LocalDateTime.now().plusHours(1))) {
            throw new ConflictException("Дата Event при ПУБЛИКАЦИИ должна быть в будущем, мин. через 1 час");
        }
    }
}