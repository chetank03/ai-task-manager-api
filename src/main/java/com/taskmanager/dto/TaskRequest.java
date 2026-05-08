package com.taskmanager.dto;

import com.taskmanager.enums.Priority;
import com.taskmanager.enums.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Inbound DTO for creating or updating a task.
 * The entity is never exposed directly from the controller.
 */
public class TaskRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private LocalDate dueDate;

    @NotNull(message = "Priority is required (LOW, MEDIUM, HIGH)")
    private Priority priority;

    @NotNull(message = "Status is required (TODO, IN_PROGRESS, DONE)")
    private Status status;

    // ---- getters & setters ----

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
