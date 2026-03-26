package com.example.myapplication.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Contact;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ContactCheckAdapter extends RecyclerView.Adapter<ContactCheckAdapter.ContactCheckViewHolder> {

    public interface OnSelectionChanged {
        void onChanged(Set<Long> selectedIds);
    }

    private final List<Contact> contacts = new ArrayList<>();
    private final Set<Long> selectedIds = new HashSet<>();
    private final OnSelectionChanged listener;

    private static final int[] AVATAR_BG_COLORS = new int[] {
            R.color.icon_bg_blue,
            R.color.icon_bg_green,
            R.color.icon_bg_amber,
            R.color.icon_bg_purple,
            R.color.blue_50
    };

    public ContactCheckAdapter(OnSelectionChanged listener) {
        this.listener = listener;
    }

    public void submitList(List<Contact> items) {
        contacts.clear();
        if (items != null) {
            contacts.addAll(items);
        }
        notifyDataSetChanged();
    }

    public Set<Long> getSelectedIds() {
        return selectedIds;
    }

    @NonNull
    @Override
    public ContactCheckViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact_checkable, parent, false);
        return new ContactCheckViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactCheckViewHolder holder, int position) {
        Contact contact = contacts.get(position);
        String name = contact.getFullName() != null ? contact.getFullName().trim() : "";
        if (name.isEmpty()) name = holder.itemView.getContext().getString(R.string.chat_user_fallback);
        holder.tvFullName.setText(name);
        int colorIndex = resolveAvatarColorIndex(contact, name);
        int bgColor = ContextCompat.getColor(holder.itemView.getContext(), AVATAR_BG_COLORS[colorIndex]);
        holder.tvInitial.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bgColor));
        holder.tvInitial.setTextColor(getReadableTextColor(bgColor));
        holder.tvInitial.setText(String.valueOf(Character.toUpperCase(name.charAt(0))));
        String positionDept = (contact.getPosition() != null ? contact.getPosition() : "") +
                " • " + (contact.getDepartmentName() != null ? contact.getDepartmentName() : "");
        holder.tvPositionDept.setText(positionDept);

        boolean checked = selectedIds.contains(contact.getUserId());
        holder.cbSelect.setChecked(checked);
        if (holder.cardItem != null) {
            int bg = checked ? R.color.blue_50 : android.R.color.white;
            int stroke = checked ? R.color.colorPrimary : R.color.gray_200;
            holder.cardItem.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), bg));
            holder.cardItem.setStrokeColor(ContextCompat.getColor(holder.itemView.getContext(), stroke));
        }

        View.OnClickListener toggle = v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            animatePress(holder.itemView);
            if (selectedIds.contains(contact.getUserId())) {
                selectedIds.remove(contact.getUserId());
            } else {
                selectedIds.add(contact.getUserId());
            }
            notifyItemChanged(pos);
            if (listener != null) {
                listener.onChanged(selectedIds);
            }
        };

        holder.itemLayout.setOnClickListener(toggle);
        holder.cbSelect.setOnClickListener(toggle);
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    static class ContactCheckViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardItem;
        final CheckBox cbSelect;
        final TextView tvInitial;
        final TextView tvFullName;
        final TextView tvPositionDept;
        final View itemLayout;

        ContactCheckViewHolder(@NonNull View itemView) {
            super(itemView);
            cardItem = itemView.findViewById(R.id.cardItem);
            cbSelect = itemView.findViewById(R.id.cbSelect);
            tvInitial = itemView.findViewById(R.id.tvInitial);
            tvFullName = itemView.findViewById(R.id.tvFullName);
            tvPositionDept = itemView.findViewById(R.id.tvPositionDept);
            itemLayout = itemView.findViewById(R.id.itemLayout);
        }
    }

    private int resolveAvatarColorIndex(Contact contact, String fallbackName) {
        int seed;
        if (contact != null && contact.getUserId() != null) {
            seed = contact.getUserId().hashCode();
        } else {
            seed = fallbackName != null ? fallbackName.hashCode() : 0;
        }
        return Math.abs(seed) % AVATAR_BG_COLORS.length;
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

    private int getReadableTextColor(int backgroundColor) {
        double luminance = (0.299 * Color.red(backgroundColor)
                + 0.587 * Color.green(backgroundColor)
                + 0.114 * Color.blue(backgroundColor)) / 255d;
        return luminance > 0.62 ? Color.parseColor("#1F2937") : Color.WHITE;
    }
}
