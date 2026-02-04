package ru.practicum.ewm.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.config.AggregatorKafkaEventSimilarityProducerConfig;
import ru.practicum.ewm.config.AggregatorKafkaTopics;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
public class AggregatorService {

    private final KafkaProducer<Void, SpecificRecordBase> eventsSimilarityProducer;
    private final AggregatorKafkaTopics aggregatorKafkaTopics;

    private final Map<Long, Map<Long, Double>> eventUserWeight = new HashMap<>();
    private final Map<Long, Map<Long, Double>> scalarResultMatrix = new HashMap<>();

    public AggregatorService(AggregatorKafkaEventSimilarityProducerConfig kafkaEventsSimilarityProducerConfig,
                             AggregatorKafkaTopics aggregatorKafkaTopics) {

        this.aggregatorKafkaTopics = aggregatorKafkaTopics;
        this.eventsSimilarityProducer = new KafkaProducer<>(kafkaEventsSimilarityProducerConfig.getProperties());
    }

    public void sendAvro(EventSimilarityAvro avro) {

        String topic = aggregatorKafkaTopics.getEventsSimilarity();
        ProducerRecord<Void, SpecificRecordBase> record = new ProducerRecord<>(topic, avro);

        try {
            eventsSimilarityProducer.send(record);
        } catch (KafkaException e) {
            log.error(e.getMessage(), e);
        }
    }

    @KafkaListener(
            topics = "#{@aggregatorKafkaTopics.userActions}",
            containerFactory = "userActionKafkaListenerContainerFactory")
    public void consumeUserActions(UserActionAvro avro) {
        try {

            Long eventId = avro.getEventId();
            Long userId = avro.getUserId();
            Double newWeight = getActionWeight(avro.getActionType());

            List<EventSimilarityAvro> similatities = updateEventWeight(eventId, userId, newWeight, avro.getTimestamp());

            similatities.stream()
                    .sorted(Comparator.comparingLong(EventSimilarityAvro::getEventA)
                            .thenComparingLong(EventSimilarityAvro::getEventB))
                    .forEach(this::sendAvro);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private List<EventSimilarityAvro> updateEventWeight(Long eventId, Long userId, Double newWeight, Instant timestamp) {

        Map<Long, Double> userWeights = eventUserWeight.computeIfAbsent(eventId, key -> new HashMap<>());
        Double oldWeight = userWeights.get(userId);

        if (oldWeight == null || newWeight > oldWeight) {
            userWeights.put(userId, newWeight);

            return recalculateSimilarities(eventId, userId, newWeight, oldWeight, timestamp);
        }

        return Collections.emptyList();
    }

    private Optional<EventSimilarityAvro> calculateSimilarity(Long eventA,
                                                              Long eventB,
                                                              Double dotProduct,
                                                              Instant timestamp) {
        double normA = calculateNorm(eventA);
        double normB = calculateNorm(eventB);

        if (normA == 0 || normB == 0) {
            return Optional.empty();
        }

        double similarity = dotProduct / (normA * normB);
        EventSimilarityAvro similarityAvro = EventSimilarityAvro.newBuilder()
                .setEventA(eventA)
                .setEventB(eventB)
                .setScore(similarity)
                .setTimestamp(timestamp)
                .build();

        Optional<EventSimilarityAvro> result = Optional.of(similarityAvro);

        return result;
    }

    private List<EventSimilarityAvro> recalculateSimilarities(Long eventId,
                                                              Long userId,
                                                              Double newWeight,
                                                              Double oldWeight,
                                                              Instant timestamp) {

        List<EventSimilarityAvro> updatedSimilarities = new ArrayList<>();
        Map<Long, Double> eventDotProductMap = scalarResultMatrix.computeIfAbsent(eventId, key -> new HashMap<>());
        double dotProduct = eventDotProductMap.getOrDefault(eventId, 0.0);

        double delta;
        if (oldWeight == null) {
            delta = newWeight;
        } else {
            delta = newWeight - oldWeight;
        }
        double newDotProductValue = dotProduct + delta;
        eventDotProductMap.put(eventId, newDotProductValue);

        for (Long otherEventId : eventUserWeight.keySet()) {
            if (!eventId.equals(otherEventId)) {
                Map<Long, Double> otherUserWeights = eventUserWeight.get(otherEventId);

                if (otherUserWeights != null) {
                    Double otherWeight = otherUserWeights.get(userId);

                    if (otherWeight != null) {
                        long eventA = Math.min(eventId, otherEventId);
                        long eventB = Math.max(eventId, otherEventId);

                        Map<Long, Double> dotMap = scalarResultMatrix.computeIfAbsent(eventA, k -> new HashMap<>());
                        double currentDot = dotMap.getOrDefault(eventB, 0.0);

                        double oldMinWeight;

                        if (oldWeight == null) {
                            oldMinWeight = 0.0;
                        } else {
                            oldMinWeight = Math.min(oldWeight, otherWeight);
                        }

                        double newMinWeight = Math.min(newWeight, otherWeight);
                        double dotDelta = newMinWeight - oldMinWeight;
                        double updatedDot = currentDot + dotDelta;

                        dotMap.put(eventB, updatedDot);

                        Optional<EventSimilarityAvro> similarity =
                                calculateSimilarity(eventA, eventB, updatedDot, timestamp);

                        similarity.ifPresent(updatedSimilarities::add);
                    }
                }
            }
        }
        return updatedSimilarities;
    }

    private double calculateNorm(Long eventId) {

        Map<Long, Double> eventDotProductMap = scalarResultMatrix.get(eventId);
        if (eventDotProductMap == null) return 0.0;
        double result = Math.sqrt(eventDotProductMap.getOrDefault(eventId, 0.0));

        return result;
    }

    private double getActionWeight(ActionTypeAvro action) {
        return switch (action) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }
}