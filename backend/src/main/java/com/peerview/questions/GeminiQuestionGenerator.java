package com.peerview.questions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peerview.catalog.InterviewSection;
import com.peerview.catalog.InterviewType;
import com.peerview.sessions.InterviewSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class GeminiQuestionGenerator {

    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final boolean enabled;

    public GeminiQuestionGenerator(
            ObjectMapper objectMapper,
            @Value("${peerview.ai.api-key:}") String apiKey,
            @Value("${peerview.ai.model:gemini-2.5-flash}") String model,
            @Value("${peerview.ai.enabled:false}") boolean enabled) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().baseUrl("https://generativelanguage.googleapis.com").build();
        this.apiKey = apiKey;
        this.model = model;
        this.enabled = enabled;
    }

    public List<GeneratedQuestion> generate(InterviewSession session) {
        if (!enabled || apiKey.isBlank()) return fallback(session);
        try {
            String sectionNames = session.getInterviewType().getSections().stream()
                    .map(InterviewSection::getName).reduce((left, right) -> left + ", " + right).orElse("");
            String prompt = "Generate a complete interview question set for a " + session.getDomain().getName()
                    + " " + session.getInterviewType().getName() + " interview. Split questions across these sections in order: "
                    + sectionNames + ". Provide 4 questions per section. For each include keyPoints. Return ONLY valid JSON in the shape {section: [{question, keyPoints}]}";
            String response = restClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/v1beta/models/{model}:generateContent").queryParam("key", apiKey).build(model))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))))
                    .retrieve().body(String.class);
            JsonNode root = objectMapper.readTree(response);
            String json = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText()
                    .replace("```json", "").replace("```", "").trim();
            JsonNode sections = objectMapper.readTree(json);
            List<GeneratedQuestion> result = new ArrayList<>();
            int order = 0;
            for (InterviewSection section : session.getInterviewType().getSections()) {
                for (JsonNode item : sections.path(section.getName())) {
                    result.add(new GeneratedQuestion(section.getName(), order++, item.path("question").asText(), item.path("keyPoints").asText()));
                }
            }
            if (!result.isEmpty()) return result;
        } catch (Exception ignored) {
            // TODO(assumption): A temporary provider failure should keep a demo session usable with fallback questions.
        }
        return fallback(session);
    }

    private List<GeneratedQuestion> fallback(InterviewSession session) {
        List<GeneratedQuestion> questions = new ArrayList<>();
        int order = 0;
        for (InterviewSection section : session.getInterviewType().getSections()) {
            questions.add(new GeneratedQuestion(section.getName(), order++, "Tell me about a recent challenge related to " + session.getDomain().getName() + ".", "Context, decision-making, result, reflection."));
            questions.add(new GeneratedQuestion(section.getName(), order++, "How would you explain your approach to a complex problem in this area?", "Clear reasoning, tradeoffs, communication, validation."));
            questions.add(new GeneratedQuestion(section.getName(), order++, "What would you improve if you had more time?", "Self-awareness, prioritization, concrete next steps."));
            questions.add(new GeneratedQuestion(section.getName(), order++, "What questions would you ask a team working in this space?", "Curiosity, relevance, thoughtful evaluation."));
        }
        return questions;
    }

    public record GeneratedQuestion(String sectionName, int orderIndex, String text, String keyPoints) {}
}