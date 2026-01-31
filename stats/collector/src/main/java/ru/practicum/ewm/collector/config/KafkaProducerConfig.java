package ru.practicum.ewm.collector.config;

import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaProducerConfig {

    private final KafkaProducerProperties producerProperties;

    public KafkaProducerConfig(KafkaProducerProperties producerProperties) {
        this.producerProperties = producerProperties;
    }

    @Bean
    public KafkaProducer<Void, SpecificRecordBase> kafkaProducer() {
        return new KafkaProducer<>(producerProperties.getProperties());
    }
}