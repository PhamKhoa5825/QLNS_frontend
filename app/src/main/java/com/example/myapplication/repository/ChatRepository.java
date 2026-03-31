package com.example.myapplication.repository;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.model.ChatRoom;
import com.example.myapplication.model.Contact;
import com.example.myapplication.model.CreateGroupRequest;
import com.example.myapplication.model.Message;
import com.example.myapplication.model.RoomMember;
import com.example.myapplication.network.ApiClient;
import com.example.myapplication.network.ChatApiService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatRepository {

    private final ChatApiService chatApiService;

    public ChatRepository(Context context) {
        // [Chat] Repository dùng ApiClient để lấy Retrofit service theo đúng base URL và token hiện tại.
        this.chatApiService = ApiClient.getService(context, ChatApiService.class);
    }

    public LiveData<List<ChatRoom>> getRooms() {
        MutableLiveData<List<ChatRoom>> roomsLiveData = new MutableLiveData<>();

        // [Chat] Gọi API lấy danh sách phòng chat của chính mình.
        chatApiService.getRooms().enqueue(new Callback<List<ChatRoom>>() {
            @Override
            public void onResponse(Call<List<ChatRoom>> call, Response<List<ChatRoom>> response) {
                if (response.isSuccessful()) {
                    roomsLiveData.setValue(response.body());
                } else {
                    Log.e("ChatRepository", "getRooms failed code=" + response.code());
                    roomsLiveData.setValue(null);
                }
            }

            @Override
            public void onFailure(Call<List<ChatRoom>> call, Throwable t) {
                // [Chat] Lỗi mạng/timeout thì đẩy null để ViewModel và View hiển thị trạng thái lỗi.
                Log.e("ChatRepository", "getRooms error", t);
                roomsLiveData.setValue(null);
            }
        });

        return roomsLiveData;
    }

    public void markRoomSeen(Long roomId, Long lastSeenMessageId) {
        if (roomId == null || lastSeenMessageId == null) {
            return;
        }
        chatApiService.markRoomSeen(roomId, lastSeenMessageId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                // no-op; backend updates seen status
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.w("ChatRepository", "markRoomSeen failed", t);
            }
        });
    }

    public LiveData<List<Message>> getMessages(Long roomId) {
        MutableLiveData<List<Message>> messagesLiveData = new MutableLiveData<>();

        chatApiService.getMessages(roomId).enqueue(new Callback<List<Message>>() {
            @Override
            public void onResponse(Call<List<Message>> call, Response<List<Message>> response) {
                if (response.isSuccessful()) {
                    messagesLiveData.setValue(response.body());
                } else {
                    messagesLiveData.setValue(null);
                }
            }

            @Override
            public void onFailure(Call<List<Message>> call, Throwable t) {
                messagesLiveData.setValue(null);
            }
        });

        return messagesLiveData;
    }

    public LiveData<List<Message>> searchMessages(Long roomId, String keyword) {
        MutableLiveData<List<Message>> results = new MutableLiveData<>();

        chatApiService.searchMessages(roomId, keyword).enqueue(new Callback<List<Message>>() {
            @Override
            public void onResponse(Call<List<Message>> call, Response<List<Message>> response) {
                if (response.isSuccessful()) {
                    results.setValue(response.body());
                } else {
                    results.setValue(null);
                }
            }

            @Override
            public void onFailure(Call<List<Message>> call, Throwable t) {
                results.setValue(null);
            }
        });

        return results;
    }

    public LiveData<List<Contact>> searchContacts(String keyword, String skill, String position, String status, Long roomId) {
        MutableLiveData<List<Contact>> contactsLiveData = new MutableLiveData<>();

        // [Chat] Gọi API tìm đồng nghiệp theo keyword + bộ lọc backend hỗ trợ.
        chatApiService.searchContacts(keyword, skill, position, status, roomId).enqueue(new Callback<List<Contact>>() {
            @Override
            public void onResponse(Call<List<Contact>> call, Response<List<Contact>> response) {
                if (response.isSuccessful()) {
                    contactsLiveData.setValue(response.body());
                } else {
                    contactsLiveData.setValue(null);
                }
            }

            @Override
            public void onFailure(Call<List<Contact>> call, Throwable t) {
                // [Chat] Lỗi kết nối trả null để View hiển thị trạng thái lỗi.
                contactsLiveData.setValue(null);
            }
        });

        return contactsLiveData;
    }

    public LiveData<List<Contact>> searchContacts(String keyword, String skill, String position, String status) {
        return searchContacts(keyword, skill, position, status, null);
    }

    public LiveData<List<Contact>> searchContacts(String keyword, Long roomId) {
        return searchContacts(keyword, null, null, null, roomId);
    }

    public LiveData<List<Contact>> searchContacts(String keyword) {
        return searchContacts(keyword, null, null, null, null);
    }

    public LiveData<ChatRoom> createPrivateRoom(Long targetUserId) {
        MutableLiveData<ChatRoom> roomLiveData = new MutableLiveData<>();

        // [Chat] Gọi API tạo phòng chat riêng; backend tự lấy user hiện tại từ token.
        chatApiService.createPrivateRoom(targetUserId).enqueue(new Callback<ChatRoom>() {
            @Override
            public void onResponse(Call<ChatRoom> call, Response<ChatRoom> response) {
                if (response.isSuccessful()) {
                    roomLiveData.setValue(response.body());
                } else {
                    roomLiveData.setValue(null);
                }
            }

            @Override
            public void onFailure(Call<ChatRoom> call, Throwable t) {
                // [Chat] Lỗi kết nối trả null để Activity xử lý thông báo lỗi.
                roomLiveData.setValue(null);
            }
        });

        return roomLiveData;
    }

    public LiveData<ChatRoom> createGroupRoom(CreateGroupRequest request) {
        MutableLiveData<ChatRoom> roomLiveData = new MutableLiveData<>();

        // [Chat] Gọi API tạo nhóm chat với danh sách thành viên đã chọn.
        chatApiService.createGroupRoom(request).enqueue(new Callback<ChatRoom>() {
            @Override
            public void onResponse(Call<ChatRoom> call, Response<ChatRoom> response) {
                if (response.isSuccessful()) {
                    roomLiveData.setValue(response.body());
                } else {
                    roomLiveData.setValue(null);
                }
            }

            @Override
            public void onFailure(Call<ChatRoom> call, Throwable t) {
                // [Chat] Lỗi kết nối -> trả null để ViewModel/Activity hiện thông báo.
                roomLiveData.setValue(null);
            }
        });

        return roomLiveData;
    }

    public LiveData<List<RoomMember>> getMembers(Long roomId) {
        MutableLiveData<List<RoomMember>> data = new MutableLiveData<>();
        // [Chat] Lấy danh sách thành viên của phòng.
        chatApiService.getRoomMembers(roomId).enqueue(new Callback<List<RoomMember>>() {
            @Override
            public void onResponse(Call<List<RoomMember>> call, Response<List<RoomMember>> response) {
                if (response.isSuccessful()) {
                    data.setValue(response.body());
                } else {
                    data.setValue(null);
                }
            }

            @Override
            public void onFailure(Call<List<RoomMember>> call, Throwable t) {
                // [Chat] Lỗi mạng trả null để UI hiển thị lỗi.
                data.setValue(null);
            }
        });
        return data;
    }

    public LiveData<Boolean> addMembers(Long roomId, List<Long> memberIds) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("memberUserIds", memberIds);

        // [Chat] Thêm thành viên mới vào phòng.
        chatApiService.addMembers(roomId, body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                result.setValue(response.isSuccessful());
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                result.setValue(false);
            }
        });
        return result;
    }

    public LiveData<Boolean> removeMember(Long roomId, Long userId) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();

        // [Chat] Xóa thành viên khỏi phòng (backend biết ai thực hiện qua token).
        chatApiService.removeMember(roomId, userId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                result.setValue(response.isSuccessful());
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                result.setValue(false);
            }
        });
        return result;
    }

    public LiveData<ChatRoom> renameRoom(Long roomId, String name) {
        MutableLiveData<ChatRoom> result = new MutableLiveData<>();
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("name", name);

        // [Chat] Đổi tên phòng chat.
        chatApiService.renameRoom(roomId, body).enqueue(new Callback<ChatRoom>() {
            @Override
            public void onResponse(Call<ChatRoom> call, Response<ChatRoom> response) {
                if (response.isSuccessful()) {
                    result.setValue(response.body());
                } else {
                    result.setValue(null);
                }
            }

            @Override
            public void onFailure(Call<ChatRoom> call, Throwable t) {
                result.setValue(null);
            }
        });
        return result;
    }

    public static class UploadResult {
        private final Message message;
        private final int httpCode;
        private final String errorMessage;

        public UploadResult(Message message, int httpCode, String errorMessage) {
            this.message = message;
            this.httpCode = httpCode;
            this.errorMessage = errorMessage;
        }

        public Message getMessage() {
            return message;
        }

        public int getHttpCode() {
            return httpCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public boolean isSuccess() {
            return message != null;
        }
    }

    public LiveData<UploadResult> uploadFile(Long roomId, Uri fileUri, String messageType, Context ctx) {
        MutableLiveData<UploadResult> liveData = new MutableLiveData<>();
        try {
            String resolvedType = messageType != null ? messageType.toUpperCase() : "FILE";
            String originalName = resolveFileName(ctx, fileUri);
            String mime = ctx.getContentResolver().getType(fileUri);
            if (mime == null || mime.trim().isEmpty()) {
                mime = "application/octet-stream";
            }

            byte[] bytes;
            String uploadName = originalName;
            if ("IMAGE".equals(resolvedType)) {
                bytes = compressImageUnder1Mb(ctx, fileUri);
                if (uploadName == null || uploadName.trim().isEmpty()) {
                    uploadName = "image.jpg";
                } else if (!uploadName.toLowerCase().endsWith(".jpg") && !uploadName.toLowerCase().endsWith(".jpeg")) {
                    uploadName = uploadName + ".jpg";
                }
                mime = "image/jpeg";
            } else {
                bytes = readBytes(ctx, fileUri);
                if (uploadName == null || uploadName.trim().isEmpty()) {
                    uploadName = "upload";
                }
            }

            MediaType mediaType = MediaType.parse(mime);
            RequestBody fileBody = RequestBody.create(mediaType, bytes);
            MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", uploadName, fileBody);

            RequestBody roomPart = RequestBody.create(null, String.valueOf(roomId));
            RequestBody typePart = RequestBody.create(null, resolvedType);

            // [Chat] Lưu bản sao ở bộ nhớ trong trước khi upload để có thể hiển thị local ngay lập tức hoặc offline.
            com.example.myapplication.utils.MessageFileManager.saveToLocal(ctx, fileUri, uploadName);

            Call<Message> call = chatApiService.uploadFile(roomPart, filePart, typePart);
            enqueueUploadCall(call, liveData, 0);
        } catch (IOException e) {
            liveData.setValue(new UploadResult(null, -1, "IO Error: " + e.getMessage()));
        }
        return liveData;
    }

    private void enqueueUploadCall(Call<Message> call, MutableLiveData<UploadResult> liveData, int retryCount) {
        call.enqueue(new Callback<Message>() {
            @Override
            public void onResponse(Call<Message> c, Response<Message> response) {
                if (response.isSuccessful() && response.body() != null) {
                    liveData.setValue(new UploadResult(response.body(), response.code(), null));
                    return;
                }
                
                String errorMsg = mapUploadErrorMessage(response.code());
                Log.e("ChatRepository", "Upload failed: code=" + response.code() + " msg=" + response.message());
                liveData.setValue(new UploadResult(null, response.code(), errorMsg));
            }

            @Override
            public void onFailure(Call<Message> c, Throwable t) {
                String msg = (t != null) ? t.toString() : "Unknown network error";
                Log.e("ChatRepository", "Upload network failure: " + msg, t);
                liveData.setValue(new UploadResult(null, -1, msg));
            }
        });
    }

    private String mapUploadErrorMessage(int code) {
        if (code == 400) return "Sai messageType hoặc thiếu dữ liệu";
        if (code == 401) return "Phiên đăng nhập hết hạn";
        if (code == 413) return "File quá lớn";
        return "Upload thất bại";
    }

    private String resolveFileName(Context ctx, Uri uri) {
        String fallback = "upload";
        if (uri == null) return fallback;
        try (android.database.Cursor cursor = ctx.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    String name = cursor.getString(index);
                    if (name != null && !name.trim().isEmpty()) {
                        return name;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private byte[] compressImageUnder1Mb(Context ctx, Uri uri) throws IOException {
        byte[] original = readBytes(ctx, uri);
        Bitmap bitmap = BitmapFactory.decodeByteArray(original, 0, original.length);
        if (bitmap == null) {
            return original;
        }

        int maxBytes = 1024 * 1024;
        int quality = 90;
        Bitmap working = bitmap;
        byte[] compressed = compressBitmap(working, quality);

        while (compressed.length > maxBytes && quality > 40) {
            quality -= 10;
            compressed = compressBitmap(working, quality);
        }

        // Nếu vẫn >1MB thì giảm kích thước ảnh từng bước và nén lại.
        while (compressed.length > maxBytes && working.getWidth() > 300 && working.getHeight() > 300) {
            int newWidth = (int) (working.getWidth() * 0.85f);
            int newHeight = (int) (working.getHeight() * 0.85f);
            Bitmap scaled = Bitmap.createScaledBitmap(working, newWidth, newHeight, true);
            if (working != bitmap) {
                working.recycle();
            }
            working = scaled;
            quality = 85;
            compressed = compressBitmap(working, quality);
            while (compressed.length > maxBytes && quality > 40) {
                quality -= 10;
                compressed = compressBitmap(working, quality);
            }
        }

        if (working != bitmap) {
            working.recycle();
        }
        bitmap.recycle();
        return compressed;
    }

    private byte[] compressBitmap(Bitmap bitmap, int quality) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            return baos.toByteArray();
        }
    }

    private byte[] readBytes(Context ctx, Uri uri) throws IOException {
        try (InputStream is = ctx.getContentResolver().openInputStream(uri);
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] data = new byte[4096];
            int nRead;
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            return buffer.toByteArray();
        }
    }

    public LiveData<Boolean> recallMessageFallback(Long messageId) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        chatApiService.recallMessage(messageId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                result.setValue(response.isSuccessful());
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                result.setValue(false);
            }
        });
        return result;
    }

    public LiveData<Message> getMessageDetail(Long messageId) {
        MutableLiveData<Message> data = new MutableLiveData<>();
        chatApiService.getMessageDetail(messageId).enqueue(new Callback<Message>() {
            @Override
            public void onResponse(Call<Message> call, Response<Message> response) {
                if (response.isSuccessful()) {
                    data.setValue(response.body());
                } else {
                    data.setValue(null);
                }
            }

            @Override
            public void onFailure(Call<Message> call, Throwable t) {
                data.setValue(null);
            }
        });
        return data;
    }
}
