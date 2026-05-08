package com.taskmanager.service.ai;

import com.taskmanager.dto.TaskResponse;
import com.taskmanager.enums.Priority;
import com.taskmanager.enums.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic fallback used when no GEMINI_API_KEY is configured.
 *
 * Why this approach for a take-home project?
 *  1. Reviewers can run the app immediately without any external credentials.
 *  2. The response is meaningfully derived from the input text (keyword extraction),
 *     so it demonstrates the feature intent rather than returning a static stub.
 *  3. The real AI path (GeminiSuggestionService) and this mock share the same
 *     interface, so swapping them is purely a configuration concern.
 */
public class MockAiSuggestionService implements AiSuggestionService {

    private static final Logger log = LoggerFactory.getLogger(MockAiSuggestionService.class);

    /**
     * Matches "10 may", "3 december", "21 jan" etc.
     * Group 1 = day digits, Group 2 = month name (full or abbreviated).
     */
    private static final Pattern DAY_MONTH = Pattern.compile(
            "\\b(\\d{1,2})\\s+" +
            "(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|" +
            "jul(?:y)?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)" +
            "\\b"
    );

    /**
     * Matches "may 10", "december 3", "jan 21" etc.
     * Group 1 = month name (full or abbreviated), Group 2 = day digits.
     */
    private static final Pattern MONTH_DAY = Pattern.compile(
            "\\b(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|" +
            "jul(?:y)?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)" +
            "\\s+(\\d{1,2})\\b"
    );

    @Override
    public TaskResponse suggest(String text) {
        log.info("[MockAI] Generating suggestion for: {}", text);

        String lower = text.toLowerCase(Locale.ROOT);

        // ---- derive priority from urgency keywords ----
        Priority priority;
        if (containsAny(lower, "urgent", "asap", "critical", "immediately", "emergency")) {
            priority = Priority.HIGH;
        } else if (containsAny(lower, "soon", "today", "tomorrow", "tonight")) {
            priority = Priority.MEDIUM;
        } else {
            priority = Priority.LOW;
        }

        // ---- derive due date ----
        LocalDate dueDate = parseDueDate(lower);

        // ---- derive title: capitalise a concise version of the input ----
        String title = buildTitle(text);

        // ---- derive description ----
        String description = "Auto-suggested from: \"" + text + "\"";

        // ---- new tasks always start as TODO ----
        TaskResponse response = new TaskResponse();
        response.setTitle(title);
        response.setDescription(description);
        response.setDueDate(dueDate);
        response.setPriority(priority);
        response.setStatus(Status.TODO);
        return response;
    }

    // ---- date parsing -------------------------------------------------------

    /**
     * Attempts to extract a due date from the lowercased input text.
     * Resolution order:
     *   1. Relative keywords (today, tomorrow, named weekday, next week, end of month)
     *   2. Explicit day+month patterns ("10 may", "may 10", etc.)
     * Returns null if nothing matches.
     */
    public LocalDate parseDueDate(String lower) {
        LocalDate today = LocalDate.now();

        // ---- 1. relative keywords ----
        if (containsAny(lower, "today", "tonight")) {
            return today;
        } else if (lower.contains("tomorrow")) {
            return today.plusDays(1);
        } else if (lower.contains("monday")) {
            return nextWeekday(today, 1);
        } else if (lower.contains("tuesday")) {
            return nextWeekday(today, 2);
        } else if (lower.contains("wednesday")) {
            return nextWeekday(today, 3);
        } else if (lower.contains("thursday")) {
            return nextWeekday(today, 4);
        } else if (lower.contains("friday")) {
            return nextWeekday(today, 5);
        } else if (lower.contains("saturday")) {
            return nextWeekday(today, 6);
        } else if (lower.contains("sunday")) {
            return nextWeekday(today, 7);
        } else if (lower.contains("next week")) {
            return today.plusWeeks(1);
        } else if (lower.contains("end of month") || lower.contains("eom")) {
            return today.withDayOfMonth(today.lengthOfMonth());
        }

        // ---- 2. explicit day + month ("10 may", "may 10", etc.) ----
        // Try "DD MonthName" first
        Matcher m1 = DAY_MONTH.matcher(lower);
        if (m1.find()) {
            Month month = parseMonth(m1.group(2));
            if (month != null) {
                return resolveYear(today, month, Integer.parseInt(m1.group(1)));
            }
        }

        // Then try "MonthName DD"
        Matcher m2 = MONTH_DAY.matcher(lower);
        if (m2.find()) {
            Month month = parseMonth(m2.group(1));
            if (month != null) {
                return resolveYear(today, month, Integer.parseInt(m2.group(2)));
            }
        }

        return null;
    }

    /**
     * Builds a LocalDate from a month and day.
     * Uses the current year unless that date is already in the past,
     * in which case it rolls forward to next year.
     */
    private LocalDate resolveYear(LocalDate today, Month month, int day) {
        try {
            LocalDate candidate = LocalDate.of(today.getYear(), month, day);
            if (!candidate.isBefore(today)) {
                return candidate;
            }
            return candidate.plusYears(1);
        } catch (Exception e) {
            // Invalid day for month (e.g. "31 feb") — return null rather than crash
            log.warn("[MockAI] Could not build date for {}/{}, ignoring", day, month);
            return null;
        }
    }

    /**
     * Maps a lowercased month string (full name or 3-letter abbreviation) to a Month.
     * Returns null for unrecognised input.
     */
    private Month parseMonth(String token) {
        switch (token) {
            case "jan": case "january":   return Month.JANUARY;
            case "feb": case "february":  return Month.FEBRUARY;
            case "mar": case "march":     return Month.MARCH;
            case "apr": case "april":     return Month.APRIL;
            case "may":                   return Month.MAY;
            case "jun": case "june":      return Month.JUNE;
            case "jul": case "july":      return Month.JULY;
            case "aug": case "august":    return Month.AUGUST;
            case "sep": case "september": return Month.SEPTEMBER;
            case "oct": case "october":   return Month.OCTOBER;
            case "nov": case "november":  return Month.NOVEMBER;
            case "dec": case "december":  return Month.DECEMBER;
            default: return null;
        }
    }

    // ---- summarize ---------------------------------------------------------

    @Override
    public String summarize(TaskResponse task) {
        StringBuilder sb = new StringBuilder("This task is about ");
        sb.append(task.getTitle());
        if (task.getDueDate() != null) {
            sb.append(" by ").append(task.getDueDate());
        }
        sb.append(". It is currently ").append(task.getStatus())
          .append(" with ").append(task.getPriority()).append(" priority.");
        return sb.toString();
    }

    // ---- breakdown ---------------------------------------------------------

    /**
     * Produces a deterministic breakdown of the task into concrete subtasks.
     *
     * The subtasks are derived from:
     *  - A universal set of planning steps (research, draft, review, …)
     *  - Priority-sensitive additions (expedited review for HIGH tasks)
     *  - Status-sensitive additions (skip completed prep for DONE tasks)
     *
     * This is intentionally simple — the point is to demonstrate the feature
     * without requiring an API key.  The Gemini implementation uses a real
     * LLM prompt for genuinely useful results.
     */
    @Override
    public List<String> breakdown(TaskResponse task) {
        log.info("[MockAI] Generating breakdown for task id={}", task.getId());

        String title  = task.getTitle() != null ? task.getTitle() : "the task";
        boolean isDone = task.getStatus() == Status.DONE;
        boolean isHigh = task.getPriority() == Priority.HIGH;

        List<String> steps = new ArrayList<>();
        steps.add("Clarify the scope and acceptance criteria for \"" + title + "\"");
        steps.add("Identify any dependencies or blockers");
        steps.add("Break the work into a first draft or prototype");
        steps.add("Review the draft for completeness and correctness");

        if (isHigh) {
            steps.add("Escalate blockers immediately and keep stakeholders updated");
        } else {
            steps.add("Share progress update with relevant stakeholders");
        }

        if (!isDone) {
            steps.add("Mark task as complete and document any follow-up items");
        }

        return steps;
    }

    // ---- helpers ------------------------------------------------------------

    private boolean containsAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) return true;
        }
        return false;
    }

    /**
     * Returns the next occurrence of the given ISO day-of-week (1=Mon, 7=Sun)
     * that is strictly after today.
     */
    private LocalDate nextWeekday(LocalDate from, int targetDow) {
        int todayDow = from.getDayOfWeek().getValue();
        int daysUntil = (targetDow - todayDow + 7) % 7;
        if (daysUntil == 0) daysUntil = 7;
        return from.plusDays(daysUntil);
    }

    /**
     * Builds a short title by trimming filler phrases and capitalising.
     */
    private String buildTitle(String text) {
        String t = text.trim();
        for (String prefix : new String[]{
                "remind me to ", "remember to ", "don't forget to ",
                "i need to ", "i have to ", "make sure to ", "please "}) {
            if (t.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                t = t.substring(prefix.length());
                break;
            }
        }
        if (!t.isEmpty()) {
            t = Character.toUpperCase(t.charAt(0)) + t.substring(1);
        }
        return t.length() > 80 ? t.substring(0, 77) + "..." : t;
    }
}
