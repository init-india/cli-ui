package com.cliui.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.HashSet;
import java.util.Set;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.location.LocationManager;
import android.provider.Settings;
import android.hardware.camera2.CameraManager;

public class PermissionManager {
    private static PermissionManager instance;
    private Context context;
    
    // Standard Android permissions
    private static final Set<String> STANDARD_PERMISSION_COMMANDS = new HashSet<>();
    private static final Set<String> SHIZUKU_COMMANDS = new HashSet<>();
    
    // Permission groups
    public static final String[] PHONE_PERMISSIONS = {
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG
    };
    
    public static final String[] SMS_PERMISSIONS = {
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.RECEIVE_SMS
    };
    
    public static final String[] CONTACT_PERMISSIONS = {
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.WRITE_CONTACTS
    };
    
    public static final String[] LOCATION_PERMISSIONS = {
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    };
    
    public static final String[] STORAGE_PERMISSIONS = {
        Manifest.permission.READ_EXTERNAL_STORAGE
    };
    
    public static final String[] CAMERA_PERMISSIONS = {
        Manifest.permission.CAMERA
    };
    
    public static final String[] MICROPHONE_PERMISSIONS = {
        Manifest.permission.RECORD_AUDIO
    };

    public static final String[] NOTIFICATION_PERMISSIONS = {
        Manifest.permission.POST_NOTIFICATIONS  // For API 33+
    };

    public static final String[] BLUETOOTH_PERMISSIONS = {
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_FINE_LOCATION  // Bluetooth often needs location
    };

    public static final String[] SYSTEM_PERMISSIONS = {
    Manifest.permission.CAMERA,
    Manifest.permission.RECORD_AUDIO,
    Manifest.permission.WRITE_SETTINGS
   };



    static {
        // Standard permission commands
        STANDARD_PERMISSION_COMMANDS.add("call");
        STANDARD_PERMISSION_COMMANDS.add("sms");
        STANDARD_PERMISSION_COMMANDS.add("contact");
        STANDARD_PERMISSION_COMMANDS.add("mail");
        STANDARD_PERMISSION_COMMANDS.add("map");
        STANDARD_PERMISSION_COMMANDS.add("nav");
        STANDARD_PERMISSION_COMMANDS.add("location");
        STANDARD_PERMISSION_COMMANDS.add("camera");
        STANDARD_PERMISSION_COMMANDS.add("mic");
        STANDARD_PERMISSION_COMMANDS.add("notification");
        STANDARD_PERMISSION_COMMANDS.add("whatsapp");
        
        // Shizuku commands (system control)
        SHIZUKU_COMMANDS.add("wifi on");
        SHIZUKU_COMMANDS.add("wifi off");
        SHIZUKU_COMMANDS.add("bluetooth on");
        SHIZUKU_COMMANDS.add("bluetooth off");
        SHIZUKU_COMMANDS.add("hotspot on");
        SHIZUKU_COMMANDS.add("hotspot off");
        SHIZUKU_COMMANDS.add("location on");
        SHIZUKU_COMMANDS.add("location off");
        SHIZUKU_COMMANDS.add("flash on");
        SHIZUKU_COMMANDS.add("flash off");
        SHIZUKU_COMMANDS.add("brightness");
        SHIZUKU_COMMANDS.add("volume");
        SHIZUKU_COMMANDS.add("airplane");
    }

    private PermissionManager(Context context) {
        this.context = context;
    }

    public static PermissionManager getInstance(Context context) {
        if (instance == null) {
            instance = new PermissionManager(context);
        }
        return instance;
    }

    /**
     * Check if a command can be executed based on permissions
     */
    public boolean canExecute(String command) {
        String baseCommand = getBaseCommand(command);
        
        if (isShizukuCommand(command)) {
            return isShizukuAvailable() && isShizukuPermitted();
        }
        
        return hasRequiredPermissions(baseCommand);
    }

    /**
     * Get required permissions for a command
     */
    public String[] getRequiredPermissions(String command) {
        String baseCommand = getBaseCommand(command);
        
        switch (baseCommand) {
            case "call":
                return PHONE_PERMISSIONS;
            case "sms":
                return SMS_PERMISSIONS;
            case "contact":
                return CONTACT_PERMISSIONS;
            case "map":
            case "nav":
            case "location":
                return LOCATION_PERMISSIONS;
            case "mail":
                return STORAGE_PERMISSIONS; // For attachments
            case "camera":
                return CAMERA_PERMISSIONS;
            case "mic":
                return MICROPHONE_PERMISSIONS;
            case "whatsapp":
                return combinePermissions(CONTACT_PERMISSIONS, STORAGE_PERMISSIONS);
            default:
                return new String[0]; // No permissions required
        }
    }

    /**
     * Check if all required permissions are granted for a command
     */
    private boolean hasRequiredPermissions(String baseCommand) {
        String[] requiredPermissions = getRequiredPermissions(baseCommand);
        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Detect if command is a Shizuku system-level command
     */
    private boolean isShizukuCommand(String command) {
        String lowerCommand = command.toLowerCase();
        for (String shizukuCmd : SHIZUKU_COMMANDS) {
            if (lowerCommand.startsWith(shizukuCmd)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Request permissions for a command
     */
    public void requestPermissions(Activity activity, String command, int requestCode) {
        String baseCommand = getBaseCommand(command);
        
        if (isShizukuCommand(command)) {
            requestShizukuSetup(activity, command);
            return;
        }
        
        String[] requiredPermissions = getRequiredPermissions(baseCommand);
        if (requiredPermissions.length > 0) {
            ActivityCompat.requestPermissions(activity, requiredPermissions, requestCode);
        }
    }

    /**
     * Get user-friendly explanation for why permission is needed
     */
    public String getPermissionExplanation(String command) {
        String baseCommand = getBaseCommand(command);
        
        if (isShizukuCommand(command)) {
            return "🔧 System control requires Shizuku\n" +
                   "This command needs system-level access to toggle " + getShizukuFeature(command) + "\n" +
                   "Install Shizuku for direct control or use system settings";
        }
        
        switch (baseCommand) {
            case "call":
                return "📞 Phone call permission required\nAllow this app to make phone calls?";
            case "sms":
                return "💬 SMS permission required\nAllow this app to send and read messages?";
            case "contact":
                return "👥 Contact access required\nAllow this app to read and manage contacts?";
            case "map":
            case "nav":
            case "location":
                return "📍 Location access required\nAllow this app to access your location for navigation?";
            case "camera":
                return "📷 Camera access required\nAllow this app to use camera?";
            case "mic":
                return "🎤 Microphone access required\nAllow this app to record audio?";
            case "mail":
                return "📧 Storage access required\nAllow this app to access storage for email attachments?";
            default:
                return "Additional permissions required for this command";
        }
    }

    /**
     * Check if all permissions in array are granted
     */
    public boolean areAllPermissionsGranted(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    // Quick permission check methods
    public boolean canMakeCalls() {
        return areAllPermissionsGranted(PHONE_PERMISSIONS);
    }

    public boolean canSendSMS() {
        return areAllPermissionsGranted(SMS_PERMISSIONS);
    }

    public boolean canAccessLocation() {
        return areAllPermissionsGranted(LOCATION_PERMISSIONS);
    }

    /**
     * Handle permission request results
     */
    public boolean handlePermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        boolean allGranted = true;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        return allGranted;
    }

    /**
     * Shizuku-related methods (stubs for now)
     */
    private boolean isShizukuAvailable() {
        // Placeholder for actual Shizuku availability check
        return false;
    }

    private boolean isShizukuPermitted() {
        // Placeholder for actual Shizuku permission check
        return false;
    }

    private void requestShizukuSetup(Activity activity, String command) {
        // Placeholder for showing Shizuku installation instructions
    }

    private String getShizukuFeature(String command) {
        if (command.contains("wifi")) return "WiFi";
        if (command.contains("bluetooth")) return "Bluetooth";
        if (command.contains("hotspot")) return "Hotspot";
        if (command.contains("location")) return "Location";
        if (command.contains("flash")) return "Flashlight";
        if (command.contains("brightness")) return "Brightness";
        if (command.contains("volume")) return "Volume";
        return "System setting";
    }

    /**
     * Utility methods
     */
    private String getBaseCommand(String command) {
        if (command == null || command.trim().isEmpty()) return "";
        return command.split(" ")[0].toLowerCase();
    }

    private String[] combinePermissions(String[]... permissionArrays) {
        Set<String> combined = new HashSet<>();
        for (String[] array : permissionArrays) {
            for (String permission : array) {
                combined.add(permission);
            }
        }
        return combined.toArray(new String[0]);
    }
}
