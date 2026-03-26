package com.example.myapplication.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.model.ChatRoom;
import com.example.myapplication.model.Contact;
import com.example.myapplication.repository.ChatRepository;

import java.util.List;

public class ContactViewModel extends ViewModel {

    private final ChatRepository repository;
    private final MutableLiveData<List<Contact>> contacts = new MutableLiveData<>();
    private final MutableLiveData<ChatRoom> createdRoom = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private String selectedSkill;
    private String selectedPosition;
    private String selectedStatus;

    public ContactViewModel(ChatRepository repository) {
        // [Chat] ViewModel nhận repository để gọi API search và tạo phòng chat riêng.
        this.repository = repository;
    }

    public MutableLiveData<List<Contact>> getContacts() {
        return contacts;
    }

    public MutableLiveData<ChatRoom> getCreatedRoom() {
        return createdRoom;
    }

    public MutableLiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public void setFilters(String skill, String position, String status) {
        this.selectedSkill = skill;
        this.selectedPosition = position;
        this.selectedStatus = status;
    }

    public void searchContacts(String keyword) {
        searchContacts(keyword, selectedSkill, selectedPosition, selectedStatus);
    }

    public void searchContacts(String keyword, String skill, String position, String status) {
        isLoading.setValue(true);
        LiveData<List<Contact>> source = repository.searchContacts(keyword, skill, position, status);
        Observer<List<Contact>> observer = new Observer<List<Contact>>() {
            @Override
            public void onChanged(List<Contact> result) {
                contacts.setValue(result);
                isLoading.setValue(false);
                source.removeObserver(this);
            }
        };
        source.observeForever(observer);
    }

    public void createPrivateRoom(Long targetUserId) {
        LiveData<ChatRoom> source = repository.createPrivateRoom(targetUserId);
        Observer<ChatRoom> observer = new Observer<ChatRoom>() {
            @Override
            public void onChanged(ChatRoom room) {
                createdRoom.setValue(room);
                source.removeObserver(this);
            }
        };
        source.observeForever(observer);
    }

    public static class Factory implements ViewModelProvider.Factory {
        private final ChatRepository repository;

        public Factory(ChatRepository repository) {
            // [Chat] Factory giúp inject repository cho ViewModel.
            this.repository = repository;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(ContactViewModel.class)) {
                return (T) new ContactViewModel(repository);
            }
            throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
        }
    }
}
