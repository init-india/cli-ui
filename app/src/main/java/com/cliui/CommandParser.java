package com.cliui;

import android.content.Context;
import android.telephony.TelephonyManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import com.cliui.modules.*;

import com.cliui.utils.PermissionManager;



public class CommandParser {
    private Context context;
    private Map<String, CommandModule> modules;
    private Stack<String> contextStack;
    private TelephonyManager telephonyManager;
    private PermissionManager permissionManager;
    private String pendingCommand; // Store command waiting for permission
   
    public CommandParser(Context context) {
        this.context = context;
        this.modules = new HashMap<>();
        this.contextStack = new Stack<>();
        this.telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        this.permissionManager = PermissionManager.getInstance(context);
        this.pendingCommand = null;
        initializeModules();
    }
    
    private void initializeModules() {
        modules.put("call", new CallModule(context));
        modules.put("sms", new SMSModule(context));
        modules.put("map", new NavigationModule(context));
        modules.put("nav", new NavigationModule(context));
        modules.put("wifi", new ConnectivityModule(context));
        modules.put("bluetooth", new ConnectivityModule(context));
        modules.put("hotspot", new ConnectivityModule(context));
        modules.put("flash", new SystemModule(context));
        modules.put("location", new SystemModule(context));
        modules.put("mic", new SystemModule(context));
        modules.put("camera", new SystemModule(context));
        modules.put("mail", new EmailModule(context));
        modules.put("whatsapp", new WhatsAppModule(context));
        modules.put("wh", new WhatsAppModule(context));
       // modules.put("notifications", new NotificationManager(context));
        modules.put("settings", new SettingsModule(context));
        modules.put("contact", new ContactModule(context));
    }
    
    public String parse(String input) {
        try {
            String[] tokens = input.trim().split("\\s+");
            if (tokens.length == 0) return "";
            
            String command = tokens[0].toLowerCase();
            
            // Handle special commands first (no permissions needed)
            if (isSpecialCommand(command)) {
                return handleSpecialCommand(command, tokens);
            }
            
            // Handle Linux commands
            if (isLinuxCommand(command)) {
                return executeLinuxCommand(input);
            }
            
            // Check permissions before executing
            if (!permissionManager.canExecute(input)) {
                pendingCommand = input; // Store for retry after permission grant
                return permissionManager.getPermissionExplanation(input) + 
                       "\n\nType the command again to grant permission or 'exit' to cancel.";
            }
            
            // Clear pending command if permission check passed
            pendingCommand = null;
            
            // Handle telephony commands
            if (command.equals("telephony") || command.equals("phone")) {
                return handleTelephonyCommand(tokens);
            }
            
            if (command.equals("network")) {
                return getNetworkInfo();
            }
            
            if (command.equals("sim")) {
                return getSimInfo();
            }
            
            // Route to appropriate module
            if (modules.containsKey(command)) {
                return modules.get(command).execute(tokens);
            }
            
            return "Command not found: " + command + "\nType 'help' for available commands.";
            
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    /**
     * Handle permission grant result from MainActivity
     */
    public String onPermissionsGranted() {
        if (pendingCommand != null) {
            String commandToRetry = pendingCommand;
            pendingCommand = null;
            return "✓ Permissions granted!\n" + parse(commandToRetry);
        }
        return "No pending commands waiting for permissions.";
    }
    
    /**
     * Handle permission denial
     */
    public String onPermissionsDenied() {
        pendingCommand = null;
        return "❌ Permissions denied. Command cancelled.\nSome features may not work without permissions.";
    }
    
    private boolean isSpecialCommand(String command) {
        return command.equals("exit") || command.equals("clear") || 
               command.equals("help") || command.equals("apps");
    }
    
    private String handleSpecialCommand(String command, String[] tokens) {
        switch (command) {
            case "exit":
                return handleExit();
            case "clear":
                return "CLEAR_SCREEN";
            case "help":
                return showHelp();
            case "apps":
                return showInstalledApps();
            default:
                return "Unknown command: " + command;
        }
    }
    
    private boolean isLinuxCommand(String command) {
        String[] linuxCommands = {
            "ls", "pwd", "cd", "cat", "echo", "mkdir", "rm", "cp", "mv", 
            "ps", "grep", "find", "whoami", "date", "cal", "df", "du"
        };
        for (String linuxCmd : linuxCommands) {
            if (command.equals(linuxCmd)) {
                return true;
            }
        }
        return false;
    }
    
    private String executeLinuxCommand(String command) {
        // Integrate with your TerminalSession from linux package
        try {
            // This will use your existing TerminalSession class
            return "🧩 Linux: " + command + "\n" +
                   "Linux environment integration active\n" +
                   "Executing via TerminalSession...";
        } catch (Exception e) {
            return "❌ Linux command failed: " + e.getMessage() + 
                   "\nEnsure Linux environment is properly initialized.";
        }
    }
    
    // TELEPHONY METHODS (keep your existing implementations)
    private String handleTelephonyCommand(String[] tokens) {
        if (tokens.length < 2) {
            return "Telephony Commands:\n" +
                   "• telephony network - Show network info\n" +
                   "• telephony sim - Show SIM status\n" +
                   "• telephony signal - Show signal strength\n" +
                   "• telephony device - Show device info";
        }
        
        switch (tokens[1].toLowerCase()) {
            case "network":
                return getNetworkInfo();
            case "sim":
                return getSimInfo();
            case "signal":
                return getSignalInfo();
            case "device":
                return getDeviceInfo();
            default:
                return "Unknown telephony command: " + tokens[1];
        }
    }
    
    private String getNetworkInfo() {
        try {
            if (!permissionManager.areAllPermissionsGranted(PermissionManager.PHONE_PERMISSIONS)) {
                return "📞 Phone permission required for network information\n" +
                       "Use 'call' command to request phone permissions";
            }
            
            StringBuilder info = new StringBuilder();
            info.append("=== Network Information ===\n");
            
            String networkOperator = telephonyManager.getNetworkOperatorName();
            info.append("Operator: ").append(networkOperator != null ? networkOperator : "Unknown").append("\n");
            
            String networkType = getNetworkTypeName(telephonyManager.getNetworkType());
            info.append("Network Type: ").append(networkType).append("\n");
            
            info.append("Roaming: ").append(telephonyManager.isNetworkRoaming() ? "Yes" : "No").append("\n");
            
            return info.toString();
        } catch (SecurityException e) {
            return "Error: Telephony permission required";
        }
    }
    
    private String getSimInfo() {
        try {
            StringBuilder info = new StringBuilder();
            info.append("=== SIM Information ===\n");
            
            int simState = telephonyManager.getSimState();
            String simStatus = getSimStateName(simState);
            info.append("SIM State: ").append(simStatus).append("\n");
            
            if (simState == TelephonyManager.SIM_STATE_READY) {
                String simOperator = telephonyManager.getSimOperatorName();
                if (simOperator != null && !simOperator.isEmpty()) {
                    info.append("SIM Operator: ").append(simOperator).append("\n");
                }
                
                String countryCode = telephonyManager.getSimCountryIso();
                if (countryCode != null && !countryCode.isEmpty()) {
                    info.append("SIM Country: ").append(countryCode.toUpperCase()).append("\n");
                }
            }
            
            return info.toString();
        } catch (SecurityException e) {
            return "Error: Telephony permission required";
        }
    }
    
    private String getSignalInfo() {
        try {
            StringBuilder info = new StringBuilder();
            info.append("=== Signal Information ===\n");
            info.append("Basic signal info available\n");
            info.append("For detailed signal strength, check system settings\n");
            return info.toString();
        } catch (SecurityException e) {
            return "Error: Telephony permission required";
        }
    }
    
    private String getDeviceInfo() {
        try {
            if (!permissionManager.areAllPermissionsGranted(PermissionManager.PHONE_PERMISSIONS)) {
                return "📞 Phone permission required for device information";
            }
            
            StringBuilder info = new StringBuilder();
            info.append("=== Device Information ===\n");
            
            String deviceId = telephonyManager.getDeviceId();
            if (deviceId != null) {
                info.append("Device ID: ").append(deviceId).append("\n");
            }
            
            String deviceSoftware = telephonyManager.getDeviceSoftwareVersion();
            if (deviceSoftware != null) {
                info.append("Software: ").append(deviceSoftware).append("\n");
            }
            
            return info.toString();
        } catch (SecurityException e) {
            return "Error: Telephony permission required";
        }
    }
    
    private String showInstalledApps() {
        return "📱 Installed Apps:\n" +
               "• Use 'app [name]' to launch applications\n" +
               "• Example: 'app settings' launches Settings\n" +
               "• App launching integration in progress...";
    }
    
    private String handleExit() {
        if (contextStack.isEmpty()) {
            return "Already at main context";
        }
        contextStack.pop();
        return "Returning to previous context...";
    }
    
    private String showHelp() {
        return "🆘 CLI Phone Help - Available Commands:\n\n" +
               "📞 PHONE & MESSAGING:\n" +
               "  call [number]        - Make phone call\n" +
               "  sms [number] [msg]   - Send SMS\n" +
               "  contact              - Manage contacts\n\n" +
               
               "🌐 NETWORK & CONNECTIVITY:\n" +
               "  wifi [on/off]        - WiFi control\n" +
               "  bluetooth [on/off]   - Bluetooth control\n" +
               "  hotspot [on/off]     - Mobile hotspot\n" +
               "  telephony [cmd]      - Phone information\n\n" +
               
               "🗺️ NAVIGATION & LOCATION:\n" +
               "  map [destination]    - Show map\n" +
               "  nav [dest]           - Start navigation\n" +
               "  location [on/off]    - Location services\n\n" +
               
               "📧 COMMUNICATION APPS:\n" +
               "  mail                 - Email client\n" +
               "  whatsapp / wh        - WhatsApp integration\n\n" +
               
               "🔧 SYSTEM CONTROLS:\n" +
               "  flash [on/off]       - Flashlight\n" +
               "  camera               - Camera app\n" +
               "  mic [on/off]         - Microphone\n" +
               "  settings             - System settings\n\n" +
               
               "💻 LINUX ENVIRONMENT:\n" +
               "  ls, pwd, cd, cat     - File operations\n" +
               "  ps, grep, find       - System commands\n\n" +
               
               "🛠️ UTILITIES:\n" +
               "  clear                - Clear screen\n" +
               "  help                 - This help message\n" +
               "  exit                 - Exit current context\n" +
               "  apps                 - Show installed apps";
    }
    
    // Keep your existing utility methods
    private String getNetworkTypeName(int networkType) {
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS: return "GPRS";
            case TelephonyManager.NETWORK_TYPE_EDGE: return "EDGE";
            case TelephonyManager.NETWORK_TYPE_UMTS: return "UMTS";
            case TelephonyManager.NETWORK_TYPE_HSDPA: return "HSDPA";
            case TelephonyManager.NETWORK_TYPE_HSUPA: return "HSUPA";
            case TelephonyManager.NETWORK_TYPE_HSPA: return "HSPA";
            case TelephonyManager.NETWORK_TYPE_LTE: return "LTE";
            case TelephonyManager.NETWORK_TYPE_NR: return "5G";
            default: return "Unknown (" + networkType + ")";
        }
    }
    
    private String getSimStateName(int simState) {
        switch (simState) {
            case TelephonyManager.SIM_STATE_UNKNOWN: return "Unknown";
            case TelephonyManager.SIM_STATE_ABSENT: return "No SIM";
            case TelephonyManager.SIM_STATE_PIN_REQUIRED: return "PIN Required";
            case TelephonyManager.SIM_STATE_PUK_REQUIRED: return "PUK Required";
            case TelephonyManager.SIM_STATE_NETWORK_LOCKED: return "Network Locked";
            case TelephonyManager.SIM_STATE_READY: return "Ready";
            case TelephonyManager.SIM_STATE_NOT_READY: return "Not Ready";
            case TelephonyManager.SIM_STATE_PERM_DISABLED: return "Permanently Disabled";
            case TelephonyManager.SIM_STATE_CARD_IO_ERROR: return "Card IO Error";
            case TelephonyManager.SIM_STATE_CARD_RESTRICTED: return "Card Restricted";
            default: return "Unknown State";
        }
    }
}
