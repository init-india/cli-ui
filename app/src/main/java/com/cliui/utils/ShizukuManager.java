package com.cliui.utils;

import rikka.shizuku.Shizuku;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class ShizukuManager {
    
    // Check if Shizuku is available and we have permission
    public static boolean isAvailable() {
        try {
            return Shizuku.pingBinder() && 
                   Shizuku.checkSelfPermission() == Shizuku.PERMISSION_GRANTED;
        } catch (Exception e) {
            return false;
        }
    }
    
    // Check if we have permission for specific operations
    public static boolean hasPermission() {
        try {
            return Shizuku.checkSelfPermission() == Shizuku.PERMISSION_GRANTED;
        } catch (Exception e) {
            return false;
        }
    }
    
    // Execute command with proper error handling
    public static CommandResult executeCommand(String cmd) {
        if (!isAvailable()) {
            return new CommandResult(false, "Shizuku not available or no permission");
        }
        
        // Validate command input
        if (cmd == null || cmd.trim().isEmpty()) {
            return new CommandResult(false, "Empty command");
        }
        
        // Check for potentially dangerous commands
        if (isDangerousCommand(cmd)) {
            return new CommandResult(false, "Potentially dangerous command blocked: " + cmd);
        }
        
        Process process = null;
        BufferedReader reader = null;
        BufferedReader errorReader = null;
        
        try {
            process = Shizuku.newProcess(
                new String[]{"/system/bin/sh", "-c", cmd},
                null,
                null
            );
            
            // Add timeout to prevent hanging
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroy();
                return new CommandResult(false, "Command timeout: " + cmd);
            }
            
            int exitCode = process.exitValue();
            
            // Read output
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            // Read errors
            errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            StringBuilder error = new StringBuilder();
            while ((line = errorReader.readLine()) != null) {
                error.append(line).append("\n");
            }
            
            String result = output.toString().trim();
            String errorMsg = error.toString().trim();
            
            if (exitCode == 0) {
                return new CommandResult(true, result.isEmpty() ? "Success" : result);
            } else {
                return new CommandResult(false, 
                    errorMsg.isEmpty() ? "Exit code: " + exitCode : errorMsg);
            }
            
        } catch (Exception e) {
            return new CommandResult(false, "Execution failed: " + e.getMessage());
        } finally {
            // Cleanup resources
            try {
                if (reader != null) reader.close();
                if (errorReader != null) errorReader.close();
                if (process != null) process.destroy();
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }
    
    // Simplified version for boolean results (backward compatibility)
    public static boolean executeCommandSimple(String cmd) {
        CommandResult result = executeCommand(cmd);
        return result.success;
    }
    
    // Simplified version for string output (backward compatibility)
    public static String executeCommandWithOutput(String cmd) {
        CommandResult result = executeCommand(cmd);
        return result.message;
    }
    
    // Execute command with timeout
    public static CommandResult executeCommandWithTimeout(String cmd, long timeout, TimeUnit unit) {
        if (!isAvailable()) {
            return new CommandResult(false, "Shizuku not available");
        }
        
        Process process = null;
        try {
            process = Shizuku.newProcess(
                new String[]{"/system/bin/sh", "-c", cmd},
                null,
                null
            );
            
            boolean finished = process.waitFor(timeout, unit);
            if (!finished) {
                process.destroy();
                return new CommandResult(false, "Command timeout after " + timeout + " " + unit.toString().toLowerCase());
            }
            
            int exitCode = process.exitValue();
            String output = readStream(process.getInputStream());
            String error = readStream(process.getErrorStream());
            
            if (exitCode == 0) {
                return new CommandResult(true, output.isEmpty() ? "Success" : output);
            } else {
                return new CommandResult(false, error.isEmpty() ? "Exit code: " + exitCode : error);
            }
            
        } catch (Exception e) {
            return new CommandResult(false, "Execution failed: " + e.getMessage());
        } finally {
            if (process != null) process.destroy();
        }
    }
    
    // Check system status
    public static SystemStatus getSystemStatus() {
        SystemStatus status = new SystemStatus();
        
        status.available = isAvailable();
        status.hasPermission = hasPermission();
        
        if (status.available) {
            // Test basic command execution
            CommandResult testResult = executeCommand("echo 'test'");
            status.working = testResult.success;
            status.apiVersion = getShizukuVersion();
        }
        
        return status;
    }
    
    // Security: Block dangerous commands
    private static boolean isDangerousCommand(String cmd) {
        if (cmd == null) return false;
        
        String lowerCmd = cmd.toLowerCase();
        String[] dangerousPatterns = {
            "rm -rf", "dd if=", "mkfs", "fdisk", "busybox",
            "su -c", "pm uninstall", "am force-stop",
            "settings put global development_settings_enabled 1"
        };
        
        for (String pattern : dangerousPatterns) {
            if (lowerCmd.contains(pattern)) {
                return true;
            }
        }
        
        return false;
    }
    
    private static String readStream(java.io.InputStream inputStream) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            return sb.toString().trim();
        } catch (Exception e) {
            return "";
        }
    }
    
    private static String getShizukuVersion() {
        try {
            // Try to get Shizuku version information
            CommandResult result = executeCommand("shizuku version");
            if (result.success) {
                return result.message;
            }
        } catch (Exception e) {
            // Ignore version check errors
        }
        return "Unknown";
    }
    
    // Result class for detailed command execution results
    public static class CommandResult {
        public final boolean success;
        public final String message;
        
        public CommandResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        @Override
        public String toString() {
            return (success ? "✅ " : "❌ ") + message;
        }
    }
    
    // System status information
    public static class SystemStatus {
        public boolean available = false;
        public boolean hasPermission = false;
        public boolean working = false;
        public String apiVersion = "Unknown";
        
        @Override
        public String toString() {
            return "Shizuku Status: " + 
                   (available ? "Available" : "Not Available") + " | " +
                   "Permission: " + (hasPermission ? "Granted" : "Denied") + " | " +
                   "Working: " + (working ? "Yes" : "No") + " | " +
                   "Version: " + apiVersion;
        }
    }
}
