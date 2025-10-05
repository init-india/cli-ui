package com.cliui.utils;

import android.content.Context;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationManager extends NotificationListenerService {
    private static final String TAG = "NotificationManager";
    private static NotificationManager instance;
    
    // Permanent storage for all notifications
    private static final List<NotificationItem> allNotifications = Collections.synchronizedList(new ArrayList<>());
    
    // Live feed buffer for home screen display
    private static final List<String> liveFeedBuffer = Collections.synchronizedList(new ArrayList<>());
    
    private Context context;
    private boolean isHomeScreenActive = false;
    private SimpleDateFormat timeFormat;
    
    // Notification item structure
    private static class NotificationItem {
        long timestamp;
        String packageName;
        String title;
        String text;
        String type;
        
        NotificationItem(long timestamp, String packageName, String title, String text) {
            this.timestamp = timestamp;
            this.packageName = packageName;
            this.title = title;
            this.text = text;
            this.type = determineType(packageName, title);
        }
        
        private String determineType(String packageName, String title) {
            if (packageName.contains("sms") || packageName.contains("mms") || title.toLowerCase().contains("sms")) {
                return "📱";
            } else if (packageName.contains("dialer") || packageName.contains("call") || title.toLowerCase().contains("call")) {
                return "📞";
            } else if (packageName.contains("email") || packageName.contains("gmail") || title.toLowerCase().contains("email")) {
                return "📧";
            } else if (packageName.contains("whatsapp")) {
                return "💬";
            } else if (packageName.contains("battery") || title.toLowerCase().contains("battery")) {
                return "⚡";
            } else if (packageName.contains("location") || title.toLowerCase().contains("location")) {
                return "📍";
            } else if (packageName.contains("system") || packageName.contains("android")) {
                return "🔄";
            } else {
                return "📢";
            }
        }
        
        String toCLIFormat() {
            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
            return String.format("[%s] %s %s: %s", time, type, title, text);
        }
    }
    
    public NotificationManager() {}
    
    public NotificationManager(Context context) {
        this.context = context;
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        instance = this;
    }
    
    public static NotificationManager getInstance(Context context) {
        if (instance == null) {
            instance = new NotificationManager(context);
        }
        return instance;
    }
    
    // Called when a new notification is posted
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            String packageName = sbn.getPackageName();
            String title = extractTitle(sbn);
            String text = extractText(sbn);
            long timestamp = sbn.getPostTime();
            
            // Create notification item
            NotificationItem item = new NotificationItem(timestamp, packageName, title, text);
            String cliFormat = item.toCLIFormat();
            
            Log.d(TAG, "New notification: " + cliFormat);
            
            // Always store in permanent storage
            synchronized (allNotifications) {
                allNotifications.add(item);
            }
            
            // Add to live feed if home screen is active
            if (isHomeScreenActive) {
                addToLiveFeed(cliFormat);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing notification", e);
        }
    }
    
    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Notifications persist even when dismissed from system
    }
    
    @Override
    public void onListenerConnected() {
        Log.i(TAG, "Notification listener connected");
        loadExistingNotifications();
    }
    
    private void loadExistingNotifications() {
        try {
            StatusBarNotification[] activeNotifications = getActiveNotifications();
            if (activeNotifications != null) {
                for (StatusBarNotification sbn : activeNotifications) {
                    onNotificationPosted(sbn);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading existing notifications", e);
        }
    }
    
    private String extractTitle(StatusBarNotification sbn) {
        try {
            CharSequence title = sbn.getNotification().extras.getCharSequence("android.title");
            return title != null ? title.toString() : "Notification";
        } catch (Exception e) {
            return "Notification";
        }
    }
    
    private String extractText(StatusBarNotification sbn) {
        try {
            CharSequence text = sbn.getNotification().extras.getCharSequence("android.text");
            return text != null ? text.toString() : "";
        } catch (Exception e) {
            return "";
        }
    }
    
    // Home screen state management
    public void setHomeScreenActive(boolean active) {
        this.isHomeScreenActive = active;
        if (active) {
            refreshLiveFeed();
        }
    }
    
    private void refreshLiveFeed() {
        synchronized (liveFeedBuffer) {
            liveFeedBuffer.clear();
            // Add all notifications to live feed
            synchronized (allNotifications) {
                for (NotificationItem item : allNotifications) {
                    liveFeedBuffer.add(item.toCLIFormat());
                }
            }
        }
    }
    
    private void addToLiveFeed(String notification) {
        synchronized (liveFeedBuffer) {
            // Add to bottom - live feed grows indefinitely
            liveFeedBuffer.add(notification);
        }
    }
    
    // Clear screen display only
    public String clearScreen() {
        synchronized (liveFeedBuffer) {
            int clearedCount = liveFeedBuffer.size();
            liveFeedBuffer.clear();
            return "Screen cleared. " + allNotifications.size() + " notifications remain in storage.";
        }
    }
    
    // Get current live feed for home screen display
    public List<String> getLiveFeed() {
        synchronized (liveFeedBuffer) {
            return new ArrayList<>(liveFeedBuffer);
        }
    }
    
    public boolean hasNotifications() {
        return !allNotifications.isEmpty();
    }
    
    public int getNotificationCount() {
        return allNotifications.size();
    }
    
    // Get all notifications for manual viewing (if needed later)
    public String getAllNotifications() {
        if (allNotifications.isEmpty()) {
            return "No notifications yet.";
        }
        
        StringBuilder sb = new StringBuilder();
        // Show all notifications in chronological order
        synchronized (allNotifications) {
            for (NotificationItem item : allNotifications) {
                sb.append(item.toCLIFormat()).append("\n");
            }
        }
        
        return sb.toString().trim();
    }
}
