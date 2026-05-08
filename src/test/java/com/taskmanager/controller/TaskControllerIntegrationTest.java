package com.taskmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanager.dto.TaskRequest;
import com.taskmanager.enums.Priority;
import com.taskmanager.enums.Status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests: starts the full Spring context with an in-memory H2 database.
 * Each test class gets a fresh context (@DirtiesContext) so IDs are predictable.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class TaskControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ── helpers ──────────────────────────────────────────────────────────────

    private TaskRequest buildRequest(String title, Priority priority, Status status) {
        TaskRequest req = new TaskRequest();
        req.setTitle(title);
        req.setDescription("Test description");
        req.setDueDate(LocalDate.of(2025, 12, 31));
        req.setPriority(priority);
        req.setStatus(status);
        return req;
    }

    private String json(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    // ── POST /tasks ───────────────────────────────────────────────────────────

    @Test
    void createTask_returns201_withValidBody() throws Exception {
        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(buildRequest("Write tests", Priority.HIGH, Status.TODO))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Write tests"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    void createTask_returns400_whenTitleBlank() throws Exception {
        TaskRequest bad = buildRequest("", Priority.LOW, Status.TODO);
        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors", hasItem(containsString("title"))));
    }

    @Test
    void createTask_returns400_whenPriorityMissing() throws Exception {
        String body = "{\"title\":\"No priority\",\"status\":\"TODO\"}";
        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ── GET /tasks ────────────────────────────────────────────────────────────

    @Test
    void getAllTasks_returnsEmptyList_whenNoTasks() throws Exception {
        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getAllTasks_returnsList_afterCreating() throws Exception {
        mockMvc.perform(post("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(buildRequest("Task A", Priority.LOW, Status.TODO))));

        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Task A"));
    }

    // ── GET /tasks/{id} ───────────────────────────────────────────────────────

    @Test
    void getTaskById_returnsTask_whenExists() throws Exception {
        mockMvc.perform(post("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(buildRequest("Find me", Priority.MEDIUM, Status.IN_PROGRESS))));

        mockMvc.perform(get("/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Find me"));
    }

    @Test
    void getTaskById_returns404_whenNotFound() throws Exception {
        mockMvc.perform(get("/tasks/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ── PUT /tasks/{id} ───────────────────────────────────────────────────────

    @Test
    void updateTask_returns200_withUpdatedFields() throws Exception {
        mockMvc.perform(post("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(buildRequest("Original", Priority.LOW, Status.TODO))));

        TaskRequest updated = buildRequest("Updated", Priority.HIGH, Status.DONE);
        mockMvc.perform(put("/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    void updateTask_returns404_whenNotFound() throws Exception {
        mockMvc.perform(put("/tasks/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(buildRequest("Ghost", Priority.LOW, Status.TODO))))
                .andExpect(status().isNotFound());
    }

    // ── DELETE /tasks/{id} ────────────────────────────────────────────────────

    @Test
    void deleteTask_returns204_whenExists() throws Exception {
        mockMvc.perform(post("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(buildRequest("Delete me", Priority.LOW, Status.TODO))));

        mockMvc.perform(delete("/tasks/1"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/tasks/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteTask_returns404_whenNotFound() throws Exception {
        mockMvc.perform(delete("/tasks/999"))
                .andExpect(status().isNotFound());
    }

    // ── POST /tasks/{id}/breakdown ────────────────────────────────────────────

    @Test
    void breakdownTask_returns200_withTaskIdAndSubtasks() throws Exception {
        mockMvc.perform(post("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(buildRequest("Launch new feature", Priority.MEDIUM, Status.TODO))));

        mockMvc.perform(post("/tasks/1/breakdown"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(1))
                .andExpect(jsonPath("$.subtasks").isArray())
                .andExpect(jsonPath("$.subtasks", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.subtasks[0]").isString())
                .andExpect(jsonPath("$.subtasks[0]").isNotEmpty());
    }

    @Test
    void breakdownTask_returns404_whenTaskNotFound() throws Exception {
        mockMvc.perform(post("/tasks/999/breakdown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ── POST /tasks/{id}/summarize ────────────────────────────────────────────

    @Test
    void summarizeTask_returns200_withTaskIdAndSummary() throws Exception {
        // Create a task first
        mockMvc.perform(post("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(buildRequest("Submit the English project", Priority.MEDIUM, Status.TODO))));

        mockMvc.perform(post("/tasks/1/summarize"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(1))
                .andExpect(jsonPath("$.summary").isString())
                .andExpect(jsonPath("$.summary").isNotEmpty())
                // Mock always produces a sentence containing title, status, priority
                .andExpect(jsonPath("$.summary", containsString("Submit the English project")))
                .andExpect(jsonPath("$.summary", containsString("TODO")))
                .andExpect(jsonPath("$.summary", containsString("MEDIUM")));
    }

    @Test
    void summarizeTask_returns404_whenTaskNotFound() throws Exception {
        mockMvc.perform(post("/tasks/999/summarize"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
