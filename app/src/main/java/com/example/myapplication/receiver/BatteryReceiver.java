package com.example.myapplication.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.example.myapplication.utils.NotificationHelper;

public class BatteryReceiver extends BroadcastReceiver {
    private static boolean isNotified = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BATTERY_LOW.equals(action)) {
            if (!isNotified) {
                NotificationHelper.showNotification(
                        context,
                        NotificationHelper.BATTERY_NOTIF_ID,
                        "Cảnh báo Pin",
                        "Pin yếu! Hãy lưu công việc hoặc cắm sạc"
                );
                isNotified = true;
            }
        } else if (Intent.ACTION_BATTERY_OKAY.equals(action)) {
            isNotified = false;
            // Optionally cancel the notification when battery is okay
            // NotificationHelper.cancelNotification(context, NotificationHelper.BATTERY_NOTIF_ID);
        }
    }
}
