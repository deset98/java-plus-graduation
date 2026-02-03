package ru.practicum.ewm.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.analyzer.model.EventSimilarity;
import ru.practicum.ewm.analyzer.model.UserInteraction;
import ru.practicum.ewm.analyzer.repository.EventSimilarityRepository;
import ru.practicum.ewm.analyzer.repository.UserInteractionRepository;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyzerConsumer {

    private final UserInteractionRepository userInteractionRepository;
    private final EventSimilarityRepository eventSimilarityRepository;

    @KafkaListener(
            topics = "#{@analyzerKafkaTopics.userActions}",
            containerFactory = "userActionKafkaListenerContainerFactory")
    public void consumeUserActions(UserActionAvro userActionAvro) {
        try {
            Optional<UserInteraction> existingInteraction =
                    userInteractionRepository.findByUserIdAndEventId(userActionAvro.getUserId(),
                            userActionAvro.getEventId());

            Double score = 0.0;
            switch (userActionAvro.getActionType()) {
                case VIEW -> {
                    score = 0.4;
                }
                case REGISTER -> {
                    score = 0.8;
                }
                case LIKE -> {
                    score = 1.0;
                }
            }

            if (existingInteraction.isPresent()) {
                UserInteraction userInteraction = existingInteraction.get();
                if (userInteraction.getRating() < score) {

                    userInteraction.setRating(score);
                    userInteraction.setTimestamp(
                            LocalDateTime.ofInstant(userActionAvro.getTimestamp(), ZoneId.systemDefault()));

                    userInteractionRepository.save(userInteraction);
                }
            } else {
                UserInteraction userInteraction = UserInteraction.builder()
                        .userId(userActionAvro.getUserId())
                        .eventId(userActionAvro.getEventId())
                        .rating(score)
                        .timestamp(LocalDateTime.now())
                        .build();

                userInteractionRepository.save(userInteraction);
            }
        } catch (Exception e) {
            log.error("Ошибка в UserInteractionConsumer", e);
        }
    }

    @KafkaListener(
            topics = "#{@analyzerKafkaTopics.eventsSimilarity}",
            containerFactory = "eventSimilarityKafkaListenerContainerFactory"
    )
    public void consumeEventSimilarity(EventSimilarityAvro avro) {
        try {
            Optional<EventSimilarity> existingSimilarity =
                    eventSimilarityRepository.findByEventAAndEventB(avro.getEventA(), avro.getEventB());

            if (existingSimilarity.isPresent()) {

                EventSimilarity eventSimilarity = existingSimilarity.get();
                eventSimilarity.setScore(avro.getScore());
                eventSimilarity.setTimestamp(LocalDateTime.now());

                eventSimilarityRepository.save(eventSimilarity);
            } else {

                EventSimilarity similarity = EventSimilarity.builder()
                        .eventA(avro.getEventA())
                        .eventB(avro.getEventB())
                        .score(avro.getScore())
                        .timestamp(LocalDateTime.now())
                        .build();

                eventSimilarityRepository.save(similarity);
            }
        } catch (Exception e) {
            log.error("Ошибка в EventSimilarityConsumer", e);
        }
    }
}
