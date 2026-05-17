package com.ielts.writing.dto;

import java.math.BigDecimal;
import java.util.List;

public record TrCcResult(
    BigDecimal trScore,
    BigDecimal ccScore,
    String structureAnalysis,
    List<String> improvements,
    String summary
) {}
