package com.example.myapplication.model;

import com.google.gson.annotations.SerializedName;

public class Message {

    @SerializedName("id")
    private Long id;

    @SerializedName("roomId")
    private Long roomId;

    @SerializedName("senderId")
    private Long senderId;

    @SerializedName("replyToId")
    private Long replyToId;

    @SerializedName("senderName")
    private String senderName;

    @SerializedName("message")
    private String message;

    @SerializedName("messageType")
    private String messageType;

    @SerializedName("fileUrl")
    private String fileUrl;

    @SerializedName("fileName")
    private String fileName;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("isRecalled")
    private Boolean isRecalled;

    @SerializedName("fileSize")
    private Long fileSize;

    @SerializedName("replyToMessage")
    private Message replyToMessage;

    @SerializedName("metadata")
    private String metadata;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }

    public Long getReplyToId() { return replyToId; }
    public void setReplyToId(Long replyToId) { this.replyToId = replyToId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public Boolean getIsRecalled() { return isRecalled; }
    public void setIsRecalled(Boolean isRecalled) { this.isRecalled = isRecalled; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public Message getReplyToMessage() { return replyToMessage; }
    public void setReplyToMessage(Message replyToMessage) { this.replyToMessage = replyToMessage; }
}
