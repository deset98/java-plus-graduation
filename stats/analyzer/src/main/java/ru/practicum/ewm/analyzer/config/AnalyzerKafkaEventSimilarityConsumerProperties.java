package ru.practicum.ewm.analyzer.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kafka-analyzer.events-similarity-consumer")
public class AnalyzerKafkaEventSimilarityConsumerProperties {

    private Properties properties;

}