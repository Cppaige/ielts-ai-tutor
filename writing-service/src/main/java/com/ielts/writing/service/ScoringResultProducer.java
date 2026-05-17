package com.ielts.writing.service;

import com.ielts.writing.dto.ScoringResultMessage;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ScoringResultProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ScoringResultProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(ScoringResultMessage message) {
        kafkaTemplate.send("writing.scoring.result", String.valueOf(message.userId()), message);
    }
}
