package com.example.myapplication.model;

import com.google.gson.annotations.SerializedName;

public class RoomMember {

    @SerializedName("userId")
    private Long userId;

    @SerializedName("username")
    private String username;

    @SerializedName("fullName")
    private String fullName;

    @SerializedName("position")
    private String position;

    @SerializedName("departmentName")
    private String departmentName;

    @SerializedName("role")
    private String role;

    @SerializedName("joinedAt")
    private String joinedAt;

    @SerializedName("lastReadMessageId")
    private Long lastReadMessageId;

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getUserName() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getDisplayName() {
        if (fullName != null && !fullName.isEmpty()) return fullName;
        if (username != null && !username.isEmpty()) return username;
        return "Người dùng";
    }

    public String getPosition() {
        return position;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public String getRole() {
        return role;
    }

    public String getJoinedAt() {
        return joinedAt;
    }

    public Long getLastReadMessageId() {
        return lastReadMessageId;
    }

    public void setLastReadMessageId(Long lastReadMessageId) {
        this.lastReadMessageId = lastReadMessageId;
    }
}
