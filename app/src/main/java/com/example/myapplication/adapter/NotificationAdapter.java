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

import java.util.List;

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
        holder.tvTime.setText(notification.getCreatedAt());

        // Unread indicator
        holder.unreadIndicator.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);

        // Category Badge
        String targetType = notification.getTargetType();
        if (targetType != null) {
            holder.chipCategory.setText(targetType);
            if ("COMPANY".equals(targetType)) {
                holder.chipCategory.setChipBackgroundColorResource(android.R.color.holo_blue_light);
                holder.chipCategory.setTextColor(Color.WHITE);
            } else if ("DEPARTMENT".equals(targetType)) {
                holder.chipCategory.setChipBackgroundColorResource(android.R.color.holo_green_light);
                holder.chipCategory.setTextColor(Color.WHITE);
            } else {
                holder.chipCategory.setChipBackgroundColorResource(android.R.color.darker_gray);
                holder.chipCategory.setTextColor(Color.WHITE);
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

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        View unreadIndicator, iconBackground;
        TextView tvTitle, tvMessage, tvTime;
        Chip chipCategory;
        android.widget.ImageView ivIcon;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            unreadIndicator = itemView.findViewById(R.id.unreadIndicator);
            iconBackground = itemView.findViewById(R.id.iconBackground);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            chipCategory = itemView.findViewById(R.id.chipCategory);
            ivIcon = itemView.findViewById(R.id.ivIcon);
        }
    }
}
