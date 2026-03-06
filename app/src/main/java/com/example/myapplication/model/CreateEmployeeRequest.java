package com.example.myapplication.model;

import com.google.gson.annotations.SerializedName;

public class CreateEmployeeRequest {

    @SerializedName("fullName")
    private String fullName;

    @SerializedName("employeeCode")
    private String employeeCode;

    @SerializedName("email")
    private String email;

    @SerializedName("password")
    private String password;

    @SerializedName("phone")
    private String phone;

    @SerializedName("position")
    private String position;

    @SerializedName("salary")
    private Double salary;

    @SerializedName("department")
    private Department department;

    public CreateEmployeeRequest(String fullName, String employeeCode, String email,
                                 String password, String position, Long departmentId) {
        this.fullName = fullName;
        this.employeeCode = employeeCode;
        this.email = email;
        this.password = password;
        this.position = position;
        Department dept = new Department();
        dept.setId(departmentId);
        this.department = dept;
    }

    public void setId(Long id) { if (department != null) department.setId(id); }
    public String getFullName() { return fullName; }
    public String getEmployeeCode() { return employeeCode; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPosition() { return position; }
    public Double getSalary() { return salary; }
    public void setSalary(Double salary) { this.salary = salary; }
    public Department getDepartment() { return department; }
}
