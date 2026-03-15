package com.example.myapplication.model;

public class NotificationModels {

    public static class NotificationResponse {
        public Long id;
        public String title;
        public String content;
        public String targetType;   // COMPANY / DEPARTMENT
        public Long departmentId;
        public String createdByName;
        public String createdAt;
        public boolean isRead;
    }

    public static class CreateNotificationRequest {
        public String title;
        public String content;
        public String targetType;   // COMPANY / DEPARTMENT
        public Long departmentId;   // null nếu targetType = COMPANY
        public Long createdById;

        public CreateNotificationRequest(String title, String content,
                                         String targetType, Long deptId, Long userId) {
            this.title        = title;
            this.content      = content;
            this.targetType   = targetType;
            this.departmentId = deptId;
            this.createdById  = userId;
        }
    }
}