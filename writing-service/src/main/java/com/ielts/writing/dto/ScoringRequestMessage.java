package com.ielts.writing.dto;

import java.time.Instant;

public record ScoringRequestMessage(
    int version,
    Long submissionId,
    Long userId,
    Long topicId,
    int taskType,
    String essayText,
    String chartType,
    String chartDescription,
    Instant requestedAt
) {}
