package com.example.myapplication.model;

public class SendMessageRequest {
    private Long roomId;
    private Long senderId;
    private String message;
    private String messageType;

    public SendMessageRequest(Long roomId, Long senderId, String message) {
        this.roomId = roomId;
        this.senderId = senderId;
        this.message = message;
        this.messageType = "TEXT";
    }
}
