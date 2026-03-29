package com.example.myapplication.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myapplication.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class EmojiBottomSheet extends BottomSheetDialogFragment {

    public interface OnEmojiSelectedListener {
        void onEmojiSelected(Long messageId, String emoji);
    }

    private static final String ARG_MESSAGE_ID = "arg_message_id";
    private OnEmojiSelectedListener listener;

    public static EmojiBottomSheet newInstance(Long messageId) {
        EmojiBottomSheet sheet = new EmojiBottomSheet();
        Bundle args = new Bundle();
        args.putLong(ARG_MESSAGE_ID, messageId != null ? messageId : -1L);
        sheet.setArguments(args);
        return sheet;
    }

    public void setOnEmojiSelectedListener(OnEmojiSelectedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_emoji, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Long messageId = getArguments() != null ? getArguments().getLong(ARG_MESSAGE_ID, -1L) : -1L;

        View[] buttons = new View[] {
                view.findViewById(R.id.btnEmoji1),
                view.findViewById(R.id.btnEmoji2),
                view.findViewById(R.id.btnEmoji3),
                view.findViewById(R.id.btnEmoji4),
                view.findViewById(R.id.btnEmoji5),
                view.findViewById(R.id.btnEmoji6)
        };

        String[] emojis = new String[] {"😀", "😂", "❤️", "👍", "😮", "😢"};

        for (int i = 0; i < buttons.length; i++) {
            final String emoji = emojis[i];
            buttons[i].setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEmojiSelected(messageId, emoji);
                }
                dismiss();
            });
        }
    }
}
