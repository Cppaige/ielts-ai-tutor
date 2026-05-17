package com.ielts.speaking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ielts.speaking.entity.SessionTurn;
import com.ielts.speaking.entity.SpeakingReport;
import com.ielts.speaking.repository.SessionTurnRepository;
import com.ielts.speaking.repository.SpeakingReportRepository;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportGenerator {

    private static final String REPORT_PROMPT = """
            You are an IELTS speaking examiner. Analyze the following speaking test transcript and provide scores.

            Return a JSON object with this exact structure:
            {
              "fluencyScore": <number 0-9, step 0.5>,
              "lexicalScore": <number 0-9, step 0.5>,
              "grammarScore": <number 0-9, step 0.5>,
              "pronunciationScore": <number 0-9, step 0.5>,
              "overallBand": <number, average rounded to nearest 0.5>,
              "feedback": "<detailed feedback>"
            }

            Transcript:
            %s

            Return ONLY the JSON object.
            """;

    private final ChatModel chatModel;
    private final SessionTurnRepository turnRepository;
    private final SpeakingReportRepository reportRepository;
    private final ObjectMapper objectMapper;

    public ReportGenerator(ChatModel chatModel, SessionTurnRepository turnRepository,
                           SpeakingReportRepository reportRepository, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.turnRepository = turnRepository;
        this.reportRepository = reportRepository;
        this.objectMapper = objectMapper;
    }

    public SpeakingReport generate(Long sessionId) {
        List<SessionTurn> turns = turnRepository.findBySessionIdOrderByTurnOrder(sessionId);
        String transcript = turns.stream()
                .map(t -> t.getRole().name() + ": " + t.getContent())
                .collect(Collectors.joining("\n"));

        String prompt = String.format(REPORT_PROMPT, transcript);

        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                var response = chatModel.call(new Prompt(prompt));
                String content = response.getResult().getOutput().getContent().trim();
                if (content.startsWith("```")) {
                    content = content.replaceAll("```json?\\n?", "").replaceAll("```$", "").trim();
                }

                var node = objectMapper.readTree(content);

                SpeakingReport report = new SpeakingReport();
                report.setSessionId(sessionId);
                report.setFluencyScore(new BigDecimal(node.get("fluencyScore").asText()));
                report.setLexicalScore(new BigDecimal(node.get("lexicalScore").asText()));
                report.setGrammarScore(new BigDecimal(node.get("grammarScore").asText()));
                report.setPronunciationScore(new BigDecimal(node.get("pronunciationScore").asText()));
                report.setOverallBand(new BigDecimal(node.get("overallBand").asText()));
                report.setDetail(content);

                return reportRepository.save(report);
            } catch (Exception e) {
                if (attempt == 1) {
                    throw new RuntimeException("Report generation failed after retry", e);
                }
            }
        }
        throw new RuntimeException("Report generation failed");
    }
}
