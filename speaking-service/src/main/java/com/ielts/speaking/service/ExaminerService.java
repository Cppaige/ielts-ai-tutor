package com.ielts.speaking.service;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExaminerService {

    private static final String ENCOURAGING_PERSONA = """
            You are a friendly and encouraging IELTS speaking examiner. You make candidates feel comfortable,
            give positive acknowledgments, and ask follow-up questions naturally. Speak in a warm, conversational tone.
            """;

    private static final String STRICT_PERSONA = """
            You are a professional and formal IELTS speaking examiner. You maintain a neutral tone,
            ask questions directly, and move through the test efficiently without excessive encouragement.
            """;

    private static final String EXAMINER_INSTRUCTIONS = """
            You are conducting an IELTS speaking test Part %d.
            Current question: %s

            Based on the candidate's response, provide a brief natural acknowledgment and then ask the next question.
            If this is the last question in the set, just provide a brief transition statement.

            Keep your response concise (1-3 sentences). Speak naturally as an examiner would.
            Only output your spoken response, no labels or formatting.
            """;

    private final ChatModel chatModel;

    public ExaminerService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String generateResponse(String persona, int part, String currentQuestion, List<DialogTurn> history) {
        String personaPrompt = "ENCOURAGING".equals(persona) ? ENCOURAGING_PERSONA : STRICT_PERSONA;
        String instructions = String.format(EXAMINER_INSTRUCTIONS, part, currentQuestion);

        String historyText = history.stream()
                .map(turn -> turn.role() + ": " + turn.content())
                .collect(Collectors.joining("\n"));

        String fullPrompt = personaPrompt + "\n" + instructions + "\n\nConversation so far:\n" + historyText;

        var response = chatModel.call(new Prompt(fullPrompt));
        return response.getResult().getOutput().getContent();
    }

    public record DialogTurn(String role, String content) {}
}
