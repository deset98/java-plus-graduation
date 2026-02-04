package ru.practicum.ewm.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.analyzer.model.UserInteraction;

import java.util.List;
import java.util.Optional;

public interface UserInteractionRepository extends JpaRepository<UserInteraction, Long> {

    Optional<UserInteraction> findByUserIdAndEventId(Long userId, Long eventId);

    List<UserInteraction> findAllByUserIdAndEventIdIn(Long userId, List<Long> eventId);

    List<UserInteraction> findAllByEventIdIn(List<Long> eventId);

    List<UserInteraction> findAllByUserId(Long userId);
}