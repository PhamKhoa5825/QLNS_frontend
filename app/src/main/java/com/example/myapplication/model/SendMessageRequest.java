package com.example.myapplication.model;

import com.google.gson.Gson;

public class SendMessageRequest {
    private Long roomId;
    private String message;
    private String messageType;
    private Long replyToId;
    private String metadata;

    public SendMessageRequest(Long roomId, String message) {
        this(roomId, message, "TEXT", null, (String) null);
    }

    public SendMessageRequest(Long roomId, String message, String messageType, Long replyToId, java.util.Map<String, Object> metadata) {
        this(roomId, message, messageType, replyToId, metadata != null ? new Gson().toJson(metadata) : null);
    }

    public SendMessageRequest(Long roomId, String message, String messageType, Long replyToId, String metadata) {
        this.roomId = roomId;
        this.message = message;
        this.messageType = messageType;
        this.replyToId = replyToId;
        this.metadata = metadata;
    }

    public Long getRoomId() { return roomId; }
    public String getMessage() { return message; }
    public String getMessageType() { return messageType; }
    public Long getReplyToId() { return replyToId; }
    public String getMetadata() { return metadata; }
}
