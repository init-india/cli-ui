package com.cliui.modules;

import android.content.Context;
import android.provider.Settings;
import android.content.Intent;

public class SettingsModule implements CommandModule {
    private Context context;
    private int brightness = 75;
    private int mediaVolume = 80;
    private int callVolume = 90;
    private int alarmVolume = 100;
    private boolean darkMode = true;
    private String ringtone = "Classic Bell";
    
    public SettingsModule(Context context) {
        this.context = context;
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
        return "⚙️ System Settings:\n" +
               "• display - Brightness: " + brightness + "%, Dark Mode: " + (darkMode ? "ON" : "OFF") + "\n" +
               "• sound - Media: " + mediaVolume + "%, Call: " + callVolume + "%, Alarm: " + alarmVolume + "%\n" +
               "• ringtone - " + ringtone + "\n" +
               "• security - Biometric: Enabled, PIN: Set\n\n" +
               "Type 'settings display', 'settings sound', etc. for details";
    }
    
    private String showSpecificSettings(String category) {
        switch (category.toLowerCase()) {
            case "display":
                return "💡 Display Settings:\n" +
                       "Brightness: " + brightness + "%\n" +
                       "Dark Mode: " + (darkMode ? "ON" : "OFF") + "\n" +
                       "Auto-rotate: ON\n" +
                       "Timeout: 2 minutes\n\n" +
                       "Usage: settings brightness <0-100>, settings dark on/off";
                       
            case "sound":
                return "🔊 Sound Settings:\n" +
                       "Media Volume: " + mediaVolume + "%\n" +
                       "Call Volume: " + callVolume + "%\n" +
                       "Alarm Volume: " + alarmVolume + "%\n" +
                       "Ringtone: " + ringtone + "\n" +
                       "Vibration: ON\n\n" +
                       "Usage: settings media <0-100>, settings call <0-100>, settings ringtone <name>";
                       
            case "security":
                return "🔒 Security Settings:\n" +
                       "Biometric: Enabled\n" +
                       "PIN: Set\n" +
                       "App Lock: SMS, Email\n" +
                       "Unknown Sources: Blocked\n\n" +
                       "Usage: Open system settings for security changes";
                       
            default:
                return "Unknown settings category: " + category;
        }
    }
    
    private String changeSetting(String setting, String value) {
        switch (setting.toLowerCase()) {
            case "brightness":
                try {
                    int newBrightness = Integer.parseInt(value);
                    if (newBrightness >= 0 && newBrightness <= 100) {
                        brightness = newBrightness;
                        // Open system brightness settings
                        Intent intent = new Intent(Settings.ACTION_DISPLAY_SETTINGS);
                        context.startActivity(intent);
                        return "✅ Opening brightness settings...";
                    } else {
                        return "❌ Brightness must be between 0-100";
                    }
                } catch (NumberFormatException e) {
                    return "❌ Invalid brightness value. Use 0-100";
                }
                
            case "media":
                try {
                    int newVolume = Integer.parseInt(value);
                    if (newVolume >= 0 && newVolume <= 100) {
                        mediaVolume = newVolume;
                        return "✅ Media volume set to " + mediaVolume + "%\n[Open sound settings to apply]";
                    } else {
                        return "❌ Volume must be between 0-100";
                    }
                } catch (NumberFormatException e) {
                    return "❌ Invalid volume value. Use 0-100";
                }
                
            case "ringtone":
                ringtone = value;
                // Open system ringtone settings
                Intent intent = new Intent(Settings.ACTION_SOUND_SETTINGS);
                context.startActivity(intent);
                return "✅ Opening sound settings for ringtone...";
                
            default:
                return "❌ Unknown setting: " + setting + "\nOpen system settings for advanced changes";
        }
    }
}
