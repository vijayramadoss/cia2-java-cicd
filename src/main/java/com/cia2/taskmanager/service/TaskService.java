package com.cia2.taskmanager.service;

import com.cia2.taskmanager.model.Task;
import com.cia2.taskmanager.model.TaskStats;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

/**
 * Keeps tasks in memory. Each pod has its own list, which keeps the
 * application simple (no database) for the CI/CD demonstration.
 */
@Service
public class TaskService {

    static final int MAX_TITLE_LENGTH = 100;

    private final Map<Long, Task> tasks = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public List<Task> findAll() {
        return tasks.values().stream()
                .sorted(Comparator.comparingLong(Task::getId))
                .toList();
    }

    public Task findById(long id) {
        Task task = tasks.get(id);
        if (task == null) {
            throw new TaskNotFoundException(id);
        }
        return task;
    }

    public Task create(String title) {
        String cleaned = title == null ? "" : title.trim();
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("Title must not be empty");
        }
        if (cleaned.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("Title must be at most " + MAX_TITLE_LENGTH + " characters");
        }
        Task task = new Task(nextId.getAndIncrement(), cleaned);
        tasks.put(task.getId(), task);
        return task;
    }

    public Task complete(long id) {
        Task task = findById(id);
        task.markCompleted();
        return task;
    }

    public void delete(long id) {
        if (tasks.remove(id) == null) {
            throw new TaskNotFoundException(id);
        }
    }

    public TaskStats stats() {
        int total = tasks.size();
        int completed = (int) tasks.values().stream().filter(Task::isCompleted).count();
        return new TaskStats(total, completed, total - completed);
    }
}
