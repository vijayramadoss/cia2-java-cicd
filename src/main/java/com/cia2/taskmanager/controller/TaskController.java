package com.cia2.taskmanager.controller;

import com.cia2.taskmanager.model.Task;
import com.cia2.taskmanager.model.TaskRequest;
import com.cia2.taskmanager.model.TaskStats;
import com.cia2.taskmanager.service.TaskService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public List<Task> list() {
        return taskService.findAll();
    }

    @GetMapping("/{id}")
    public Task get(@PathVariable long id) {
        return taskService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Task create(@Valid @RequestBody TaskRequest request) {
        return taskService.create(request.title());
    }

    @PutMapping("/{id}/complete")
    public Task complete(@PathVariable long id) {
        return taskService.complete(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        taskService.delete(id);
    }

    @GetMapping("/stats")
    public TaskStats stats() {
        return taskService.stats();
    }
}
