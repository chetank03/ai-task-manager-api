package com.taskmanager.service.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for GeminiSuggestionService.extractJson().
 *
 * No Spring context, no RestClient, no real API call.
 * The service is constructed with a dummy key — only the parser method is exercised.
 */
class GeminiSuggestionServiceParserTest {

    private GeminiSuggestionService service;

    private static final String CLEAN_JSON =
            "{\"title\":\"Submit report\",\"description\":null," +
            "\"dueDate\":\"2025-05-10\",\"priority\":\"HIGH\",\"status\":\"TODO\"}";

    @BeforeEach
    void setUp() {
        // Dummy key — RestClient is never called in these tests
        service = new GeminiSuggestionService("dummy-key-for-parser-tests");
    }

    // ── already-clean JSON ────────────────────────────────────────────────────

    @Test
    void extractJson_cleanJson_returnedUnchanged() {
        assertThat(service.extractJson(CLEAN_JSON)).isEqualTo(CLEAN_JSON);
    }

    @Test
    void extractJson_cleanJsonWithWhitespace_trimmed() {
        assertThat(service.extractJson("  " + CLEAN_JSON + "  ")).isEqualTo(CLEAN_JSON);
    }

    // ── markdown fences ───────────────────────────────────────────────────────

    @Test
    void extractJson_jsonFence_stripsBackticks() {
        String input = "```json\n" + CLEAN_JSON + "\n```";
        assertThat(service.extractJson(input)).isEqualTo(CLEAN_JSON);
    }

    @Test
    void extractJson_plainFence_stripsBackticks() {
        String input = "```\n" + CLEAN_JSON + "\n```";
        assertThat(service.extractJson(input)).isEqualTo(CLEAN_JSON);
    }

    // ── preamble and trailing prose ───────────────────────────────────────────

    @Test
    void extractJson_preambleBeforeJson_stripped() {
        String input = "Here is the task suggestion:\n" + CLEAN_JSON;
        assertThat(service.extractJson(input)).isEqualTo(CLEAN_JSON);
    }

    @Test
    void extractJson_trailingTextAfterJson_stripped() {
        String input = CLEAN_JSON + "\nLet me know if you need changes.";
        assertThat(service.extractJson(input)).isEqualTo(CLEAN_JSON);
    }

    @Test
    void extractJson_preambleAndTrailingText_bothStripped() {
        String input = "Sure! Here is the JSON:\n" + CLEAN_JSON + "\nHope that helps!";
        assertThat(service.extractJson(input)).isEqualTo(CLEAN_JSON);
    }

    // ── preamble + markdown fence ─────────────────────────────────────────────

    @Test
    void extractJson_preamblePlusFencedJson_fullyStripped() {
        // Closest match to the observed failure: text before the fence
        String input = "Here is the suggested task:\n```json\n" + CLEAN_JSON + "\n```";
        assertThat(service.extractJson(input)).isEqualTo(CLEAN_JSON);
    }

    // ── no braces — graceful degradation ─────────────────────────────────────

    @Test
    void extractJson_noBraces_returnsOriginalTrimmed() {
        // Ensures the caller's exception path gets a readable string, not null
        String input = "  Sorry, I cannot help with that.  ";
        assertThat(service.extractJson(input)).isEqualTo("Sorry, I cannot help with that.");
    }

    // ── the exact failure scenario from the bug report ────────────────────────

    @Test
    void extractJson_reportedBugScenario_extractsValidJson() {
        // Simulates Gemini responding with explanation text wrapping the JSON
        String input =
                "I've analyzed your request and here is the structured task:\n" +
                "```json\n" +
                "{\"title\":\"Project due tomorrow\",\"description\":\"Complete the project deliverable\"," +
                "\"dueDate\":\"2025-05-08\",\"priority\":\"HIGH\",\"status\":\"TODO\"}\n" +
                "```\n" +
                "This task has been set to HIGH priority since it's due tomorrow.";

        String result = service.extractJson(input);

        assertThat(result).startsWith("{");
        assertThat(result).endsWith("}");
        assertThat(result).contains("\"title\":\"Project due tomorrow\"");
        assertThat(result).doesNotContain("```");
        assertThat(result).doesNotContain("I've analyzed");
        assertThat(result).doesNotContain("HIGH priority since");
    }
}
