package com.example.myapplication.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.ChatRoom;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private Context context;
    private List<ChatRoom> roomList;
    private OnRoomClickListener listener;
    private Long currentUserId;

    public interface OnRoomClickListener {
        void onRoomSelected(ChatRoom room);
    }

    public ChatAdapter(Context context, List<ChatRoom> roomList, Long currentUserId, OnRoomClickListener listener) {
        this.context = context;
        this.roomList = roomList;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void setRoomList(List<ChatRoom> roomList) {
        this.roomList = roomList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_item_chat_room, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatRoom room = roomList.get(position);
        
        String name = room.getName();
        if ("PRIVATE".equals(room.getType()) && room.getOtherParticipantName() != null) {
            name = room.getOtherParticipantName();
        } else if (name == null) {
            name = "Phòng chat " + room.getId();
        }
        
        holder.tvRoomName.setText(name);
        
        // Last message & Time
        String lastMsg = room.getLastMessage() != null ? room.getLastMessage() : "Chưa có tin nhắn";
        holder.tvLastMessage.setText(lastMsg);
        
        String time = room.getLastMessageTime() != null ? room.getLastMessageTime() : "";
        holder.tvTime.setText(time);
        
        // Unread badge
        if (room.getUnreadCount() > 0) {
            holder.tvUnreadBadge.setVisibility(View.VISIBLE);
            holder.tvUnreadBadge.setText(String.valueOf(room.getUnreadCount()));
        } else {
            holder.tvUnreadBadge.setVisibility(View.GONE);
        }
        
        // Avatar logic
        String firstChar = name.isEmpty() ? "?" : String.valueOf(name.charAt(0)).toUpperCase();
        holder.tvAvatar.setText(firstChar);
        
        if ("DEPARTMENT".equals(room.getType())) {
            holder.viewAvatarBg.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#DBEAFE")));
            holder.tvAvatar.setTextColor(Color.parseColor("#2563EB"));
            holder.presenceIndicator.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#10B981")));
        } else {
            holder.viewAvatarBg.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FCE7F3")));
            holder.tvAvatar.setTextColor(Color.parseColor("#DB2777"));
            holder.presenceIndicator.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F59E0B")));
        }

        holder.itemView.setOnClickListener(v -> listener.onRoomSelected(room));
    }

    @Override
    public int getItemCount() {
        return roomList == null ? 0 : roomList.size();
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoomName, tvLastMessage, tvTime, tvUnreadBadge, tvAvatar;
        View viewAvatarBg, presenceIndicator;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoomName = itemView.findViewById(R.id.tvRoomNameItem);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessagePreview);
            tvTime = itemView.findViewById(R.id.tvLastMessageTime);
            tvUnreadBadge = itemView.findViewById(R.id.tvUnreadCountBadge);
            tvAvatar = itemView.findViewById(R.id.tvAvatarText);
            viewAvatarBg = itemView.findViewById(R.id.viewAvatarBg);
            presenceIndicator = itemView.findViewById(R.id.presenceIndicator);
        }
    }
}
