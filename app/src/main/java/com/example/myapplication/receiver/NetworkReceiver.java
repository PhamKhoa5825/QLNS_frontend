package com.example.myapplication.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.example.myapplication.utils.NotificationHelper;

public class NetworkReceiver extends BroadcastReceiver {
    private static boolean wasOffline = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

            if (!isConnected) {
                NotificationHelper.showNotification(
                        context,
                        NotificationHelper.NETWORK_NOTIF_ID,
                        "Kết nối mạng",
                        "Mất kết nối mạng"
                );
                wasOffline = true;
            } else {
                if (wasOffline) {
                    NotificationHelper.showNotification(
                            context,
                            NotificationHelper.NETWORK_NOTIF_ID,
                            "Kết nối mạng",
                            "Đã kết nối lại"
                    );
                    wasOffline = false;
                }
            }
        }
    }
}
