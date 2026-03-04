package com.example.myapplication;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<Notification> list;

    public NotificationAdapter(List<Notification> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification noti = list.get(position);

        holder.tvNotiTitle.setText(noti.getTitle());
        holder.tvNotiContent.setText(noti.getContent());
        holder.tvNotiTime.setText(noti.getTime());
        holder.tvNotiAction.setText(noti.getActionText());

        // Xử lý trạng thái Chưa đọc/Đã đọc
        if (noti.isUnread()) {
            holder.viewUnreadBar.setVisibility(View.VISIBLE);
            holder.viewUnreadDot.setVisibility(View.VISIBLE);
            holder.tvNotiTitle.setTextColor(Color.parseColor("#111827"));
        } else {
            holder.viewUnreadBar.setVisibility(View.GONE);
            holder.viewUnreadDot.setVisibility(View.GONE);
            holder.tvNotiTitle.setTextColor(Color.parseColor("#6B7280"));
        }

        // Xử lý Icon và Màu sắc dựa theo Type (Giống ảnh)
        int bgColor = Color.WHITE;
        int tintColor = Color.BLACK;
        int iconRes = android.R.drawable.ic_dialog_info;

        switch (noti.getType()) {
            case Notification.TYPE_TASK: // Xanh dương
                bgColor = Color.parseColor("#DBEAFE"); tintColor = Color.parseColor("#2563EB"); iconRes = android.R.drawable.ic_menu_agenda; break;
            case Notification.TYPE_MESSAGE: // Hồng
                bgColor = Color.parseColor("#FCE7F3"); tintColor = Color.parseColor("#DB2777"); iconRes = android.R.drawable.sym_action_chat; break;
            case Notification.TYPE_MEETING: // Cam
                bgColor = Color.parseColor("#FFEDD5"); tintColor = Color.parseColor("#EA580C"); iconRes = android.R.drawable.ic_menu_today; break;
            case Notification.TYPE_CHECKIN: // Xanh lá
                bgColor = Color.parseColor("#D1FAE5"); tintColor = Color.parseColor("#10B981"); iconRes = android.R.drawable.checkbox_on_background; break;
            case Notification.TYPE_HR: // Xanh nhạt
                bgColor = Color.parseColor("#E0F2FE"); tintColor = Color.parseColor("#0284C7"); iconRes = android.R.drawable.ic_dialog_info; break;
            case Notification.TYPE_WARNING: // Đỏ
                bgColor = Color.parseColor("#FEE2E2"); tintColor = Color.parseColor("#DC2626"); iconRes = android.R.drawable.ic_dialog_alert; break;
        }

        holder.imgNotiIcon.setBackgroundTintList(ColorStateList.valueOf(bgColor));
        holder.imgNotiIcon.setColorFilter(tintColor);
        holder.imgNotiIcon.setImageResource(iconRes);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNotiTitle, tvNotiContent, tvNotiTime, tvNotiAction;
        ImageView imgNotiIcon;
        View viewUnreadBar, viewUnreadDot;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNotiTitle = itemView.findViewById(R.id.tvNotiTitle);
            tvNotiContent = itemView.findViewById(R.id.tvNotiContent);
            tvNotiTime = itemView.findViewById(R.id.tvNotiTime);
            tvNotiAction = itemView.findViewById(R.id.tvNotiAction);
            imgNotiIcon = itemView.findViewById(R.id.imgNotiIcon);
            viewUnreadBar = itemView.findViewById(R.id.viewUnreadBar);
            viewUnreadDot = itemView.findViewById(R.id.viewUnreadDot);
        }
    }
}
