package com.taskmanager.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanager.dto.TaskResponse;
import com.taskmanager.enums.Priority;
import com.taskmanager.enums.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Calls the Gemini generateContent API to generate a structured task suggestion.
 *
 * The API key is read from the GEMINI_API_KEY environment variable and injected
 * via {@link com.taskmanager.config.AiServiceConfig}. It is never committed to
 * source control or logged.
 */
public class GeminiSuggestionService implements AiSuggestionService {

    private static final Logger log = LoggerFactory.getLogger(GeminiSuggestionService.class);
    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    // %1$s = today's date (ISO-8601), %2$s = user's description
    private static final String PROMPT_TEMPLATE =
            "You are a task management assistant. Today's date is %1$s.\n" +
            "Given the description below, return ONLY a valid JSON object with these fields:\n" +
            "  title       – string, max 80 chars; a clear, action-oriented task name\n" +
            "  description – one concise sentence (max 160 chars) that clarifies what\n" +
            "                'done' looks like or why the task matters, written in the\n" +
            "                third person (e.g. 'Submit the Q3 expense report to finance\n" +
            "                before the month-end deadline.'). Set to null only when the\n" +
            "                input is so vague that no meaningful sentence can be inferred.\n" +
            "  dueDate     – ISO-8601 date (YYYY-MM-DD) or null if not mentioned.\n" +
            "                IMPORTANT: dueDate must not be before %1$s unless the user\n" +
            "                explicitly asks for a past date. Resolve relative references\n" +
            "                such as 'tomorrow', 'Friday', or 'next week' relative to %1$s.\n" +
            "  priority    – LOW, MEDIUM, or HIGH\n" +
            "  status      – always TODO\n" +
            "Do not include markdown, code fences, or any explanation outside the JSON.\n\n" +
            "Description: %2$s";

    private final String apiKey;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GeminiSuggestionService(String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.create();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public TaskResponse suggest(String text) {
        log.info("[Gemini] Requesting suggestion for: {}", text);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", PROMPT_TEMPLATE.formatted(LocalDate.now(), text))
                        ))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.2,
                        "maxOutputTokens", 512,
                        "responseMimeType", "application/json"
                )
        );

        try {
            String responseJson = restClient.post()
                    .uri(GEMINI_URL)
                    .header("x-goog-api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseJson);
            String content = root
                    .path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text")
                    .asText();

            return parseTaskResponse(content);

        } catch (Exception e) {
            log.error("[Gemini] API call failed, falling back to mock: {}", e.getMessage());
            return new MockAiSuggestionService().suggest(text);
        }
    }

    @Override
    public String summarize(TaskResponse task) {
        log.info("[Gemini] Requesting summary for task id={}", task.getId());

        String prompt = String.format(
                "Summarize this task in one plain-English sentence (no JSON, no markdown):\n" +
                "Title: %s\nDescription: %s\nDue date: %s\nPriority: %s\nStatus: %s",
                task.getTitle(),
                task.getDescription() != null ? task.getDescription() : "none",
                task.getDueDate() != null ? task.getDueDate().toString() : "not set",
                task.getPriority(),
                task.getStatus());

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.2,
                        "maxOutputTokens", 128
                )
        );

        try {
            String responseJson = restClient.post()
                    .uri(GEMINI_URL)
                    .header("x-goog-api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseJson);
            return root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText().trim();

        } catch (Exception e) {
            log.error("[Gemini] Summary call failed, falling back to mock: {}", e.getMessage());
            return new MockAiSuggestionService().summarize(task);
        }
    }

    @Override
    public List<String> breakdown(TaskResponse task) {
        log.info("[Gemini] Requesting breakdown for task id={}", task.getId());

        String prompt = String.format(
                "You are a task management assistant.\n" +
                "Break the following task into 4-6 concrete, actionable subtask steps.\n" +
                "Return ONLY a JSON array of strings — no explanation, no markdown, no code fences.\n" +
                "Example format: [\"Step one\", \"Step two\", \"Step three\"]\n\n" +
                "Task title: %s\n" +
                "Description: %s\n" +
                "Priority: %s\n" +
                "Status: %s",
                task.getTitle(),
                task.getDescription() != null ? task.getDescription() : "none",
                task.getPriority(),
                task.getStatus());

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.3,
                        "maxOutputTokens", 512,
                        "responseMimeType", "application/json"
                )
        );

        try {
            String responseJson = restClient.post()
                    .uri(GEMINI_URL)
                    .header("x-goog-api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseJson);
            String content = root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

            // Strip markdown fences if present, then extract the JSON array
            String cleaned = content.trim();
            if (cleaned.contains("```")) {
                cleaned = cleaned.replaceFirst("(?s)```(?:json)?\\s*", "");
                cleaned = cleaned.replaceFirst("(?s)\\s*```.*$", "").trim();
            }
            int first = cleaned.indexOf('[');
            int last  = cleaned.lastIndexOf(']');
            if (first != -1 && last != -1 && last > first) {
                cleaned = cleaned.substring(first, last + 1);
            }

            JsonNode array = objectMapper.readTree(cleaned);
            List<String> subtasks = new ArrayList<>();
            if (array.isArray()) {
                for (JsonNode node : array) {
                    String step = node.asText("").trim();
                    if (!step.isEmpty()) subtasks.add(step);
                }
            }
            if (!subtasks.isEmpty()) return subtasks;

        } catch (Exception e) {
            log.error("[Gemini] Breakdown call failed, falling back to mock: {}", e.getMessage());
        }

        return new MockAiSuggestionService().breakdown(task);
    }

    private TaskResponse parseTaskResponse(String json) throws Exception {
        JsonNode node = objectMapper.readTree(extractJson(json));

        TaskResponse response = new TaskResponse();
        response.setTitle(node.path("title").asText("Untitled Task"));
        String desc = node.path("description").isNull() ? null : node.path("description").asText();
        response.setDescription(desc != null && !desc.isBlank() ? desc : null);

        String dueDateStr = node.path("dueDate").asText(null);
        if (dueDateStr != null && !dueDateStr.isBlank() && !dueDateStr.equals("null")) {
            try {
                response.setDueDate(LocalDate.parse(dueDateStr));
            } catch (DateTimeParseException e) {
                log.warn("[Gemini] Could not parse dueDate '{}', leaving null", dueDateStr);
            }
        }

        try {
            response.setPriority(Priority.valueOf(node.path("priority").asText("MEDIUM")));
        } catch (IllegalArgumentException e) {
            response.setPriority(Priority.MEDIUM);
        }

        try {
            response.setStatus(Status.valueOf(node.path("status").asText("TODO")));
        } catch (IllegalArgumentException e) {
            response.setStatus(Status.TODO);
        }

        return response;
    }

    /**
     * Extracts a clean JSON object string from the model's raw text output.
     *
     * Gemini may wrap the JSON in markdown fences, prepend an explanation
     * sentence, or append trailing text — even when responseMimeType is set to
     * "application/json".  This method handles all observed variants:
     *
     *   1. Strip markdown code fences (```json ... ``` or ``` ... ```) wherever
     *      they appear — not just at the start of the string.
     *   2. Find the first '{' and the last '}' and return only that substring,
     *      discarding any preamble or trailing prose.
     *
     * If no braces are found the original trimmed string is returned so the
     * caller's exception handling can surface a readable error.
     *
     * Package-private to allow direct unit testing without mocking RestClient.
     */
    String extractJson(String content) {
        String s = content.trim();

        // Step 1 – strip markdown fences anywhere in the string
        if (s.contains("```")) {
            // Remove the opening fence and optional language tag (e.g. ```json)
            s = s.replaceFirst("(?s)```(?:json)?\\s*", "");
            // Remove the closing fence and anything after it
            s = s.replaceFirst("(?s)\\s*```.*$", "");
            s = s.trim();
        }

        // Step 2 – extract between the outermost braces
        int first = s.indexOf('{');
        int last  = s.lastIndexOf('}');
        if (first != -1 && last != -1 && last > first) {
            s = s.substring(first, last + 1);
        }

        return s.trim();
    }
}
