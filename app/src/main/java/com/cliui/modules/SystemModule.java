public class SystemModule implements CommandModule {
    private Context context;
    private PermissionManager permissionManager;
    
    public String execute(String[] tokens) {
        if (!permissionManager.canExecute(tokens[0])) {
            return "🔧 System control requires permissions\nType command again to grant";
        }
        
        String command = tokens[0].toLowerCase();
        
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
                return "Unknown system command";
        }
    }
    
    private String toggleFlashlight() {
        // Use Shizuku to control flashlight
        boolean currentState = isFlashlightEnabled();
        
        if (ShizukuManager.executeCommand(currentState ? "cmd flashlight disable" : "cmd flashlight enable")) {
            return "🔦 Flashlight " + (currentState ? "OFF" : "ON");
        }
        return "❌ Failed to toggle flashlight";
    }
    
    private String toggleLocation() {
        boolean currentState = isLocationEnabled();
        
        if (ShizukuManager.executeCommand(
            currentState ? 
            "settings put secure location_providers_allowed -gps" :
            "settings put secure location_providers_allowed +gps"
        )) {
            return "📍 Location " + (currentState ? "OFF" : "ON");
        }
        return "❌ Failed to toggle location";
    }
    
    private String toggleMicrophone() {
        // This would require more complex audio routing control
        return "🎤 Microphone control requires advanced audio permissions";
    }
    
    private String handleAlarmCommand(String[] tokens) {
        if (tokens.length == 1) {
            return showAlarms();
        } else if (tokens.length >= 2) {
            return setAlarm(tokens);
        }
        return "Usage: alarm | alarm set HH:MM [message]";
    }
    
    private String showAlarms() {
        // Use Shizuku to list alarms
        String alarms = ShizukuManager.executeCommandWithOutput("dumpsys alarm");
        // Parse alarms and display
        return "⏰ Active Alarms:\n[Alarm list from system]\n\n💡 Type 'alarm set HH:MM' to set new alarm";
    }
    
    private String showTime() {
        // Use Linux date command via Shizuku
        String time = ShizukuManager.executeCommandWithOutput("date '+%H:%M:%S'");
        return "🕒 Current Time: " + time.trim();
    }
    
    private String showDate() {
        // Use Linux date command via Shizuku
        String date = ShizukuManager.executeCommandWithOutput("date '+%d-%b-%Y'");
        return "📅 Current Date: " + date.trim();
    }
}
