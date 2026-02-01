package ru.practicum.ewm.event.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.enums.event.EventState;
import ru.practicum.ewm.event.model.Event;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>, QuerydslPredicateExecutor<Event> {

    Page<Event> findAllByInitiatorId(Long userId, Pageable pageable);

    Optional<Event> findByIdAndInitiatorId(Long eventId, Long userId);

    Optional<Event> findByIdAndState(Long eventId, EventState state);

    List<Event> findAllByIdIn(Collection<Long> ids);

    boolean existsByCategoryId(Long categoryId);

    boolean existsByIdAndInitiatorId(Long eventId, Long userId);

    @Modifying
    @Query("UPDATE Event e SET e.confirmedRequests = e.confirmedRequests + 1 WHERE e.id = :eventId")
    void incrementConfirmedRequests(Long eventId);

    boolean eventDateIsBefore(Long eventId, LocalDateTime date);
}