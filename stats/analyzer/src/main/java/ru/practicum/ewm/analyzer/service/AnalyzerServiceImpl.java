package ru.practicum.ewm.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.analyzer.model.EventSimilarity;
import ru.practicum.ewm.analyzer.model.UserInteraction;
import ru.practicum.ewm.analyzer.repository.EventSimilarityRepository;
import ru.practicum.ewm.analyzer.repository.UserInteractionRepository;
import ru.practicum.ewm.stats.proto.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.proto.UserPredictionsRequestProto;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyzerServiceImpl implements AnalyzerService {

    private final UserInteractionRepository userInteractionRepository;
    private final EventSimilarityRepository eventSimilarityRepository;

    @Override
    public Stream<RecommendedEventProto> getRecommendationsForUser(UserPredictionsRequestProto request) {
        List<UserInteraction> interactionsForUser = userInteractionRepository.findAllByUserId(request.getUserId());

        if (interactionsForUser.isEmpty()) {
            return Stream.empty();
        }

        interactionsForUser = interactionsForUser.stream()
                .sorted(Comparator.comparing(UserInteraction::getTimestamp).reversed())
                .limit(request.getMaxResults())
                .toList();

        Set<Long> alreadyWatchedEvents = interactionsForUser.stream()
                .map(UserInteraction::getEventId)
                .collect(Collectors.toSet());

        List<EventSimilarity> similarEvents =
                eventSimilarityRepository.findByEventIdIn(
                        interactionsForUser.stream()
                                .map(UserInteraction::getEventId)
                                .collect(Collectors.toList())
                );

        Set<Long> notInteractedEvents = similarEvents.stream()
                .flatMap(similarity -> Stream.of(
                        Map.entry(similarity.getEventA(), similarity.getScore()),
                        Map.entry(similarity.getEventB(), similarity.getScore())
                ))
                .filter(e -> !alreadyWatchedEvents.contains(e.getKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        Double::max
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(request.getMaxResults())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        Map<Long, List<EventSimilarity>> nearestNeighbors = new HashMap<>();

        for (UserInteraction interaction : interactionsForUser) {
            Long recentEventId = interaction.getEventId();
            List<EventSimilarity> similarities = eventSimilarityRepository.findAllByEventId(recentEventId);

            List<EventSimilarity> neighbors = similarities.stream()
                    .filter(similarity -> {
                        Long neighborId;

                        if (similarity.getEventA().equals(recentEventId)) {
                            neighborId = similarity.getEventB();
                        } else {
                            neighborId = similarity.getEventA();
                        }

                        return alreadyWatchedEvents.contains(neighborId);
                    })
                    .sorted(Comparator.comparing(EventSimilarity::getScore).reversed())
                    .limit(20)
                    .toList();
            nearestNeighbors.put(recentEventId, neighbors);
        }

        Set<Long> neighborEventIds = nearestNeighbors.values().stream()
                .flatMap(List::stream)
                .map(similarity -> {
                    if (nearestNeighbors.containsKey(similarity.getEventA())) {
                        return similarity.getEventB();
                    } else {
                        return similarity.getEventA();
                    }
                })
                .collect(Collectors.toSet());

        Map<Long, Double> neighborRatingMap = userInteractionRepository
                .findAllByUserIdAndEventIdIn(request.getUserId(), new ArrayList<>(neighborEventIds))
                .stream()
                .collect(Collectors.toMap(UserInteraction::getEventId, UserInteraction::getRating));


        List<UserInteraction> finalInteractionsForUser = interactionsForUser;

        return notInteractedEvents.stream()
                .map(newEventId -> {
                    List<EventSimilarity> neighbors = finalInteractionsForUser.stream()
                            .flatMap(interaction ->
                                    eventSimilarityRepository.findAllByEventId(interaction.getEventId()).stream())
                            .filter(similarity -> {
                                Long neighborId;
                                if (similarity.getEventA().equals(newEventId)) {
                                    neighborId = similarity.getEventB();
                                } else {
                                    neighborId = similarity.getEventA();
                                }
                                return alreadyWatchedEvents.contains(neighborId);
                            })
                            .sorted(Comparator.comparing(EventSimilarity::getScore).reversed())
                            .limit(20)
                            .toList();

                    double weightedSum = neighbors.stream()
                            .mapToDouble(eventSimilarity -> {
                                Long neighborId;
                                if (eventSimilarity.getEventA().equals(newEventId)) {
                                    neighborId = eventSimilarity.getEventB();
                                } else {
                                    neighborId = eventSimilarity.getEventA();
                                }

                                Double rating = neighborRatingMap.get(neighborId);
                                if (rating == null) {
                                    return 0.0;
                                }

                                double similarity = eventSimilarity.getScore();
                                return rating * similarity;
                            })
                            .sum();

                    double similaritySum = neighbors.stream()
                            .mapToDouble(EventSimilarity::getScore)
                            .sum();

                    double predictedRating;
                    if (similaritySum == 0.0) {
                        predictedRating = 0.0;
                    } else {
                        predictedRating = weightedSum / similaritySum;
                    }

                    return RecommendedEventProto.newBuilder()
                            .setEventId(newEventId)
                            .setScore(predictedRating)
                            .build();
                });
    }

    @Override
    public Stream<RecommendedEventProto> getSimilarEvents(SimilarEventsRequestProto request) {

        List<EventSimilarity> similarEvents =
                eventSimilarityRepository.findAllByEventId(request.getEventId());
        List<UserInteraction> userInteractions =
                userInteractionRepository.findAllByUserId(request.getUserId());

        Set<Long> events =
                userInteractions.stream()
                        .map(UserInteraction::getEventId)
                        .collect(Collectors.toSet());

        return similarEvents.stream()
                .filter(eventSimilarity -> {
                    boolean A = events.contains(eventSimilarity.getEventA());
                    boolean B = events.contains(eventSimilarity.getEventB());
                    return !(A && B);
                })
                .sorted(Comparator.comparing(EventSimilarity::getScore).reversed())
                .limit(request.getMaxResults())
                .map(eventSimilarity -> {
                    if (eventSimilarity.getEventA().equals(request.getEventId())) {
                        return RecommendedEventProto.newBuilder()
                                .setEventId(eventSimilarity.getEventB())
                                .setScore(eventSimilarity.getScore())
                                .build();
                    } else {
                        return RecommendedEventProto.newBuilder()
                                .setEventId(eventSimilarity.getEventA())
                                .setScore(eventSimilarity.getScore())
                                .build();
                    }
                });
    }

    @Override
    public Stream<RecommendedEventProto> getInteractionsCount(InteractionsCountRequestProto request) {

        List<UserInteraction> userInteractions =
                userInteractionRepository.findAllByEventIdIn(request.getEventIdList());

        Map<Long, Double> eventRatingMap = userInteractions.stream()
                .collect(Collectors.groupingBy(
                        UserInteraction::getEventId,
                        Collectors.summingDouble(UserInteraction::getRating)));

        return eventRatingMap.entrySet().stream()
                .map(entry ->
                        RecommendedEventProto.newBuilder()
                                .setEventId(entry.getKey())
                                .setScore(entry.getValue())
                                .build());
    }
}