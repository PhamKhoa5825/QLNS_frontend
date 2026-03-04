package com.example.myapplication;

public class Employee {
    private String name, role, department, status, avatarText;
    private boolean isWorking;

    public Employee(String name, String role, String department, boolean isWorking) {
        this.name = name;
        this.role = role;
        this.department = department;
        this.isWorking = isWorking;
        this.status = isWorking ? "Đang làm việc" : "Nghỉ phép";
        this.avatarText = String.valueOf(name.charAt(0)).toUpperCase();
    }

    // Getters
    public String getName() { return name; }
    public String getRole() { return role; }
    public String getDepartment() { return department; }
    public String getStatus() { return status; }
    public String getAvatarText() { return avatarText; }
    public boolean isWorking() { return isWorking; }
}
