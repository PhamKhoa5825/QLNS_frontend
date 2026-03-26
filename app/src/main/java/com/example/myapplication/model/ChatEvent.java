package com.example.myapplication.model;

import com.google.gson.annotations.SerializedName;

public class ChatEvent {

    @SerializedName("eventType")
    private String eventType;

    @SerializedName("roomId")
    private Long roomId;

    @SerializedName("message")
    private Message message;

    @SerializedName("data")
    private Object data;

    @SerializedName("triggeredBy")
    private Long triggeredBy;

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public Long getTriggeredBy() {
        return triggeredBy;
    }

    public void setTriggeredBy(Long triggeredBy) {
        this.triggeredBy = triggeredBy;
    }
}
