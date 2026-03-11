package com.example.myapplication.model;

import com.google.gson.annotations.SerializedName;

public class ChatRoom {
    @SerializedName("id")
    private Long id;

    @SerializedName("name")
    private String name;

    @SerializedName("type")
    private String type; // PRIVATE, DEPARTMENT

    @SerializedName("departmentId")
    private Long departmentId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
}
