package com.example.myapplication.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Contact;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder> {

    public interface OnChatClickListener {
        void onChatClick(Contact contact);
    }

    private final List<Contact> contacts = new ArrayList<>();
    private final OnChatClickListener listener;

    private static final int[] AVATAR_BG_COLORS = new int[] {
            R.color.icon_bg_blue,
            R.color.icon_bg_green,
            R.color.icon_bg_amber,
            R.color.icon_bg_purple,
            R.color.blue_50
    };

    public ContactAdapter(OnChatClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Contact> items) {
        contacts.clear();
        if (items != null) {
            contacts.addAll(items);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        Contact contact = contacts.get(position);
        String fullName = contact.getFullName() != null ? contact.getFullName().trim() : "";
        if (fullName.isEmpty()) {
            fullName = holder.itemView.getContext().getString(R.string.chat_user_fallback);
        }
        holder.tvFullName.setText(fullName);
        holder.tvInitial.setText(String.valueOf(Character.toUpperCase(fullName.charAt(0))));

        int colorIndex = resolveAvatarColorIndex(contact, fullName);
        int bgColor = ContextCompat.getColor(holder.itemView.getContext(), AVATAR_BG_COLORS[colorIndex]);
        holder.tvInitial.setBackgroundTintList(ColorStateList.valueOf(bgColor));
        holder.tvInitial.setTextColor(getReadableTextColor(bgColor));

        String positionDept = (contact.getPosition() != null ? contact.getPosition() : "") +
                " • " + (contact.getDepartmentName() != null ? contact.getDepartmentName() : "");
        holder.tvPositionDept.setText(positionDept);

        View.OnClickListener openChat = v -> {
            animatePress(holder.itemView);
            listener.onChatClick(contact);
        };
        holder.itemView.setOnClickListener(openChat);
        holder.btnChat.setOnClickListener(openChat);
    }

    private int resolveAvatarColorIndex(Contact contact, String fallbackName) {
        int seed;
        if (contact.getUserId() != null) {
            seed = contact.getUserId().hashCode();
        } else {
            seed = fallbackName != null ? fallbackName.hashCode() : 0;
        }
        return Math.abs(seed) % AVATAR_BG_COLORS.length;
    }

    private int getReadableTextColor(int backgroundColor) {
        double luminance = (0.299 * Color.red(backgroundColor)
                + 0.587 * Color.green(backgroundColor)
                + 0.114 * Color.blue(backgroundColor)) / 255d;
        return luminance > 0.62 ? Color.parseColor("#1F2937") : Color.WHITE;
    }

    private void animatePress(View view) {
        view.animate().cancel();
        view.animate()
                .scaleX(0.975f)
                .scaleY(0.975f)
                .setDuration(70)
                .withEndAction(() -> view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(130)
                        .start())
                .start();
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    static class ContactViewHolder extends RecyclerView.ViewHolder {
        final ShapeableImageView ivAvatar;
        final TextView tvInitial;
        final TextView tvFullName;
        final TextView tvPositionDept;
        final MaterialButton btnChat;

        ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvInitial = itemView.findViewById(R.id.tvInitial);
            tvFullName = itemView.findViewById(R.id.tvFullName);
            tvPositionDept = itemView.findViewById(R.id.tvPositionDept);
            btnChat = itemView.findViewById(R.id.btnChat);
        }
    }
}
