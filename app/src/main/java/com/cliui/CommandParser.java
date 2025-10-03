package com.cliui;

import android.content.Context;
import android.telephony.TelephonyManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import com.cliui.modules.*;
import com.cliui.utils.NotificationManager;




public class CommandParser {
    private Context context;
    private Map<String, CommandModule> modules;
    private Stack<String> contextStack;
    private TelephonyManager telephonyManager;  // ADD THIS

   
    public CommandParser(Context context) {
        this.context = context;
        this.modules = new HashMap<>();
        this.contextStack = new Stack<>();
        this.telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);  // ADD THIS
        initializeModules();
    }
    
    private void initializeModules() {
        modules.put("call", new CallModule(context));
        modules.put("sms", new SMSModule(context));
        modules.put("map", new NavigationModule(context));
        modules.put("wifi", new ConnectivityModule(context));
        modules.put("bluetooth", new ConnectivityModule(context));
        modules.put("hotspot", new ConnectivityModule(context));
        modules.put("flash", new SystemModule(context));
        modules.put("location", new SystemModule(context));
        modules.put("mic", new SystemModule(context));
        modules.put("camera", new SystemModule(context));
        modules.put("mail", new EmailModule(context));
        modules.put("whatsapp", new WhatsAppModule(context));
        modules.put("notifications", new NotificationManager(context));
        modules.put("settings", new SettingsModule(context));
    }
    
    public String parse(String input) {
        try {
            String[] tokens = input.trim().split("\\s+");
            String command = tokens[0].toLowerCase();
            
            if (command.equals("exit")) {
                return handleExit();
            }
            
            if (command.equals("clear")) {
                return "CLEAR_SCREEN";
            }
            
            if (command.equals("help")) {
                return showHelp();
            }
            
            // ADD TELEPHONY COMMANDS HERE
            if (command.equals("telephony") || command.equals("phone")) {
                return handleTelephonyCommand(tokens);
            }
            
            if (command.equals("network")) {
                return handleNetworkCommand(tokens);
            }
            
            if (command.equals("sim")) {
                return handleSimCommand(tokens);
            }
            
            if (modules.containsKey(command)) {
                return modules.get(command).execute(tokens);
            }
            
            return "Command not found: " + command + "\nType 'help' for available commands.";
            
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    // ADD THESE NEW TELEPHONY METHODS
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
    
    private String handleNetworkCommand(String[] tokens) {
        return getNetworkInfo();
    }
    
    private String handleSimCommand(String[] tokens) {
        return getSimInfo();
    }
    
    private String getNetworkInfo() {
        try {
            StringBuilder info = new StringBuilder();
            info.append("=== Network Information ===\n");
            
            String networkOperator = telephonyManager.getNetworkOperatorName();
            if (networkOperator != null && !networkOperator.isEmpty()) {
                info.append("Operator: ").append(networkOperator).append("\n");
            } else {
                info.append("Operator: Unknown\n");
            }
            
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
            
            // Note: Getting detailed signal info requires more permissions
            info.append("Basic signal info available\n");
            info.append("For detailed signal strength, check system settings\n");
            
            return info.toString();
        } catch (SecurityException e) {
            return "Error: Telephony permission required";
        }
    }
    
    private String getDeviceInfo() {
        try {
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
    
    private String handleExit() {
        if (contextStack.isEmpty()) {
            return "Already at main context";
        }
        contextStack.pop();
        return "Returning to previous context...";
    }
    
    private String showHelp() {
        return "Available Commands:\n" +
               "• call [contact] - Make call\n" +
               "• sms [contact] [message] - Send SMS\n" +
               "• telephony [network|sim|signal|device] - Phone information\n" +
               "• network - Show network info\n" +
               "• sim - Show SIM status\n" +
               "• map [destination] - Navigation (OpenStreetMap)\n" +
               "• wifi - Toggle WiFi\n" +
               "• bluetooth - Toggle Bluetooth\n" +
               "• hotspot - Toggle hotspot\n" +
               "• flash - Toggle flashlight\n" +
               "• location - Toggle location\n" +
               "• camera - Open camera\n" +
               "• mail - Check email (IMAP)\n" +
               "• whatsapp - Open WhatsApp\n" +
               "• notifications - Show notifications\n" +
               "• settings - System settings\n" +
               "• clear - Clear screen\n" +
               "• exit - Return to previous context";
    }
}
