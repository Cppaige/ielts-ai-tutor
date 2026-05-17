package com.ielts.speaking.kafka;

import com.ielts.speaking.entity.SpeakingReport;
import com.ielts.speaking.entity.SpeakingSession;
import com.ielts.speaking.repository.SpeakingSessionRepository;
import com.ielts.speaking.service.ReportGenerator;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ReportRequestConsumer {

    private final ReportGenerator reportGenerator;
    private final SessionResultProducer resultProducer;
    private final SpeakingSessionRepository sessionRepository;

    public ReportRequestConsumer(ReportGenerator reportGenerator,
                                 SessionResultProducer resultProducer,
                                 SpeakingSessionRepository sessionRepository) {
        this.reportGenerator = reportGenerator;
        this.resultProducer = resultProducer;
        this.sessionRepository = sessionRepository;
    }

    @KafkaListener(topics = "speaking.report.request", groupId = "speaking-service")
    public void consume(Map<String, Object> message) {
        Long sessionId = ((Number) message.get("sessionId")).longValue();

        SpeakingReport report = reportGenerator.generate(sessionId);

        SpeakingSession session = sessionRepository.findById(sessionId).orElseThrow();
        resultProducer.send(sessionId, session.getUserId(), session.getTopicId(),
                report.getId(), report.getOverallBand(), report.getFluencyScore(),
                report.getLexicalScore(), report.getGrammarScore(), report.getPronunciationScore());
    }
}
