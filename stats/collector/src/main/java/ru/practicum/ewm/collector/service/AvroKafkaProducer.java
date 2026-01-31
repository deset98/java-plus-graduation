package ru.practicum.ewm.collector.service;

import org.apache.avro.specific.SpecificRecordBase;

public interface AvroKafkaProducer {
    void sendAvro(String topic, SpecificRecordBase avroMessage);
}