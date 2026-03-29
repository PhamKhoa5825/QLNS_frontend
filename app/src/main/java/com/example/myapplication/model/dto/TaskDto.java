package com.example.myapplication.model.dto;

/**
 * TaskDto — DTO request cho nhiệm vụ.
 * Response đã tách thành entity Task.
 */
public class TaskDto {

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
