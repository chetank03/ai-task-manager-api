package com.taskmanager.dto;

/**
 * Response body for POST /tasks/{id}/summarize.
 */
public class SummarizeResponse {

    private Long taskId;
    private String summary;

    public SummarizeResponse() {}

    public SummarizeResponse(Long taskId, String summary) {
        this.taskId  = taskId;
        this.summary = summary;
    }

    public Long getTaskId()     { return taskId;  }
    public void setTaskId(Long taskId)      { this.taskId  = taskId;  }

    public String getSummary()  { return summary; }
    public void setSummary(String summary)  { this.summary = summary; }
}
