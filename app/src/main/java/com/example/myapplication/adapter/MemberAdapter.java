package com.example.myapplication.adapter;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.RoomMember;
import com.google.android.material.chip.Chip;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {

    public interface OnRemoveClickListener {
        void onRemove(RoomMember member);
    }

    private final List<RoomMember> members = new ArrayList<>();
    private final OnRemoveClickListener removeClickListener;
    private String myRole = "MEMBER";
    private boolean allowManageMembers = true;

    public MemberAdapter(OnRemoveClickListener removeClickListener) {
        this.removeClickListener = removeClickListener;
    }

    public void submitList(List<RoomMember> list, String myRole, boolean allowManageMembers) {
        members.clear();
        if (list != null) {
            members.addAll(list);
        }
        this.myRole = myRole;
        this.allowManageMembers = allowManageMembers;
        notifyDataSetChanged();
    }

    public void submitList(List<RoomMember> list, String myRole) {
        submitList(list, myRole, true);
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        RoomMember member = members.get(position);
        String displayName = member.getDisplayName();
        holder.tvUsername.setText(displayName);

        String positionText = member.getPosition();
        String deptText = member.getDepartmentName();
        if ((positionText != null && !positionText.isEmpty()) || (deptText != null && !deptText.isEmpty())) {
            if (positionText == null) positionText = "";
            if (deptText == null) deptText = "";
            String separator = (!positionText.isEmpty() && !deptText.isEmpty()) ? " • " : "";
            holder.tvMeta.setText(positionText + separator + deptText);
            holder.tvMeta.setVisibility(View.VISIBLE);
        } else {
            holder.tvMeta.setText("");
            holder.tvMeta.setVisibility(View.GONE);
        }

        // Hiển thị Role bằng Chip
        String role = member.getRole() != null ? member.getRole().toUpperCase() : "MEMBER";
        holder.chipRole.setText(role);
        if ("ADMIN".equals(role)) {
            holder.chipRole.setChipBackgroundColorResource(android.R.color.holo_orange_light);
            holder.chipRole.setTextColor(Color.WHITE);
        } else {
            holder.chipRole.setChipBackgroundColorResource(R.color.gray_50);
            holder.chipRole.setTextColor(Color.DKGRAY);
        }

        // Hiển thị chữ cái đầu nếu không có ảnh
        holder.tvInitial.setText(String.valueOf(displayName.charAt(0)).toUpperCase());

        // Logic ẩn/hiện nút xóa
        boolean canRemove = allowManageMembers && "ADMIN".equalsIgnoreCase(myRole) && !"ADMIN".equalsIgnoreCase(role);
        holder.btnRemove.setVisibility(canRemove ? View.VISIBLE : View.GONE);
        holder.btnRemove.setOnClickListener(v -> {
            if (removeClickListener != null) {
                removeClickListener.onRemove(member);
            }
        });
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        final ShapeableImageView ivAvatar;
        final TextView tvUsername;
        final TextView tvMeta;
        final Chip chipRole;
        final ImageButton btnRemove;
        final TextView tvInitial;

        MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            chipRole = itemView.findViewById(R.id.chipRole);
            btnRemove = itemView.findViewById(R.id.btnRemove);

            // Tạo TextView hiển thị chữ cái đầu đè lên Avatar
            tvInitial = new TextView(itemView.getContext());
            tvInitial.setTextColor(Color.WHITE);
            tvInitial.setTypeface(null, Typeface.BOLD);
            tvInitial.setGravity(Gravity.CENTER);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
            tvInitial.setLayoutParams(lp);

            // Tìm container chứa avatar để add tvInitial vào (Giả sử ivAvatar nằm trong FrameLayout)
            if (ivAvatar.getParent() instanceof FrameLayout) {
                ((FrameLayout) ivAvatar.getParent()).addView(tvInitial);
            }
        }
    }
}
