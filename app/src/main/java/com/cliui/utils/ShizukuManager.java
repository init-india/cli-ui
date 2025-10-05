package com.cliui.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ShizukuManager {
    private static final String SHIZUKU_PACKAGE = "moe.shizuku.privileged.api";
    
    public static boolean isAvailable() {
        return isShizukuInstalled() && isShizukuRunning();
    }
    
    public static boolean hasPermission() {
        if (!isAvailable()) return false;
        // Simple permission check
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", "id"});
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    public static boolean isReady() {
        return isAvailable() && hasPermission();
    }
    
    public static String executeCommandWithOutput(String cmd) {
        if (!isReady()) {
            return "❌ Shizuku not ready: " + getStatus();
        }
        
        Process process = null;
        BufferedReader reader = null;
        try {
            process = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            process.waitFor();
            return output.toString().trim();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        } finally {
            try {
                if (reader != null) reader.close();
                if (process != null) process.destroy();
            } catch (Exception e) {
                // Ignore
            }
        }
    }
    
    public static boolean executeCommand(String cmd) {
        if (!isReady()) return false;
        
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        } finally {
            if (process != null) process.destroy();
        }
    }
    
    public static String getStatus() {
        if (!isAvailable()) return "❌ Shizuku not installed/running";
        if (!hasPermission()) return "⚠️ Shizuku available but permission denied";
        return "✅ Shizuku ready";
    }
    
    private static boolean isShizukuInstalled() {
        try {
            Process process = Runtime.getRuntime().exec("pm list packages " + SHIZUKU_PACKAGE);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String output = reader.readLine();
            process.waitFor();
            return output != null && output.contains(SHIZUKU_PACKAGE);
        } catch (Exception e) {
            return false;
        }
    }
    
    private static boolean isShizukuRunning() {
        try {
            Process process = Runtime.getRuntime().exec("ps -A | grep shizuku");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String output = reader.readLine();
            process.waitFor();
            return output != null && output.contains("shizuku");
        } catch (Exception e) {
            return false;
        }
    }
    
    public static class CommandResult {
        public final boolean success;
        public final String output;
        public final int exitCode;
        
        public CommandResult(boolean success, String output, int exitCode) {
            this.success = success;
            this.output = output;
            this.exitCode = exitCode;
        }
    }

    // ADD THIS MISSING METHOD THAT PermissionManager EXPECTS
    public static CommandResult executeCommandDetailed(String cmd) {
        if (!isReady()) {
            return new CommandResult(false, "Shizuku not ready: " + getStatus(), -1);
        }
        
        Process process = null;
        BufferedReader reader = null;
        BufferedReader errorReader = null;
        try {
            process = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            
            StringBuilder output = new StringBuilder();
            StringBuilder errorOutput = new StringBuilder();
            
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
            
            int exitCode = process.waitFor();
            
            String finalOutput = output.toString().trim();
            if (errorOutput.length() > 0) {
                if (finalOutput.length() > 0) {
                    finalOutput += "\n" + errorOutput.toString().trim();
                } else {
                    finalOutput = errorOutput.toString().trim();
                }
            }
            
            return new CommandResult(exitCode == 0, finalOutput, exitCode);
        } catch (Exception e) {
            return new CommandResult(false, "Error: " + e.getMessage(), -1);
        } finally {
            try {
                if (reader != null) reader.close();
                if (errorReader != null) errorReader.close();
                if (process != null) process.destroy();
            } catch (Exception e) {
                // Ignore
            }
        }
    }
}
