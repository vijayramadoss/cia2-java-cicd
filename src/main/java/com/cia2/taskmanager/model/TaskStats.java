package com.cia2.taskmanager.model;

/**
 * Summary counts shown on the dashboard.
 */
public record TaskStats(int total, int completed, int pending) {
}
