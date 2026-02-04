package ru.practicum.ewm.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.analyzer.model.EventSimilarity;

import java.util.List;
import java.util.Optional;

public interface EventSimilarityRepository extends JpaRepository<EventSimilarity, Long> {

    @Query("""
            SELECT s
            FROM EventSimilarity s
            WHERE s.eventA IN :eventIds OR s.eventB IN :eventIds
            """)
    List<EventSimilarity> findByEventIdIn(@Param("eventIds") List<Long> eventIds);

    @Query("""
            SELECT s
            FROM EventSimilarity s
            WHERE s.eventA = :eventId OR s.eventB = :eventId
            """)
    List<EventSimilarity> findAllByEventId(@Param("eventId") Long eventId);

    Optional<EventSimilarity> findByEventAAndEventB(Long eventIdA, Long eventIdB);
}