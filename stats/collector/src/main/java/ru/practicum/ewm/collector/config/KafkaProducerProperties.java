package ru.practicum.ewm.collector.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kafka-collector.producer")
public class KafkaProducerProperties {
    private Properties properties;
}