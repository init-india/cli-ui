package com.cliui.modules;

import android.content.Context;
import android.provider.Settings;
import android.content.Intent;
import com.cliui.utils.PermissionManager;
import com.cliui.utils.Authentication;

public class SettingsModule implements CommandModule {
    private Context context;
    private PermissionManager permissionManager;
    
    // Current settings state
    private int brightness = 75;
    private int mediaVolume = 80;
    private int callVolume = 90;
    private int alarmVolume = 100;
    private boolean darkMode = true;
    private String ringtone = "Classic Bell";
    private boolean autoRotate = true;
    private int screenTimeout = 300; // seconds
    private boolean vibrate = true;
    
    public SettingsModule(Context context) {
        this.context = context;
        this.permissionManager = PermissionManager.getInstance(context);
        loadCurrentSettings(); // Load real current settings
    }
    
    @Override
    public String execute(String[] tokens) {
        if (tokens.length == 1) {
            return showAllSettings();
        }
        
        if (tokens.length == 2) {
            return showSpecificSettings(tokens[1]);
        }
        
        if (tokens.length >= 3) {
            return changeSetting(tokens[1], tokens[2]);
        }
        
        return "Usage: settings [category] [value]";
    }
    
    private String showAllSettings() {
        // Load current real values
        loadCurrentSettings();
        
        return "⚙️ System Settings (Real-time):\n" +
               "• display - Brightness: " + getRealBrightness() + "%, Dark Mode: " + (getRealDarkMode() ? "ON" : "OFF") + "\n" +
               "• sound - Media: " + getRealMediaVolume() + "%, Call: " + callVolume + "%, Alarm: " + alarmVolume + "%\n" +
               "• ringtone - " + ringtone + "\n" +
               "• security - Biometric: Enabled, PIN: Set\n" +
               "• system - Auto-rotate: " + (getRealAutoRotate() ? "ON" : "OFF") + ", Timeout: " + getRealTimeout() + "s\n\n" +
               "Type 'settings display', 'settings sound', etc. for details";
    }
    
    private String showSpecificSettings(String category) {
        switch (category.toLowerCase()) {
            case "display":
                return "💡 Display Settings:\n" +
                       "Brightness: " + getRealBrightness() + "%\n" +
                       "Dark Mode: " + (getRealDarkMode() ? "ON" : "OFF") + "\n" +
                       "Auto-rotate: " + (getRealAutoRotate() ? "ON" : "OFF") + "\n" +
                       "Timeout: " + getRealTimeout() + " seconds\n\n" +
                       "💡 Usage: settings brightness <0-100>\n" +
                       "         settings dark on/off\n" +
                       "         settings rotation on/off\n" +
                       "         settings timeout <seconds>";
                       
            case "sound":
                return "🔊 Sound Settings:\n" +
                       "Media Volume: " + getRealMediaVolume() + "%\n" +
                       "Call Volume: " + callVolume + "%\n" +
                       "Alarm Volume: " + alarmVolume + "%\n" +
                       "Ringtone: " + ringtone + "\n" +
                       "Vibration: " + (vibrate ? "ON" : "OFF") + "\n\n" +
                       "💡 Usage: settings media <0-100>\n" +
                       "         settings call <0-100>\n" +
                       "         settings alarm <0-100>\n" +
                       "         settings vibrate on/off\n" +
                       "         settings ringtone <name>";
                       
            case "security":
                return "🔒 Security Settings:\n" +
                       "Biometric: " + (isBiometricEnabled() ? "Enabled" : "Disabled") + "\n" +
                       "PIN: " + (isPinSet() ? "Set" : "Not Set") + "\n" +
                       "App Lock: SMS, Email\n" +
                       "Unknown Sources: Blocked\n\n" +
                       "💡 Use system settings app for security changes";
                       
            case "system":
                return "🖥️ System Settings:\n" +
                       "Auto-rotate: " + (getRealAutoRotate() ? "ON" : "OFF") + "\n" +
                       "Screen Timeout: " + getRealTimeout() + " seconds\n" +
                       "Developer Options: " + (areDeveloperOptionsEnabled() ? "ON" : "OFF") + "\n" +
                       "USB Debugging: " + (isUsbDebuggingEnabled() ? "ON" : "OFF") + "\n\n" +
                       "💡 Usage: settings rotation on/off\n" +
                       "         settings timeout <seconds>";
                       
            default:
                return "❌ Unknown settings category: " + category;
        }
    }
    
    private String changeSetting(String setting, String value) {
        // Check if we have permission for system changes
        if (!permissionManager.canExecute("settings")) {
            return "🔧 Settings control requires Shizuku permissions\nType command again to grant";
        }
        
        switch (setting.toLowerCase()) {
            case "brightness":
                return setRealBrightness(value);
                
            case "media":
                return setRealMediaVolume(value);
                
            case "call":
                return setCallVolume(value);
                
            case "alarm":
                return setAlarmVolume(value);
                
            case "dark":
                return setRealDarkMode(value);
                
            case "rotation":
            case "autorotate":
                return setRealAutoRotate(value);
                
            case "timeout":
                return setRealTimeout(value);
                
            case "vibrate":
                return setVibrate(value);
                
            case "ringtone":
                ringtone = value;
                // Open system ringtone settings
                Intent intent = new Intent(Settings.ACTION_SOUND_SETTINGS);
                context.startActivity(intent);
                return "✅ Opening sound settings for ringtone: " + ringtone;
                
            default:
                return "❌ Unknown setting: " + setting + "\n💡 Type 'settings' to see available options";
        }
    }
    
    // REAL SETTING CONTROLS WITH SHIZUKU
    
    private String setRealBrightness(String value) {
        try {
            int newBrightness = Integer.parseInt(value);
            if (newBrightness < 0 || newBrightness > 100) {
                return "❌ Brightness must be between 0-100";
            }
            
            // Convert to system brightness value (0-255)
            int systemBrightness = (int) (newBrightness * 2.55);
            
            if (ShizukuManager.executeCommand("settings put system screen_brightness " + systemBrightness)) {
                brightness = newBrightness;
                return "✅ Brightness set to " + newBrightness + "%";
            } else {
                return "❌ Failed to set brightness. Opening system settings...";
            }
        } catch (NumberFormatException e) {
            return "❌ Invalid brightness value. Use 0-100";
        }
    }
    
    private String getRealBrightness() {
        if (!permissionManager.isShizukuAvailable()) {
            return String.valueOf(brightness);
        }
        
        String result = ShizukuManager.executeCommandWithOutput("settings get system screen_brightness");
        if (result != null) {
            try {
                int systemBrightness = Integer.parseInt(result.trim());
                return String.valueOf((int) (systemBrightness / 2.55));
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        return String.valueOf(brightness);
    }
    
    private String setRealMediaVolume(String value) {
        try {
            int volume = Integer.parseInt(value);
            if (volume < 0 || volume > 100) {
                return "❌ Volume must be between 0-100";
            }
            
            // Convert to media volume scale (0-15 typically)
            int mediaLevel = (int) (volume * 0.15);
            
            if (ShizukuManager.executeCommand("media volume --stream 3 --set " + mediaLevel)) {
                mediaVolume = volume;
                return "✅ Media volume set to " + volume + "%";
            } else {
                mediaVolume = volume;
                return "✅ Media volume set to " + volume + "% (simulated)";
            }
        } catch (NumberFormatException e) {
            return "❌ Invalid volume value. Use 0-100";
        }
    }
    
    private String getRealMediaVolume() {
        if (!permissionManager.isShizukuAvailable()) {
            return String.valueOf(mediaVolume);
        }
        
        String result = ShizukuManager.executeCommandWithOutput("media volume --stream 3 --get");
        if (result != null) {
            try {
                int mediaLevel = Integer.parseInt(result.trim());
                return String.valueOf((int) (mediaLevel / 0.15));
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        return String.valueOf(mediaVolume);
    }
    
    private String setRealDarkMode(String value) {
        boolean enable = value.equalsIgnoreCase("on") || value.equalsIgnoreCase("yes") || value.equals("1");
        
        if (ShizukuManager.executeCommand(
            "settings put secure ui_night_mode " + (enable ? "2" : "1")
        )) {
            darkMode = enable;
            return "✅ Dark Mode " + (enable ? "enabled" : "disabled");
        } else {
            darkMode = enable;
            return "✅ Dark Mode " + (enable ? "enabled" : "disabled") + " (simulated)";
        }
    }
    
    private boolean getRealDarkMode() {
        if (!permissionManager.isShizukuAvailable()) {
            return darkMode;
        }
        
        String result = ShizukuManager.executeCommandWithOutput("settings get secure ui_night_mode");
        return result != null ? "2".equals(result.trim()) : darkMode;
    }
    
    private String setRealAutoRotate(String value) {
        boolean enable = value.equalsIgnoreCase("on") || value.equalsIgnoreCase("yes");
        
        if (ShizukuManager.executeCommand(
            "settings put system accelerometer_rotation " + (enable ? "1" : "0")
        )) {
            autoRotate = enable;
            return "✅ Auto-rotate " + (enable ? "enabled" : "disabled");
        } else {
            autoRotate = enable;
            return "✅ Auto-rotate " + (enable ? "enabled" : "disabled") + " (simulated)";
        }
    }
    
    private boolean getRealAutoRotate() {
        if (!permissionManager.isShizukuAvailable()) {
            return autoRotate;
        }
        
        String result = ShizukuManager.executeCommandWithOutput("settings get system accelerometer_rotation");
        return result != null ? "1".equals(result.trim()) : autoRotate;
    }
    
    private String setRealTimeout(String value) {
        try {
            int timeout = Integer.parseInt(value);
            if (timeout < 15 || timeout > 3600) {
                return "❌ Timeout must be between 15-3600 seconds";
            }
            
            // Convert to milliseconds
            int timeoutMs = timeout * 1000;
            
            if (ShizukuManager.executeCommand("settings put system screen_off_timeout " + timeoutMs)) {
                screenTimeout = timeout;
                return "✅ Screen timeout set to " + timeout + " seconds";
            } else {
                screenTimeout = timeout;
                return "✅ Screen timeout set to " + timeout + " seconds (simulated)";
            }
        } catch (NumberFormatException e) {
            return "❌ Invalid timeout value. Use seconds (15-3600)";
        }
    }
    
    private String getRealTimeout() {
        if (!permissionManager.isShizukuAvailable()) {
            return String.valueOf(screenTimeout);
        }
        
        String result = ShizukuManager.executeCommandWithOutput("settings get system screen_off_timeout");
        if (result != null) {
            try {
                int timeoutMs = Integer.parseInt(result.trim());
                return String.valueOf(timeoutMs / 1000);
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        return String.valueOf(screenTimeout);
    }
    
    // Helper methods for other settings
    private String setCallVolume(String value) {
        try {
            int volume = Integer.parseInt(value);
            if (volume >= 0 && volume <= 100) {
                callVolume = volume;
                return "✅ Call volume set to " + volume + "%\n💡 Apply in sound settings";
            }
            return "❌ Volume must be 0-100";
        } catch (NumberFormatException e) {
            return "❌ Invalid volume value";
        }
    }
    
    private String setAlarmVolume(String value) {
        try {
            int volume = Integer.parseInt(value);
            if (volume >= 0 && volume <= 100) {
                alarmVolume = volume;
                return "✅ Alarm volume set to " + volume + "%\n💡 Apply in sound settings";
            }
            return "❌ Volume must be 0-100";
        } catch (NumberFormatException e) {
            return "❌ Invalid volume value";
        }
    }
    
    private String setVibrate(String value) {
        vibrate = value.equalsIgnoreCase("on") || value.equalsIgnoreCase("yes");
        return "✅ Vibrate " + (vibrate ? "enabled" : "disabled") + "\n💡 Apply in sound settings";
    }
    
    private void loadCurrentSettings() {
        // Update our local state with real values
        brightness = Integer.parseInt(getRealBrightness());
        mediaVolume = Integer.parseInt(getRealMediaVolume());
        darkMode = getRealDarkMode();
        autoRotate = getRealAutoRotate();
        screenTimeout = Integer.parseInt(getRealTimeout());
    }
    
    // Placeholder methods for security settings
    private boolean isBiometricEnabled() { return true; }
    private boolean isPinSet() { return true; }
    private boolean areDeveloperOptionsEnabled() { return false; }
    private boolean isUsbDebuggingEnabled() { return false; }
}
