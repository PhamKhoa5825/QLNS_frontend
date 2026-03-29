package com.example.myapplication.model.dto;

/**
 * NotificationDto — DTO request cho thông báo.
 * Response đã tách thành entity Notification.
 */
public class NotificationDto {

    public static class CreateNotificationRequest {
        public String title;
        public String content;
        public String targetType;       // COMPANY / DEPARTMENT / EMPLOYEE
        public Long departmentId;
        public Long targetEmployeeId;
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
