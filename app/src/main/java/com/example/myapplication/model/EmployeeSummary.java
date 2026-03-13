package com.example.myapplication.model;

public class EmployeeSummary {
    private String fullName;
    private String avatarUrl;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getAvatarText() {
        if (fullName == null || fullName.isEmpty()) return "NA";
        String[] parts = fullName.split(" ");
        if (parts.length > 1) {
            return (parts[parts.length - 2].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
        }
        return fullName.substring(0, Math.min(fullName.length(), 2)).toUpperCase();
    }
}
