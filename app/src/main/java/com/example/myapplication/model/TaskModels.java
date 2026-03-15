package com.example.myapplication.model;

public class TaskModels {

    public static class TaskResponse {
        public Long id;
        public String title;
        public String description;
        public String priority;     // LOW / MEDIUM / HIGH
        public String status;       // PENDING / ACCEPTED / DONE / OVERDUE
        public String deadline;
        public String assignedToName;
        public Long assignedToId;
        public Long assignedById;
        public String assignedByName;
    }

    public static class CreateTaskRequest {
        public String title;
        public String description;
        public String priority;
        public String deadline;
        public Long assignedToId;
        public Long assignedById;
    }

    public static class UpdateTaskStatusRequest {
        public String status;
        public String note;
        public UpdateTaskStatusRequest(String status, String note) {
            this.status = status;
            this.note   = note;
        }
    }

    public static class AcceptTaskRequest {
        public Long employeeId;
        public AcceptTaskRequest(Long employeeId) {
            this.employeeId = employeeId;
        }
    }
}