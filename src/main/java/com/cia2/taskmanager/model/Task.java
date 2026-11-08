package com.cia2.taskmanager.model;

import java.time.Instant;

/**
 * A single to-do item.
 */
public class Task {

    private final long id;
    private final String title;
    private final Instant createdAt;
    private boolean completed;

    public Task(long id, String title) {
        this.id = id;
        this.title = title;
        this.createdAt = Instant.now();
        this.completed = false;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void markCompleted() {
        this.completed = true;
    }
}
