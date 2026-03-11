package com.example.myapplication.model;

import com.google.gson.annotations.SerializedName;

// Model nhận JSON từ Spring Boot API
public class Department {

    @SerializedName("id")
    private Long id;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("managerId")
    private Long managerId;

    @SerializedName("managerName")       // Server trả thẳng tên, không phải object
    private String managerName;

    @SerializedName("employeeCount")     // Server đếm thật từ DB
    private int employeeCount;

    @SerializedName("createdAt")
    private String createdAt;

    public Department() {}

    public Department(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getManagerId() { return managerId; }
    public String getManagerName() {
        return managerName != null ? managerName : "Chưa có trưởng phòng";
    }
    public int getEmployeeCount() { return employeeCount; }
    public String getCreatedAt() { return createdAt; }
}

