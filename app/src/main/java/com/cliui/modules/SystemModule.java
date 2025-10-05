package com.cliui.modules;

import android.content.Context;
import android.content.Intent;
import android.provider.AlarmClock;
import android.provider.Settings;
import com.cliui.utils.PermissionManager;
import com.cliui.utils.ShizukuManager;

import java.text.SimpleDateFormat;
import java.util.*;

public class SystemModule implements CommandModule {

    private Context context;
    private PermissionManager permissionManager;

    public SystemModule(Context context) {
        this.context = context;
        this.permissionManager = PermissionManager.getInstance(context);
    }

    @Override
    public String execute(String[] tokens) {
        if (tokens.length == 0) return getUsage();

        String command = tokens[0].toLowerCase();
        String fullCommand = String.join(" ", tokens).toLowerCase();

        // Check permissions using PermissionManager
        if (!permissionManager.canExecute(fullCommand)) {
            return permissionManager.getPermissionExplanation(fullCommand);
        }

        // Require authentication for system control
       PermissionManager permissionManager = PermissionManager.getInstance(context);
if (!permissionManager.authenticate("system_control")) {
            return "🔒 Authentication required for system control\n" +
                   "Please authenticate to modify system settings";
        }

        switch (command) {
            case "flash":
            case "flashlight":
                return toggleFlashlight();
            case "location":
                return toggleLocation();
            case "mic":
            case "microphone":
                return toggleMicrophone();
            case "camera":
                return toggleCamera();
            case "alarm":
                return handleAlarmCommand(tokens);
            case "time":
                return showTime();
            case "date":
                return showDate();
            case "reboot":
                return rebootSystem();
            case "shutdown":
                return shutdownSystem();
            case "airplane":
            case "airplanemode":
                return toggleAirplaneMode();
            case "battery":
                return showBatteryInfo();
            case "volume":
                return handleVolumeCommand(tokens);
            case "brightness":
                return handleBrightnessCommand(tokens);
            case "status":
                return getSystemStatus();
            default:
                return "❌ Unknown system command\n" + getUsage();
        }
    }

    // ===== Flashlight =====
    private String toggleFlashlight() {
        if (!ShizukuManager.isAvailable()) {
            return "❌ Shizuku not available\n" +
                   "Flashlight control requires system-level access via Shizuku";
        }

        boolean enabled = isFlashlightEnabled();
        boolean success = ShizukuManager.executeCommand(
            enabled ? "cmd flashlight disable" : "cmd flashlight enable"
        );
        
        if (success) {
            return "🔦 Flashlight " + (enabled ? "❌ OFF" : "✅ ON");
        } else {
            return "❌ Failed to toggle flashlight\n" +
                   "💡 This device may not support flashlight control via CLI";
        }
    }

    private boolean isFlashlightEnabled() {
        if (!ShizukuManager.isAvailable()) return false;
        
        // Try to get flashlight status
        String result = ShizukuManager.executeCommandWithOutput("dumpsys torch");
        return result != null && result.contains("active");
    }

    // ===== Location =====
    private String toggleLocation() {
        if (!ShizukuManager.isAvailable()) {
            return "❌ Shizuku not available\n" +
                   "Location control requires system-level access via Shizuku";
        }

        boolean enabled = isLocationEnabled();
        boolean success = ShizukuManager.executeCommand(
            enabled ? "settings put secure location_providers_allowed -gps" :
                      "settings put secure location_providers_allowed +gps"
        );
        
        if (success) {
            return "📍 Location " + (enabled ? "❌ OFF" : "✅ ON");
        } else {
            return "❌ Failed to toggle location\n" +
                   "💡 Opening system location settings...";
        }
    }

    private boolean isLocationEnabled() {
        if (!ShizukuManager.isAvailable()) return false;
        
        String result = ShizukuManager.executeCommandWithOutput("settings get secure location_providers_allowed");
        return result != null && result.contains("gps");
    }

    // ===== Microphone =====
    private String toggleMicrophone() {
        if (!ShizukuManager.isAvailable()) {
            return "❌ Shizuku not available\n" +
                   "Microphone control requires system-level access via Shizuku";
        }

        // This is a simplified implementation
        // Real microphone control would require audio policy management
        return "🎤 Microphone control requires advanced system permissions\n" +
               "💡 Use system settings for microphone management";
    }

    // ===== Camera =====
    private String toggleCamera() {
        // Camera control at system level is complex and requires device policy
        return "📷 Camera control requires device administrator permissions\n" +
               "💡 Use system settings or camera app for camera control";
    }

    // ===== Alarm =====
    private String handleAlarmCommand(String[] tokens) {
        if (tokens.length == 1) {
            return showAlarms();
        } else if (tokens.length >= 2) {
            String subCommand = tokens[1].toLowerCase();
            switch (subCommand) {
                case "set":
                    return setAlarm(tokens);
                case "list":
                    return showAlarms();
                case "clear":
                    return clearAlarms();
                default:
                    return "❌ Unknown alarm command\n" +
                           "💡 Usage: alarm set HH:MM [message] | alarm list | alarm clear";
            }
        }
        return getAlarmUsage();
    }

    private String showAlarms() {
        if (ShizukuManager.isAvailable()) {
            try {
                String alarms = ShizukuManager.executeCommandWithOutput("dumpsys alarm");
                if (alarms != null && alarms.contains("Alarm Stats")) {
                    return "⏰ Active Alarms:\n" + 
                           parseAlarmOutput(alarms) + 
                           "\n💡 Type 'alarm set HH:MM' to set new alarm";
                }
            } catch (Exception e) {
                // Fall through to intent method
            }
        }
        
        // Fallback: Open system alarm app
        Intent intent = new Intent(AlarmClock.ACTION_SHOW_ALARMS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        return "✅ Opening system alarm app...";
    }

    private String setAlarm(String[] tokens) {
        if (tokens.length < 3) {
            return "❌ Usage: alarm set HH:MM [message]\n" +
                   "💡 Example: alarm set 07:30 Wake up for work";
        }

        String time = tokens[2];
        String message = tokens.length > 3 ? 
            String.join(" ", Arrays.copyOfRange(tokens, 3, tokens.length)) : "Alarm";

        // Validate time format
        if (!time.matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
            return "❌ Invalid time format. Use HH:MM (24-hour format)";
        }

        // Use Android's alarm intent
        Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM);
        intent.putExtra(AlarmClock.EXTRA_HOUR, Integer.parseInt(time.split(":")[0]));
        intent.putExtra(AlarmClock.EXTRA_MINUTES, Integer.parseInt(time.split(":")[1]));
        intent.putExtra(AlarmClock.EXTRA_MESSAGE, message);
        intent.putExtra(AlarmClock.EXTRA_SKIP_UI, true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            context.startActivity(intent);
            return "✅ Alarm set for " + time + " - " + message;
        } catch (Exception e) {
            return "❌ Failed to set alarm\n" +
                   "💡 No alarm app available on this device";
        }
    }

    private String clearAlarms() {
        if (!ShizukuManager.isAvailable()) {
            return "❌ Shizuku not available\n" +
                   "Alarm clearing requires system-level access via Shizuku";
        }

        // This would require specific implementation for each alarm app
        return "🗑️ Alarm clearing not implemented\n" +
               "💡 Use system alarm app to manage alarms";
    }

    private String parseAlarmOutput(String alarmOutput) {
        // Simplified parsing - real implementation would parse dumpsys output
        if (alarmOutput.contains("RTC_WAKEUP")) {
            return "• Wake-up alarm(s) scheduled";
        } else if (alarmOutput.contains("ELAPSED")) {
            return "• Countdown timer(s) active";
        } else {
            return "• No readable alarms found\n" +
                   "💡 Use system alarm app for detailed view";
        }
    }

    // ===== Time & Date =====
    private String showTime() {
        if (ShizukuManager.isAvailable()) {
            String time = ShizukuManager.executeCommandWithOutput("date '+%H:%M:%S'");
            if (time != null) {
                return "🕒 Current Time: " + time.trim();
            }
        }
        
        // Fallback to Java date
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return "🕒 Current Time: " + sdf.format(new Date());
    }

    private String showDate() {
        if (ShizukuManager.isAvailable()) {
            String date = ShizukuManager.executeCommandWithOutput("date '+%A, %d %B %Y'");
            if (date != null) {
                return "📅 Current Date: " + date.trim();
            }
        }
        
        // Fallback to Java date
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMMM yyyy");
        return "📅 Current Date: " + sdf.format(new Date());
    }

    // ===== System Controls =====
    private String rebootSystem() {
        if (!ShizukuManager.isAvailable()) {
            return "❌ Shizuku not available\n" +
                   "System reboot requires root access via Shizuku";
        }

        if (ShizukuManager.executeCommand("reboot")) {
            return "🔄 System rebooting...";
        } else {
            return "❌ Reboot failed\n" +
                   "💡 Root access may be required for system reboot";
        }
    }

    private String shutdownSystem() {
        if (!ShizukuManager.isAvailable()) {
            return "❌ Shizuku not available\n" +
                   "System shutdown requires root access via Shizuku";
        }

        if (ShizukuManager.executeCommand("am start -a android.intent.action.ACTION_REQUEST_SHUTDOWN")) {
            return "⏻ Shutdown requested...";
        } else {
            return "❌ Shutdown failed\n" +
                   "💡 Root access may be required for system shutdown";
        }
    }

    private String toggleAirplaneMode() {
        if (!ShizukuManager.isAvailable()) {
            return "❌ Shizuku not available\n" +
                   "Airplane mode control requires system-level access via Shizuku";
        }

        boolean enabled = isAirplaneModeEnabled();
        boolean success = ShizukuManager.executeCommand(
            "settings put global airplane_mode_on " + (enabled ? "0" : "1") +
            " && am broadcast -a android.intent.action.AIRPLANE_MODE"
        );
        
        if (success) {
            return "✈️ Airplane Mode " + (enabled ? "❌ OFF" : "✅ ON");
        } else {
            return "❌ Failed to toggle airplane mode\n" +
                   "💡 Opening system settings...";
        }
    }

    private boolean isAirplaneModeEnabled() {
        if (!ShizukuManager.isAvailable()) return false;
        
        String result = ShizukuManager.executeCommandWithOutput("settings get global airplane_mode_on");
        return result != null && "1".equals(result.trim());
    }

    private String showBatteryInfo() {
        if (!ShizukuManager.isAvailable()) {
            return "❌ Shizuku not available\n" +
                   "Battery info requires system-level access via Shizuku";
        }

        String level = ShizukuManager.executeCommandWithOutput("dumpsys battery | grep level");
        String status = ShizukuManager.executeCommandWithOutput("dumpsys battery | grep status");
        String health = ShizukuManager.executeCommandWithOutput("dumpsys battery | grep health");
        
        StringBuilder info = new StringBuilder();
        info.append("🔋 Battery Information:\n");
        
        if (level != null) info.append("• Level: ").append(extractValue(level)).append("%\n");
        if (status != null) info.append("• Status: ").append(parseBatteryStatus(extractValue(status))).append("\n");
        if (health != null) info.append("• Health: ").append(parseBatteryHealth(extractValue(health))).append("\n");
        
        if (info.length() == 0) {
            info.append("• Information unavailable\n");
        }
        
        return info.toString();
    }

    private String handleVolumeCommand(String[] tokens) {
        if (tokens.length == 1) {
            return getVolumeLevels();
        } else if (tokens.length == 2) {
            try {
                int volume = Integer.parseInt(tokens[1]);
                return setMediaVolume(volume);
            } catch (NumberFormatException e) {
                return "❌ Invalid volume level. Use 0-100";
            }
        }
        return "❌ Usage: volume | volume <0-100>";
    }

    private String getVolumeLevels() {
        if (!ShizukuManager.isAvailable()) {
            return "❌ Shizuku not available\n" +
                   "Volume control requires system-level access via Shizuku";
        }

        String media = ShizukuManager.executeCommandWithOutput("media volume --stream 3 --get");
        String call = ShizukuManager.executeCommandWithOutput("media volume --stream 0 --get");
        String alarm = ShizukuManager.executeCommandWithOutput("media volume --stream 4 --get");
        
        StringBuilder volumes = new StringBuilder();
        volumes.append("🔊 Volume Levels:\n");
        
        if (media != null) volumes.append("• Media: ").append(media.trim()).append("/15\n");
        if (call != null) volumes.append("• Call: ").append(call.trim()).append("/15\n");
        if (alarm != null) volumes.append("• Alarm: ").append(alarm.trim()).append("/15\n");
        
        return volumes.toString();
    }

    private String setMediaVolume(int volume) {
        if (volume < 0 || volume > 100) {
            return "❌ Volume must be between 0-100";
        }

        if (!ShizukuManager.isAvailable()) {
            return "❌ Shizuku not available\n" +
                   "Volume control requires system-level access via Shizuku";
        }

        int systemVolume = (int) (volume * 0.15);
        boolean success = ShizukuManager.executeCommand("media volume --stream 3 --set " + systemVolume);
        
        if (success) {
            return "✅ Media volume set to " + volume + "%";
        } else {
            return "❌ Failed to set volume\n" +
                   "💡 Opening system sound settings...";
        }
    }

    private String handleBrightnessCommand(String[] tokens) {
        if (tokens.length == 1) {
            return getCurrentBrightness();
        } else if (tokens.length == 2) {
            try {
                int brightness = Integer.parseInt(tokens[1]);
                return setBrightness(brightness);
            } catch (NumberFormatException e) {
                return "❌ Invalid brightness level. Use 0-100";
            }
        }
        return "❌ Usage: brightness | brightness <0-100>";
    }

    private String getCurrentBrightness() {
        if (!ShizukuManager.isAvailable()) {
            return "❌ Shizuku not available\n" +
                   "Brightness control requires system-level access via Shizuku";
        }

        String result = ShizukuManager.executeCommandWithOutput("settings get system screen_brightness");
        if (result != null) {
            try {
                int systemBrightness = Integer.parseInt(result.trim());
                int percent = (int) (systemBrightness / 2.55);
                return "💡 Current Brightness: " + percent + "%";
            } catch (NumberFormatException e) {
                // Fall through
            }
        }
        return "💡 Brightness information unavailable";
    }

    private String setBrightness(int brightness) {
        if (brightness < 0 || brightness > 100) {
            return "❌ Brightness must be between 0-100";
        }

        if (!ShizukuManager.isAvailable()) {
            return "❌ Shizuku not available\n" +
                   "Brightness control requires system-level access via Shizuku";
        }

        int systemBrightness = (int) (brightness * 2.55);
        boolean success = ShizukuManager.executeCommand("settings put system screen_brightness " + systemBrightness);
        
        if (success) {
            return "✅ Brightness set to " + brightness + "%";
        } else {
            return "❌ Failed to set brightness\n" +
                   "💡 Opening system display settings...";
        }
    }

    private String getSystemStatus() {
        StringBuilder status = new StringBuilder();
        status.append("🖥️ System Status:\n");
        
        // Location
        status.append("📍 Location: ").append(isLocationEnabled() ? "✅ ON" : "❌ OFF").append("\n");
        
        // Flashlight
        status.append("🔦 Flashlight: ").append(isFlashlightEnabled() ? "✅ ON" : "❌ OFF").append("\n");
        
        // Airplane Mode
        status.append("✈️ Airplane Mode: ").append(isAirplaneModeEnabled() ? "✅ ON" : "❌ OFF").append("\n");
        
        // Shizuku Status
        status.append("🔧 Shizuku: ").append(ShizukuManager.isAvailable() ? "✅ Available" : "❌ Not Available").append("\n");
        
        // Time
        status.append("🕒 ").append(showTime().replace("🕒 Current Time: ", "")).append("\n");
        
        // Date
        status.append("📅 ").append(showDate().replace("📅 Current Date: ", ""));
        
        return status.toString();
    }

    // ===== Utility Methods =====
    private String extractValue(String input) {
        if (input == null) return "Unknown";
        String[] parts = input.split(":");
        return parts.length > 1 ? parts[1].trim() : "Unknown";
    }

    private String parseBatteryStatus(String status) {
        switch (status) {
            case "2": return "Charging";
            case "3": return "Discharging";
            case "4": return "Not charging";
            case "5": return "Full";
            default: return "Unknown";
        }
    }

    private String parseBatteryHealth(String health) {
        switch (health) {
            case "2": return "Good";
            case "3": return "Overheat";
            case "4": return "Dead";
            case "5": return "Over voltage";
            case "6": return "Unspecified failure";
            case "7": return "Cold";
            default: return "Unknown";
        }
    }

    private String getAlarmUsage() {
        return "⏰ Alarm Commands:\n" +
               "• alarm list          - Show active alarms\n" +
               "• alarm set HH:MM     - Set new alarm\n" +
               "• alarm set HH:MM msg - Set alarm with message\n" +
               "• alarm clear         - Clear all alarms (Shizuku)";
    }

    private String getUsage() {
        return "🖥️ System Module Usage:\n" +
               "• flash/flashlight    - Toggle flashlight (Shizuku)\n" +
               "• location            - Toggle location (Shizuku)\n" +
               "• alarm set HH:MM     - Set alarm\n" +
               "• alarm list          - Show alarms\n" +
               "• time                - Show current time\n" +
               "• date                - Show current date\n" +
               "• volume [0-100]      - Set media volume (Shizuku)\n" +
               "• brightness [0-100]  - Set brightness (Shizuku)\n" +
               "• airplane            - Toggle airplane mode (Shizuku)\n" +
               "• battery             - Show battery info (Shizuku)\n" +
               "• status              - System status overview\n" +
               "• reboot              - Reboot system (Root)\n" +
               "• shutdown            - Shutdown system (Root)\n" +
               "\n🔒 Requires: Authentication + System permissions" +
               "\n🔧 Shizuku: " + (ShizukuManager.isAvailable() ? "Available ✅" : "Not Available ❌") +
               "\n⚠️  Some features require root access";
    }
}
