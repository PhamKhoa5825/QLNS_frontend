package com.example.myapplication.model;

public class RequestModels {

    public static class RequestResponse {
        public Long id;
        public Long employeeId;
        public String employeeName;
        public String departmentName;       // THÊM: khớp backend RequestDTO
        public String title;
        public String description;
        public String fileUrl;
        public String fileName;
        public String status;               // PENDING / APPROVED / REJECTED
        public Long reviewedById;           // SỬA: backend trả "reviewedById" không phải "reviewedBy"
        public String reviewedByName;       // SỬA: backend trả "reviewedByName" không phải "reviewerName"
        public String rejectionReason;
        public String createdAt;
        public String updatedAt;

        // Helper: lấy tên người duyệt (dùng trong adapter)
        public String getReviewerName() { return reviewedByName; }
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