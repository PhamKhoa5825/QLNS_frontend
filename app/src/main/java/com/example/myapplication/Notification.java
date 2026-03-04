package com.example.myapplication;

public class Notification {
    public static final int TYPE_TASK = 1;
    public static final int TYPE_MESSAGE = 2;
    public static final int TYPE_MEETING = 3;
    public static final int TYPE_CHECKIN = 4;
    public static final int TYPE_HR = 5;
    public static final int TYPE_WARNING = 6;

    private String title, content, time, actionText;
    private int type;
    private boolean isUnread;

    public Notification(String title, String content, String time, String actionText, int type, boolean isUnread) {
        this.title = title;
        this.content = content;
        this.time = time;
        this.actionText = actionText;
        this.type = type;
        this.isUnread = isUnread;
    }

    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getTime() { return time; }
    public String getActionText() { return actionText; }
    public int getType() { return type; }
    public boolean isUnread() { return isUnread; }
}
