public class ConnectivityModule implements CommandModule {
    private Context context;
    private PermissionManager permissionManager;
    private ConnectivityState currentState = ConnectivityState.IDLE;
    private List<WifiNetwork> availableNetworks = new ArrayList<>();
    private List<BluetoothDevice> availableDevices = new ArrayList<>();
    private String selectedNetworkId = null;
    
    public String execute(String[] tokens) {
        if (!permissionManager.canExecute(tokens[0])) {
            return "🔧 System control requires permissions\nType command again to grant";
        }
        
        String command = tokens[0].toLowerCase();
        
        switch (command) {
            case "wifi":
                return handleWifiCommand(tokens);
            case "bluetooth":
                return handleBluetoothCommand(tokens);
            case "hotspot":
                return handleHotspotCommand(tokens);
            default:
                return "Unknown connectivity command";
        }
    }
    
    private String handleWifiCommand(String[] tokens) {
        if (tokens.length == 1) {
            // Show WiFi status
            boolean isWifiEnabled = isWifiEnabled();
            String status = isWifiEnabled ? "ON" : "OFF";
            return "📶 WiFi: " + status + "\n" + 
                   (isWifiEnabled ? "💡 Type 'wifi' again to see networks or 'wifi off' to disable" : "💡 Type 'wifi' again to enable");
        }
        
        if (tokens.length == 2) {
            String action = tokens[1].toLowerCase();
            if (action.equals("off")) {
                return disableWifi();
            } else {
                // Assume it's a network selection
                return connectToWifiNetwork(action);
            }
        }
        
        return toggleWifi();
    }
    
    private String toggleWifi() {
        boolean currentState = isWifiEnabled();
        
        if (ShizukuManager.executeCommand(currentState ? "svc wifi disable" : "svc wifi enable")) {
            return "✅ WiFi " + (currentState ? "disabled" : "enabled");
        }
        return "❌ Failed to toggle WiFi";
    }
    
    private String disableWifi() {
        if (ShizukuManager.executeCommand("svc wifi disable")) {
            return "✅ WiFi disabled";
        }
        return "❌ Failed to disable WiFi";
    }
    
    private boolean isWifiEnabled() {
        // Use Shizuku to check WiFi status
        String result = ShizukuManager.executeCommandWithOutput("settings get global wifi_on");
        return "1".equals(result.trim());
    }
    
    private String scanWifiNetworks() {
        // Use Shizuku to scan for networks
        String scanResult = ShizukuManager.executeCommandWithOutput("cmd wifi list-networks");
        // Parse scan result and populate availableNetworks
        availableNetworks = parseWifiNetworks(scanResult);
        
        StringBuilder sb = new StringBuilder();
        sb.append("📶 Available Networks:\n");
        for (int i = 0; i < Math.min(availableNetworks.size(), 10); i++) {
            WifiNetwork network = availableNetworks.get(i);
            sb.append(i + 1).append(". ").append(network.ssid)
              .append(" - ").append(network.signalStrength)
              .append(network.connected ? " (Connected)" : "")
              .append("\n");
        }
        sb.append("\n💡 Type number to connect or 'exit' to cancel");
        
        currentState = ConnectivityState.WIFI_SELECTION;
        return sb.toString();
    }
    
    // Similar implementations for Bluetooth and Hotspot...
}
