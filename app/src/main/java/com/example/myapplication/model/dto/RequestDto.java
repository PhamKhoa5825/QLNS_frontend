package com.example.myapplication.model.dto;

/**
 * RequestDto — DTO request cho đơn từ.
 * Response đã tách thành entity Request.
 */
public class RequestDto {

    public static class CreateRequestBody {
        public String title;
        public String description;
        public String targetRole;

        public CreateRequestBody(String title, String description, String targetRole) {
            this.title       = title;
            this.description = description;
            this.targetRole  = targetRole;
        }

        public CreateRequestBody(String title, String description) {
            this(title, description, "MANAGER");
        }
    }

    public static class ReviewRequestBody {
        public boolean approved;
        public String rejectionReason;
        public ReviewRequestBody(boolean approved, String rejectionReason) {
            this.approved        = approved;
            this.rejectionReason = rejectionReason;
        }
    }
}
