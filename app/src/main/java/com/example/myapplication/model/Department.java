package com.example.myapplication.model;

import com.google.gson.annotations.SerializedName;

// Model nhận JSON từ Spring Boot API
// Thay thế Department.java cũ (hardcode)
public class Department {

    @SerializedName("id")
    private Long id;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("manager")
    private Employee manager;

    // employeeCount và performance không có trong API
    // → tính từ danh sách employees hoặc bỏ khỏi UI
    // colorBg → tự gán màu theo vị trí trong adapter

    public Department() {}

    public Department(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    // Dùng khi gửi lên API (chỉ cần id)
    public void setId(Long id) { this.id = id; }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Employee getManager() { return manager; }

    // Lấy tên manager để hiển thị UI
    public String getManagerName() {
        if (manager != null && manager.getFullName() != null) {
            return manager.getFullName();
        }
        return "Chưa có trưởng phòng";
    }
}
