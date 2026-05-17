package com.ielts.writing.controller;

import com.ielts.common.dto.ApiResponse;
import com.ielts.writing.dto.ScoringRequestMessage;
import com.ielts.writing.entity.WritingSubmission;
import com.ielts.writing.repository.WritingSubmissionRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/writing")
public class WritingController {

    private final WritingSubmissionRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public WritingController(WritingSubmissionRepository repository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping("/submit")
    public ApiResponse<Long> submit(@RequestHeader("X-User-Id") Long userId,
                                    @RequestBody SubmitRequest request) {
        WritingSubmission submission = new WritingSubmission();
        submission.setUserId(userId);
        submission.setTopicId(request.topicId());
        submission.setTaskType(request.taskType());
        submission.setEssayText(request.essayText());
        submission.setChartType(request.chartType());
        submission.setChartDescription(request.chartDescription());
        submission = repository.save(submission);

        var message = new ScoringRequestMessage(
                1, submission.getId(), userId, request.topicId(),
                request.taskType(), request.essayText(),
                request.chartType(), request.chartDescription(), Instant.now()
        );
        kafkaTemplate.send("writing.scoring.request", String.valueOf(userId), message);

        return ApiResponse.success(submission.getId());
    }

    @GetMapping("/submissions/{id}")
    public ApiResponse<WritingSubmission> getSubmission(@PathVariable Long id) {
        return ApiResponse.success(repository.findById(id).orElseThrow());
    }

    record SubmitRequest(Long topicId, int taskType, String essayText, String chartType, String chartDescription) {}
}
