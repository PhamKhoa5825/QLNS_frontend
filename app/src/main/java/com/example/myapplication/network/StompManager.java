package com.example.myapplication.network;

import android.util.Log;

import com.example.myapplication.model.ChatEvent;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;
import ua.naiksoftware.stomp.dto.StompHeader;

public class StompManager {

    private static final String TAG = "StompManager";
    private static final String WS_URL = "ws://10.0.2.2:8080/ws";

    private static volatile StompManager instance;

    private final Gson gson = new Gson();
    private final CompositeDisposable lifecycleDisposables = new CompositeDisposable();

    private StompClient stompClient;
    private Disposable roomSubscription;

    private StompManager() {
    }

    public static StompManager getInstance() {
        if (instance == null) {
            synchronized (StompManager.class) {
                if (instance == null) {
                    instance = new StompManager();
                }
            }
        }
        return instance;
    }

    public void connect(String token) {
        if (stompClient == null) {
            stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, WS_URL);
        }

        // [Chat] Đăng ký lifecycle để theo dõi trạng thái socket và log lỗi kết nối.
        lifecycleDisposables.clear();
        lifecycleDisposables.add(
                stompClient.lifecycle()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                event -> Log.d(TAG, "Lifecycle event: " + event.getType()),
                                throwable -> Log.e(TAG, "Lỗi lifecycle STOMP", throwable)
                        )
        );

        List<StompHeader> headers = new ArrayList<>();
        if (token != null && !token.isEmpty()) {
            headers.add(new StompHeader("Authorization", "Bearer " + token));
        }

        // [Chat] Kết nối WebSocket STOMP kèm Authorization header.
        stompClient.connect(headers);
    }

    public void subscribeRoom(Long roomId, Consumer<ChatEvent> callback) {
        unsubscribeRoom();

        if (stompClient == null) {
            Log.w(TAG, "STOMP chưa connect, không thể subscribe phòng");
            return;
        }

        String destination = "/topic/room/" + roomId;

        // [Chat] Lắng nghe sự kiện realtime của phòng chat và trả dữ liệu về main thread.
        roomSubscription = stompClient.topic(destination)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        stompMessage -> {
                            ChatEvent event = gson.fromJson(stompMessage.getPayload(), ChatEvent.class);
                            if (callback != null) {
                                callback.accept(event);
                            }
                        },
                        throwable -> Log.e(TAG, "Lỗi subscribe phòng chat", throwable)
                );
    }

    public void unsubscribeRoom() {
        if (roomSubscription != null && !roomSubscription.isDisposed()) {
            roomSubscription.dispose();
            roomSubscription = null;
        }
    }

    public void sendMessage(Long roomId, String content) {
        if (stompClient == null) {
            Log.w(TAG, "STOMP chưa connect, không gửi được tin nhắn");
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("roomId", roomId);
        payload.put("message", content);
        payload.put("messageType", "TEXT");

        String body = gson.toJson(payload);

        // [Chat] Gửi frame tới /app/chat.send để backend broadcast message realtime.
        lifecycleDisposables.add(
                stompClient.send("/app/chat.send", body)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> Log.d(TAG, "Đã gửi message qua STOMP"),
                                throwable -> Log.e(TAG, "Lỗi gửi message STOMP", throwable)
                        )
        );
    }

    public void sendMessage(Long roomId, String content, Long replyToId) {
        if (stompClient == null) {
            Log.w(TAG, "STOMP chưa connect, không gửi được tin nhắn");
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("roomId", roomId);
        payload.put("message", content);
        payload.put("messageType", "TEXT");
        if (replyToId != null) {
            payload.put("replyToId", replyToId);
        }

        String body = gson.toJson(payload);

        // [Chat] Gửi frame tới /app/chat.send để backend broadcast message realtime kèm reply nếu có.
        lifecycleDisposables.add(
                stompClient.send("/app/chat.send", body)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> Log.d(TAG, "Đã gửi message qua STOMP"),
                                throwable -> Log.e(TAG, "Lỗi gửi message STOMP", throwable)
                        )
        );
    }

    public void recallMessage(Long messageId) {
        if (stompClient == null) {
            Log.w(TAG, "STOMP chưa connect, không thể thu hồi tin nhắn");
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("messageId", messageId);
        String body = gson.toJson(payload);

        // [Chat] Gửi yêu cầu thu hồi tin nhắn tới backend.
        lifecycleDisposables.add(
                stompClient.send("/app/chat.recall", body)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> Log.d(TAG, "Đã gửi yêu cầu thu hồi"),
                                throwable -> Log.e(TAG, "Lỗi recall message STOMP", throwable)
                        )
        );
    }

    public void disconnect() {
        unsubscribeRoom();

        if (stompClient != null) {
            // [Chat] Ngắt kết nối socket khi rời màn hình để tránh leak subscription.
            stompClient.disconnect();
        }

        lifecycleDisposables.clear();
    }
}
