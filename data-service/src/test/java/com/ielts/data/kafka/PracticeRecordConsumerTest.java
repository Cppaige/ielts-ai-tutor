package com.ielts.data.kafka;

import com.ielts.data.dto.PracticeRecordMessage;
import com.ielts.data.service.PracticeRecordService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PracticeRecordConsumerTest {

    @Mock
    private PracticeRecordService practiceRecordService;

    @InjectMocks
    private PracticeRecordConsumer consumer;

    @Test
    void consumeWritingResult_savesRecord() {
        var message = new PracticeRecordMessage(1, 1001L, 42L, 12345L, "WRITING", new BigDecimal("7.0"), Instant.now());
        consumer.consumeWritingResult(message);
        verify(practiceRecordService).saveFromMessage(message);
    }

    @Test
    void consumeSpeakingResult_savesRecord() {
        var message = new PracticeRecordMessage(1, 1001L, 88L, 5678L, "SPEAKING", new BigDecimal("6.5"), Instant.now());
        consumer.consumeSpeakingResult(message);
        verify(practiceRecordService).saveFromMessage(message);
    }
}
