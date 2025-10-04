package com.cliui.modules;

import android.content.Context;
import com.cliui.utils.PermissionManager;

public class SystemModule implements CommandModule {

    private Context context;
    private PermissionManager permissionManager;

    public SystemModule(Context context) {
        this.context = context;
        this.permissionManager = PermissionManager.getInstance(context);
    }

    @Override
    public String execute(String[] tokens) {
        if (tokens.length == 0) return "❌ No command provided";

        String command = tokens[0].toLowerCase();

        if (!permissionManager.canExecute(command)) {
            return permissionManager.getPermissionExplanation(command);
        }

        switch (command) {
            case "flash":
                return toggleFlashlight();
            case "location":
                return toggleLocation();
            case "mic":
                return toggleMicrophone();
            case "camera":
                return toggleCamera();
            case "alarm":
                return handleAlarmCommand(tokens);
            case "time":
                return showTime();
            case "date":
                return showDate();
            default:
                return "❌ Unknown system command";
        }
    }

    // ===== Flashlight =====
    private String toggleFlashlight() {
        boolean enabled = isFlashlightEnabled();
        boolean success = ShizukuManager.isAvailable() &&
                          ShizukuManager.executeCommand(enabled ? "cmd flashlight disable" : "cmd flashlight enable");
        return success ? "🔦 Flashlight " + (enabled ? "OFF" : "ON")
                       : "❌ Failed to toggle flashlight";
    }

    private boolean isFlashlightEnabled() {
        // Placeholder: return false as default
        return false;
    }

    // ===== Location =====
    private String toggleLocation() {
        boolean enabled = isLocationEnabled();
        boolean success = ShizukuManager.isAvailable() &&
                          ShizukuManager.executeCommand(
                              enabled ? "settings put secure location_providers_allowed -gps" :
                                        "settings put secure location_providers_allowed +gps"
                          );
        return success ? "📍 Location " + (enabled ? "OFF" : "ON")
                       : "❌ Failed to toggle location";
    }

    private boolean isLocationEnabled() {
        // Placeholder: return false as default
        return false;
    }

    // ===== Microphone =====
    private String toggleMicrophone() {
        // Advanced audio routing requires more setup
        return "🎤 Microphone control requires advanced permissions";
    }

    // ===== Camera =====
    private String toggleCamera() {
        // Placeholder: actual implementation depends on camera API
        return "📷 Camera control requires user interaction or advanced permissions";
    }

    // ===== Alarm =====
    private String handleAlarmCommand(String[] tokens) {
        if (tokens.length == 1) {
            return showAlarms();
        } else if (tokens.length >= 2) {
            return setAlarm(tokens);
        }
        return "Usage: alarm | alarm set HH:MM [message]";
    }

    private String showAlarms() {
        if (ShizukuManager.isAvailable()) {
            String alarms = ShizukuManager.executeCommandWithOutput("dumpsys alarm");
            return "⏰ Active Alarms:\n" + alarms + "\n💡 Type 'alarm set HH:MM' to set new alarm";
        }
        return "🔧 Cannot fetch alarms, Shizuku not available";
    }

    private String setAlarm(String[] tokens) {
        // Placeholder: actual alarm setting would require parsing and sending intent
        return "⏰ Setting alarm is not implemented yet";
    }

    // ===== Time & Date =====
    private String showTime() {
        if (ShizukuManager.isAvailable()) {
            String time = ShizukuManager.executeCommandWithOutput("date '+%H:%M:%S'");
            return "🕒 Current Time: " + time.trim();
        }
        return "🔧 Cannot get time, Shizuku not available";
    }

    private String showDate() {
        if (ShizukuManager.isAvailable()) {
            String date = ShizukuManager.executeCommandWithOutput("date '+%d-%b-%Y'");
            return "📅 Current Date: " + date.trim();
        }
        return "🔧 Cannot get date, Shizuku not available";
    }
}
