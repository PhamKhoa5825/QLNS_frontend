package com.example.myapplication.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.model.ChatRoom;
import com.example.myapplication.model.CreateGroupRequest;
import com.example.myapplication.repository.ChatRepository;

import java.util.List;

public class ChatViewModel extends ViewModel {

    private final ChatRepository repository;
    private final ChatRoomStore roomStore = ChatRoomStore.getInstance();
    private final MutableLiveData<List<ChatRoom>> rooms = roomStore.getRoomsLiveData();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<ChatRoom> createdGroup = new MutableLiveData<>();

    public ChatViewModel(ChatRepository repository) {
        // [Chat] Inject Repository qua constructor để ViewModel chỉ tập trung quản lý state.
        this.repository = repository;
    }

    public MutableLiveData<List<ChatRoom>> getRooms() {
        return rooms;
    }

    public MutableLiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public MutableLiveData<ChatRoom> getCreatedGroup() {
        return createdGroup;
    }

    /**
     * [Chat] Reset trạng thái phòng vừa tạo để tránh việc tự động mở lại phòng cũ khi vào lại Dialog.
     */
    public void resetCreatedGroup() {
        createdGroup.setValue(null);
    }

    public void loadRooms() {
        // [Chat] Bật loading trước khi gọi API.
        isLoading.setValue(true);

        LiveData<List<ChatRoom>> source = repository.getRooms();
        Observer<List<ChatRoom>> observer = new Observer<List<ChatRoom>>() {
            @Override
            public void onChanged(List<ChatRoom> chatRooms) {
                roomStore.setRooms(chatRooms);
                // [Chat] Tắt loading khi đã nhận được kết quả (cả thành công lẫn thất bại).
                isLoading.setValue(false);
                source.removeObserver(this);
            }
        };
        source.observeForever(observer);
    }

    public void loadRooms(Long ignoredUserId) {
        loadRooms();
    }

    public void clearUnread(Long roomId) {
        roomStore.clearUnread(roomId);
    }

    public void createGroupRoom(String name, List<Long> memberIds) {
        CreateGroupRequest request = new CreateGroupRequest(name, memberIds);
        LiveData<ChatRoom> source = repository.createGroupRoom(request);
        Observer<ChatRoom> observer = new Observer<ChatRoom>() {
            @Override
            public void onChanged(ChatRoom chatRoom) {
                createdGroup.setValue(chatRoom);
                source.removeObserver(this);
            }
        };
        source.observeForever(observer);
    }

    public static class Factory implements ViewModelProvider.Factory {

        private final ChatRepository repository;

        public Factory(ChatRepository repository) {
            // [Chat] Factory giúp truyền dependency vào ViewModelProvider một cách rõ ràng.
            this.repository = repository;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(ChatViewModel.class)) {
                return (T) new ChatViewModel(repository);
            }
            throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
        }
    }
}
