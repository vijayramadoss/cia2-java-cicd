package com.cia2.taskmanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cia2.taskmanager.model.Task;
import com.cia2.taskmanager.model.TaskStats;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TaskServiceTest {

    private TaskService service;

    @BeforeEach
    void setUp() {
        service = new TaskService();
    }

    @Test
    void createAddsTaskWithTrimmedTitle() {
        Task task = service.create("  Write report  ");

        assertEquals(1, task.getId());
        assertEquals("Write report", task.getTitle());
        assertFalse(task.isCompleted());
    }

    @Test
    void createRejectsBlankTitle() {
        assertThrows(IllegalArgumentException.class, () -> service.create("   "));
        assertThrows(IllegalArgumentException.class, () -> service.create(null));
    }

    @Test
    void createRejectsTooLongTitle() {
        String longTitle = "x".repeat(TaskService.MAX_TITLE_LENGTH + 1);
        assertThrows(IllegalArgumentException.class, () -> service.create(longTitle));
    }

    @Test
    void findAllReturnsTasksInCreationOrder() {
        service.create("First");
        service.create("Second");
        service.create("Third");

        List<String> titles = service.findAll().stream().map(Task::getTitle).toList();

        assertEquals(List.of("First", "Second", "Third"), titles);
    }

    @Test
    void completeMarksTaskAsDone() {
        Task task = service.create("Deploy to AKS");

        service.complete(task.getId());

        assertTrue(service.findById(task.getId()).isCompleted());
    }

    @Test
    void completeUnknownTaskThrows() {
        assertThrows(TaskNotFoundException.class, () -> service.complete(42));
    }

    @Test
    void deleteRemovesTask() {
        Task task = service.create("Temporary");

        service.delete(task.getId());

        assertTrue(service.findAll().isEmpty());
        assertThrows(TaskNotFoundException.class, () -> service.delete(task.getId()));
    }

    @Test
    void statsCountsCompletedAndPending() {
        service.create("A");
        Task b = service.create("B");
        service.create("C");
        service.complete(b.getId());

        TaskStats stats = service.stats();

        assertEquals(3, stats.total());
        assertEquals(1, stats.completed());
        assertEquals(2, stats.pending());
    }
}
