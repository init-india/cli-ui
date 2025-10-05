package com.cliui.utils;

import rikka.shizuku.Shizuku;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ExecutionException;

public class ShizukuManager {
    private static final String TAG = "ShizukuManager";
    private static final long COMMAND_TIMEOUT_MS = 30000; // 30 seconds timeout

    public static boolean isAvailable() {
        try {
            return Shizuku.pingBinder() && Shizuku.getVersion() >= 11;
        } catch (Exception e) {
            android.util.Log.e(TAG, "Shizuku availability check failed", e);
            return false;
        }
    }

    public static boolean hasPermission() {
        try {
            return Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED;
        } catch (Exception e) {
            android.util.Log.e(TAG, "Shizuku permission check failed", e);
            return false;
        }
    }

    /**
     * Execute command with timeout and better error handling
     */
    public static boolean executeCommand(String cmd) {
        if (!isAvailable() || !hasPermission()) {
            android.util.Log.w(TAG, "Shizuku not available or no permission for command: " + cmd);
            return false;
        }
        
        Process process = null;
        try {
            process = Shizuku.newProcess(
                new String[]{"/system/bin/sh", "-c", cmd},
                null,
                null
            );
            
            // Wait for process with timeout
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
     * Execute command and return output with timeout
     */
    public static String executeCommandWithOutput(String cmd) {
        if (!isAvailable() || !hasPermission()) {
            return "❌ Shizuku not available or permission denied";
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
            
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            
            StringBuilder output = new StringBuilder();
            StringBuilder errorOutput = new StringBuilder();
            
            // Read stdout and stderr in parallel
            Future<String> stdoutFuture = readStreamAsync(reader);
            Future<String> stderrFuture = readStreamAsync(errorReader);
            
            // Wait for process completion with timeout
            boolean finished = process.waitFor(COMMAND_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                return "❌ Command timeout: " + cmd;
            }
            
            int exitCode = process.exitValue();
            
            // Get the results
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
     * Read stream asynchronously to prevent blocking
     */
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

    /**
     * Execute command with detailed result including exit code and error stream
     */
    public static CommandResult executeCommandDetailed(String cmd) {
        if (!isAvailable() || !hasPermission()) {
            return new CommandResult(false, "Shizuku not available or permission denied", -1);
        }
        
        Process process = null;
        BufferedReader stdoutReader = null;
        BufferedReader stderrReader = null;
        
        try {
            process = Shizuku.newProcess(
                new String[]{"/system/bin/sh", "-c", cmd},
                null,
                null
            );
            
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
     * Check if Shizuku is ready (available + permission granted)
     */
    public static boolean isReady() {
        return isAvailable() && hasPermission();
    }

    /**
     * Get Shizuku status message
     */
    public static String getStatus() {
        if (!isAvailable()) {
            return "❌ Shizuku not available";
        }
        if (!hasPermission()) {
            return "⚠️ Shizuku available but permission denied";
        }
        return "✅ Shizuku ready";
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
        
        @Override
        public String toString() {
            return "Success: " + success + 
                   ", Exit Code: " + exitCode + 
                   ", Output: " + output;
        }
    }
}
