package com.cliui.modules;

import android.content.Context;
import android.bluetooth.BluetoothDevice;
import java.util.List;
import java.util.ArrayList;
import com.cliui.utils.PermissionManager;

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
    }

    private Context context;
    private PermissionManager permissionManager;
    private ConnectivityState currentState = ConnectivityState.IDLE;
    private List<WifiNetwork> availableNetworks = new ArrayList<>();
    private List<BluetoothDevice> availableDevices = new ArrayList<>();

    public ConnectivityModule(Context context) {
        this.context = context;
        this.permissionManager = PermissionManager.getInstance(context);
    }

    @Override
    public String execute(String[] tokens) {
        if (tokens.length == 0) return "❌ No command provided";

        String command = tokens[0].toLowerCase();

        if (!permissionManager.canExecute(command)) {
            return permissionManager.getPermissionExplanation(command);
        }

        switch (command) {
            case "wifi":
                return handleWifiCommand(tokens);
            case "bluetooth":
                return handleBluetoothCommand(tokens);
            case "hotspot":
                return handleHotspotCommand(tokens);
            default:
                return "❌ Unknown connectivity command";
        }
    }

    // ===== WiFi Handling =====
    private String handleWifiCommand(String[] tokens) {
        if (tokens.length == 1) {
            boolean isEnabled = isWifiEnabled();
            return "📶 WiFi: " + (isEnabled ? "ON" : "OFF") +
                   "\n💡 Type 'wifi off' to disable or 'wifi <SSID>' to connect";
        } else if (tokens.length == 2) {
            String action = tokens[1].toLowerCase();
            if ("off".equals(action)) return disableWifi();
            return connectToWifiNetwork(action);
        }
        return toggleWifi();
    }

    private boolean isWifiEnabled() {
        if (ShizukuManager.isAvailable()) {
            String result = ShizukuManager.executeCommandWithOutput("settings get global wifi_on");
            return "1".equals(result.trim());
        }
        return false; // fallback
    }

    private String toggleWifi() {
        boolean enabled = isWifiEnabled();
        boolean success = ShizukuManager.isAvailable() &&
                          ShizukuManager.executeCommand(enabled ? "svc wifi disable" : "svc wifi enable");
        return success ? "✅ WiFi " + (enabled ? "disabled" : "enabled")
                       : "❌ Failed to toggle WiFi";
    }

    private String disableWifi() {
        if (ShizukuManager.isAvailable() && ShizukuManager.executeCommand("svc wifi disable")) {
            return "✅ WiFi disabled";
        }
        return "❌ Failed to disable WiFi";
    }

    private String connectToWifiNetwork(String ssid) {
        // Sandbox fallback
        return ShizukuManager.isAvailable()
                ? (ShizukuManager.executeCommand("am start -a android.intent.action.VIEW -d wifi:" + ssid)
                    ? "✅ Connecting to " + ssid
                    : "❌ Failed to connect to " + ssid)
                : "🔧 Cannot connect, Shizuku not available";
    }

    // ===== Bluetooth Handling =====
    private String handleBluetoothCommand(String[] tokens) {
        return "🔧 Bluetooth commands not implemented yet";
    }

    // ===== Hotspot Handling =====
    private String handleHotspotCommand(String[] tokens) {
        return "🔧 Hotspot commands not implemented yet";
    }

    // ===== Dummy WiFi parser =====
    private List<WifiNetwork> parseWifiNetworks(String output) {
        return new ArrayList<>();
    }
}
