package com.example.myapplication.network;

import com.example.myapplication.model.ChatRoom;
import com.example.myapplication.model.Contact;
import com.example.myapplication.model.CreateGroupRequest;
import com.example.myapplication.model.Message;
import com.example.myapplication.model.SendMessageRequest;
import com.example.myapplication.model.RoomMember;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Multipart;
import retrofit2.http.Part;

public interface ChatApiService {

    // [Chat] API lấy danh sách phòng chat theo userId.
    @GET("api/chat/rooms/me")
    Call<List<ChatRoom>> getRooms();

    // [Chat] API tìm kiếm đồng nghiệp để chat (lọc nâng cao theo backend).
    @GET("api/chat/contacts")
    Call<List<Contact>> searchContacts(
            @Query("keyword") String keyword,
            @Query("skill") String skill,
            @Query("position") String position,
            @Query("status") String status,
            @Query("roomId") Long roomId
    );

    // [Chat] API tạo phòng chat riêng giữa 2 người.
    @POST("api/chat/rooms/private")
    Call<ChatRoom> createPrivateRoom(@Query("targetUserId") Long targetUserId);

    // [Chat] API tạo nhóm chat từ danh sách member truyền trong body.
    @POST("api/chat/rooms/group")
    Call<ChatRoom> createGroupRoom(@Body CreateGroupRequest request);

    // [Chat] API lấy danh sách tin nhắn của phòng.
    @GET("api/chat/messages/{roomId}")
    Call<List<Message>> getMessages(@Path("roomId") Long roomId);

    // [Chat] API tìm kiếm tin nhắn trong phòng theo keyword.
    @GET("api/chat/messages/{roomId}/search")
    Call<List<Message>> searchMessages(@Path("roomId") Long roomId, @Query("keyword") String keyword);

    // [Chat] API lấy chi tiết một tin nhắn (dùng để fetch tin gốc khi có replyToId).
    @GET("api/chat/messages/detail/{messageId}")
    Call<Message> getMessageDetail(@Path("messageId") Long messageId);

    // [Chat] API gửi tin nhắn mới.
    @POST("api/chat/messages")
    Call<Message> sendMessage(@Body SendMessageRequest request);

    // [Chat] API lấy danh sách thành viên của phòng.
    @GET("api/chat/rooms/{roomId}/members")
    Call<List<RoomMember>> getRoomMembers(@Path("roomId") Long roomId);

    // [Chat] API thêm thành viên vào phòng.
    @POST("api/chat/rooms/{roomId}/members")
    Call<Void> addMembers(@Path("roomId") Long roomId, @Body java.util.Map<String, Object> body);

    // [Chat] API xóa thành viên khỏi phòng.
    @DELETE("api/chat/rooms/{roomId}/members/{userId}")
    Call<Void> removeMember(@Path("roomId") Long roomId, @Path("userId") Long userId);

    // [Chat] API đổi tên phòng chat.
    @PUT("api/chat/rooms/{roomId}/name")
    Call<ChatRoom> renameRoom(@Path("roomId") Long roomId, @Body java.util.Map<String, Object> body);

    // [Chat] API thu hồi tin nhắn đã gửi.
    @PUT("api/chat/messages/{messageId}/recall")
    Call<Void> recallMessage(@Path("messageId") Long messageId);

    // [Chat] API upload file/ảnh cho phòng chat.
    @POST("api/chat/upload")
    @Multipart
    Call<Message> uploadFile(
            @Part("roomId") RequestBody roomId,
            @Part MultipartBody.Part file,
            @Part("messageType") RequestBody messageType
    );

    // [Chat] API đánh dấu phòng đã được xem.
    @POST("api/chat/rooms/{roomId}/seen")
    Call<Void> markRoomSeen(@Path("roomId") Long roomId, @Query("lastSeenMessageId") Long lastSeenMessageId);
}
