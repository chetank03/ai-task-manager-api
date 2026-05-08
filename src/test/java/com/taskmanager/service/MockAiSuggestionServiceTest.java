package com.taskmanager.service;

import com.taskmanager.dto.TaskResponse;
import com.taskmanager.service.ai.MockAiSuggestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Focused unit tests for MockAiSuggestionService date parsing.
 * No Spring context — pure Java.
 */
class MockAiSuggestionServiceTest {

    private MockAiSuggestionService service;

    @BeforeEach
    void setUp() {
        service = new MockAiSuggestionService();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /**
     * Returns the expected date for a given month+day using the same
     * year-rolling logic as the service: current year unless already past,
     * otherwise next year.
     */
    private LocalDate expected(Month month, int day) {
        LocalDate today = LocalDate.now();
        LocalDate candidate = LocalDate.of(today.getYear(), month, day);
        return candidate.isBefore(today) ? candidate.plusYears(1) : candidate;
    }

    // ── explicit date formats — DD MonthName ──────────────────────────────────

    @Test
    void parseDueDate_dayThenFullMonth_lowercase() {
        LocalDate result = service.parseDueDate("do quarterly report due on 10 may");
        assertThat(result).isEqualTo(expected(Month.MAY, 10));
    }

    @Test
    void parseDueDate_dayThenFullMonth_mixedCase_handledViaLower() {
        // Input is lowercased by parseDueDate callers; direct call uses lower already
        LocalDate result = service.parseDueDate("due on 10 may");
        assertThat(result).isEqualTo(expected(Month.MAY, 10));
    }

    @Test
    void parseDueDate_dayThenFullMonth_beforeKeyword() {
        LocalDate result = service.parseDueDate("before 10 may");
        assertThat(result).isEqualTo(expected(Month.MAY, 10));
    }

    @Test
    void parseDueDate_dayThenFullMonth_singleDigitDay() {
        LocalDate result = service.parseDueDate("submit by 5 june");
        assertThat(result).isEqualTo(expected(Month.JUNE, 5));
    }

    // ── explicit date formats — MonthName DD ──────────────────────────────────

    @Test
    void parseDueDate_monthThenDay_lowercase() {
        LocalDate result = service.parseDueDate("report due may 10");
        assertThat(result).isEqualTo(expected(Month.MAY, 10));
    }

    @Test
    void parseDueDate_monthThenDay_singleDigitDay() {
        LocalDate result = service.parseDueDate("finish by june 5");
        assertThat(result).isEqualTo(expected(Month.JUNE, 5));
    }

    // ── abbreviated month names ───────────────────────────────────────────────

    @Test
    void parseDueDate_abbreviatedMonth_dayFirst() {
        LocalDate result = service.parseDueDate("meeting on 15 jan");
        assertThat(result).isEqualTo(expected(Month.JANUARY, 15));
    }

    @Test
    void parseDueDate_abbreviatedMonth_monthFirst() {
        LocalDate result = service.parseDueDate("dec 25 celebration");
        assertThat(result).isEqualTo(expected(Month.DECEMBER, 25));
    }

    // ── full sentence matching the reported bug ───────────────────────────────

    @Test
    void parseDueDate_fullSentence_reportedBugCase() {
        // This is the exact input that previously returned null
        LocalDate result = service.parseDueDate(
                "remind me to do quarterly report which is due on 10 may");
        assertThat(result)
                .as("Should extract 10 May from the full sentence")
                .isEqualTo(expected(Month.MAY, 10));
    }

    // ── year-rolling logic ────────────────────────────────────────────────────

    @Test
    void parseDueDate_pastDate_rollsToNextYear() {
        // January 1 is always in the past by the time most tests run (after Jan 1)
        // Build "1 jan" and verify year logic
        LocalDate today = LocalDate.now();
        LocalDate result = service.parseDueDate("deadline 1 jan");
        assertThat(result).isNotNull();
        assertThat(result.getMonth()).isEqualTo(Month.JANUARY);
        assertThat(result.getDayOfMonth()).isEqualTo(1);
        // Must not be before today
        assertThat(result).isAfterOrEqualTo(today);
    }

    @Test
    void parseDueDate_futureDate_useCurrentYear() {
        // December 31 is always in the future unless today IS Dec 31
        LocalDate today = LocalDate.now();
        LocalDate result = service.parseDueDate("finish by 31 december");
        assertThat(result).isNotNull();
        assertThat(result.getMonth()).isEqualTo(Month.DECEMBER);
        assertThat(result.getDayOfMonth()).isEqualTo(31);
        assertThat(result).isAfterOrEqualTo(today);
    }

    // ── relative keywords still work (regression guard) ──────────────────────

    @Test
    void parseDueDate_today_stillWorks() {
        assertThat(service.parseDueDate("do this today")).isEqualTo(LocalDate.now());
    }

    @Test
    void parseDueDate_tomorrow_stillWorks() {
        assertThat(service.parseDueDate("finish tomorrow")).isEqualTo(LocalDate.now().plusDays(1));
    }

    @Test
    void parseDueDate_nextWeek_stillWorks() {
        assertThat(service.parseDueDate("review next week")).isEqualTo(LocalDate.now().plusWeeks(1));
    }

    @Test
    void parseDueDate_friday_stillWorks() {
        LocalDate result = service.parseDueDate("submit by friday");
        assertThat(result).isNotNull();
        assertThat(result.getDayOfWeek().getValue()).isEqualTo(5); // Friday = 5
        assertThat(result).isAfter(LocalDate.now());
    }

    // ── no date → null ────────────────────────────────────────────────────────

    @Test
    void parseDueDate_noDateMentioned_returnsNull() {
        assertThat(service.parseDueDate("just do some work")).isNull();
    }

    // ── full suggest() method (smoke test) ───────────────────────────────────

    @Test
    void suggest_fullSentence_returnsCorrectDueDate() {
        TaskResponse response = service.suggest("remind me to do quarterly report which is due on 10 may");
        assertThat(response.getDueDate())
                .as("Full suggest() call should propagate the parsed date")
                .isEqualTo(expected(Month.MAY, 10));
        assertThat(response.getTitle()).isNotBlank();
        assertThat(response.getStatus()).isNotNull();
        assertThat(response.getPriority()).isNotNull();
    }

    // ── summarize() ───────────────────────────────────────────────────────────

    @Test
    void summarize_withDueDate_containsKeyFields() {
        TaskResponse task = new TaskResponse();
        task.setId(1L);
        task.setTitle("Submit the English project");
        task.setDueDate(LocalDate.of(2026, 5, 8));
        task.setPriority(com.taskmanager.enums.Priority.MEDIUM);
        task.setStatus(com.taskmanager.enums.Status.TODO);

        String summary = service.summarize(task);

        assertThat(summary).contains("Submit the English project");
        assertThat(summary).contains("2026-05-08");
        assertThat(summary).contains("TODO");
        assertThat(summary).contains("MEDIUM");
    }

    @Test
    void summarize_withoutDueDate_omitsDateClause() {
        TaskResponse task = new TaskResponse();
        task.setId(2L);
        task.setTitle("Read the docs");
        task.setDueDate(null);
        task.setPriority(com.taskmanager.enums.Priority.LOW);
        task.setStatus(com.taskmanager.enums.Status.IN_PROGRESS);

        String summary = service.summarize(task);

        assertThat(summary).contains("Read the docs");
        assertThat(summary).contains("IN_PROGRESS");
        assertThat(summary).contains("LOW");
        // no date phrase expected
        assertThat(summary).doesNotContain("by null");
        assertThat(summary).doesNotContain("by 20");
    }

    @Test
    void summarize_matchesExpectedFormat() {
        TaskResponse task = new TaskResponse();
        task.setId(3L);
        task.setTitle("Deploy to production");
        task.setDueDate(LocalDate.of(2026, 6, 1));
        task.setPriority(com.taskmanager.enums.Priority.HIGH);
        task.setStatus(com.taskmanager.enums.Status.TODO);

        String summary = service.summarize(task);

        assertThat(summary).isEqualTo(
                "This task is about Deploy to production by 2026-06-01. " +
                "It is currently TODO with HIGH priority.");
    }

    // ── breakdown() ──────────────────────────────────────────────────────────

    @Test
    void breakdown_returnsNonEmptyList() {
        TaskResponse task = new TaskResponse();
        task.setId(1L);
        task.setTitle("Launch new feature");
        task.setPriority(com.taskmanager.enums.Priority.MEDIUM);
        task.setStatus(com.taskmanager.enums.Status.TODO);

        List<String> subtasks = service.breakdown(task);

        assertThat(subtasks).isNotNull().isNotEmpty();
        // Every subtask should be a non-blank string
        assertThat(subtasks).allSatisfy(s -> assertThat(s).isNotBlank());
    }

    @Test
    void breakdown_highPriority_includesEscalationStep() {
        TaskResponse task = new TaskResponse();
        task.setId(2L);
        task.setTitle("Fix production outage");
        task.setPriority(com.taskmanager.enums.Priority.HIGH);
        task.setStatus(com.taskmanager.enums.Status.IN_PROGRESS);

        List<String> subtasks = service.breakdown(task);

        boolean hasEscalation = subtasks.stream()
                .anyMatch(s -> s.toLowerCase().contains("escalat") ||
                               s.toLowerCase().contains("stakeholder"));
        assertThat(hasEscalation)
                .as("HIGH priority task should include an escalation/stakeholder step")
                .isTrue();
    }

    @Test
    void breakdown_doneTask_omitsCompletionStep() {
        TaskResponse task = new TaskResponse();
        task.setId(3L);
        task.setTitle("Archive old reports");
        task.setPriority(com.taskmanager.enums.Priority.LOW);
        task.setStatus(com.taskmanager.enums.Status.DONE);

        List<String> subtasks = service.breakdown(task);

        // Should not contain a "mark complete" step since it's already done
        boolean hasCompletion = subtasks.stream()
                .anyMatch(s -> s.toLowerCase().contains("mark task as complete"));
        assertThat(hasCompletion)
                .as("DONE task should not include a 'mark complete' step")
                .isFalse();
    }

    @Test
    void breakdown_titleAppearsInFirstStep() {
        TaskResponse task = new TaskResponse();
        task.setId(4L);
        task.setTitle("Write the design doc");
        task.setPriority(com.taskmanager.enums.Priority.MEDIUM);
        task.setStatus(com.taskmanager.enums.Status.TODO);

        List<String> subtasks = service.breakdown(task);

        assertThat(subtasks.get(0)).contains("Write the design doc");
    }
}
