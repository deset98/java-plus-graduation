package ru.practicum.ewm.collector.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvroKafkaProducerImpl implements AvroKafkaProducer, AutoCloseable {

    private final KafkaProducer<Void, SpecificRecordBase> kafkaProducer;

    @Override
    public void sendAvro(String topic, SpecificRecordBase avroMessage) {

        ProducerRecord<Void, SpecificRecordBase> record = new ProducerRecord<>(topic, avroMessage);

        kafkaProducer.send(record);
    }

    @Override
    public void close() throws Exception {
        kafkaProducer.flush();
        kafkaProducer.close(Duration.ofSeconds(10));
    }
}