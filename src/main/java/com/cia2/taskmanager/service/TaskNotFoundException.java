package com.cia2.taskmanager.service;

public class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException(long id) {
        super("Task " + id + " not found");
    }
}
