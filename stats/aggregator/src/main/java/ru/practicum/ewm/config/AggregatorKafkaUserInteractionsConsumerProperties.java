package ru.practicum.ewm.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kafka-aggregator.user-actions-consumer")
public class AggregatorKafkaUserInteractionsConsumerProperties {

    private Properties properties;

}