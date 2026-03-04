package com.example.myapplication;

public class Department {
    private String name, managerName;
    private int employeeCount, performance, colorBg;

    public Department(String name, String managerName, int employeeCount, int performance, int colorBg) {
        this.name = name;
        this.managerName = managerName;
        this.employeeCount = employeeCount;
        this.performance = performance;
        this.colorBg = colorBg; // Màu nền icon (Ví dụ Xanh, Hồng, Xanh lá...)
    }

    public String getName() { return name; }
    public String getManagerName() { return managerName; }
    public int getEmployeeCount() { return employeeCount; }
    public int getPerformance() { return performance; }
    public int getColorBg() { return colorBg; }
}
