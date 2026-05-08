package com.taskmanager.service.ai;

import com.taskmanager.dto.TaskResponse;

import java.util.List;

/**
 * Abstraction over any AI provider used to generate task suggestions.
 *
 * There are two implementations:
 *  - {@link MockAiSuggestionService}   – active when GEMINI_API_KEY is absent.
 *  - {@link GeminiSuggestionService}   – active when GEMINI_API_KEY is present.
 *
 * The active implementation is selected in {@link com.taskmanager.config.AiServiceConfig}.
 * In tests, this interface is simply mocked with Mockito.
 */
public interface AiSuggestionService {

    /**
     * Given a plain-text description of what the user wants to do,
     * return a structured task suggestion.
     *
     * @param text e.g. "remind me to submit the quarterly report before Friday"
     * @return a TaskResponse populated with suggested field values (id will be null)
     */
    TaskResponse suggest(String text);

    /**
     * Given an existing task, return a short human-readable summary sentence.
     *
     * @param task a fully populated TaskResponse
     * @return a one- or two-sentence plain-English summary
     */
    String summarize(TaskResponse task);

    /**
     * Given an existing task, return a list of concrete subtask descriptions
     * that together constitute the work needed to complete it.
     *
     * @param task a fully populated TaskResponse
     * @return an ordered list of subtask description strings (typically 3–6 items)
     */
    List<String> breakdown(TaskResponse task);
}
