package com.taskmanager.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanager.dto.TaskResponse;
import com.taskmanager.enums.Priority;
import com.taskmanager.enums.Status;
import com.taskmanager.service.ai.AiSuggestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for POST /tasks/suggest with the external AI call mocked out.
 *
 * @MockBean replaces the AiSuggestionService bean in the Spring context with a
 * Mockito mock, so no real AI provider is ever contacted during tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AiSuggestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiSuggestionService aiSuggestionService;

    // ── happy path ────────────────────────────────────────────────────────────

    @Test
    void suggest_returns200_withStructuredResponse() throws Exception {
        TaskResponse mockResponse = new TaskResponse();
        mockResponse.setTitle("Submit quarterly report");
        mockResponse.setDescription("File the Q3 report to management before the Friday deadline.");
        mockResponse.setDueDate(LocalDate.of(2025, 1, 10));
        mockResponse.setPriority(Priority.HIGH);
        mockResponse.setStatus(Status.TODO);

        when(aiSuggestionService.suggest(anyString())).thenReturn(mockResponse);

        String requestBody = objectMapper.writeValueAsString(
                java.util.Map.of("text", "remind me to submit the quarterly report before Friday")
        );

        mockMvc.perform(post("/tasks/suggest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Submit quarterly report"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.dueDate").value("2025-01-10"));
    }

    // ── validation ────────────────────────────────────────────────────────────

    @Test
    void suggest_returns400_whenTextBlank() throws Exception {
        mockMvc.perform(post("/tasks/suggest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void suggest_returns400_whenBodyMissing() throws Exception {
        mockMvc.perform(post("/tasks/suggest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
