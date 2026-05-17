package com.ielts.data.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PracticeRecordMessage(
    int version,
    Long userId,
    Long topicId,
    Long serviceRecordId,
    String type,
    BigDecimal overallBand,
    Instant completedAt
) {}
