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

    @SerializedName("memberNames")
    private java.util.List<String> memberNames;

    @SerializedName("otherParticipantName")
    private String otherParticipantName;

    @SerializedName("lastMessage")
    private String lastMessage;

    @SerializedName("lastMessageTime")
    private String lastMessageTime;

    @SerializedName("unreadCount")
    private int unreadCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }

    public java.util.List<String> getMemberNames() { return memberNames; }
    public void setMemberNames(java.util.List<String> memberNames) { this.memberNames = memberNames; }

    public String getOtherParticipantName() { return otherParticipantName; }
    public void setOtherParticipantName(String otherParticipantName) { this.otherParticipantName = otherParticipantName; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public String getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(String lastMessageTime) { this.lastMessageTime = lastMessageTime; }

    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }
}
