package ru.practicum.ewm.analyzer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "topics")
public class AnalyzerKafkaTopics {

    private String userActions;
    
    private String eventsSimilarity;

}