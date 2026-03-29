package com.example.myapplication.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.myapplication.R;
import com.example.myapplication.model.ChatRoom;
import com.example.myapplication.network.ChatApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.ui.MessageActivity;
import com.example.myapplication.ui.RequestActivity;
import com.example.myapplication.utils.SharedPrefsManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;
import ua.naiksoftware.stomp.dto.StompHeader;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatForegroundService extends Service {
    private static final String TAG = "ChatForegroundService";
    private static final String CHANNEL_ID = "chat_service_channel";
    private static final String MSG_CHANNEL_ID = "chat_messages_channel_v2";
    private static final String SYSTEM_NOTI_CHANNEL_ID = "system_notifications_channel_v2";
    private static final int NOTIF_ID = 1;

    public static final String ACTION_STOP_SERVICE = "STOP_CHAT_SERVICE";
    public static final String BROADCAST_CHAT_EVENT = "com.example.myapplication.CHAT_EVENT";
    
    // Static variable to track currently open room
    public static Long currentOpenRoomId = null;

    private StompClient mStompClient;
    private CompositeDisposable compositeDisposable;
    private final Gson gson = new Gson();
    private final List<Long> subscribedRooms = new ArrayList<>();
    private final Handler reconnectHandler = new Handler(Looper.getMainLooper());
    private boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP_SERVICE.equals(intent.getAction())) {
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        if (!isRunning) {
            try {
                Notification notif = createForegroundNotification();
                if (Build.VERSION.SDK_INT >= 34) {
                    startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
                } else {
                    startForeground(NOTIF_ID, notif);
                }
                isRunning = true;
                initChat();
            } catch (SecurityException se) {
                Log.e(TAG, "Cannot start foreground service", se);
                stopSelf();
                return START_NOT_STICKY;
            }
        }

        return START_STICKY;
    }

    private void initChat() {
        String token = SharedPrefsManager.getInstance(this).getToken();
        if (token == null) {
            stopSelf();
            return;
        }

        ChatApiService apiService = RetrofitClient.getInstance().create(ChatApiService.class);
        apiService.getRooms().enqueue(new Callback<List<ChatRoom>>() {
            @Override
            public void onResponse(Call<List<ChatRoom>> call, Response<List<ChatRoom>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    connectWebSocket(token, response.body());
                } else {
                    scheduleReconnect();
                }
            }

            @Override
            public void onFailure(Call<List<ChatRoom>> call, Throwable t) {
                scheduleReconnect();
            }
        });
    }

    private void connectWebSocket(String token, List<ChatRoom> rooms) {
        String baseUrl = com.example.myapplication.network.RetrofitClient.BASE_URL;
        String wsUrl;
        if (baseUrl.contains("10.0.2.2")) {
            wsUrl = "ws://10.0.2.2:8080/ws";
        } else {
            // Trích xuất host từ BASE_URL (ví dụ http://192.168.1.5:8080/ -> 192.168.1.5:8080)
            String host = baseUrl.replace("http://", "").replace("https://", "");
            if (host.endsWith("/")) host = host.substring(0, host.length() - 1);
            wsUrl = "ws://" + host + "/ws";
        }
        
        Log.d(TAG, "Connecting to WebSocket: " + wsUrl);
        mStompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, wsUrl);

        List<StompHeader> headers = new ArrayList<>();
        headers.add(new StompHeader("Authorization", "Bearer " + token));

        compositeDisposable = new CompositeDisposable();

        Disposable lifecycleDisposable = mStompClient.lifecycle()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(lifecycleEvent -> {
                    switch (lifecycleEvent.getType()) {
                        case OPENED:
                            Log.d(TAG, "Stomp connection opened");
                            updateForegroundNotification("HRM App: Đã kết nối");
                            subscribeToRooms(rooms);
                            subscribeToNotifications(token);
                            break;
                        case ERROR:
                            Log.e(TAG, "Stomp connection error", lifecycleEvent.getException());
                            updateForegroundNotification("HRM App: Lỗi kết nối / Đang thử lại...");
                            scheduleReconnect();
                            break;
                        case CLOSED:
                            Log.d(TAG, "Stomp connection closed");
                            break;
                    }
                });

        compositeDisposable.add(lifecycleDisposable);
        mStompClient.connect(headers);
    }

    private void subscribeToRooms(List<ChatRoom> rooms) {
        for (ChatRoom room : rooms) {
            if (subscribedRooms.contains(room.getId())) continue;

            String topic = "/topic/room/" + room.getId();
            Disposable topicDisposable = mStompClient.topic(topic)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(stompMessage -> {
                        handleNewMessage(stompMessage.getPayload());
                    }, throwable -> {
                        Log.e(TAG, "Error on subscribe topic: " + topic, throwable);
                    });
            compositeDisposable.add(topicDisposable);
            subscribedRooms.add(room.getId());
        }
    }

    private void subscribeToNotifications(String token) {
        Long userId = SharedPrefsManager.getInstance(this).getUserId();
        if (userId == -1L) return;

        String topic = "/topic/notifications/" + userId;
        Disposable topicDisposable = mStompClient.topic(topic)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(stompMessage -> {
                    handleNewNotification(stompMessage.getPayload());
                }, throwable -> {
                    Log.e(TAG, "Error on subscribe notifications topic: " + topic, throwable);
                });
        compositeDisposable.add(topicDisposable);
    }

    private void handleNewNotification(String payload) {
        try {
            JsonObject json = gson.fromJson(payload, JsonObject.class);
            String title = json.has("title") ? json.get("title").getAsString() : "Thông báo hệ thống";
            String content = json.has("content") ? json.get("content").getAsString() : "";

            Class<?> targetActivity = com.example.myapplication.ui.NotificationActivity.class;
            
            // Phân loại điều hướng dựa trên từ khóa
            if (title.contains("Nhiệm vụ") || content.contains("nhiệm vụ") || title.contains("duyệt nhiệm vụ")) {
                targetActivity = com.example.myapplication.ui.TaskActivity.class;
            } else if (title.contains("đơn") || title.contains("Yêu cầu") || content.contains("nghỉ phép")) {
                targetActivity = com.example.myapplication.ui.RequestActivity.class;
            }

            showSystemNotification(title, content, targetActivity);
            
            // Phát broadcast để thông báo trong app (Toast)
            Intent it = new Intent("com.example.myapplication.SYSTEM_NOTIFICATION");
            it.putExtra("title", title);
            it.setPackage(getPackageName());
            sendBroadcast(it);
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing notification websocket message", e);
        }
    }

    private void showSystemNotification(String title, String content, Class<?> targetActivity) {
        Intent intent = new Intent(this, targetActivity);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, SYSTEM_NOTI_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setDefaults(Notification.DEFAULT_ALL)
                .setVibrate(new long[]{0, 500, 200, 500})
                .setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((int) System.currentTimeMillis() % 10000 + 10000, builder.build());
        }
    }

    private void handleNewMessage(String payload) {
        try {
            JsonObject json = gson.fromJson(payload, JsonObject.class);
            String eventType = json.get("eventType").getAsString();
            if (!"NEW_MESSAGE".equals(eventType)) return;

            long roomId = json.get("roomId").getAsLong();
            JsonObject msgJson = json.getAsJsonObject("message");
            String messageType = msgJson.get("messageType").getAsString();

            if ("SYSTEM".equals(messageType)) return;

            // Check if user is currently in this room
            if (currentOpenRoomId != null && currentOpenRoomId == roomId) {
                Log.d(TAG, "User is in room " + roomId + ", skipping notification");
                return;
            }

            String senderName = msgJson.get("senderName").getAsString();
            String content = msgJson.get("message").getAsString();

            showNewMessageNotification(roomId, senderName, content);

            // Notify app components if they are alive (Local broadcast or EventBus)
            Intent intent = new Intent(BROADCAST_CHAT_EVENT);
            intent.putExtra("payload", payload);
            sendBroadcast(intent);

        } catch (Exception e) {
            Log.e(TAG, "Error parsing websocket message", e);
        }
    }

    private void showNewMessageNotification(long roomId, String sender, String content) {
        Intent intent = new Intent(this, MessageActivity.class);
        intent.putExtra("roomId", roomId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, (int) roomId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, MSG_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(sender)
                .setContentText(content)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setDefaults(Notification.DEFAULT_ALL)
                .setVibrate(new long[]{0, 500, 200, 500})
                .setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify((int) roomId, builder.build());
    }

    private void scheduleReconnect() {
        if (mStompClient != null && mStompClient.isConnected()) return;
        
        reconnectHandler.postDelayed(this::initChat, 5000);
    }

    private Notification createForegroundNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("HRM App")
                .setContentText("Đang kết nối để nhận thông báo...")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void updateForegroundNotification(String text) {
        Notification notification = createForegroundNotification();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("HRM App")
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_LOW);
        
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIF_ID, builder.build());
        }
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID, "Dịch vụ thông báo", NotificationManager.IMPORTANCE_LOW);
            
            NotificationChannel msgChannel = new NotificationChannel(
                    MSG_CHANNEL_ID, "Tin nhắn mới", NotificationManager.IMPORTANCE_HIGH);
            msgChannel.enableVibration(true);
            msgChannel.setVibrationPattern(new long[]{0, 500, 200, 500});
            msgChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            
            NotificationChannel systemChannel = new NotificationChannel(
                    SYSTEM_NOTI_CHANNEL_ID, "Thông báo hệ thống", NotificationManager.IMPORTANCE_HIGH);
            systemChannel.enableVibration(true);
            systemChannel.setVibrationPattern(new long[]{0, 500, 200, 500});
            systemChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
                manager.createNotificationChannel(msgChannel);
                manager.createNotificationChannel(systemChannel);
            }
        }
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        if (mStompClient != null) mStompClient.disconnect();
        if (compositeDisposable != null) compositeDisposable.dispose();
        reconnectHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
