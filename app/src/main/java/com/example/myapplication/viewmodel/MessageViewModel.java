package com.example.myapplication.viewmodel;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.model.ChatEvent;
import com.example.myapplication.model.Message;
import com.example.myapplication.model.RoomMember;
import com.example.myapplication.network.StompManager;
import com.example.myapplication.repository.ChatRepository;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MessageViewModel extends ViewModel {

    private final ChatRepository repository;
    private final String token;
    private final Long myUserId;
    private final Gson gson = new Gson();

    private final MutableLiveData<List<Message>> messages = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Message> newMessage = new MutableLiveData<>();
    private final MutableLiveData<Message> recalledMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isUploading = new MutableLiveData<>(false);
    private final MutableLiveData<ChatRepository.UploadResult> uploadResult = new MutableLiveData<>();
    private final MutableLiveData<Message> selectedReplyMessage = new MutableLiveData<>(null);
    private final MutableLiveData<List<RoomMember>> members = new MutableLiveData<>();

    private final StompManager stompManager = StompManager.getInstance();
    private final ChatRoomStore roomStore = ChatRoomStore.getInstance();
    private final Set<Long> fetchingReplyIds = new HashSet<>();

    public MessageViewModel(ChatRepository repository, String token, Long myUserId) {
        // [Chat] ViewModel nhận repository để tải lịch sử và token để connect STOMP có auth.
        this.repository = repository;
        this.token = token;
        this.myUserId = myUserId;
    }

    public MutableLiveData<List<Message>> getMessages() {
        return messages;
    }

    public MutableLiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public MutableLiveData<Message> getNewMessage() {
        return newMessage;
    }

    public MutableLiveData<Message> getRecalledMessage() {
        return recalledMessage;
    }

    public MutableLiveData<Boolean> getIsUploading() {
        return isUploading;
    }

    public MutableLiveData<ChatRepository.UploadResult> getUploadResult() {
        return uploadResult;
    }

    public MutableLiveData<List<RoomMember>> getMembers() {
        return members;
    }

    public void loadMembers(Long roomId) {
        LiveData<List<RoomMember>> source = repository.getMembers(roomId);
        Observer<List<RoomMember>> observer = new Observer<List<RoomMember>>() {
            @Override
            public void onChanged(List<RoomMember> list) {
                members.setValue(list);
                source.removeObserver(this);
            }
        };
        source.observeForever(observer);
    }

    public MutableLiveData<Message> getSelectedReplyMessage() {
        return selectedReplyMessage;
    }

    public void setReplyTarget(Message message) {
        // [Chat] Lưu message đang được chọn để reply, để View quan sát và render UI preview.
        selectedReplyMessage.setValue(message);
    }

    public void clearReplyTarget() {
        // [Chat] Xóa trạng thái reply hiện tại khi hủy hoặc gửi xong.
        selectedReplyMessage.setValue(null);
    }

    public void markRoomAsSeen(Long roomId, Long lastSeenMessageId) {
        if (roomId == null || lastSeenMessageId == null) {
            return;
        }
        repository.markRoomSeen(roomId, lastSeenMessageId);
        roomStore.clearUnread(roomId);
    }

    public void loadMessages(Long roomId) {
        // [Chat] Bật trạng thái loading trước khi gọi repository.
        isLoading.setValue(true);

        LiveData<List<Message>> source = repository.getMessages(roomId);
        Observer<List<Message>> observer = new Observer<List<Message>>() {
            @Override
            public void onChanged(List<Message> newMessages) {
                messages.setValue(newMessages);
                hydrateMissingReplies(newMessages);
                isLoading.setValue(false);
                source.removeObserver(this);
            }
        };
        source.observeForever(observer);
    }

    public void connectAndSubscribe(Long roomId) {
        // [Chat] Kết nối STOMP và subscribe room để nhận event realtime.
        stompManager.connect(token);
        stompManager.subscribeRoom(roomId, this::handleEvent);
    }

    public void sendMessage(Long roomId, String content) {
        // [Chat] Đẩy tin nhắn mới qua WebSocket; backend tự nhận diện user từ token.
        Message reply = selectedReplyMessage.getValue();
        Long replyToId = reply != null ? reply.getId() : null;
        stompManager.sendMessage(roomId, content, replyToId);
    }

    public void disconnectRoom() {
        // [Chat] Hủy subscribe + ngắt socket khi rời màn hình chat.
        stompManager.unsubscribeRoom();
        stompManager.disconnect();
    }

    public void uploadFile(Long roomId, Uri uri, String type, Context ctx) {
        isUploading.setValue(true);
        LiveData<ChatRepository.UploadResult> source = repository.uploadFile(roomId, uri, type, ctx);
        Observer<ChatRepository.UploadResult> observer = new Observer<ChatRepository.UploadResult>() {
            @Override
            public void onChanged(ChatRepository.UploadResult result) {
                // Upload xong chỉ cập nhật trạng thái; tin nhắn sẽ hiển thị khi nhận NEW_MESSAGE từ WS.
                isUploading.setValue(false);
                uploadResult.setValue(result);
                source.removeObserver(this);
            }
        };
        source.observeForever(observer);
    }

    public void recallMessage(Long messageId) {
        // [Chat] Gửi yêu cầu thu hồi tin nhắn qua STOMP (server tự kiểm tra user từ token).
        stompManager.recallMessage(messageId);
        repository.recallMessageFallback(messageId);
    }

    private void handleEvent(ChatEvent event) {
        if (event == null || event.getEventType() == null) {
            return;
        }

        switch (event.getEventType()) {
            case "NEW_MESSAGE":
                Message incoming = event.getMessage();
                if (incoming != null) {
                    newMessage.setValue(incoming);
                    roomStore.updateLastMessage(event.getRoomId(), incoming.getMessage(), incoming.getCreatedAt());
                    requestReplyDetailIfMissing(incoming);
                }
                if (event.getTriggeredBy() != null && !event.getTriggeredBy().equals(myUserId)) {
                    roomStore.incrementUnread(event.getRoomId());
                }
                break;
            case "RECALL":
                recalledMessage.setValue(event.getMessage());
                break;
            default:
                break;
        }
    }

    private void hydrateMissingReplies(List<Message> baseList) {
        if (baseList == null || baseList.isEmpty()) {
            return;
        }
        for (Message msg : baseList) {
            Long replyId = msg != null ? msg.getReplyToId() : null;
            if (replyId != null && msg.getReplyToMessage() == null) {
                requestReplyDetail(replyId, null);
            }
        }
    }

    private void requestReplyDetailIfMissing(Message target) {
        if (target == null) return;
        Long replyId = target.getReplyToId();
        if (replyId == null || target.getReplyToMessage() != null) return;
        requestReplyDetail(replyId, target);
    }

    private void requestReplyDetail(Long replyId, Message targetToUpdate) {
        if (fetchingReplyIds.contains(replyId)) {
            return;
        }
        fetchingReplyIds.add(replyId);
        LiveData<Message> source = repository.getMessageDetail(replyId);
        Observer<Message> observer = new Observer<Message>() {
            @Override
            public void onChanged(Message detail) {
                source.removeObserver(this);
                fetchingReplyIds.remove(replyId);
                if (detail == null) return;
                if (targetToUpdate != null) {
                    targetToUpdate.setReplyToMessage(detail);
                    newMessage.setValue(targetToUpdate);
                }
                applyReplyDetailToMessages(replyId, detail);
            }
        };
        source.observeForever(observer);
    }

    private void applyReplyDetailToMessages(Long replyId, Message detail) {
        List<Message> current = messages.getValue();
        if (current == null || current.isEmpty()) return;
        boolean updated = false;
        List<Message> copy = new ArrayList<>(current.size());
        for (Message item : current) {
            if (replyId.equals(item.getReplyToId()) && item.getReplyToMessage() == null) {
                item.setReplyToMessage(detail);
                updated = true;
            }
            copy.add(item);
        }
        if (updated) {
            messages.setValue(copy);
        }
    }

    public static class Factory implements ViewModelProvider.Factory {
        private final ChatRepository repository;
        private final String token;
        private final Long myUserId;

        public Factory(ChatRepository repository, String token, Long myUserId) {
            // [Chat] Factory giúp inject dependency vào ViewModelProvider.
            this.repository = repository;
            this.token = token;
            this.myUserId = myUserId;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(MessageViewModel.class)) {
                return (T) new MessageViewModel(repository, token, myUserId);
            }
            throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
        }
    }
}
