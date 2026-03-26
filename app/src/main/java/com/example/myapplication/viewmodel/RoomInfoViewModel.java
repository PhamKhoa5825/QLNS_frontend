package com.example.myapplication.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.model.ChatRoom;
import com.example.myapplication.model.Contact;
import com.example.myapplication.model.RoomMember;
import com.example.myapplication.repository.ChatRepository;

import java.util.List;

public class RoomInfoViewModel extends ViewModel {

    private final ChatRepository repository;
    private final MutableLiveData<List<RoomMember>> members = new MutableLiveData<>();
    private final MutableLiveData<Boolean> actionSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> myRole = new MutableLiveData<>("MEMBER");
    private final MutableLiveData<ChatRoom> renamedRoom = new MutableLiveData<>();
    private final MutableLiveData<List<Contact>> contacts = new MutableLiveData<>();

    public RoomInfoViewModel(ChatRepository repository) {
        // [Chat] ViewModel quản lý thông tin phòng: members, role, các hành động thêm/xóa/đổi tên.
        this.repository = repository;
    }

    public MutableLiveData<List<RoomMember>> getMembers() { return members; }
    public MutableLiveData<Boolean> getActionSuccess() { return actionSuccess; }
    public MutableLiveData<String> getMyRole() { return myRole; }
    public MutableLiveData<ChatRoom> getRenamedRoom() { return renamedRoom; }
    public MutableLiveData<List<Contact>> getContacts() { return contacts; }

    public void loadMembers(Long roomId, Long myUserId) {
        LiveData<List<RoomMember>> source = repository.getMembers(roomId);
        Observer<List<RoomMember>> observer = new Observer<List<RoomMember>>() {
            @Override
            public void onChanged(List<RoomMember> list) {
                members.setValue(list);
                // [Chat] Xác định role của chính mình để điều khiển UI.
                if (list != null) {
                    for (RoomMember member : list) {
                        if (member.getUserId() != null && member.getUserId().equals(myUserId)) {
                            myRole.setValue(member.getRole());
                            break;
                        }
                    }
                }
                source.removeObserver(this);
            }
        };
        source.observeForever(observer);
    }

    public void addMembers(Long roomId, List<Long> memberIds) {
        LiveData<Boolean> source = repository.addMembers(roomId, memberIds);
        Observer<Boolean> observer = new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean success) {
                actionSuccess.setValue(success);
                source.removeObserver(this);
            }
        };
        source.observeForever(observer);
    }

    public void removeMember(Long roomId, Long userId) {
        LiveData<Boolean> source = repository.removeMember(roomId, userId);
        Observer<Boolean> observer = new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean success) {
                actionSuccess.setValue(success);
                source.removeObserver(this);
            }
        };
        source.observeForever(observer);
    }

    public void renameRoom(Long roomId, String name) {
        LiveData<ChatRoom> source = repository.renameRoom(roomId, name);
        Observer<ChatRoom> observer = new Observer<ChatRoom>() {
            @Override
            public void onChanged(ChatRoom room) {
                actionSuccess.setValue(room != null);
                renamedRoom.setValue(room);
                source.removeObserver(this);
            }
        };
        source.observeForever(observer);
    }

    public void searchContacts(String keyword, Long roomId) {
        LiveData<List<Contact>> source = repository.searchContacts(keyword, roomId);
        Observer<List<Contact>> observer = new Observer<List<Contact>>() {
            @Override
            public void onChanged(List<Contact> list) {
                contacts.setValue(list);
                source.removeObserver(this);
            }
        };
        source.observeForever(observer);
    }

    public void searchContacts(String keyword) {
        searchContacts(keyword, null);
    }

    public static class Factory implements ViewModelProvider.Factory {
        private final ChatRepository repository;

        public Factory(ChatRepository repository) {
            // [Chat] Factory để inject repository vào ViewModel.
            this.repository = repository;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(RoomInfoViewModel.class)) {
                return (T) new RoomInfoViewModel(repository);
            }
            throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
        }
    }
}
