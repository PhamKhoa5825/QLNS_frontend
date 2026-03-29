package com.example.myapplication.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.ChatRoom;
import com.google.android.material.imageview.ShapeableImageView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class RoomListAdapter extends RecyclerView.Adapter<RoomListAdapter.RoomViewHolder> {

    public interface OnRoomClickListener {
        void onRoomClick(ChatRoom room);
    }

    private final List<ChatRoom> rooms = new ArrayList<>();
    private final OnRoomClickListener listener;
    private Context context;

    public RoomListAdapter(OnRoomClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<ChatRoom> newRooms) {
        rooms.clear();
        if (newRooms != null) {
            rooms.addAll(newRooms);
        }
        notifyDataSetChanged();
    }

    public void addRoomToTop(ChatRoom room) {
        if (room == null) {
            return;
        }
        rooms.add(0, room);
        notifyItemInserted(0);
    }

    @NonNull
    @Override
    public RoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (context == null) {
            context = parent.getContext();
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_room, parent, false);
        return new RoomViewHolder(view);
    }

    private int lastAnimatedPosition = -1;

    @Override
    public void onBindViewHolder(@NonNull RoomViewHolder holder, int position) {
        ChatRoom room = rooms.get(position);

        String displayName = resolveRoomName(room);
        holder.tvRoomName.setText(displayName);

        String lastMessage = room.getLastMessage() != null
                ? room.getLastMessage()
                : holder.itemView.getContext().getString(R.string.chat_empty_message_placeholder);
        holder.tvLastMessage.setText(lastMessage);

        String time = formatRoomTime(room.getLastMessageTime(), holder.itemView.getContext().getString(R.string.time_just_now));
        holder.tvTime.setText(time);

        String type = room.getType() != null ? room.getType() : "CHAT";

        if (room.getUnreadCount() > 0) {
            holder.tvUnread.setVisibility(View.VISIBLE);
            holder.tvUnread.setText(String.valueOf(room.getUnreadCount()));
        } else {
            holder.tvUnread.setVisibility(View.GONE);
        }

        if ("DEPARTMENT".equalsIgnoreCase(type)) {
            holder.ivAvatar.setImageResource(R.drawable.ic_room_department);
            holder.ivAvatar.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#DBEAFE")));
            holder.ivAvatar.setImageTintList(ColorStateList.valueOf(Color.parseColor("#2563EB")));
            holder.tvType.setText(holder.itemView.getContext().getString(R.string.chat_type_department));
        } else if ("GROUP".equalsIgnoreCase(type)) {
            holder.ivAvatar.setImageResource(R.drawable.ic_room_group);
            holder.ivAvatar.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0F2FE")));
            holder.ivAvatar.setImageTintList(ColorStateList.valueOf(Color.parseColor("#0284C7")));
            holder.tvType.setText(holder.itemView.getContext().getString(R.string.chat_type_group));
        } else {
            holder.ivAvatar.setImageResource(R.drawable.ic_room_private);
            holder.ivAvatar.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FCE7F3")));
            holder.ivAvatar.setImageTintList(ColorStateList.valueOf(Color.parseColor("#DB2777")));
            holder.tvType.setText(holder.itemView.getContext().getString(R.string.chat_type_private));
        }

        holder.itemView.setOnClickListener(v -> listener.onRoomClick(room));
        animateItemIn(holder.itemView, position);
    }

    private void animateItemIn(View itemView, int position) {
        if (position <= lastAnimatedPosition) {
            itemView.setAlpha(1f);
            itemView.setTranslationY(0f);
            return;
        }
        itemView.setAlpha(0f);
        itemView.setTranslationY(18f);
        itemView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(200)
                .start();
        lastAnimatedPosition = position;
    }

    private String formatRoomTime(String rawDate, String justNowLabel) {
        if (rawDate == null || rawDate.isEmpty()) return justNowLabel;
        try {
            String clean = normalizeDate(rawDate);
            java.text.SimpleDateFormat parser = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
            java.util.Date date = parser.parse(clean);
            if (date == null) return rawDate;

            java.util.Calendar now = java.util.Calendar.getInstance();
            java.util.Calendar target = java.util.Calendar.getInstance();
            target.setTime(date);

            if (now.get(java.util.Calendar.YEAR) == target.get(java.util.Calendar.YEAR) &&
                now.get(java.util.Calendar.DAY_OF_YEAR) == target.get(java.util.Calendar.DAY_OF_YEAR)) {
                return new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(date);
            }
            return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(date);
        } catch (Exception e) {
            try {
                if (rawDate.contains("T")) return rawDate.replace("T", " ");
            } catch (Exception ignored) {}
            return rawDate;
        }
    }

    private String normalizeDate(String raw) {
        String clean = raw.replace("T", " ");
        if (clean.contains(".")) clean = clean.split("\\.")[0];
        if (clean.contains("+")) clean = clean.split("\\+")[0];
        if (clean.endsWith("Z")) clean = clean.substring(0, clean.length() - 1);
        return clean.trim();
    }

    private String resolveRoomName(ChatRoom room) {
        String type = room.getType() != null ? room.getType() : "";
        String otherName = room.getOtherParticipantName();
        String name = room.getName();

        if ("PRIVATE".equalsIgnoreCase(type) && otherName != null && !otherName.isEmpty()) {
            return otherName;
        }
        if (name != null && !name.isEmpty()) {
            return name;
        }
        if (context != null) {
            return context.getString(R.string.chat_room_fallback, room != null && room.getId() != null ? room.getId() : 0L);
        }
        return room != null && room.getId() != null ? ("#" + room.getId()) : "#0";
    }

    @Override
    public int getItemCount() {
        return rooms.size();
    }

    static class RoomViewHolder extends RecyclerView.ViewHolder {
        final ShapeableImageView ivAvatar;
        final TextView tvRoomName;
        final TextView tvLastMessage;
        final TextView tvTime;
        final TextView tvUnread;
        final TextView tvType;

        RoomViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvRoomName = itemView.findViewById(R.id.tvRoomName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvUnread = itemView.findViewById(R.id.tvUnread);
            tvType = itemView.findViewById(R.id.tvType);
        }
    }
}
