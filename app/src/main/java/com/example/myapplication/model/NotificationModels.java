package com.example.myapplication.model;

public class NotificationModels {

    public static class NotificationResponse {
        public Long id;
        public String title;
        public String content;
        public String targetType;   // COMPANY / DEPARTMENT / EMPLOYEE
        public Long departmentId;
        public String departmentName;
        public String createdByName;
        public String createdAt;
        public boolean isRead;
    }

    public static class CreateNotificationRequest {
        public String title;
        public String content;
        public String targetType;       // COMPANY / DEPARTMENT / EMPLOYEE
        public Long departmentId;       // dùng khi DEPARTMENT
        public Long targetEmployeeId;   // THÊM: dùng khi EMPLOYEE
        public Long createdById;

        public CreateNotificationRequest(String title, String content,
                                         String targetType, Long deptId,
                                         Long targetEmpId, Long createdById) {
            this.title           = title;
            this.content         = content;
            this.targetType      = targetType;
            this.departmentId    = deptId;
            this.targetEmployeeId = targetEmpId;
            this.createdById     = createdById;
        }
    }

    public static class UpdateNotificationRequest {
        public String title;
        public String content;
        public UpdateNotificationRequest(String title, String content) {
            this.title = title;
            this.content = content;
        }
    }
}