package com.cliui.utils;

import rikka.shizuku.Shizuku;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class ShizukuManager {

    public static boolean isAvailable() {
        try {
            return Shizuku.pingBinder() && Shizuku.getVersion() >= 11;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean hasPermission() {
        try {
            return Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED;
        } catch (Exception e) {
            return false;
        }
    }

    // Maintain backward compatibility - return boolean
    public static boolean executeCommand(String cmd) {
        if (!isAvailable()) {
            return false;
        }
        
        Process process = null;
        try {
            process = Shizuku.newProcess(
                new String[]{"/system/bin/sh", "-c", cmd},
                null,
                null
            );
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    public static String executeCommandWithOutput(String cmd) {
        if (!isAvailable()) {
            return "Shizuku not available";
        }
        
        Process process = null;
        BufferedReader reader = null;
        try {
            process = Shizuku.newProcess(
                new String[]{"/system/bin/sh", "-c", cmd},
                null,
                null
            );
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            process.waitFor();
            return sb.toString().trim();
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

    // New method for detailed results (optional)
    public static CommandResult executeCommandDetailed(String cmd) {
        boolean success = executeCommand(cmd);
        String output = executeCommandWithOutput(cmd);
        return new CommandResult(success, output);
    }

    public static class CommandResult {
        public final boolean success;
        public final String message;
        
        public CommandResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }
}
