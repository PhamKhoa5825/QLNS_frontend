package com.example.myapplication;

public class Task {
    private String title, description, assignee, deadline, status, priority;

    public Task(String title, String priority, String description, String assignee, String deadline, String status) {
        this.title = title;
        this.priority = priority;
        this.description = description;
        this.assignee = assignee;
        this.deadline = deadline;
        this.status = status;
    }

    public String getTitle() { return title; }
    public String getPriority() { return priority; }
    public String getDescription() { return description; }
    public String getAssignee() { return assignee; }
    public String getDeadline() { return deadline; }
    public String getStatus() { return status; }
}
