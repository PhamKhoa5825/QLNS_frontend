package com.example.myapplication.utils;
 
import android.app.Activity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
 
import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.ui.NotificationActivity;
import com.example.myapplication.ui.ProfileActivity;
import com.example.myapplication.utils.SharedPrefsManager;
import com.google.android.material.appbar.MaterialToolbar;

public class TopBarHelper {
 
    public static void setupTopBar(Activity activity) {
        SharedPrefsManager prefs = SharedPrefsManager.getInstance(activity);
        String fullName = prefs.getFullName();
        String deptName = prefs.getDepartmentName();
        String avatarUrl = prefs.getAvatarUrl();
        String username = prefs.getUsername();
 
        TextView tvName = activity.findViewById(R.id.tvHeaderName);
        TextView tvDept = activity.findViewById(R.id.tvHeaderDept);
        TextView tvAvatarText = activity.findViewById(R.id.tvHeaderAvatarText);
        ImageView ivAvatar = activity.findViewById(R.id.ivHeaderAvatar);
 
        if (tvName != null) {
            tvName.setText(fullName.isEmpty() ? username : fullName);
        }
        if (tvDept != null) {
            tvDept.setText(deptName.isEmpty() ? "" : deptName);
        }
 
        if (ivAvatar != null && tvAvatarText != null) {
            String fullNameFinal = fullName.isEmpty() ? username : fullName;
            String initials = getInitial(fullNameFinal);
            tvAvatarText.setText(initials);
            tvAvatarText.setVisibility(View.VISIBLE);

            String finalAvatarUrl = avatarUrl;
            boolean hasValidUrl = finalAvatarUrl != null && !finalAvatarUrl.isEmpty() && !finalAvatarUrl.equalsIgnoreCase("null");

            if (hasValidUrl) {
                // Normalize URL
                if (!finalAvatarUrl.startsWith("http")) {
                    String baseUrl = RetrofitClient.BASE_URL;
                    if (baseUrl.endsWith("/") && finalAvatarUrl.startsWith("/")) {
                        finalAvatarUrl = baseUrl + finalAvatarUrl.substring(1);
                    } else if (!baseUrl.endsWith("/") && !finalAvatarUrl.startsWith("/")) {
                        finalAvatarUrl = baseUrl + "/" + finalAvatarUrl;
                    } else {
                        finalAvatarUrl = baseUrl + finalAvatarUrl;
                    }
                }
                
                ivAvatar.setVisibility(View.VISIBLE);
                Glide.with(activity)
                        .load(finalAvatarUrl)
                        .circleCrop()
                        .error(R.drawable.ic_user_placeholder)
                        .into(ivAvatar);
            } else {
                ivAvatar.setVisibility(View.GONE);
            }
        }

        // ── New: Standardize Click Listeners ──
        View btnNotifications = activity.findViewById(R.id.btnHeaderNotifications);
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> {
                if (!(activity instanceof NotificationActivity)) {
                    activity.startActivity(new android.content.Intent(activity, NotificationActivity.class));
                }
            });
        }

        View profileLink = activity.findViewById(R.id.containerProfileLink);
        if (profileLink != null) {
            profileLink.setOnClickListener(v -> {
                if (!(activity instanceof ProfileActivity)) {
                    activity.startActivity(new android.content.Intent(activity, ProfileActivity.class));
                }
            });
        }

        View btnExtra = activity.findViewById(R.id.btnHeaderExtra);
        if (btnExtra != null && btnExtra.getVisibility() == View.VISIBLE) {
             // Optional: shared behavior for extra button
        }
    }
 
    private static String getInitial(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.trim().split(" ");
        return String.valueOf(parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    public static void setupAdminHeader(Activity activity, String title) {
        MaterialToolbar toolbar = activity.findViewById(R.id.toolbar);

        if (toolbar != null) {
            toolbar.setTitle(title);
            toolbar.setNavigationOnClickListener(v -> activity.onBackPressed());
        }
    }
}
