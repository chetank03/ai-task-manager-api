package com.taskmanager.dto;

import com.taskmanager.entity.Task;
import com.taskmanager.enums.Priority;
import com.taskmanager.enums.Status;

import java.time.LocalDate;

/**
 * Outbound DTO returned from all task endpoints.
 * Keeps the JPA entity decoupled from the API contract.
 */
public class TaskResponse {

    private Long id;
    private String title;
    private String description;
    private LocalDate dueDate;
    private Priority priority;
    private Status status;

    // ---- static factory ----

    public static TaskResponse from(Task task) {
        TaskResponse dto = new TaskResponse();
        dto.id          = task.getId();
        dto.title       = task.getTitle();
        dto.description = task.getDescription();
        dto.dueDate     = task.getDueDate();
        dto.priority    = task.getPriority();
        dto.status      = task.getStatus();
        return dto;
    }

    // ---- getters & setters (needed by Jackson) ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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
