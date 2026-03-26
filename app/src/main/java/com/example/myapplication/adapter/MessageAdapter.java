package com.example.myapplication.adapter;

import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.model.Message;
import com.example.myapplication.model.RoomMember;
import com.example.myapplication.ui.ImageViewerActivity;
import com.google.android.material.imageview.ShapeableImageView;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "MessageAdapter";

    public interface OnRecallRequestListener {
        void onRecallRequested(Long messageId);
    }

    public interface OnReplyRequestListener {
        void onReplyRequested(Message message);
    }

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;
    private static final int VIEW_TYPE_SYSTEM = 3;
    private static final int VIEW_TYPE_DATE = 4;
    private static final int MENU_ID_REPLY = 1001;
    private static final int MENU_ID_RECALL = 1002;
    private static final int[] AVATAR_BG_COLORS = new int[] {
            R.color.icon_bg_blue,
            R.color.icon_bg_green,
            R.color.icon_bg_amber,
            R.color.icon_bg_purple,
            R.color.blue_50
    };
    private static final int IMAGE_DEFAULT_TOP_MARGIN_DP = 6;
    private static final int IMAGE_COLLAPSED_TOP_MARGIN_DP = 0;

    private final Context context;
    private final Long myId;
    private final List<Message> messages = new ArrayList<>();
    private final OnRecallRequestListener recallListener;
    private final OnReplyRequestListener replyRequestListener;
    private final List<RoomMember> members = new ArrayList<>();
    private Long highlightedMessageId = null;
    private int lastAnimatedPosition = -1;
    private final Set<Long> downloadingMessageIds = Collections.synchronizedSet(new HashSet<>());
    private final ExecutorService downloadExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final OkHttpClient downloadClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    public MessageAdapter(Context context, Long myId) {
        this(context, myId, null, null);
    }

    public MessageAdapter(Context context, Long myId, OnRecallRequestListener recallListener, OnReplyRequestListener replyRequestListener) {
        this.context = context;
        this.myId = myId;
        this.recallListener = recallListener;
        this.replyRequestListener = replyRequestListener;
    }

    public void setMembers(List<RoomMember> newMembers) {
        members.clear();
        if (newMembers != null) {
            members.addAll(newMembers);
        }
        notifyDataSetChanged();
    }

    public void submitList(List<Message> newMessages) {
        messages.clear();
        if (newMessages != null) {
            messages.addAll(withDateSeparators(newMessages));
        }
        notifyDataSetChanged();
    }

    public void addMessage(Message incoming) {
        if (incoming == null) {
            return;
        }

        int existingIndex = findMessageIndexById(incoming.getId());
        if (existingIndex >= 0) {
            messages.set(existingIndex, incoming);
            notifyItemChanged(existingIndex);
            return;
        }

        // append date separator if day changed
        String incomingDate = extractDateKey(incoming.getCreatedAt());
        String lastDate = findLastMessageDate();
        if (lastDate == null || !lastDate.equals(incomingDate)) {
            messages.add(createDateSeparator(incomingDate));
            notifyItemInserted(messages.size() - 1);
        }

        messages.add(incoming);
        notifyItemInserted(messages.size() - 1);
    }

    public void updateRecalled(Message incoming) {
        if (incoming == null || incoming.getId() == null) return;
        int index = findMessageIndexById(incoming.getId());
        if (index >= 0) {
            messages.set(index, incoming);
            notifyItemChanged(index);
        }
    }

    private void setImageTopMargin(ImageView ivImage, int topMarginDp) {
        if (ivImage == null || ivImage.getLayoutParams() == null) return;
        ViewGroup.LayoutParams params = ivImage.getLayoutParams();
        if (params instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) params;
            lp.topMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, topMarginDp, context.getResources().getDisplayMetrics());
            ivImage.setLayoutParams(lp);
        }
    }

    private void openImageViewer(String imageUrl, String fileName) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            Toast.makeText(context, context.getString(R.string.chat_file_url_missing), Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(context, ImageViewerActivity.class);
        intent.putExtra(ImageViewerActivity.EXTRA_IMAGE_URL, imageUrl);
        if (fileName != null) {
            intent.putExtra(ImageViewerActivity.EXTRA_FILE_NAME, fileName);
        }
        if (!(context instanceof android.app.Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    public Message getLastMessage() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message m = messages.get(i);
            if (!isDateSeparator(m)) return m;
        }
        return null;
    }

    private int findMessageIndexById(Long id) {
        if (id == null) {
            return -1;
        }
        for (int i = 0; i < messages.size(); i++) {
            Message message = messages.get(i);
            if (isDateSeparator(message)) continue;
            if (message.getId() != null && message.getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        if (isDateSeparator(message)) {
            return VIEW_TYPE_DATE;
        }
        if ("SYSTEM".equalsIgnoreCase(message.getMessageType())) {
            return VIEW_TYPE_SYSTEM;
        }
        if (message.getSenderId() != null && message.getSenderId().equals(myId)) {
            return VIEW_TYPE_SENT;
        }
        return VIEW_TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_SENT) {
            return new SentViewHolder(inflater.inflate(R.layout.item_message_sent, parent, false));
        }
        if (viewType == VIEW_TYPE_SYSTEM) {
            return new SystemViewHolder(inflater.inflate(R.layout.item_message_system, parent, false));
        }
        if (viewType == VIEW_TYPE_DATE) {
            return new DateViewHolder(inflater.inflate(R.layout.item_message_date_separator, parent, false));
        }
        return new ReceivedViewHolder(inflater.inflate(R.layout.item_message_received, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);
        if (holder instanceof SentViewHolder) {
            ((SentViewHolder) holder).bind(message);
        } else if (holder instanceof ReceivedViewHolder) {
            ((ReceivedViewHolder) holder).bind(message);
        } else if (holder instanceof SystemViewHolder) {
            ((SystemViewHolder) holder).bind(message);
        } else if (holder instanceof DateViewHolder) {
            ((DateViewHolder) holder).bind(message);
        }
        animateItemIn(holder.itemView, position);
    }

    private void animateItemIn(View itemView, int position) {
        if (position <= lastAnimatedPosition) {
            itemView.setAlpha(1f);
            itemView.setTranslationY(0f);
            return;
        }
        itemView.setAlpha(0f);
        itemView.setTranslationY(14f);
        itemView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(180)
                .start();
        lastAnimatedPosition = position;
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    private int findLastRealMessagePosition() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (!isDateSeparator(messages.get(i))) return i;
        }
        return -1;
    }

    private void bindCommonMessageContent(
            Message message,
            TextView tvMessage,
            TextView tvTime,
            ImageView ivImage,
            LinearLayout llFile,
            TextView tvFileName,
            ImageButton btnDownload,
            ProgressBar pbDownload,
            TextView tvFileSize,
            LinearLayout llReplyPreview,
            TextView tvReplySender,
            TextView tvReplyContent,
            View bubbleContainer,
            boolean isSender,
            int defaultTextColor,
            boolean showTime
    ) {
        tvTime.setText(formatTime(message.getCreatedAt()));
        tvTime.setVisibility(showTime ? View.VISIBLE : View.GONE);

        bindReplyPreview(message, llReplyPreview, tvReplySender, tvReplyContent);

        tvMessage.setVisibility(View.VISIBLE);
        setImageTopMargin(ivImage, IMAGE_DEFAULT_TOP_MARGIN_DP);
        ivImage.setOnClickListener(null);
        restoreBubble(bubbleContainer, isSender);
        if (llFile != null) {
            llFile.setVisibility(View.GONE);
        }

        boolean recalled = Boolean.TRUE.equals(message.getIsRecalled());
        if (recalled) {
            tvMessage.setText(context.getString(R.string.chat_message_recalled));
            tvMessage.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));
            tvMessage.setTextColor(Color.parseColor("#9CA3AF"));
            ivImage.setVisibility(View.GONE);
            llFile.setVisibility(View.GONE);
            Glide.with(context).clear(ivImage);
        } else {
            tvMessage.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
            tvMessage.setText(message.getMessage() != null ? message.getMessage() : "");
            tvMessage.setTextColor(defaultTextColor);

            String type = resolveMessageType(message);
            String resolvedFileUrl = resolveMessageFileUrl(message);
            if ("IMAGE".equals(type)) {
                tvMessage.setVisibility(View.GONE);
                setImageTopMargin(ivImage, IMAGE_COLLAPSED_TOP_MARGIN_DP);
                removeBubble(bubbleContainer);
                ivImage.setVisibility(View.VISIBLE);
                llFile.setVisibility(View.GONE);
                String imageUrl = normalizeRemoteUrl(message.getFileUrl());
                if (imageUrl.isEmpty()) {
                    imageUrl = resolvedFileUrl;
                }
                if (imageUrl.isEmpty()) {
                    Log.w(TAG, "Image URL missing for message=" + message.getId());
                }
                Glide.with(context)
                        .load(imageUrl.isEmpty() ? null : imageUrl)
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_broken_image)
                        .fallback(R.drawable.ic_broken_image)
                        .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                                Log.e(TAG, "Glide load failed url=" + model, e);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                                return false;
                            }
                        })
                        .into(ivImage);
                final String finalImageUrl = imageUrl;
                ivImage.setOnClickListener(v -> openImageViewer(finalImageUrl, message.getFileName()));
            } else if ("FILE".equals(type)) {
                restoreBubble(bubbleContainer, isSender);
                ivImage.setVisibility(View.GONE);
                llFile.setVisibility(View.VISIBLE);
                Glide.with(context).clear(ivImage);
                tvFileName.setText(message.getFileName() != null ? message.getFileName() : context.getString(R.string.chat_attachment_default));

                File cachedFile = resolveCacheFile(message);
                boolean isCached = cachedFile.exists() && cachedFile.length() > 0;
                boolean isDownloading = message.getId() != null && downloadingMessageIds.contains(message.getId());
                if (tvFileSize != null) {
                    tvFileSize.setText(isDownloading ? context.getString(R.string.chat_downloading_file) : formatFileSize(message.getFileSize()));
                }
                if (pbDownload != null) {
                    pbDownload.setVisibility(isDownloading ? View.VISIBLE : View.GONE);
                }
                btnDownload.setVisibility(isDownloading ? View.GONE : View.VISIBLE);
                if (!isDownloading) {
                    int iconColor = ContextCompat.getColor(context,
                            isSender ? android.R.color.white : R.color.text_primary);
                    btnDownload.setImageResource(isCached ? R.drawable.ic_eye : R.drawable.ic_download_arrow);
                    btnDownload.setImageTintList(ColorStateList.valueOf(iconColor));
                    btnDownload.setContentDescription(context.getString(
                            isCached ? R.string.chat_action_open_file : R.string.chat_action_download_file
                    ));
                }

                View.OnClickListener fileClick = v -> onFileMessageClick(message);
                btnDownload.setOnClickListener(fileClick);
                llFile.setOnClickListener(fileClick);
            } else {
                restoreBubble(bubbleContainer, isSender);
                ivImage.setVisibility(View.GONE);
                llFile.setVisibility(View.GONE);
                Glide.with(context).clear(ivImage);
            }
        }
    }

    private void removeBubble(View bubbleContainer) {
        if (bubbleContainer == null) return;
        bubbleContainer.setBackground(null);
        bubbleContainer.setPadding(0, 0, 0, 0);
    }

    private void restoreBubble(View bubbleContainer, boolean isSender) {
        if (bubbleContainer == null) return;
        int pad = dpToPx(11);
        bubbleContainer.setPadding(pad, pad, pad, pad);
        bubbleContainer.setBackground(ContextCompat.getDrawable(context,
                isSender ? R.drawable.bg_message_sent_bubble : R.drawable.bg_message_received_bubble));
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    private void bindReplyPreview(Message message, LinearLayout container, TextView tvSender, TextView tvContent) {
        if (container == null || tvSender == null || tvContent == null) {
            return;
        }
        Message replied = message != null ? message.getReplyToMessage() : null;
        if (replied == null) {
            container.setVisibility(View.GONE);
            return;
        }
        container.setVisibility(View.VISIBLE);
        String preview = replied.getMessage();
        if (preview == null || preview.isEmpty()) {
            preview = context.getString(R.string.chat_reply_no_content);
        }
        // Hiển thị duy nhất nội dung tin nhắn gốc, ẩn dòng tên để tránh nhầm lẫn
        tvSender.setText(preview);
        tvContent.setText("");
        tvContent.setVisibility(View.GONE);
    }

    private String formatFileSize(Long size) {
        if (size == null || size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    private String formatTime(String rawDate) {
        if (rawDate == null || rawDate.isEmpty()) return "";
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
                if (rawDate.contains(" ")) return rawDate;
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

    private String extractDateKey(String rawDate) {
        String clean = normalizeDate(rawDate != null ? rawDate : "");
        if (clean.length() >= 10) {
            return clean.substring(0, 10);
        }
        return clean;
    }

    private String findLastMessageDate() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message m = messages.get(i);
            if (isDateSeparator(m)) continue;
            return extractDateKey(m.getCreatedAt());
        }
        return null;
    }

    private List<Message> withDateSeparators(List<Message> base) {
        List<Message> out = new ArrayList<>();
        String prevDate = null;
        for (Message m : base) {
            String dateKey = extractDateKey(m != null ? m.getCreatedAt() : "");
            if (prevDate == null || !prevDate.equals(dateKey)) {
                out.add(createDateSeparator(dateKey));
                prevDate = dateKey;
            }
            out.add(m);
        }
        return out;
    }

    private Message createDateSeparator(String dateText) {
        Message sep = new Message();
        sep.setMessageType("DATE_SEPARATOR");
        sep.setMessage(dateText != null ? dateText : "");
        return sep;
    }

    private boolean isDateSeparator(Message m) {
        return m != null && "DATE_SEPARATOR".equalsIgnoreCase(m.getMessageType());
    }

    class DateViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvDate;

        DateViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDateSeparator);
        }

        void bind(Message message) {
            tvDate.setText(message.getMessage() != null ? message.getMessage() : "");
        }
    }

    class SentViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvMessage;
        private final TextView tvTime;
        private final ImageView ivImage;
        private final LinearLayout llFile;
        private final TextView tvFileName;
        private final TextView tvFileSize;
        private final ImageButton btnDownload;
        private final ProgressBar pbDownload;
        private final LinearLayout llReplyPreview;
        private final TextView tvReplySender;
        private final TextView tvReplyContent;
        private final View bubbleContainer;

        SentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            ivImage = itemView.findViewById(R.id.ivImage);
            llFile = itemView.findViewById(R.id.llFile);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvFileSize = itemView.findViewById(R.id.tvFileSize);
            btnDownload = itemView.findViewById(R.id.btnDownload);
            pbDownload = itemView.findViewById(R.id.pbDownload);
            llReplyPreview = itemView.findViewById(R.id.llReplyPreview);
            tvReplySender = itemView.findViewById(R.id.tvReplySender);
            tvReplyContent = itemView.findViewById(R.id.tvReplyContent);
            bubbleContainer = itemView.findViewById(R.id.bubbleContainer);
            itemView.setOnLongClickListener(v -> {
                Message msg = messages.get(getAdapterPosition());
                showActionMenu(itemView, msg, true);
                return true;
            });
        }

        void bind(Message message) {
            int defaultColor = ContextCompat.getColor(context, android.R.color.white);
            int pos = getAdapterPosition();
            boolean isLast = pos == findLastRealMessagePosition();
            boolean isHighlighted = highlightedMessageId != null && highlightedMessageId.equals(message.getId());
            bindCommonMessageContent(message, tvMessage, tvTime, ivImage, llFile, tvFileName, btnDownload, pbDownload, tvFileSize, llReplyPreview, tvReplySender, tvReplyContent, bubbleContainer, true, defaultColor, isLast || isHighlighted);
            View clickTarget = bubbleContainer != null ? bubbleContainer : itemView;
            clickTarget.setOnClickListener(v -> {
                if (isHighlighted) {
                    highlightMessage(null);
                } else {
                    highlightMessage(message.getId());
                }
            });
        }
    }

    class ReceivedViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvSenderName;
        private final TextView tvMessage;
        private final TextView tvTime;
        private final ImageView ivImage;
        private final LinearLayout llFile;
        private final TextView tvFileName;
        private final TextView tvFileSize;
        private final ImageButton btnDownload;
        private final ProgressBar pbDownload;
        private final LinearLayout llReplyPreview;
        private final TextView tvReplySender;
        private final TextView tvReplyContent;
        private final ShapeableImageView ivAvatar;
        private final TextView tvAvatarInitial;
        private final View bubbleContainer;
        private final View flAvatarContainer;

        ReceivedViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSenderName = itemView.findViewById(R.id.tvSenderName);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            ivImage = itemView.findViewById(R.id.ivImage);
            llFile = itemView.findViewById(R.id.llFile);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvFileSize = itemView.findViewById(R.id.tvFileSize);
            btnDownload = itemView.findViewById(R.id.btnDownload);
            pbDownload = itemView.findViewById(R.id.pbDownload);
            llReplyPreview = itemView.findViewById(R.id.llReplyPreview);
            tvReplySender = itemView.findViewById(R.id.tvReplySender);
            tvReplyContent = itemView.findViewById(R.id.tvReplyContent);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvAvatarInitial = itemView.findViewById(R.id.tvAvatarInitial);
            bubbleContainer = itemView.findViewById(R.id.bubbleContainer);
            flAvatarContainer = itemView.findViewById(R.id.flAvatarContainer);
            itemView.setOnLongClickListener(v -> {
                Message msg = messages.get(getAdapterPosition());
                showActionMenu(itemView, msg, false);
                return true;
            });
        }

        void bind(Message message) {
            String sender = message.getSenderName() != null ? message.getSenderName() : context.getString(R.string.chat_sender_anonymous);
            int pos = getAdapterPosition();
            boolean showSenderMeta = pos == RecyclerView.NO_POSITION || shouldShowSenderMeta(pos, message);
            tvSenderName.setText(sender);
            tvSenderName.setVisibility(showSenderMeta ? View.VISIBLE : View.GONE);

            // Avatar chữ cái random theo người (ổn định theo senderId hoặc senderName)
            if (tvAvatarInitial != null) {
                String initials = sender.isEmpty() ? context.getString(R.string.chat_unknown_short) : String.valueOf(Character.toUpperCase(sender.charAt(0)));
                tvAvatarInitial.setText(initials);
                int colorIndex = resolveAvatarColorIndex(message, sender);
                int bgColor = ContextCompat.getColor(context, AVATAR_BG_COLORS[colorIndex]);
                tvAvatarInitial.setBackgroundTintList(ColorStateList.valueOf(bgColor));
                tvAvatarInitial.setTextColor(getReadableTextColor(bgColor));
            }
            if (ivAvatar != null) {
                ivAvatar.setVisibility(View.GONE);
            }
            if (flAvatarContainer != null) {
                // Giữ cột avatar để các bubble cùng hàng dọc, tránh lệch trái/phải khi group.
                flAvatarContainer.setVisibility(showSenderMeta ? View.VISIBLE : View.INVISIBLE);
            }

            int defaultColor = ContextCompat.getColor(context, R.color.text_primary);
            boolean isLast = pos == findLastRealMessagePosition();
            boolean isHighlighted = highlightedMessageId != null && highlightedMessageId.equals(message.getId());
            bindCommonMessageContent(message, tvMessage, tvTime, ivImage, llFile, tvFileName, btnDownload, pbDownload, tvFileSize, llReplyPreview, tvReplySender, tvReplyContent, bubbleContainer, false, defaultColor, isLast || isHighlighted);
            View clickTarget = bubbleContainer != null ? bubbleContainer : itemView;
            clickTarget.setOnClickListener(v -> {
                if (isHighlighted) {
                    highlightMessage(null);
                } else {
                    highlightMessage(message.getId());
                }
            });
        }
    }

    class SystemViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvSystemMessage;

        SystemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSystemMessage = itemView.findViewById(R.id.tvSystemMessage);
        }

        void bind(Message message) {
            tvSystemMessage.setText(message.getMessage() != null ? message.getMessage() : context.getString(R.string.chat_system_default));
        }
    }

    private void showActionMenu(View anchor, Message msg, boolean isMine) {
        PopupMenu menu = new PopupMenu(context, anchor);
        if (replyRequestListener != null) {
            menu.getMenu().add(0, MENU_ID_REPLY, 0, context.getString(R.string.chat_action_reply));
        }
        if (isMine && recallListener != null) {
            menu.getMenu().add(0, MENU_ID_RECALL, 1, context.getString(R.string.chat_action_recall));
        }

        menu.setOnMenuItemClickListener(it -> {
            if (it.getItemId() == MENU_ID_REPLY && replyRequestListener != null) {
                replyRequestListener.onReplyRequested(msg);
            } else if (it.getItemId() == MENU_ID_RECALL && recallListener != null) {
                recallListener.onRecallRequested(msg.getId());
            }
            return true;
        });
        menu.show();
    }

    public void highlightMessage(Long messageId) {
        highlightedMessageId = messageId;
        notifyDataSetChanged();
    }

    private int resolveAvatarColorIndex(Message message, String fallbackName) {
        int seed;
        if (message != null && message.getSenderId() != null) {
            seed = message.getSenderId().hashCode();
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

    private boolean shouldShowSenderMeta(int position, Message current) {
        if (position <= 0 || current == null) return true;

        Message prev = messages.get(position - 1);
        if (prev == null || isDateSeparator(prev) || "SYSTEM".equalsIgnoreCase(prev.getMessageType())) return true;

        if (current.getSenderId() == null || prev.getSenderId() == null) return true;
        if (!current.getSenderId().equals(prev.getSenderId())) return true;

        // Group theo cùng phút HH:mm (không dùng chênh lệch < 60s).
        return !isSameMinute(prev.getCreatedAt(), current.getCreatedAt());
    }

    private boolean isSameMinute(String firstRaw, String secondRaw) {
        if (firstRaw == null || secondRaw == null) return false;
        String first = normalizeDate(firstRaw);
        String second = normalizeDate(secondRaw);
        if (first.length() < 16 || second.length() < 16) return false;
        return first.substring(0, 16).equals(second.substring(0, 16));
    }

    private void onFileMessageClick(Message message) {
        if (message == null || message.getId() == null) {
            Toast.makeText(context, context.getString(R.string.chat_file_open_failed), Toast.LENGTH_SHORT).show();
            return;
        }
        if (downloadingMessageIds.contains(message.getId())) {
            return;
        }

        String fileUrl = resolveMessageFileUrl(message);
        if (fileUrl.isEmpty()) {
            Log.w(TAG, "File URL missing for message=" + message.getId());
            Toast.makeText(context, context.getString(R.string.chat_file_url_missing), Toast.LENGTH_SHORT).show();
            return;
        }

        File cachedFile = resolveCacheFile(message);
        if (cachedFile.exists() && cachedFile.length() > 0) {
            openFile(cachedFile, message.getFileName());
            return;
        }

        downloadingMessageIds.add(message.getId());
        notifyDataSetChanged();

        downloadExecutor.execute(() -> {
            try {
                downloadToFile(fileUrl, cachedFile);
                mainHandler.post(() -> {
                    downloadingMessageIds.remove(message.getId());
                    notifyDataSetChanged();
                    openFile(cachedFile, message.getFileName());
                });
            } catch (Exception ex) {
                Log.e(TAG, "Download error for url=" + fileUrl, ex);
                mainHandler.post(() -> {
                    downloadingMessageIds.remove(message.getId());
                    notifyDataSetChanged();
                    Toast.makeText(context, context.getString(R.string.chat_file_download_failed), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private File resolveCacheFile(Message message) {
        File cacheFolder = new File(context.getCacheDir(), "chat_attachments");
        if (!cacheFolder.exists()) {
            //noinspection ResultOfMethodCallIgnored
            cacheFolder.mkdirs();
        }
        String displayName = message.getFileName() != null && !message.getFileName().trim().isEmpty()
                ? message.getFileName().trim()
                : "attachment.bin";
        String safeName = displayName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return new File(cacheFolder, message.getId() + "_" + safeName);
    }

    private void downloadToFile(String fileUrl, File destination) throws IOException {
        File tempFile = new File(destination.getAbsolutePath() + ".download");
        Request request = new Request.Builder().url(fileUrl).get().build();
        try (Response response = downloadClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Log.e(TAG, "Download failed url=" + fileUrl + " code=" + response.code());
                throw new IOException("HTTP " + response.code());
            }
            ResponseBody body = response.body();
            if (body == null) {
                Log.e(TAG, "Download empty body url=" + fileUrl);
                throw new IOException("Empty response body");
            }
            try (InputStream input = body.byteStream(); FileOutputStream output = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
                output.flush();
            }
        }

        if (destination.exists()) {
            //noinspection ResultOfMethodCallIgnored
            destination.delete();
        }
        if (!tempFile.renameTo(destination)) {
            Log.e(TAG, "Cannot move temp file for url=" + fileUrl);
            throw new IOException("Cannot move temp file");
        }
    }

    private void openFile(File file, String fileName) {
        try {
            Uri uri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    file
            );
            String mimeType = resolveMimeType(fileName);
            Intent intent = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, mimeType)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (!(context instanceof android.app.Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.chat_open_file_chooser)));
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(context, context.getString(R.string.chat_no_app_to_open_file), Toast.LENGTH_SHORT).show();
        } catch (Exception ex) {
            Toast.makeText(context, context.getString(R.string.chat_file_open_failed), Toast.LENGTH_SHORT).show();
        }
    }

    private String resolveMimeType(String fileName) {
        if (fileName == null) {
            return "application/octet-stream";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot >= fileName.length() - 1) {
            return "application/octet-stream";
        }
        String ext = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        return mime != null ? mime : "application/octet-stream";
    }

    private String normalizeRemoteUrl(String rawUrl) {
        if (rawUrl == null) {
            return "";
        }
        String url = rawUrl.trim().replace("\\/", "/");
        if (url.startsWith("\"") && url.endsWith("\"") && url.length() > 1) {
            url = url.substring(1, url.length() - 1).trim();
        }
        if (url.isEmpty()) {
            return "";
        }
        if (url.startsWith("//")) {
            return "https:" + url;
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        // Backend contract: fileUrl phải là URL tuyệt đối (Cloudinary), không tự nối BASE_URL.
        return "";
    }

    private String resolveMessageFileUrl(Message message) {
        if (message == null) {
            return "";
        }
        String direct = normalizeRemoteUrl(message.getFileUrl());
        if (!direct.isEmpty()) {
            return direct;
        }
        return extractUrlFromMetadata(message.getMetadata());
    }

    private String extractUrlFromMetadata(String metadata) {
        if (metadata == null || metadata.trim().isEmpty()) {
            return "";
        }
        try {
            JSONObject json = new JSONObject(metadata);
            String[] keys = new String[]{"fileUrl", "url", "secure_url", "file_url", "downloadUrl"};
            for (String key : keys) {
                String value = normalizeRemoteUrl(json.optString(key, ""));
                if (!value.isEmpty()) {
                    return value;
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private String resolveMessageType(Message message) {
        String declared = message.getMessageType() != null ? message.getMessageType().toUpperCase() : null;
        if (declared != null && !declared.isEmpty()) {
            return declared;
        }
        String url = normalizeRemoteUrl(message.getFileUrl());
        if (url.isEmpty()) {
            url = resolveMessageFileUrl(message);
        }
        String name = message.getFileName();
        String guessSource = !url.isEmpty() ? url : (name != null ? name : "");
        String ext = guessSource.contains(".") ? guessSource.substring(guessSource.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        if (ext.matches("(?i)(jpg|jpeg|png|gif|webp|bmp)")) {
            return "IMAGE";
        }
        if (!url.isEmpty() || (name != null && !name.trim().isEmpty())) {
            return "FILE";
        }
        return "TEXT";
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        downloadExecutor.shutdownNow();
    }
}
