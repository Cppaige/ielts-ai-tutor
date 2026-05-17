package com.ielts.writing.dto;

import java.math.BigDecimal;

public record MasterResult(
    BigDecimal overallBand,
    BigDecimal trScore,
    BigDecimal ccScore,
    BigDecimal lrScore,
    BigDecimal graScore,
    String overallFeedback,
    String polishedEssay
) {}
