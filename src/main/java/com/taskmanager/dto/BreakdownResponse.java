package com.taskmanager.dto;

import java.util.List;

/**
 * Response body for POST /tasks/{id}/breakdown.
 *
 * Contains the original task id and an ordered list of suggested subtask
 * descriptions.  The subtasks are never persisted; the caller can choose to
 * create individual tasks from them.
 */
public class BreakdownResponse {

    private Long taskId;
    private List<String> subtasks;

    public BreakdownResponse() {}

    public BreakdownResponse(Long taskId, List<String> subtasks) {
        this.taskId   = taskId;
        this.subtasks = subtasks;
    }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public List<String> getSubtasks() { return subtasks; }
    public void setSubtasks(List<String> subtasks) { this.subtasks = subtasks; }
}
