package com.taskmanager.service;

import com.taskmanager.dto.TaskRequest;
import com.taskmanager.dto.TaskResponse;
import com.taskmanager.entity.Task;
import com.taskmanager.enums.Priority;
import com.taskmanager.enums.Status;
import com.taskmanager.exception.TaskNotFoundException;
import com.taskmanager.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TaskService.
 * The repository is mocked — no Spring context is started.
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    private Task sampleTask;
    private TaskRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleTask = new Task("Buy groceries", "Milk and eggs", LocalDate.of(2025, 6, 1), Priority.LOW, Status.TODO);
        sampleTask.setId(1L);

        sampleRequest = new TaskRequest();
        sampleRequest.setTitle("Buy groceries");
        sampleRequest.setDescription("Milk and eggs");
        sampleRequest.setDueDate(LocalDate.of(2025, 6, 1));
        sampleRequest.setPriority(Priority.LOW);
        sampleRequest.setStatus(Status.TODO);
    }

    // ---- create ----

    @Test
    void create_savesTaskAndReturnsResponse() {
        when(taskRepository.save(any(Task.class))).thenReturn(sampleTask);

        TaskResponse response = taskService.create(sampleRequest);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Buy groceries");
        assertThat(response.getPriority()).isEqualTo(Priority.LOW);
        assertThat(response.getStatus()).isEqualTo(Status.TODO);
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    // ---- findAll ----

    @Test
    void findAll_returnsAllTasksAsDtos() {
        when(taskRepository.findAll()).thenReturn(List.of(sampleTask));

        List<TaskResponse> result = taskService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Buy groceries");
    }

    // ---- findById ----

    @Test
    void findById_returnsTask_whenFound() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));

        TaskResponse response = taskService.findById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Buy groceries");
    }

    @Test
    void findById_throwsTaskNotFoundException_whenNotFound() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.findById(99L))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ---- update ----

    @Test
    void update_modifiesAndReturnsTask_whenFound() {
        TaskRequest updateRequest = new TaskRequest();
        updateRequest.setTitle("Updated title");
        updateRequest.setDescription("New description");
        updateRequest.setDueDate(LocalDate.of(2025, 12, 31));
        updateRequest.setPriority(Priority.HIGH);
        updateRequest.setStatus(Status.IN_PROGRESS);

        Task updatedTask = new Task("Updated title", "New description",
                LocalDate.of(2025, 12, 31), Priority.HIGH, Status.IN_PROGRESS);
        updatedTask.setId(1L);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));
        when(taskRepository.save(any(Task.class))).thenReturn(updatedTask);

        TaskResponse response = taskService.update(1L, updateRequest);

        assertThat(response.getTitle()).isEqualTo("Updated title");
        assertThat(response.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(response.getStatus()).isEqualTo(Status.IN_PROGRESS);
    }

    @Test
    void update_throwsTaskNotFoundException_whenNotFound() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.update(99L, sampleRequest))
                .isInstanceOf(TaskNotFoundException.class);
    }

    // ---- delete ----

    @Test
    void delete_removesTask_whenFound() {
        when(taskRepository.existsById(1L)).thenReturn(true);

        taskService.delete(1L);

        verify(taskRepository, times(1)).deleteById(1L);
    }

    @Test
    void delete_throwsTaskNotFoundException_whenNotFound() {
        when(taskRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> taskService.delete(99L))
                .isInstanceOf(TaskNotFoundException.class);
        verify(taskRepository, never()).deleteById(any());
    }
}
