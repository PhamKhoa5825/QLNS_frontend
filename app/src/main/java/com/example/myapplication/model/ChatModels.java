package com.example.myapplication.model;

public class ChatModels {

    public static class ChatRoomResponse {
        public Long id;
        public String name;
        public String lastMessage;
        public String lastMessageTime;
        public int unreadCount;
    }

    public static class MessageResponse {
        public Long id;
        public Long roomId;
        public Long senderId;
        public String senderName;
        public String message;
        public String createdAt;
    }

    public static class SendMessageBody {
        public Long roomId;
        public Long senderId;
        public String message;
        public SendMessageBody(Long roomId, Long senderId, String message) {
            this.roomId   = roomId;
            this.senderId = senderId;
            this.message  = message;
        }
    }
}