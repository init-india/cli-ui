package com.cliui.modules;

import android.content.Context;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import com.cliui.utils.PermissionManager;
import com.cliui.utils.ShizukuManager;

import android.annotation.SuppressLint;

public class ConnectivityModule implements CommandModule {

    public enum ConnectivityState {
        IDLE, WIFI_SELECTION, BLUETOOTH_SELECTION, HOTSPOT_SELECTION
    }

    public static class WifiNetwork {
        public String ssid;
        public int signalStrength;
        public boolean connected;
        public WifiNetwork(String ssid, int signalStrength, boolean connected) {
            this.ssid = ssid;
            this.signalStrength = signalStrength;
            this.connected = connected;
        }
        
        @Override
        public String toString() {
            return ssid + " (" + signalStrength + "dBm)" + (connected ? " ✅" : "");
        }
    }

    private Context context;
    private PermissionManager permissionManager;
    private ConnectivityState currentState = ConnectivityState.IDLE;
    private List<WifiNetwork> availableNetworks = new ArrayList<>();
    private List<BluetoothDevice> availableDevices = new ArrayList<>();
    private BluetoothAdapter bluetoothAdapter;

    public ConnectivityModule(Context context) {
        this.context = context;
        this.permissionManager = PermissionManager.getInstance(context);
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public String execute(String[] tokens) {
        if (tokens.length == 0) return "❌ No command provided";

        String command = tokens[0].toLowerCase();
        String fullCommand = String.join(" ", tokens).toLowerCase();

        // Check permissions using PermissionManager
        if (!permissionManager.canExecute(fullCommand)) {
            return permissionManager.getPermissionExplanation(fullCommand);
        }

        // For Shizuku commands, check if Shizuku is available
        if (isShizukuCommand(fullCommand) && !ShizukuManager.isAvailable()) {
            return "❌ Shizuku not available\n" +
                   "This command requires system-level access\n" +
                   "Install Shizuku for direct control or use system settings";
        }

        switch (command) {
            case "wifi":
                return handleWifiCommand(tokens);
            case "bluetooth":
                return handleBluetoothCommand(tokens);
            case "hotspot":
                return handleHotspotCommand(tokens);
            case "scan":
                return handleScanCommand(tokens);
            case "status":
                return getConnectivityStatus();
            default:
                return "❌ Unknown connectivity command. Available: wifi, bluetooth, hotspot, scan, status";
        }
    }

    // ===== WiFi Handling =====
    private String handleWifiCommand(String[] tokens) {
        if (tokens.length == 1) {
            boolean isEnabled = isWifiEnabled();
            String status = "📶 WiFi: " + (isEnabled ? "ON" : "OFF");
            if (isEnabled) {
                status += "\n📡 Available networks: " + availableNetworks.size();
            }
            status += "\n💡 Commands: 'wifi on|off|scan|list'";
            return status;
        }

        String action = tokens[1].toLowerCase();
        String fullCommand = "wifi " + action;
        
        // Check specific command permissions
        if (!permissionManager.canExecute(fullCommand)) {
            return permissionManager.getPermissionExplanation(fullCommand);
        }

        switch (action) {
            case "on":
                return enableWifi();
            case "off":
                return disableWifi();
            case "scan":
                return scanWifiNetworks();
            case "list":
                return listWifiNetworks();
            default:
                return "❌ Unknown WiFi command: " + action;
        }
    }

    private boolean isWifiEnabled() {
        if (ShizukuManager.isAvailable()) {
            try {
                String result = ShizukuManager.executeCommandWithOutput("settings get global wifi_on");
                return "1".equals(result.trim());
            } catch (Exception e) {
                return false;
            }
        }
        return false; // fallback
    }

@SuppressLint("MissingPermission")

    private String enableWifi() {
        if (!ShizukuManager.isAvailable()) {
            return "❌ Shizuku not available for WiFi control";
        }
        
        boolean success = ShizukuManager.executeCommand("svc wifi enable");
        return success ? "✅ WiFi enabled" : "❌ Failed to enable WiFi";
    }

@SuppressLint("MissingPermission")

    private String disableWifi() {
        if (!ShizukuManager.isAvailable()) {
            return "❌ Shizuku not available for WiFi control";
        }
        
        boolean success = ShizukuManager.executeCommand("svc wifi disable");
        return success ? "✅ WiFi disabled" : "❌ Failed to disable WiFi";
    }


@SuppressLint("MissingPermission")

    private String scanWifiNetworks() {
        // For WiFi scanning, we need location permissions in addition to Shizuku
        if (!permissionManager.canAccessLocation()) {
            return "📍 Location permission required for WiFi scanning\n" +
                   "WiFi scanning needs location access to detect networks";
        }

        if (!ShizukuManager.isAvailable()) {
            return "❌ Shizuku not available for WiFi scanning";
        }
        
        try {
            // Simulate scanning - in real implementation, you'd use WifiManager
            availableNetworks.clear();
            availableNetworks.add(new WifiNetwork("Home_WiFi", -45, true));
            availableNetworks.add(new WifiNetwork("Office_Network", -60, false));
            availableNetworks.add(new WifiNetwork("Guest_WiFi", -70, false));
            availableNetworks.add(new WifiNetwork("AndroidAP", -55, false));
            
            return "✅ Scanned 4 WiFi networks\nType 'wifi list' to see them";
        } catch (Exception e) {
            return "❌ Failed to scan WiFi networks: " + e.getMessage();
        }
    }


@SuppressLint("MissingPermission")

    private String listWifiNetworks() {
        if (availableNetworks.isEmpty()) {
            return "📶 No networks scanned. Type 'wifi scan' first.";
        }
        
        StringBuilder result = new StringBuilder();
        result.append("📶 Available WiFi Networks:\n");
        for (int i = 0; i < availableNetworks.size(); i++) {
            WifiNetwork network = availableNetworks.get(i);
            result.append(i + 1).append(". ").append(network.toString()).append("\n");
        }
        return result.toString();
    }

    // ===== Bluetooth Handling =====
    private String handleBluetoothCommand(String[] tokens) {
        if (bluetoothAdapter == null) {
            return "❌ Bluetooth not available on this device";
        }

        if (tokens.length == 1) {
            boolean isEnabled = bluetoothAdapter.isEnabled();
            return "🔵 Bluetooth: " + (isEnabled ? "ON" : "OFF") +
                   "\n💡 Commands: 'bluetooth on|off|scan|devices'";
        }

        String action = tokens[1].toLowerCase();
        String fullCommand = "bluetooth " + action;
        
        // Check specific command permissions
        if (!permissionManager.canExecute(fullCommand)) {
            return permissionManager.getPermissionExplanation(fullCommand);
        }

        switch (action) {
            case "on":
                return enableBluetooth();
            case "off":
                return disableBluetooth();
            case "scan":
                return scanBluetoothDevices();
            case "devices":
                return listBluetoothDevices();
            default:
                return "❌ Unknown Bluetooth command: " + action;
        }
    }


@SuppressLint("MissingPermission")
    private String enableBluetooth() {
        // Try Shizuku first for system-level control
        if (ShizukuManager.isAvailable()) {
            boolean success = ShizukuManager.executeCommand("svc bluetooth enable");
            if (success) return "✅ Bluetooth enabled (via Shizuku)";
        }
        
        // Fallback to standard Bluetooth API
        if (bluetoothAdapter.enable()) {
            return "✅ Bluetooth enabled";
        } else {
            return "❌ Failed to enable Bluetooth";
        }
    }


@SuppressLint("MissingPermission")
    private String disableBluetooth() {
        // Try Shizuku first for system-level control
        if (ShizukuManager.isAvailable()) {
            boolean success = ShizukuManager.executeCommand("svc bluetooth disable");
            if (success) return "✅ Bluetooth disabled (via Shizuku)";
        }
        
        // Fallback to standard Bluetooth API
        if (bluetoothAdapter.disable()) {
            return "✅ Bluetooth disabled";
        } else {
            return "❌ Failed to disable Bluetooth";
        }
    }

@SuppressLint("MissingPermission")

    private String scanBluetoothDevices() {
        if (!bluetoothAdapter.isEnabled()) {
            return "❌ Bluetooth is disabled. Enable it first with 'bluetooth on'";
        }
        
        // Check if we have location permission for Bluetooth scanning
        if (!permissionManager.canAccessLocation()) {
            return "📍 Location permission required for Bluetooth scanning\n" +
                   "Bluetooth scanning needs location access to discover devices";
        }
        
        // Simulate device discovery
        availableDevices.clear();
        return "✅ Scanning for Bluetooth devices...\n" +
               "📱 Found simulated devices (in real app, would find actual devices)\n" +
               "Type 'bluetooth devices' to see paired devices";
    }

@SuppressLint("MissingPermission")
    private String listBluetoothDevices() {
        if (bluetoothAdapter == null) {
            return "❌ Bluetooth not available";
        }
        
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.isEmpty()) {
            return "📱 No paired Bluetooth devices found";
        }
        
        StringBuilder result = new StringBuilder();
        result.append("📱 Paired Bluetooth Devices:\n");
        int i = 1;
        for (BluetoothDevice device : pairedDevices) {
            result.append(i++).append(". ").append(device.getName())
                  .append(" (").append(device.getAddress()).append(")\n");
        }
        return result.toString();
    }

    // ===== Hotspot Handling =====
    private String handleHotspotCommand(String[] tokens) {
        if (tokens.length == 1) {
            return "📱 Mobile Hotspot\n" +
                   "💡 Commands: 'hotspot on|off|status'\n" +
                   "⚠️  Requires system permissions via Shizuku";
        }

        String action = tokens[1].toLowerCase();
        String fullCommand = "hotspot " + action;
        
        // Check specific command permissions
        if (!permissionManager.canExecute(fullCommand)) {
            return permissionManager.getPermissionExplanation(fullCommand);
        }

        switch (action) {
            case "on":
                return enableHotspot();
            case "off":
                return disableHotspot();
            case "status":
                return getHotspotStatus();
            default:
                return "❌ Unknown hotspot command: " + action;
        }
    }


@SuppressLint("MissingPermission")

    private String enableHotspot() {
        if (!ShizukuManager.isAvailable()) {
            return "❌ Shizuku not available for hotspot control";
        }
        
        // Hotspot enabling via Shizuku (simplified)
        boolean success = ShizukuManager.executeCommand("svc wifi set-usb-tethering enabled");
        return success ? "✅ Hotspot enabled" : "❌ Failed to enable hotspot\n" +
               "📱 Please check system settings for manual setup";
    }


@SuppressLint("MissingPermission")

    private String disableHotspot() {
        if (!ShizukuManager.isAvailable()) {
            return "❌ Shizuku not available for hotspot control";
        }
        
        boolean success = ShizukuManager.executeCommand("svc wifi set-usb-tethering disabled");
        return success ? "✅ Hotspot disabled" : "❌ Failed to disable hotspot";
    }

@SuppressLint("MissingPermission")

    private String getHotspotStatus() {
        return "📱 Hotspot Status:\n" +
               "🔧 Basic hotspot controls available via Shizuku\n" +
               "⚠️  Full control requires system permissions\n" +
               "💡 Use 'hotspot on|off' to toggle (requires Shizuku)";
    }

    // ===== Utility Methods =====
    private String handleScanCommand(String[] tokens) {
        String wifiScan = scanWifiNetworks();
        String btScan = scanBluetoothDevices();
        return wifiScan + "\n\n" + btScan;
    }

@SuppressLint("MissingPermission")

    private String getConnectivityStatus() {
        StringBuilder status = new StringBuilder();
        status.append("📊 Connectivity Status:\n");
        status.append("📶 WiFi: ").append(isWifiEnabled() ? "ON" : "OFF").append("\n");
        status.append("🔵 Bluetooth: ").append(bluetoothAdapter != null && bluetoothAdapter.isEnabled() ? "ON" : "OFF").append("\n");
        status.append("📡 Scanned networks: ").append(availableNetworks.size()).append("\n");
        status.append("📱 Paired devices: ").append(bluetoothAdapter != null ? bluetoothAdapter.getBondedDevices().size() : 0).append("\n");
        status.append("🔧 Shizuku: ").append(ShizukuManager.isAvailable() ? "Available" : "Not Available");
        return status.toString();
    }

    // ===== Permission & Shizuku Detection =====
    private boolean isShizukuCommand(String command) {
        return command.startsWith("wifi on") || 
               command.startsWith("wifi off") ||
               command.startsWith("bluetooth on") || 
               command.startsWith("bluetooth off") ||
               command.startsWith("hotspot on") || 
               command.startsWith("hotspot off");
    }

    // ===== Dummy WiFi parser (placeholder for real implementation) =====
    private List<WifiNetwork> parseWifiNetworks(String output) {
        List<WifiNetwork> networks = new ArrayList<>();
        // In real implementation, parse the actual scan output
        if (output.contains("SSID")) {
            networks.add(new WifiNetwork("Home_Network", -45, true));
            networks.add(new WifiNetwork("Office_WiFi", -60, false));
        }
        return networks;
    }
}
