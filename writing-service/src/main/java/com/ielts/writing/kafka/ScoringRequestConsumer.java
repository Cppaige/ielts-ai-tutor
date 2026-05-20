package com.ielts.writing.kafka;

import com.ielts.writing.dto.ScoringRequestMessage;
import com.ielts.writing.service.DataServiceClient;
import com.ielts.writing.service.ScoringPipeline;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ScoringRequestConsumer {

    private final ScoringPipeline scoringPipeline;
    private final DataServiceClient dataServiceClient;

    public ScoringRequestConsumer(ScoringPipeline scoringPipeline, DataServiceClient dataServiceClient) {
        this.scoringPipeline = scoringPipeline;
        this.dataServiceClient = dataServiceClient;
    }

    @KafkaListener(topics = "writing.scoring.request", groupId = "writing-service")
    public void consume(ScoringRequestMessage message) {
        List<Float> embedding = dataServiceClient.getEmbedding(message.essayText());
        scoringPipeline.execute(message.submissionId(), embedding);
    }
}
