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

public class SelectContactAdapter extends RecyclerView.Adapter<SelectContactAdapter.ViewHolder> {

    public interface OnSelectionChangeListener {
        void onSelectionChanged(int count);
    }

    private final List<Contact> items = new ArrayList<>();
    private final Set<Long> selectedIds = new HashSet<>();
    private OnSelectionChangeListener selectionChangeListener;

    private static final int[] AVATAR_BG_COLORS = new int[] {
            R.color.icon_bg_blue,
            R.color.icon_bg_green,
            R.color.icon_bg_amber,
            R.color.icon_bg_purple,
            R.color.blue_50
    };

    public void setOnSelectionChangeListener(OnSelectionChangeListener listener) {
        this.selectionChangeListener = listener;
    }

    public void submitList(List<Contact> contacts) {
        items.clear();
        if (contacts != null) {
            items.addAll(contacts);
        }
        notifyDataSetChanged();
        notifySelection();
    }

    public List<Long> getSelectedIds() {
        return new ArrayList<>(selectedIds);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_select_contact, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Contact contact = items.get(position);
        holder.bind(contact);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private void toggleSelection(Long userId) {
        if (userId == null) return;
        if (selectedIds.contains(userId)) {
            selectedIds.remove(userId);
        } else {
            selectedIds.add(userId);
        }
        notifySelection();
    }

    private void notifySelection() {
        if (selectionChangeListener != null) {
            selectionChangeListener.onSelectionChanged(selectedIds.size());
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardItem;
        final TextView tvInitial;
        final TextView tvName;
        final TextView tvMeta;
        final CheckBox cbSelect;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardItem = itemView.findViewById(R.id.cardItem);
            tvInitial = itemView.findViewById(R.id.tvInitial);
            tvName = itemView.findViewById(R.id.tvName);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            cbSelect = itemView.findViewById(R.id.cbSelect);
        }

        void bind(Contact contact) {
            String name = contact.getFullName() != null ? contact.getFullName() : "";
            if (name.trim().isEmpty()) {
                name = itemView.getContext().getString(R.string.chat_user_fallback);
            }
            String position = contact.getPosition() != null ? contact.getPosition() : "";
            String dept = contact.getDepartmentName() != null ? contact.getDepartmentName() : "";
            String separator = (!position.isEmpty() && !dept.isEmpty()) ? " • " : "";
            tvName.setText(name);
            tvMeta.setText(position + separator + dept);
            int colorIndex = resolveAvatarColorIndex(contact, name);
            int bgColor = ContextCompat.getColor(itemView.getContext(), AVATAR_BG_COLORS[colorIndex]);
            tvInitial.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bgColor));
            tvInitial.setTextColor(getReadableTextColor(bgColor));
            tvInitial.setText(name.isEmpty() ? itemView.getContext().getString(R.string.chat_unknown_short) : String.valueOf(Character.toUpperCase(name.charAt(0))));

            boolean checked = contact.getUserId() != null && selectedIds.contains(contact.getUserId());
            cbSelect.setChecked(checked);

            if (cardItem != null) {
                int bg = checked ? R.color.blue_50 : android.R.color.white;
                int stroke = checked ? R.color.colorPrimary : R.color.gray_200;
                cardItem.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(), bg));
                cardItem.setStrokeColor(ContextCompat.getColor(itemView.getContext(), stroke));
            }

            // Hiệu ứng nhỏ để thao tác chọn/bỏ chọn mượt hơn
            itemView.animate()
                    .scaleX(checked ? 0.985f : 1f)
                    .scaleY(checked ? 0.985f : 1f)
                    .alpha(checked ? 0.98f : 1f)
                    .setDuration(120)
                    .start();

            View.OnClickListener toggle = v -> {
                int pos = getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                animatePress(itemView);
                toggleSelection(contact.getUserId());
                notifyItemChanged(pos);
            };

            itemView.setOnClickListener(toggle);
            cbSelect.setOnClickListener(toggle);
        }
    }

    private int getReadableTextColor(int backgroundColor) {
        double luminance = (0.299 * Color.red(backgroundColor)
                + 0.587 * Color.green(backgroundColor)
                + 0.114 * Color.blue(backgroundColor)) / 255d;
        return luminance > 0.62 ? Color.parseColor("#1F2937") : Color.WHITE;
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
}
