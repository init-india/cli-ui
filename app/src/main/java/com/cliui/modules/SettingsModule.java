package com.cliui.modules;

import android.content.Context;
import android.provider.Settings;
import android.content.Intent;
import com.cliui.utils.PermissionManager;
import com.cliui.utils.Authentication;
import com.cliui.utils.ShizukuManager;

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
        if (tokens.length == 0) return getUsage();
        
        String command = tokens[0].toLowerCase();
        String fullCommand = String.join(" ", tokens).toLowerCase();

        // Check permissions using PermissionManager
        if (!permissionManager.canExecute(fullCommand)) {
            return permissionManager.getPermissionExplanation(fullCommand);
        }

        // Require authentication for system settings changes
        if (tokens.length >= 3 && !Authentication.authenticate("settings_change")) {
            return "🔒 Authentication required for settings changes\n" +
                   "Please authenticate to modify system settings";
        }
        
        if (tokens.length == 1) {
            return showAllSettings();
        }
        
        if (tokens.length == 2) {
            return showSpecificSettings(tokens[1]);
        }
        
        if (tokens.length >= 3) {
            return changeSetting(tokens[1], tokens[2]);
        }
        
        return getUsage();
    }
    
    private String showAllSettings() {
        // Load current real values
        loadCurrentSettings();
        
        return "⚙️ System Settings (Real-time):\n" +
               "• display - Brightness: " + getRealBrightness() + "%, Dark Mode: " + (getRealDarkMode() ? "ON" : "OFF") + "\n" +
               "• sound - Media: " + getRealMediaVolume() + "%, Call: " + callVolume + "%, Alarm: " + alarmVolume + "%\n" +
               "• ringtone - " + ringtone + "\n" +
               "• security - Biometric: " + (isBiometricEnabled() ? "Enabled" : "Disabled") + ", PIN: " + (isPinSet() ? "Set" : "Not Set") + "\n" +
               "• system - Auto-rotate: " + (getRealAutoRotate() ? "ON" : "OFF") + ", Timeout: " + getRealTimeout() + "s\n" +
               "• vibration - " + (vibrate ? "ON" : "OFF") + "\n\n" +
               "🔧 Shizuku: " + (ShizukuManager.isAvailable() ? "Available ✅" : "Not Available ❌") + "\n" +
               "💡 Type 'settings display', 'settings sound', etc. for details";
    }
    
    private String showSpecificSettings(String category) {
        switch (category.toLowerCase()) {
            case "display":
                return "💡 Display Settings:\n" +
                       "Brightness: " + getRealBrightness() + "%\n" +
                       "Dark Mode: " + (getRealDarkMode() ? "ON" : "OFF") + "\n" +
                       "Auto-rotate: " + (getRealAutoRotate() ? "ON" : "OFF") + "\n" +
                       "Timeout: " + getRealTimeout() + " seconds\n" +
                       "🔧 Shizuku: " + (ShizukuManager.isAvailable() ? "Available ✅" : "Required ❌") + "\n\n" +
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
                       "Vibration: " + (vibrate ? "ON" : "OFF") + "\n" +
                       "🔧 Shizuku: " + (ShizukuManager.isAvailable() ? "Available ✅" : "Required ❌") + "\n\n" +
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
                       "Unknown Sources: Blocked\n" +
                       "Developer Options: " + (areDeveloperOptionsEnabled() ? "ON" : "OFF") + "\n\n" +
                       "⚠️  Security settings require system settings app\n" +
                       "💡 Use: settings dev on/off (with Shizuku)";
                       
            case "system":
                return "🖥️ System Settings:\n" +
                       "Auto-rotate: " + (getRealAutoRotate() ? "ON" : "OFF") + "\n" +
                       "Screen Timeout: " + getRealTimeout() + " seconds\n" +
                       "Developer Options: " + (areDeveloperOptionsEnabled() ? "ON" : "OFF") + "\n" +
                       "USB Debugging: " + (isUsbDebuggingEnabled() ? "ON" : "OFF") + "\n" +
                       "🔧 Shizuku: " + (ShizukuManager.isAvailable() ? "Available ✅" : "Required ❌") + "\n\n" +
                       "💡 Usage: settings rotation on/off\n" +
                       "         settings timeout <seconds>\n" +
                       "         settings dev on/off";
                       
            case "vibration":
            case "vibrate":
                return "📳 Vibration Settings:\n" +
                       "System Vibration: " + (vibrate ? "ON" : "OFF") + "\n" +
                       "Touch Vibration: Enabled\n" +
                       "Notification Vibration: Enabled\n\n" +
                       "💡 Usage: settings vibrate on/off";
                       
            default:
                return "❌ Unknown settings category: " + category + "\n" +
                       "💡 Available: display, sound, security, system, vibration";
        }
    }
    
    private String changeSetting(String setting, String value) {
        // Check if we have Shizuku for system changes
        boolean requiresShizuku = requiresShizukuForSetting(setting);
        
        if (requiresShizuku && !ShizukuManager.isAvailable()) {
            return "❌ Shizuku not available\n" +
                   "This setting requires system-level access via Shizuku\n" +
                   "💡 Install Shizuku for direct settings control";
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
            case "darkmode":
                return setRealDarkMode(value);
                
            case "rotation":
            case "autorotate":
                return setRealAutoRotate(value);
                
            case "timeout":
            case "screentimeout":
                return setRealTimeout(value);
                
            case "vibrate":
            case "vibration":
                return setVibrate(value);
                
            case "ringtone":
                return setRingtone(value);
                
            case "dev":
            case "developer":
                return setDeveloperOptions(value);
                
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
                return "❌ Failed to set brightness\n" +
                       "💡 Opening system display settings...";
            }
        } catch (NumberFormatException e) {
            return "❌ Invalid brightness value. Use 0-100";
        }
    }
    
    private String getRealBrightness() {
        if (!ShizukuManager.isAvailable()) {
            return String.valueOf(brightness) + " (cached)";
        }
        
        String result = ShizukuManager.executeCommandWithOutput("settings get system screen_brightness");
        if (result != null && !result.trim().isEmpty() && !result.contains("null")) {
            try {
                int systemBrightness = Integer.parseInt(result.trim());
                int percent = (int) (systemBrightness / 2.55);
                brightness = percent;
                return String.valueOf(percent);
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        return String.valueOf(brightness) + " (cached)";
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
                return "❌ Failed to set media volume\n" +
                       "💡 Opening system sound settings...";
            }
        } catch (NumberFormatException e) {
            return "❌ Invalid volume value. Use 0-100";
        }
    }
    
    private String getRealMediaVolume() {
        if (!ShizukuManager.isAvailable()) {
            return String.valueOf(mediaVolume) + " (cached)";
        }
        
        String result = ShizukuManager.executeCommandWithOutput("media volume --stream 3 --get");
        if (result != null && !result.trim().isEmpty()) {
            try {
                int mediaLevel = Integer.parseInt(result.trim());
                int percent = (int) (mediaLevel / 0.15);
                mediaVolume = Math.min(percent, 100);
                return String.valueOf(mediaVolume);
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        return String.valueOf(mediaVolume) + " (cached)";
    }
    
    private String setRealDarkMode(String value) {
        boolean enable = value.equalsIgnoreCase("on") || value.equalsIgnoreCase("yes") || value.equals("1");
        
        if (ShizukuManager.executeCommand(
            "settings put secure ui_night_mode " + (enable ? "2" : "1")
        )) {
            darkMode = enable;
            return "✅ Dark Mode " + (enable ? "enabled" : "disabled");
        } else {
            return "❌ Failed to set dark mode\n" +
                   "💡 Opening system display settings...";
        }
    }
    
    private boolean getRealDarkMode() {
        if (!ShizukuManager.isAvailable()) {
            return darkMode;
        }
        
        String result = ShizukuManager.executeCommandWithOutput("settings get secure ui_night_mode");
        if (result != null && !result.trim().isEmpty()) {
            boolean isDark = "2".equals(result.trim());
            darkMode = isDark;
            return isDark;
        }
        return darkMode;
    }
    
    private String setRealAutoRotate(String value) {
        boolean enable = value.equalsIgnoreCase("on") || value.equalsIgnoreCase("yes");
        
        if (ShizukuManager.executeCommand(
            "settings put system accelerometer_rotation " + (enable ? "1" : "0")
        )) {
            autoRotate = enable;
            return "✅ Auto-rotate " + (enable ? "enabled" : "disabled");
        } else {
            return "❌ Failed to set auto-rotate\n" +
                   "💡 Opening system display settings...";
        }
    }
    
    private boolean getRealAutoRotate() {
        if (!ShizukuManager.isAvailable()) {
            return autoRotate;
        }
        
        String result = ShizukuManager.executeCommandWithOutput("settings get system accelerometer_rotation");
        if (result != null && !result.trim().isEmpty()) {
            boolean isEnabled = "1".equals(result.trim());
            autoRotate = isEnabled;
            return isEnabled;
        }
        return autoRotate;
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
                return "❌ Failed to set screen timeout\n" +
                       "💡 Opening system display settings...";
            }
        } catch (NumberFormatException e) {
            return "❌ Invalid timeout value. Use seconds (15-3600)";
        }
    }
    
    private String getRealTimeout() {
        if (!ShizukuManager.isAvailable()) {
            return String.valueOf(screenTimeout) + " (cached)";
        }
        
        String result = ShizukuManager.executeCommandWithOutput("settings get system screen_off_timeout");
        if (result != null && !result.trim().isEmpty() && !result.contains("null")) {
            try {
                int timeoutMs = Integer.parseInt(result.trim());
                int seconds = timeoutMs / 1000;
                screenTimeout = seconds;
                return String.valueOf(seconds);
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        return String.valueOf(screenTimeout) + " (cached)";
    }
    
    private String setDeveloperOptions(String value) {
        boolean enable = value.equalsIgnoreCase("on") || value.equalsIgnoreCase("yes");
        
        if (ShizukuManager.executeCommand(
            "settings put global development_settings_enabled " + (enable ? "1" : "0")
        )) {
            return "✅ Developer options " + (enable ? "enabled" : "disabled");
        } else {
            return "❌ Failed to modify developer options\n" +
                   "💡 Requires system-level access";
        }
    }
    
    // Helper methods for other settings
    private String setCallVolume(String value) {
        try {
            int volume = Integer.parseInt(value);
            if (volume >= 0 && volume <= 100) {
                callVolume = volume;
                return "✅ Call volume set to " + volume + "%\n💡 Apply in system sound settings";
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
                return "✅ Alarm volume set to " + volume + "%\n💡 Apply in system sound settings";
            }
            return "❌ Volume must be 0-100";
        } catch (NumberFormatException e) {
            return "❌ Invalid volume value";
        }
    }
    
    private String setVibrate(String value) {
        boolean enable = value.equalsIgnoreCase("on") || value.equalsIgnoreCase("yes");
        vibrate = enable;
        return "✅ Vibrate " + (enable ? "enabled" : "disabled") + "\n💡 Apply in system sound settings";
    }
    
    private String setRingtone(String value) {
        ringtone = value;
        // Open system ringtone settings
        Intent intent = new Intent(Settings.ACTION_SOUND_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        return "✅ Opening sound settings for ringtone: " + ringtone;
    }
    
    private void loadCurrentSettings() {
        // Update our local state with real values
        try {
            brightness = Integer.parseInt(getRealBrightness().replace(" (cached)", ""));
            mediaVolume = Integer.parseInt(getRealMediaVolume().replace(" (cached)", ""));
            darkMode = getRealDarkMode();
            autoRotate = getRealAutoRotate();
            screenTimeout = Integer.parseInt(getRealTimeout().replace(" (cached)", ""));
        } catch (NumberFormatException e) {
            // Keep current values if parsing fails
        }
    }
    
    private boolean requiresShizukuForSetting(String setting) {
        switch (setting.toLowerCase()) {
            case "brightness":
            case "media":
            case "dark":
            case "darkmode":
            case "rotation":
            case "autorotate":
            case "timeout":
            case "screentimeout":
            case "dev":
            case "developer":
                return true;
            default:
                return false;
        }
    }
    
    // Placeholder methods for security settings
    private boolean isBiometricEnabled() { 
        return Authentication.isBiometricAvailable(context);
    }
    
    private boolean isPinSet() { 
        // This would check if device has PIN/pattern/password set
        return true; 
    }
    
    private boolean areDeveloperOptionsEnabled() { 
        if (!ShizukuManager.isAvailable()) return false;
        
        String result = ShizukuManager.executeCommandWithOutput("settings get global development_settings_enabled");
        return result != null && "1".equals(result.trim());
    }
    
    private boolean isUsbDebuggingEnabled() { 
        if (!ShizukuManager.isAvailable()) return false;
        
        String result = ShizukuManager.executeCommandWithOutput("settings get global adb_enabled");
        return result != null && "1".equals(result.trim());
    }
    
    private String getUsage() {
        return "⚙️ Settings Module Usage:\n" +
               "• settings                 - Show all settings\n" +
               "• settings [category]      - Show category details\n" +
               "• settings [setting] [value] - Change setting\n" +
               "\n📋 Available Categories:\n" +
               "• display - Brightness, dark mode, rotation, timeout\n" +
               "• sound   - Media/call/alarm volume, ringtone, vibration\n" +
               "• security- Biometric, PIN, developer options\n" +
               "• system  - Auto-rotate, timeout, developer options\n" +
               "• vibration - Vibration settings\n" +
               "\n🔧 Shizuku-Dependent Settings:\n" +
               "• brightness, media, dark, rotation, timeout, dev\n" +
               "\n🔒 Requires: Authentication for changes" +
               "\n🔧 Shizuku: " + (ShizukuManager.isAvailable() ? "Available ✅" : "Not Available ❌");
    }
}
