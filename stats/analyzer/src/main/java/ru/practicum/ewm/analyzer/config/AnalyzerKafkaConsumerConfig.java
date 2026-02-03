package ru.practicum.ewm.analyzer.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@Configuration
@RequiredArgsConstructor
public class AnalyzerKafkaConsumerConfig {

    private final AnalyzerKafkaEventSimilarityConsumerProperties eventsSimilarityConsumerConfig;
    private final AnalyzerKafkaUserInteractionConsumerProperties userInteractionConsumerConfig;

    @Bean
    public ConsumerFactory<Void, UserActionAvro> userActionsConsumerFactory() {
        return createConsumerFactory(userInteractionConsumerConfig.getProperties());
    }

    @Bean
    public ConsumerFactory<Void, EventSimilarityAvro> eventSimilarityAvroConsumerFactory() {
        return createConsumerFactory(eventsSimilarityConsumerConfig.getProperties());
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Void, UserActionAvro> userActionKafkaListenerContainerFactory() {
        return createListenerContainerFactory(userActionsConsumerFactory());
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Void, EventSimilarityAvro> eventSimilarityKafkaListenerContainerFactory() {
        return createListenerContainerFactory(eventSimilarityAvroConsumerFactory());
    }


    private <V> ConsumerFactory<Void, V> createConsumerFactory(Properties props) {

        Map<String, Object> configMap = props.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().toString(),
                        Map.Entry::getValue
                ));

        return new DefaultKafkaConsumerFactory<>(configMap);
    }

    private <V> ConcurrentKafkaListenerContainerFactory<Void, V> createListenerContainerFactory(
            ConsumerFactory<Void, V> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<Void, V> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        return factory;
    }
}
