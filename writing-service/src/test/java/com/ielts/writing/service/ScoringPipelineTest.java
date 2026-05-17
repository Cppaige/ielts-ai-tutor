package com.ielts.writing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ielts.writing.agent.LrGraAgent;
import com.ielts.writing.agent.MasterAgent;
import com.ielts.writing.agent.TrCcAgent;
import com.ielts.writing.dto.*;
import com.ielts.writing.entity.WritingSubmission;
import com.ielts.writing.repository.WritingSubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScoringPipelineTest {

    @Mock private LrGraAgent lrGraAgent;
    @Mock private TrCcAgent trCcAgent;
    @Mock private MasterAgent masterAgent;
    @Mock private WritingSubmissionRepository repository;
    @Mock private ProgressNotifier progressNotifier;
    @Mock private ScoringResultProducer resultProducer;

    private ScoringPipeline pipeline;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        pipeline = new ScoringPipeline(lrGraAgent, trCcAgent, masterAgent, repository, progressNotifier, resultProducer, objectMapper);
    }

    @Test
    void execute_success_updatesDbAndPublishes() {
        WritingSubmission submission = new WritingSubmission();
        submission.setId(1L);
        submission.setUserId(100L);
        submission.setTopicId(42L);
        submission.setTaskType(2);
        submission.setEssayText("Some essay");
        submission.setStatus(WritingSubmission.SubmissionStatus.PENDING);

        when(repository.findById(1L)).thenReturn(Optional.of(submission));

        var lrGraResult = new LrGraResult(new BigDecimal("7.0"), new BigDecimal("6.5"), List.of(), List.of("good"), "summary");
        var trCcResult = new TrCcResult(new BigDecimal("7.0"), new BigDecimal("7.0"), "good structure", List.of(), "summary");
        var masterResult = new MasterResult(new BigDecimal("7.0"), new BigDecimal("7.0"), new BigDecimal("7.0"), new BigDecimal("7.0"), new BigDecimal("6.5"), "feedback", "polished");

        when(lrGraAgent.analyze(anyString())).thenReturn(lrGraResult);
        when(trCcAgent.analyze(anyString(), any(), anyInt(), any())).thenReturn(trCcResult);
        when(masterAgent.summarize(anyString(), anyString(), anyString())).thenReturn(masterResult);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        pipeline.execute(1L, List.of(0.1f, 0.2f), "education");

        verify(progressNotifier).notify(1L, "SCORING_STARTED");
        verify(progressNotifier).notify(1L, "LR_GRA_DONE");
        verify(progressNotifier).notify(1L, "TR_CC_DONE");
        verify(progressNotifier).notify(1L, "COMPLETED");
        verify(repository, times(2)).save(any());
        verify(resultProducer).send(any(ScoringResultMessage.class));
    }

    @Test
    void execute_agentFails_marksFailed() {
        WritingSubmission submission = new WritingSubmission();
        submission.setId(2L);
        submission.setEssayText("essay");
        submission.setTaskType(2);

        when(repository.findById(2L)).thenReturn(Optional.of(submission));
        when(lrGraAgent.analyze(anyString())).thenThrow(new RuntimeException("LLM error"));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        pipeline.execute(2L, List.of(0.1f), "education");

        verify(progressNotifier).notify(2L, "FAILED");
        assertEquals(WritingSubmission.SubmissionStatus.FAILED, submission.getStatus());
    }
}
