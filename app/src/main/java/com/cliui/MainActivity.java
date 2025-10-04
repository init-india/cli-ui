package com.cliui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.content.Context;
import android.widget.Toast;
import rikka.shizuku.Shizuku;
import rikka.shizuku.Shizuku.OnRequestPermissionResultListener;


public class MainActivity extends AppCompatActivity {
    
    private TextView terminalOutput;
    private EditText commandInput;
    private ScrollView terminalScroll;
    private CommandParser commandParser;
    private TelephonyManager telephonyManager;
    
    // Permission request codes
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int OVERLAY_PERMISSION_CODE = 1002;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initializeComponents();
        setupCommandParser();
        initializeTelephony();
        showWelcomeMessage();
        
        // Check for overlay permission (for potential future features)
        checkOverlayPermission();
    }
    
    private void initializeTelephony() {
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        
        // Check if this is F-Droid build or Google Play build
        if (getPackageName().endsWith(".fdroid")) {
            appendToTerminal("F-Droid Edition - Basic Telephony", "#FFFF00");
        } else {
            appendToTerminal("Google Play Edition - Enhanced Telephony", "#FFFF00");
        }
        
        // Basic telephony info available to both versions
        try {
            String networkOperator = telephonyManager.getNetworkOperatorName();
            if (networkOperator != null && !networkOperator.isEmpty()) {
                appendToTerminal("Network: " + networkOperator, "#00FFFF");
            }
            
            boolean isSimAvailable = telephonyManager.getSimState() == TelephonyManager.SIM_STATE_READY;
            appendToTerminal("SIM: " + (isSimAvailable ? "Available" : "Not Available"), "#00FFFF");
            
        } catch (SecurityException e) {
            appendToTerminal("Telephony permission required", "#FF0000");
        }
    }
    
    private void initializeComponents() {
        terminalOutput = findViewById(R.id.terminalOutput);
        commandInput = findViewById(R.id.commandInput);
        terminalScroll = findViewById(R.id.terminalScroll);
        
        commandInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                processCommand();
                return true;
            }
            return false;
        });
    }
    
    private void setupCommandParser() {
        commandParser = new CommandParser(this);
    }
    
    private void showWelcomeMessage() {
        appendToTerminal("SmartCLI v2.0 - F-Droid Edition", "#00FF00");
        appendToTerminal("Type 'help' for commands", "#FFFFFF");
        appendToTerminal("cli> ", "#00FF00");
    }
    
    private void processCommand() {
        String command = commandInput.getText().toString().trim();
        if (!command.isEmpty()) {
            appendToTerminal("cli> " + command, "#00FF00");
            String result = commandParser.parse(command);
            
            // Check if result indicates permission request needed
            if (result.contains("permission required") || result.contains("Permission required")) {
                handlePermissionRequest(command, result);
            } else if ("CLEAR_SCREEN".equals(result)) {
                clearTerminal();
            } else {
                appendToTerminal(result, "#FFFFFF");
                appendToTerminal("cli> ", "#00FF00");
            }
            
            commandInput.setText("");
            scrollToBottom();
        }
    }
    
    /**
     * Handle permission requests from CommandParser
     */
    private void handlePermissionRequest(String command, String permissionMessage) {
        appendToTerminal(permissionMessage, "#FFFF00");
        appendToTerminal("Requesting permissions...", "#FFFF00");
        
        // Extract required permissions from the command
        String[] requiredPermissions = getRequiredPermissionsForCommand(command);
        
        if (requiredPermissions.length > 0) {
            ActivityCompat.requestPermissions(this, requiredPermissions, PERMISSION_REQUEST_CODE);
        } else {
            appendToTerminal("No specific permissions required for this command", "#FF0000");
            appendToTerminal("cli> ", "#00FF00");
        }
    }
    
    /**
     * Map commands to required permissions
     */
    private String[] getRequiredPermissionsForCommand(String command) {
        String baseCommand = command.split(" ")[0].toLowerCase();
        
        switch (baseCommand) {
            case "call":
                return new String[]{
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.READ_PHONE_STATE
                };
            case "sms":
                return new String[]{
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.READ_SMS
                };
            case "contact":
                return new String[]{
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.WRITE_CONTACTS
                };
            case "map":
            case "nav":
            case "location":
                return new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                };
            case "camera":
                return new String[]{Manifest.permission.CAMERA};
            case "mic":
                return new String[]{Manifest.permission.RECORD_AUDIO};
            default:
                return new String[0];
        }
    }
    
    /**
     * Handle permission request results
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                String result = commandParser.onPermissionsGranted();
                appendToTerminal(result, "#00FF00");
            } else {
                String result = commandParser.onPermissionsDenied();
                appendToTerminal(result, "#FF0000");
            }
            
            appendToTerminal("cli> ", "#00FF00");
            scrollToBottom();
        }
    }
    
    /**
     * Check and request overlay permission if needed
     */
    private void checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                // Optional: You can request this for future overlay features
                // Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                //         Uri.parse("package:" + getPackageName()));
                // startActivityForResult(intent, OVERLAY_PERMISSION_CODE);
            }
        }
    }
    
    /**
     * Handle overlay permission result
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    appendToTerminal("Overlay permission granted", "#00FF00");
                } else {
                    appendToTerminal("Overlay permission denied", "#FF0000");
                }
            }
        }
    }
    
    private void appendToTerminal(String text, String color) {
        String html = "<font color='" + color + "'>" + text + "</font><br>";
        terminalOutput.append(android.text.Html.fromHtml(html));
    }
    
    private void clearTerminal() {
        terminalOutput.setText("");
        appendToTerminal("Terminal cleared", "#FFFFFF");
        appendToTerminal("cli> ", "#00FF00");
    }
    
    private void scrollToBottom() {
        terminalScroll.post(() -> terminalScroll.fullScroll(ScrollView.FOCUS_DOWN));
    }
    
    /**
     * Handle app resume - refresh status if needed
     */
    @Override
    protected void onResume() {
        super.onResume();
        // You can add status refresh here if needed
    }
@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check if Shizuku permission is already granted
        if (!Shizuku.pingBinder()) {
            // Request permission
            Shizuku.requestPermission(0); // 0 is the request code
        } else {
            Toast.makeText(this, "Shizuku permission already granted!", Toast.LENGTH_SHORT).show();
        }

        // Optional: listen to permission result
        Shizuku.addRequestPermissionResultListener(new OnRequestPermissionResultListener() {
            @Override
            public void onRequestPermissionResult(int requestCode, boolean granted) {
                if (requestCode == 0) {
                    if (granted) {
                        Toast.makeText(MainActivity.this, "Shizuku permission granted!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Shizuku permission denied!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }
}
