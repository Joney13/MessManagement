package com.joney.messmanagement;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

import androidx.multidex.MultiDexApplication;

import com.google.firebase.FirebaseApp;

public class MyApplication extends MultiDexApplication {
    private static final String TAG = "MyApplication";
    public static final String CHANNEL_ID = "mess_management_channel";

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            // Initialize Firebase
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this);
                Log.d(TAG, "Firebase initialized successfully");
            }

            // Create notification channel
            createNotificationChannel();

        } catch (Exception e) {
            Log.e(TAG, "Error initializing application", e);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.default_notification_channel_name);
            String description = "Channel for Mess Management notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

            Log.d(TAG, "Notification channel created");
        }
    }
}