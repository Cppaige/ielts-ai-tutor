package com.ielts.speaking.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class SessionResultProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public SessionResultProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(Long sessionId, Long userId, Long topicId, Long reportId,
                     BigDecimal overallBand, BigDecimal fluencyScore, BigDecimal lexicalScore,
                     BigDecimal grammarScore, BigDecimal pronunciationScore) {
        Map<String, Object> message = new HashMap<>();
        message.put("version", 1);
        message.put("sessionId", sessionId);
        message.put("userId", userId);
        message.put("topicId", topicId);
        message.put("serviceRecordId", reportId);
        message.put("type", "SPEAKING");
        message.put("overallBand", overallBand);
        message.put("fluencyScore", fluencyScore);
        message.put("lexicalScore", lexicalScore);
        message.put("grammarScore", grammarScore);
        message.put("pronunciationScore", pronunciationScore);
        message.put("completedAt", Instant.now().toString());
        kafkaTemplate.send("speaking.session.result", String.valueOf(userId), message);
    }
}
