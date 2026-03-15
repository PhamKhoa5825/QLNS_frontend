package com.example.myapplication.model;

public class RequestModels {

    public static class RequestResponse {
        public Long id;
        public Long employeeId;
        public String employeeName;
        public String title;
        public String description;
        public String fileUrl;
        public String fileName;
        public String status;           // PENDING / APPROVED / REJECTED
        public Long reviewedBy;
        public String reviewerName;
        public String rejectionReason;
        public String createdAt;
        public String updatedAt;
    }

    public static class CreateRequestBody {
        public String title;
        public String description;
        public CreateRequestBody(String title, String description) {
            this.title       = title;
            this.description = description;
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