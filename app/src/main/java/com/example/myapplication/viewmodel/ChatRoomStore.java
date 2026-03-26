package com.example.myapplication.viewmodel;

import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.model.ChatRoom;

import java.util.ArrayList;
import java.util.List;

/**
 * Singleton giữ state danh sách phòng chat để có thể cập nhật từ nhiều màn hình
 * (Chat list, Message) mà không cần reload API.
 */
public class ChatRoomStore {

    private static final ChatRoomStore INSTANCE = new ChatRoomStore();

    private final MutableLiveData<List<ChatRoom>> roomsLiveData = new MutableLiveData<>();

    private ChatRoomStore() {
    }

    public static ChatRoomStore getInstance() {
        return INSTANCE;
    }

    public MutableLiveData<List<ChatRoom>> getRoomsLiveData() {
        return roomsLiveData;
    }

    public void setRooms(List<ChatRoom> rooms) {
        roomsLiveData.setValue(rooms);
    }

    public void clearUnread(Long roomId) {
        if (roomId == null) {
            return;
        }
        List<ChatRoom> current = roomsLiveData.getValue();
        if (current == null || current.isEmpty()) {
            return;
        }
        List<ChatRoom> updated = new ArrayList<>(current.size());
        for (ChatRoom room : current) {
            if (room != null && roomId.equals(room.getId())) {
                room.setUnreadCount(0);
            }
            updated.add(room);
        }
        roomsLiveData.setValue(updated);
    }

    public void incrementUnread(Long roomId) {
        if (roomId == null) {
            return;
        }
        List<ChatRoom> current = roomsLiveData.getValue();
        if (current == null || current.isEmpty()) {
            return;
        }
        List<ChatRoom> updated = new ArrayList<>(current.size());
        for (ChatRoom room : current) {
            if (room != null && roomId.equals(room.getId())) {
                room.setUnreadCount(room.getUnreadCount() + 1);
            }
            updated.add(room);
        }
        roomsLiveData.setValue(updated);
    }

    public void updateLastMessage(Long roomId, String lastMessage, String lastMessageTime) {
        if (roomId == null) {
            return;
        }
        List<ChatRoom> current = roomsLiveData.getValue();
        if (current == null || current.isEmpty()) {
            return;
        }
        List<ChatRoom> updated = new ArrayList<>(current.size());
        boolean changed = false;
        for (ChatRoom room : current) {
            if (room != null && roomId.equals(room.getId())) {
                room.setLastMessage(lastMessage);
                room.setLastMessageTime(lastMessageTime);
                changed = true;
            }
            updated.add(room);
        }
        if (changed) {
            roomsLiveData.setValue(updated);
        }
    }

    public void updateRoomName(Long roomId, String newName) {
        if (roomId == null || newName == null) {
            return;
        }
        List<ChatRoom> current = roomsLiveData.getValue();
        if (current == null || current.isEmpty()) {
            return;
        }
        List<ChatRoom> updated = new ArrayList<>(current.size());
        boolean changed = false;
        for (ChatRoom room : current) {
            if (room != null && roomId.equals(room.getId())) {
                room.setName(newName);
                changed = true;
            }
            updated.add(room);
        }
        if (changed) {
            roomsLiveData.setValue(updated);
        }
    }
}
