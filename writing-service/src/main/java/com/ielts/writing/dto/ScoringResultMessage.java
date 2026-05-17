package com.ielts.writing.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ScoringResultMessage(
    int version,
    Long submissionId,
    Long userId,
    Long topicId,
    int taskType,
    BigDecimal overallBand,
    BigDecimal trScore,
    BigDecimal ccScore,
    BigDecimal lrScore,
    BigDecimal graScore,
    Long serviceRecordId,
    String type,
    Instant scoredAt
) {}
