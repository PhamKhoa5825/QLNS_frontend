package com.example.myapplication.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.model.Message;
import com.example.myapplication.repository.ChatRepository;

import java.util.List;

public class SearchViewModel extends ViewModel {

    private final ChatRepository repository;
    private final MutableLiveData<List<Message>> results = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isEmpty = new MutableLiveData<>(false);

    public SearchViewModel(ChatRepository repository) {
        // [Chat] ViewModel phục vụ màn hình search tin nhắn, gọi repository và expose LiveData kết quả.
        this.repository = repository;
    }

    public LiveData<List<Message>> getResults() {
        return results;
    }

    public LiveData<Boolean> getIsEmpty() {
        return isEmpty;
    }

    public void searchMessages(Long roomId, String keyword) {
        LiveData<List<Message>> source = repository.searchMessages(roomId, keyword);
        Observer<List<Message>> observer = new Observer<List<Message>>() {
            @Override
            public void onChanged(List<Message> list) {
                results.setValue(list);
                isEmpty.setValue(list == null || list.isEmpty());
                source.removeObserver(this);
            }
        };
        source.observeForever(observer);
    }

    public static class Factory implements ViewModelProvider.Factory {
        private final ChatRepository repository;

        public Factory(ChatRepository repository) {
            this.repository = repository;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(Class<T> modelClass) {
            if (modelClass.isAssignableFrom(SearchViewModel.class)) {
                return (T) new SearchViewModel(repository);
            }
            throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
        }
    }
}
