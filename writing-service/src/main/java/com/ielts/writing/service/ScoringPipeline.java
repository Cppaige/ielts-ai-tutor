package com.ielts.writing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ielts.writing.agent.LrGraAgent;
import com.ielts.writing.agent.MasterAgent;
import com.ielts.writing.agent.TrCcAgent;
import com.ielts.writing.dto.*;
import com.ielts.writing.entity.WritingSubmission;
import com.ielts.writing.repository.WritingSubmissionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class ScoringPipeline {

    private final LrGraAgent lrGraAgent;
    private final TrCcAgent trCcAgent;
    private final MasterAgent masterAgent;
    private final WritingSubmissionRepository repository;
    private final ProgressNotifier progressNotifier;
    private final ScoringResultProducer resultProducer;
    private final ObjectMapper objectMapper;

    public ScoringPipeline(LrGraAgent lrGraAgent, TrCcAgent trCcAgent, MasterAgent masterAgent,
                           WritingSubmissionRepository repository, ProgressNotifier progressNotifier,
                           ScoringResultProducer resultProducer, ObjectMapper objectMapper) {
        this.lrGraAgent = lrGraAgent;
        this.trCcAgent = trCcAgent;
        this.masterAgent = masterAgent;
        this.repository = repository;
        this.progressNotifier = progressNotifier;
        this.resultProducer = resultProducer;
        this.objectMapper = objectMapper;
    }

    public void execute(Long submissionId, List<Float> essayEmbedding) {
        WritingSubmission submission = repository.findById(submissionId).orElseThrow();
        submission.setStatus(WritingSubmission.SubmissionStatus.SCORING);
        repository.save(submission);
        progressNotifier.notify(submissionId, "SCORING_STARTED");

        try {
            CompletableFuture<LrGraResult> lrGraFuture = CompletableFuture.supplyAsync(
                    () -> lrGraAgent.analyze(submission.getEssayText()));

            CompletableFuture<TrCcResult> trCcFuture = CompletableFuture.supplyAsync(
                    () -> trCcAgent.analyze(submission.getEssayText(), essayEmbedding, submission.getTaskType()));

            CompletableFuture.allOf(lrGraFuture, trCcFuture).join();

            LrGraResult lrGraResult = lrGraFuture.get();
            progressNotifier.notify(submissionId, "LR_GRA_DONE");

            TrCcResult trCcResult = trCcFuture.get();
            progressNotifier.notify(submissionId, "TR_CC_DONE");

            String lrGraJson = objectMapper.writeValueAsString(lrGraResult);
            String trCcJson = objectMapper.writeValueAsString(trCcResult);

            MasterResult masterResult = masterAgent.summarize(submission.getEssayText(), lrGraJson, trCcJson);

            submission.setTrScore(masterResult.trScore());
            submission.setCcScore(masterResult.ccScore());
            submission.setLrScore(masterResult.lrScore());
            submission.setGraScore(masterResult.graScore());
            submission.setOverallBand(masterResult.overallBand());
            submission.setLrGraDetail(lrGraJson);
            submission.setTrCcDetail(trCcJson);
            submission.setMasterFeedback(objectMapper.writeValueAsString(masterResult));
            submission.setStatus(WritingSubmission.SubmissionStatus.COMPLETED);
            submission.setScoredAt(LocalDateTime.now());
            repository.save(submission);

            progressNotifier.notify(submissionId, "COMPLETED");

            resultProducer.send(new ScoringResultMessage(
                    1, submission.getId(), submission.getUserId(), submission.getTopicId(),
                    submission.getTaskType(), masterResult.overallBand(),
                    masterResult.trScore(), masterResult.ccScore(),
                    masterResult.lrScore(), masterResult.graScore(),
                    submission.getId(), "WRITING", Instant.now()
            ));

        } catch (Exception e) {
            submission.setStatus(WritingSubmission.SubmissionStatus.FAILED);
            repository.save(submission);
            progressNotifier.notify(submissionId, "FAILED");
        }
    }
}
