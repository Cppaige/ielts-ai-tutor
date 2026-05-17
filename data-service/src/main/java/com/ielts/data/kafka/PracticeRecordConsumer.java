package com.ielts.data.kafka;

import com.ielts.data.dto.PracticeRecordMessage;
import com.ielts.data.service.PracticeRecordService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PracticeRecordConsumer {

    private final PracticeRecordService practiceRecordService;

    public PracticeRecordConsumer(PracticeRecordService practiceRecordService) {
        this.practiceRecordService = practiceRecordService;
    }

    @KafkaListener(topics = "writing.scoring.result", groupId = "data-service")
    public void consumeWritingResult(PracticeRecordMessage message) {
        practiceRecordService.saveFromMessage(message);
    }

    @KafkaListener(topics = "speaking.session.result", groupId = "data-service")
    public void consumeSpeakingResult(PracticeRecordMessage message) {
        practiceRecordService.saveFromMessage(message);
    }
}
