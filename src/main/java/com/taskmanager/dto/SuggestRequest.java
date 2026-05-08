package com.taskmanager.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for POST /tasks/suggest.
 * Example: { "text": "remind me to submit the quarterly report before Friday" }
 */
public class SuggestRequest {

    @NotBlank(message = "text is required")
    private String text;

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}
