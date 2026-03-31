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
import com.example.myapplication.model.Notification;
import com.google.android.material.chip.Chip;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private Context context;
    private List<Notification> notificationList;
    private OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    public NotificationAdapter(Context context, List<Notification> notificationList, OnNotificationClickListener listener) {
        this.context = context;
        this.notificationList = notificationList;
        this.listener = listener;
    }

    public void setNotificationList(List<Notification> notificationList) {
        this.notificationList = notificationList;
        notifyDataSetChanged();
    }

    // Alias được dùng bởi NotificationActivity
    public void setNotiList(List<Notification> notificationList) {
        setNotificationList(notificationList);
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notificationList.get(position);
        
        holder.tvTitle.setText(notification.getTitle());
        holder.tvMessage.setText(notification.getContent());
        holder.tvTime.setText(formatDate(notification.getCreatedAt()));

        // Unread indicator via background tint and bold text
        if (!notification.isRead()) {
            holder.cardNotification.setCardBackgroundColor(Color.parseColor("#EFF6FF")); // Light blue
            holder.tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            holder.tvMessage.setTypeface(null, android.graphics.Typeface.BOLD);
            holder.tvMessage.setTextColor(Color.parseColor("#111827"));
        } else {
            holder.cardNotification.setCardBackgroundColor(Color.WHITE);
            holder.tvTitle.setTypeface(null, android.graphics.Typeface.NORMAL);
            holder.tvMessage.setTypeface(null, android.graphics.Typeface.NORMAL);
            holder.tvMessage.setTextColor(Color.parseColor("#6B7280"));
        }

        // Category Badge styling
        String targetType = notification.getTargetType();
        if (targetType != null) {
            holder.chipCategory.setVisibility(View.VISIBLE);
            holder.chipCategory.setText(getTranslatedTargetType(targetType));
            
            if ("COMPANY".equals(targetType)) {
                holder.chipCategory.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#1565C0")));
                holder.chipCategory.setTextColor(Color.WHITE);
                holder.chipCategory.setChipStrokeWidth(0);
            } else if ("DEPARTMENT".equals(targetType)) {
                holder.chipCategory.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#2E7D32")));
                holder.chipCategory.setTextColor(Color.WHITE);
                holder.chipCategory.setChipStrokeWidth(0);
            } else {
                holder.chipCategory.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#616161")));
                holder.chipCategory.setTextColor(Color.WHITE);
                holder.chipCategory.setChipStrokeWidth(0);
            }
        } else {
            holder.chipCategory.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationClick(notification);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notificationList != null ? notificationList.size() : 0;
    }

    private String formatDate(String isoString) {
        if (isoString == null || isoString.isEmpty()) return "";
        try {
            // ISO 8601 format: 2026-03-30T07:30:31
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = isoFormat.parse(isoString);
            
            SimpleDateFormat displayFormat = new SimpleDateFormat("HH:mm dd/MM", Locale.getDefault());
            return displayFormat.format(date);
        } catch (ParseException e) {
            return isoString; // Trả về chuỗi gốc nếu lỗi
        }
    }

    private String getTranslatedTargetType(String type) {
        if ("COMPANY".equals(type)) return "Công ty";
        if ("DEPARTMENT".equals(type)) return "Phòng ban";
        if ("INDIVIDUAL".equals(type)) return "Cá nhân";
        if ("SPECIFIC_USERS".equals(type)) return "Cá nhân";
        return type;
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        View iconBackground;
        com.google.android.material.card.MaterialCardView cardNotification;
        TextView tvTitle, tvMessage, tvTime;
        Chip chipCategory;
        android.widget.ImageView ivIcon;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            cardNotification = itemView.findViewById(R.id.cardNotification);
            iconBackground = itemView.findViewById(R.id.iconBackground);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            chipCategory = itemView.findViewById(R.id.chipCategory);
            ivIcon = itemView.findViewById(R.id.ivIcon);
        }
    }
}
