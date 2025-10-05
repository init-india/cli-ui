package com.cliui.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import com.cliui.utils.PermissionManager;


public class ShizukuManager {
    private static final String TAG = "ShizukuManager";
    private static final long COMMAND_TIMEOUT_MS = 30000;
    private static Boolean shizukuAvailable = null;
    private static Boolean shizukuPermitted = null;

    // Shizuku package constants
    private static final String SHIZUKU_PACKAGE = "moe.shizuku.privileged.api";
    private static final String SHIZUKU_DAEMON = "shizuku_daemon";
    
    /**
     * Check if Shizuku is available (installed and running)
     */
    public static boolean isAvailable() {
        if (shizukuAvailable == null) {
            shizukuAvailable = isShizukuInstalled() && isShizukuRunning();
        }
        return shizukuAvailable;
    }

    /**
     * Check if Shizuku permission is granted to our app
     */
    public static boolean hasPermission() {
        if (!isAvailable()) return false;
        if (shizukuPermitted == null) {
            shizukuPermitted = checkShizukuPermission();
        }
        return shizukuPermitted;
    }

    /**
     * Check if Shizuku is ready (available + permission granted)
     */
    public static boolean isReady() {
        return isAvailable() && hasPermission();
    }

    /**
     * Execute command via Shizuku with timeout
     */
    public static boolean executeCommand(String cmd) {
        if (!isReady()) {
            android.util.Log.w(TAG, "Shizuku not ready for command: " + cmd);
            return false;
        }
        
        Process process = null;
        try {
            process = createShizukuProcess(cmd);
            boolean finished = process.waitFor(COMMAND_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroy();
                android.util.Log.w(TAG, "Command timeout: " + cmd);
                return false;
            }
            
            int exitCode = process.exitValue();
            boolean success = exitCode == 0;
            
            if (!success) {
                android.util.Log.w(TAG, "Command failed with exit code " + exitCode + ": " + cmd);
            }
            
            return success;
        } catch (Exception e) {
            android.util.Log.e(TAG, "Command execution failed: " + cmd, e);
            return false;
        } finally {
            if (process != null) {
                try {
                    process.destroy();
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
    }

    /**
     * Execute command and return output
     */
    public static String executeCommandWithOutput(String cmd) {
        if (!isReady()) {
            return "❌ Shizuku not ready: " + getStatus();
        }
        
        Process process = null;
        BufferedReader reader = null;
        BufferedReader errorReader = null;
        
        try {
            process = createShizukuProcess(cmd);
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            
            StringBuilder output = new StringBuilder();
            StringBuilder errorOutput = new StringBuilder();
            
            // Read stdout and stderr in parallel
            Future<String> stdoutFuture = readStreamAsync(reader);
            Future<String> stderrFuture = readStreamAsync(errorReader);
            
            // Wait for process completion
            boolean finished = process.waitFor(COMMAND_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                return "❌ Command timeout: " + cmd;
            }
            
            int exitCode = process.exitValue();
            
            // Get results
            String stdout = stdoutFuture.get(5, TimeUnit.SECONDS);
            String stderr = stderrFuture.get(5, TimeUnit.SECONDS);
            
            if (stdout != null && !stdout.isEmpty()) {
                output.append(stdout);
            }
            
            if (stderr != null && !stderr.isEmpty()) {
                if (output.length() > 0) output.append("\n");
                output.append("Error: ").append(stderr);
            }
            
            if (exitCode != 0) {
                return "❌ Exit code " + exitCode + ": " + output.toString();
            }
            
            return output.toString().trim();
            
        } catch (TimeoutException e) {
            return "❌ Read operation timeout for command: " + cmd;
        } catch (Exception e) {
            return "❌ Execution error: " + e.getMessage();
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

    /**
     * Execute command with detailed result
     */
    public static CommandResult executeCommandDetailed(String cmd) {
        if (!isReady()) {
            return new CommandResult(false, "Shizuku not ready: " + getStatus(), -1);
        }
        
        Process process = null;
        BufferedReader stdoutReader = null;
        BufferedReader stderrReader = null;
        
        try {
            process = createShizukuProcess(cmd);
            stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            stderrReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            
            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();
            
            String line;
            while ((line = stdoutReader.readLine()) != null) {
                if (stdout.length() > 0) stdout.append("\n");
                stdout.append(line);
            }
            
            while ((line = stderrReader.readLine()) != null) {
                if (stderr.length() > 0) stderr.append("\n");
                stderr.append(line);
            }
            
            boolean finished = process.waitFor(COMMAND_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroy();
                return new CommandResult(false, "Command timeout", -1);
            }
            
            int exitCode = process.exitValue();
            boolean success = exitCode == 0;
            
            String fullOutput = stdout.toString();
            if (stderr.length() > 0) {
                if (fullOutput.length() > 0) fullOutput += "\n";
                fullOutput += "STDERR: " + stderr.toString();
            }
            
            return new CommandResult(success, fullOutput, exitCode);
            
        } catch (Exception e) {
            return new CommandResult(false, "Execution error: " + e.getMessage(), -1);
        } finally {
            try {
                if (stdoutReader != null) stdoutReader.close();
                if (stderrReader != null) stderrReader.close();
                if (process != null) process.destroy();
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    /**
     * Get Shizuku status for UI display
     */
    public static String getStatus() {
        if (!isAvailable()) {
            return "❌ Shizuku not installed/running";
        }
        if (!hasPermission()) {
            return "⚠️ Shizuku available but permission denied";
        }
        return "✅ Shizuku ready";
    }

    /**
     * Reset cached states (useful when app resumes)
     */
    public static void resetState() {
        shizukuAvailable = null;
        shizukuPermitted = null;
    }

    // ===== PRIVATE METHODS =====

    private static boolean isShizukuInstalled() {
        try {
            Process process = Runtime.getRuntime().exec("pm list packages " + SHIZUKU_PACKAGE);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String output = reader.readLine();
            process.waitFor();
            return output != null && output.contains(SHIZUKU_PACKAGE);
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error checking Shizuku installation", e);
            return false;
        }
    }

    private static boolean isShizukuRunning() {
        try {
            Process process = Runtime.getRuntime().exec("ps -A | grep " + SHIZUKU_DAEMON);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String output = reader.readLine();
            process.waitFor();
            return output != null && output.contains(SHIZUKU_DAEMON);
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error checking Shizuku running state", e);
            return false;
        }
    }

    private static boolean checkShizukuPermission() {
        try {
            // Try to execute a simple command that requires Shizuku permission
            Process process = createShizukuProcess("id");
            int exitCode = process.waitFor();
            process.destroy();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static Process createShizukuProcess(String cmd) throws Exception {
        // Use Shizuku's command execution method
        // This simulates what the real Shizuku API would do
        return Runtime.getRuntime().exec(new String[]{"/system/bin/sh", "-c", cmd});
    }

    private static Future<String> readStreamAsync(final BufferedReader reader) {
        return Executors.newSingleThreadExecutor().submit(() -> {
            StringBuilder content = new StringBuilder();
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    if (content.length() > 0) content.append("\n");
                    content.append(line);
                }
                return content.toString();
            } catch (Exception e) {
                return "Stream read error: " + e.getMessage();
            }
        });
    }

    // ===== COMMAND RESULT CLASS =====

    public static class CommandResult {
        public final boolean success;
        public final String output;
        public final int exitCode;
        
        public CommandResult(boolean success, String output, int exitCode) {
            this.success = success;
            this.output = output;
            this.exitCode = exitCode;
        }
        
        @Override
        public String toString() {
            return "Success: " + success + ", Exit Code: " + exitCode + ", Output: " + output;
        }
    }

    // ===== SYSTEM COMMAND HELPERS =====

    /**
     * Common system commands for easy access
     */
    public static class SystemCommands {
        public static String getInstalledPackages() {
            return executeCommandWithOutput("pm list packages");
        }
        
        public static String getPackageInfo(String packageName) {
            return executeCommandWithOutput("dumpsys package " + packageName);
        }
        
        public static boolean installPackage(String apkPath) {
            return executeCommand("pm install " + apkPath);
        }
        
        public static boolean uninstallPackage(String packageName) {
            return executeCommand("pm uninstall " + packageName);
        }
        
        public static boolean enablePackage(String packageName) {
            return executeCommand("pm enable " + packageName);
        }
        
        public static boolean disablePackage(String packageName) {
            return executeCommand("pm disable " + packageName);
        }
        
        public static String getSystemProperty(String property) {
            return executeCommandWithOutput("getprop " + property);
        }
        
        public static boolean setSystemProperty(String property, String value) {
            return executeCommand("setprop " + property + " " + value);
        }
        
        public static String getSettingsValue(String namespace, String key) {
            return executeCommandWithOutput("settings get " + namespace + " " + key);
        }
        
        public static boolean setSettingsValue(String namespace, String key, String value) {
            return executeCommand("settings put " + namespace + " " + key + " " + value);
        }
        
        public static boolean setWifiEnabled(boolean enabled) {
            return executeCommand("svc wifi " + (enabled ? "enable" : "disable"));
        }
        
        public static boolean setBluetoothEnabled(boolean enabled) {
            return executeCommand("svc bluetooth " + (enabled ? "enable" : "disable"));
        }
        
        public static String getDeviceInfo() {
            return executeCommandWithOutput("getprop ro.product.model && getprop ro.build.version.release");
        }
    }
}
