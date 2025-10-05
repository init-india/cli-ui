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
import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.util.Log;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PermissionManager {
    private static final String TAG = "PermissionManager";
    private static PermissionManager instance;
    private Context context;
    
    // Standard Android permissions
    private static final Set<String> STANDARD_PERMISSION_COMMANDS = new HashSet<>();
    private static final Set<String> SHIZUKU_COMMANDS = new HashSet<>();
    private static final Set<String> SANDBOX_COMMANDS = new HashSet<>();
    
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
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    
    public static final String[] CAMERA_PERMISSIONS = {
        Manifest.permission.CAMERA
    };
    
    public static final String[] MICROPHONE_PERMISSIONS = {
        Manifest.permission.RECORD_AUDIO
    };

    public static final String[] NOTIFICATION_PERMISSIONS = {
        Manifest.permission.POST_NOTIFICATIONS
    };

    public static final String[] BLUETOOTH_PERMISSIONS = {
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_FINE_LOCATION
    };

    public static final String[] SYSTEM_PERMISSIONS = {
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_SETTINGS
    };

    // Sandbox mode flags
    private boolean sandboxModeEnabled = true;
    private Map<String, Boolean> userPreferences = new HashMap<>();
    private Map<String, Integer> permissionDenialCount = new HashMap<>();

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
        SHIZUKU_COMMANDS.add("install");
        SHIZUKU_COMMANDS.add("uninstall");
        SHIZUKU_COMMANDS.add("package");
        
        // Sandbox commands (can work with limited functionality)
        SANDBOX_COMMANDS.add("mail");
        SANDBOX_COMMANDS.add("map");
        SANDBOX_COMMANDS.add("nav");
        SANDBOX_COMMANDS.add("contact");
        SANDBOX_COMMANDS.add("sms");
        SANDBOX_COMMANDS.add("whatsapp");
        SANDBOX_COMMANDS.add("camera");
        SANDBOX_COMMANDS.add("location");
    }

    private PermissionManager(Context context) {
        this.context = context.getApplicationContext();
        initializeUserPreferences();
    }

    public static PermissionManager getInstance(Context context) {
        if (instance == null) {
            instance = new PermissionManager(context);
        }
        return instance;
    }

    // ========== ENHANCED AUTHENTICATION METHODS ==========

    /**
     * Enhanced authenticate method with fallback and sandbox support
     */
    public boolean authenticate(String feature) {
        return authenticate(feature, true);
    }
    
    public boolean authenticate(String feature, boolean allowFallback) {
        Log.d(TAG, "Authenticating feature: " + feature);
        
        if (isFeatureDeniedByUser(feature)) {
            Log.w(TAG, "Feature denied by user: " + feature);
            return false;
        }
        
        if (isSystemFeature(feature)) {
            return authenticateSystemFeature(feature, allowFallback);
        }
        
        return authenticateStandardFeature(feature, allowFallback);
    }
    
    private boolean authenticateSystemFeature(String feature, boolean allowFallback) {
        if (isShizukuAvailable() && isShizukuPermitted()) {
            Log.d(TAG, "Using Shizuku for system feature: " + feature);
            return true;
        }
        
        if (allowFallback && hasSystemIntentFallback(feature)) {
            Log.d(TAG, "Using system intent fallback for: " + feature);
            return true;
        }
        
        if (allowFallback && sandboxModeEnabled) {
            Log.d(TAG, "Using sandbox mode for: " + feature);
            return true;
        }
        
        Log.w(TAG, "No available method for system feature: " + feature);
        return false;
    }
    
    private boolean authenticateStandardFeature(String feature, boolean allowFallback) {
        if (hasFeaturePermission(feature)) {
            Log.d(TAG, "Has required permissions for: " + feature);
            return true;
        }
        
        if (allowFallback && sandboxModeEnabled && hasSandboxFallback(feature)) {
            Log.d(TAG, "Using sandbox fallback for: " + feature);
            return true;
        }
        
        Log.w(TAG, "No permissions or fallback for: " + feature);
        return false;
    }

    /**
     * Execute command with comprehensive fallback system
     */
    public String executeWithFallback(String command, CommandExecutor executor) {
        String baseCommand = getBaseCommand(command);
        
        if (canExecute(command)) {
            return executor.executeDirect();
        }
        
        if (sandboxModeEnabled && hasSandboxFallback(baseCommand)) {
            String sandboxResult = executor.executeSandbox();
            if (sandboxResult != null && !sandboxResult.contains("failed") && !sandboxResult.contains("error")) {
                Log.i(TAG, "Used sandbox fallback for: " + command);
                return sandboxResult + "\n🔒 [Sandbox Mode - Limited Functionality]";
            }
        }
        
        if (isSystemFeature(baseCommand) && hasSystemIntentFallback(command)) {
            String intentResult = executeSystemIntentFallback(command);
            if (intentResult != null) {
                return intentResult + "\n🔧 [System Intent - Manual Action Required]";
            }
        }
        
        trackPermissionDenial(baseCommand);
        return "❌ Permission denied for: " + command + 
               "\n💡 " + getFallbackSuggestions(command);
    }

    /**
     * System intent fallbacks for when Shizuku is not available
     */
    private String executeSystemIntentFallback(String command) {
        try {
            Intent intent = null;
            String lowerCommand = command.toLowerCase();
            
            if (lowerCommand.contains("wifi")) {
                intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
            } else if (lowerCommand.contains("bluetooth")) {
                intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            } else if (lowerCommand.contains("location")) {
                intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            } else if (lowerCommand.contains("hotspot")) {
                intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
            } else if (lowerCommand.contains("brightness")) {
                intent = new Intent(Settings.ACTION_DISPLAY_SETTINGS);
            } else if (lowerCommand.contains("volume")) {
                intent = new Intent(Settings.ACTION_SOUND_SETTINGS);
            } else if (lowerCommand.contains("airplane")) {
                intent = new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS);
            }
            
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return "Opening system settings...\nPlease configure manually";
            }
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "System intent not available", e);
        } catch (Exception e) {
            Log.e(TAG, "Error executing system intent fallback", e);
        }
        
        return null;
    }

    /**
     * Sandbox implementations for various features
     */
    public String getSandboxImplementation(String feature) {
        switch (feature) {
            case "mail":
                return "💌 Sandbox Email: Compose emails locally (not sent)\nUse 'mail send' to attempt actual sending";
            case "contact":
                return "👥 Sandbox Contacts: View recent contacts only\nEnable contact permissions for full access";
            case "sms":
                return "💬 Sandbox SMS: Draft messages locally\nEnable SMS permissions to send actual messages";
            case "location":
                return "📍 Sandbox Location: Use approximate location based on WiFi\nEnable location permissions for GPS accuracy";
            case "camera":
                return "📷 Sandbox Camera: Access last taken photos\nEnable camera permissions for live camera";
            case "whatsapp":
                return "💚 Sandbox WhatsApp: View recent chats (read-only)\nEnable permissions for full WhatsApp access";
            default:
                return "🔒 Sandbox Mode: Limited functionality available\nEnable permissions for full features";
        }
    }

    /**
     * User preference and denial tracking
     */
    private void initializeUserPreferences() {
        userPreferences.put("email_access", true);
        userPreferences.put("whatsapp_access", true);
        userPreferences.put("sms_access", true);
        userPreferences.put("contact_access", true);
        userPreferences.put("location_access", true);
        userPreferences.put("system_control", true);
    }
    
    public void setUserPreference(String feature, boolean allowed) {
        userPreferences.put(feature, allowed);
    }
    
    public boolean isFeatureDeniedByUser(String feature) {
        Boolean preference = userPreferences.get(feature);
        return preference != null && !preference;
    }
    
    private void trackPermissionDenial(String feature) {
        Integer count = permissionDenialCount.get(feature);
        if (count == null) {
            permissionDenialCount.put(feature, 1);
        } else {
            permissionDenialCount.put(feature, count + 1);
        }
        
        if (count != null && count >= 3) {
            Log.i(TAG, "Feature " + feature + " denied multiple times");
        }
    }

    /**
     * Fallback availability checks
     */
    private boolean hasSystemIntentFallback(String feature) {
        String lowerFeature = feature.toLowerCase();
        return lowerFeature.contains("wifi") || lowerFeature.contains("bluetooth") || 
               lowerFeature.contains("location") || lowerFeature.contains("hotspot") ||
               lowerFeature.contains("brightness") || lowerFeature.contains("volume") ||
               lowerFeature.contains("airplane");
    }
    
    private boolean hasSandboxFallback(String feature) {
        return SANDBOX_COMMANDS.contains(feature);
    }
    
    private String getFallbackSuggestions(String command) {
        String baseCommand = getBaseCommand(command);
        
        if (isSystemFeature(baseCommand)) {
            return "Install Shizuku for system control or use system settings manually";
        } else if (hasSandboxFallback(baseCommand)) {
            return "Sandbox mode available with limited functionality";
        } else {
            return "Enable permissions in app settings for full functionality";
        }
    }

    // ========== ORIGINAL METHODS (PRESERVED) ==========

    public boolean canExecute(String command) {
        String baseCommand = getBaseCommand(command);
        
        if (isShizukuCommand(command)) {
            return isShizukuAvailable() && isShizukuPermitted();
        }
        
        return hasRequiredPermissions(baseCommand);
    }

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
                return STORAGE_PERMISSIONS;
            case "camera":
                return CAMERA_PERMISSIONS;
            case "mic":
                return MICROPHONE_PERMISSIONS;
            case "whatsapp":
                return combinePermissions(CONTACT_PERMISSIONS, STORAGE_PERMISSIONS);
            default:
                return new String[0];
        }
    }

    private boolean hasRequiredPermissions(String baseCommand) {
        String[] requiredPermissions = getRequiredPermissions(baseCommand);
        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private boolean isShizukuCommand(String command) {
        String lowerCommand = command.toLowerCase();
        for (String shizukuCmd : SHIZUKU_COMMANDS) {
            if (lowerCommand.startsWith(shizukuCmd)) {
                return true;
            }
        }
        return false;
    }

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

    public boolean areAllPermissionsGranted(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public boolean canMakeCalls() {
        return areAllPermissionsGranted(PHONE_PERMISSIONS);
    }

    public boolean canSendSMS() {
        return areAllPermissionsGranted(SMS_PERMISSIONS);
    }

    public boolean canAccessLocation() {
        return areAllPermissionsGranted(LOCATION_PERMISSIONS);
    }

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

    // ========== UPDATED SHIZUKU METHODS ==========

    /**
     * Enhanced Shizuku-related methods using ShizukuManager
     */
    public boolean isShizukuAvailable() {
        return ShizukuManager.isAvailable();
    }

    public boolean isShizukuPermitted() {
        return ShizukuManager.hasPermission();
    }

    public boolean isShizukuReady() {
        return ShizukuManager.isReady();
    }

    /**
     * Execute command via Shizuku with fallback
     */
    public String executeShizukuCommand(String command) {
        if (!isShizukuReady()) {
            return "❌ Shizuku not ready: " + ShizukuManager.getStatus();
        }
        
        ShizukuManager.CommandResult result = ShizukuManager.executeCommandDetailed(command);
        if (result.success) {
            return result.output;
        } else {
            return "❌ Command failed (exit " + result.exitCode + "): " + result.output;
        }
    }

    /**
     * Get Shizuku status for debugging
     */
    public String getShizukuStatus() {
        return ShizukuManager.getStatus();
    }

    private void requestShizukuSetup(Activity activity, String command) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://shizuku.rikka.app/"));
            activity.startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id=moe.shizuku.privileged.api"));
                activity.startActivity(intent);
            } catch (Exception e2) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api"));
                activity.startActivity(intent);
            }
        }
    }

    private String getShizukuFeature(String command) {
        if (command.contains("wifi")) return "WiFi";
        if (command.contains("bluetooth")) return "Bluetooth";
        if (command.contains("hotspot")) return "Hotspot";
        if (command.contains("location")) return "Location";
        if (command.contains("flash")) return "Flashlight";
        if (command.contains("brightness")) return "Brightness";
        if (command.contains("volume")) return "Volume";
        if (command.contains("airplane")) return "Airplane Mode";
        if (command.contains("install") || command.contains("uninstall") || command.contains("package")) 
            return "Package Management";
        return "System setting";
    }

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
    
    public boolean isPermissionGranted(String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }
    
    public boolean shouldShowRequestPermissionRationale(Activity activity, String permission) {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);
    }

    /**
     * Check if biometric authentication is available
     */
    public boolean isBiometricAvailable(Context context) {
        if (context == null) return false;
        
        try {
            PackageManager packageManager = context.getPackageManager();
            return packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT) ||
                   packageManager.hasSystemFeature(PackageManager.FEATURE_FACE) ||
                   packageManager.hasSystemFeature(PackageManager.FEATURE_IRIS);
        } catch (Exception e) {
            Log.e(TAG, "Error checking biometric availability", e);
            return false;
        }
    }

    private boolean isSystemFeature(String feature) {
        return feature.contains("system_") || 
               feature.contains("wifi") || 
               feature.contains("hotspot") ||
               feature.contains("bluetooth") ||
               feature.contains("linux_") ||
               feature.contains("settings_") ||
               feature.contains("install") ||
               feature.contains("uninstall") ||
               feature.contains("package");
    }
    
    private boolean hasFeaturePermission(String feature) {
        switch (feature) {
            case "email_access":
            case "mail_access":
                return areAllPermissionsGranted(STORAGE_PERMISSIONS);
            case "whatsapp_access":
                return areAllPermissionsGranted(combinePermissions(CONTACT_PERMISSIONS, STORAGE_PERMISSIONS));
            case "sms_access":
                return areAllPermissionsGranted(SMS_PERMISSIONS);
            case "contact_access":
                return areAllPermissionsGranted(CONTACT_PERMISSIONS);
            case "navigation_access":
            case "location_access":
                return areAllPermissionsGranted(LOCATION_PERMISSIONS);
            case "camera_access":
                return areAllPermissionsGranted(CAMERA_PERMISSIONS);
            case "microphone_access":
            case "mic_access":
                return areAllPermissionsGranted(MICROPHONE_PERMISSIONS);
            case "phone_access":
            case "call_access":
                return areAllPermissionsGranted(PHONE_PERMISSIONS);
            case "linux_access":
                return isShizukuAvailable() && isShizukuPermitted();
            default:
                return true;
        }
    }

    // ========== SANDBOX CONTROLS ==========

    public void enableSandboxMode() {
        this.sandboxModeEnabled = true;
    }
    
    public void disableSandboxMode() {
        this.sandboxModeEnabled = false;
    }
    
    public boolean isSandboxModeEnabled() {
        return sandboxModeEnabled;
    }
    
    public void resetUserPreferences() {
        userPreferences.clear();
        permissionDenialCount.clear();
        initializeUserPreferences();
    }

    /**
     * Interface for command execution with fallbacks
     */
    public interface CommandExecutor {
        String executeDirect();
        String executeSandbox();
    }
}
