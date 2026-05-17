package com.ielts.writing.dto;

import java.math.BigDecimal;
import java.util.List;

public record LrGraResult(
    BigDecimal lrScore,
    BigDecimal graScore,
    List<GrammarError> grammarErrors,
    List<String> vocabularyHighlights,
    String summary
) {
    public record GrammarError(String original, String correction, String explanation) {}
}
