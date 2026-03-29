package com.example.myapplication.utils;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.myapplication.R;
import com.example.myapplication.ui.ChatActivity;
import com.example.myapplication.ui.DashboardActivity;
import com.example.myapplication.ui.EmployeeActivity;
import com.example.myapplication.ui.TaskActivity;
import com.example.myapplication.ui.RequestActivity;
import com.example.myapplication.ui.AdminMainActivity;
import android.widget.FrameLayout;

public class BottomNavHelper {

    public static void setupBottomNav(Activity activity, int currentNavItemId) {
        FrameLayout navHome = activity.findViewById(R.id.nav_home);
        FrameLayout navPeople = activity.findViewById(R.id.nav_people);
        FrameLayout navChat = activity.findViewById(R.id.nav_chat);
        FrameLayout navTasks = activity.findViewById(R.id.nav_tasks);
        FrameLayout navRequest = activity.findViewById(R.id.nav_request);
        FrameLayout navAdmin = activity.findViewById(R.id.nav_admin);

        if (navHome == null) return; // Bottom nav not presented

        // Handle Admin visibility
        String role = SharedPrefsManager.getInstance(activity).getRole();
        if ("ADMIN".equals(role) && navAdmin != null) {
            navAdmin.setVisibility(View.VISIBLE);
        }

        // Set click listeners
        navHome.setOnClickListener(v -> navigate(activity, currentNavItemId, R.id.nav_home, DashboardActivity.class));
        navPeople.setOnClickListener(v -> navigate(activity, currentNavItemId, R.id.nav_people, EmployeeActivity.class));
        navChat.setOnClickListener(v -> navigate(activity, currentNavItemId, R.id.nav_chat, ChatActivity.class));
        navTasks.setOnClickListener(v -> navigate(activity, currentNavItemId, R.id.nav_tasks, TaskActivity.class));
        navRequest.setOnClickListener(v -> navigate(activity, currentNavItemId, R.id.nav_request, RequestActivity.class));
        if (navAdmin != null) {
            navAdmin.setOnClickListener(v -> navigate(activity, currentNavItemId, R.id.nav_admin, AdminMainActivity.class));
        }

        // Update visual states
        updateItemState(activity, activity.findViewById(R.id.pill_home), R.id.icon_home, R.id.text_home, currentNavItemId == R.id.nav_home);
        updateItemState(activity, activity.findViewById(R.id.pill_people), R.id.icon_people, R.id.text_people, currentNavItemId == R.id.nav_people);
        updateItemState(activity, activity.findViewById(R.id.pill_chat), R.id.icon_chat, R.id.text_chat, currentNavItemId == R.id.nav_chat);
        updateItemState(activity, activity.findViewById(R.id.pill_tasks), R.id.icon_tasks, R.id.text_tasks, currentNavItemId == R.id.nav_tasks);
        updateItemState(activity, activity.findViewById(R.id.pill_request), R.id.icon_request, R.id.text_request, currentNavItemId == R.id.nav_request);
        if (navAdmin != null && navAdmin.getVisibility() == View.VISIBLE) {
            updateItemState(activity, activity.findViewById(R.id.pill_admin), R.id.icon_admin, R.id.text_admin, currentNavItemId == R.id.nav_admin);
        }
    }

    private static void navigate(Activity activity, int currentId, int targetId, Class<?> targetClass) {
        if (currentId != targetId) {
            Intent intent = new Intent(activity, targetClass);
            // Using REORDER_TO_FRONT prevents the Activities from being destroyed and recreated.
            // When returning to Home or any tab, it just brings the cached instance to the front!
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            activity.startActivity(intent);
            activity.overridePendingTransition(0, 0);
        }
    }

    private static void updateItemState(Activity activity, LinearLayout navItem, int iconId, int textId, boolean isActive) {
        ImageView icon = activity.findViewById(iconId);
        TextView text = activity.findViewById(textId);

        if (icon == null || text == null) return;

        int colorValue = ContextCompat.getColor(activity, isActive ? R.color.primary : R.color.secondary);
        
        icon.setImageTintList(ColorStateList.valueOf(colorValue));
        text.setTextColor(colorValue);

        if (isActive) {
            navItem.setBackgroundResource(R.drawable.bg_bottom_nav_active);
            text.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            navItem.setBackground(null);
            text.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }
}
