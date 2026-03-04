package com.example.myapplication;

public class Chat {
    private String name, message, time, department;
    private int unreadCount;
    private boolean isOnline;

    public Chat(String name, String message, String time, String department, int unreadCount, boolean isOnline) {
        this.name = name;
        this.message = message;
        this.time = time;
        this.department = department;
        this.unreadCount = unreadCount;
        this.isOnline = isOnline;
    }

    public String getName() { return name; }
    public String getMessage() { return message; }
    public String getTime() { return time; }
    public String getDepartment() { return department; }
    public int getUnreadCount() { return unreadCount; }
    public boolean isOnline() { return isOnline; }
    public String getAvatarText() { return String.valueOf(name.charAt(0)).toUpperCase(); }
}
