package com.cia2.taskmanager.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of a "create task" request.
 */
public record TaskRequest(
        @NotBlank(message = "Title must not be empty")
        @Size(max = 100, message = "Title must be at most 100 characters")
        String title) {
}
