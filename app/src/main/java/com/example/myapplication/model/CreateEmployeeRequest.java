package com.example.myapplication.model;

import com.google.gson.annotations.SerializedName;

public class CreateEmployeeRequest {

    @SerializedName("fullName")
    private String fullName;

    @SerializedName("email")
    private String email;

    @SerializedName("password")
    private String password;

    @SerializedName("phone")
    private String phone;

    @SerializedName("address")
    private String address;

    @SerializedName("dateOfBirth")      // "yyyy-MM-dd"
    private String dateOfBirth;

    @SerializedName("gender")           // MALE / FEMALE / OTHER
    private String gender;

    @SerializedName("position")
    private String position;

    @SerializedName("joinDate")         // "yyyy-MM-dd"
    private String joinDate;

    @SerializedName("departmentId")
    private Long departmentId;

    // Constructor tối thiểu
    public CreateEmployeeRequest(String fullName, String email, String password,
                                 String position, Long departmentId) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.position = position;
        this.departmentId = departmentId;
    }

    // Setters tuỳ chọn
    public void setPhone(String phone) { this.phone = phone; }
    public void setAddress(String address) { this.address = address; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public void setGender(String gender) { this.gender = gender; }
    public void setJoinDate(String joinDate) { this.joinDate = joinDate; }

    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getPhone() { return phone; }
    public String getPosition() { return position; }
    public Long getDepartmentId() { return departmentId; }
}
