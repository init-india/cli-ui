package com.cliui.linux;

import android.content.Context;
import com.cliui.utils.PermissionManager;
import java.io.*;

public class TerminalSession {
    private Context context;
    private PermissionManager permissionManager;
    
    public TerminalSession(Context context) {
        this.context = context;
        this.permissionManager = PermissionManager.getInstance(context);
    }
    
    public String executeCommand(String command) {
        // Check if this is a file operation that requires Shizuku
        if (isFileOperation(command) && permissionManager.isShizukuAvailable()) {
            return executeWithShizuku(command);
        }
        
        // Check if this is a system info command
        if (isSystemInfoCommand(command)) {
            return executeSystemInfo(command);
        }
        
        // Regular command execution
        return executeRegularCommand(command);
    }
    
    private String executeWithShizuku(String command) {
        String[] parts = command.split(" ");
        String baseCommand = parts[0];
        
        switch (baseCommand) {
            case "ls":
                return listFiles(parts.length > 1 ? parts[1] : "/sdcard");
            case "cat":
                return readFile(parts.length > 1 ? parts[1] : "");
            case "pwd":
                return getCurrentDirectory();
            case "cd":
                return changeDirectory(parts.length > 1 ? parts[1] : "");
            case "cp":
                return copyFile(parts);
            case "mv":
                return moveFile(parts);
            case "rm":
                return deleteFile(parts.length > 1 ? parts[1] : "");
            case "mkdir":
                return createDirectory(parts.length > 1 ? parts[1] : "");
            case "find":
                return findFiles(parts.length > 1 ? parts[1] : "");
            case "df":
                return showDiskUsage();
            case "du":
                return showDirectoryUsage(parts.length > 1 ? parts[1] : ".");
            case "ps":
                return showProcesses();
            case "whoami":
                return showCurrentUser();
            case "date":
                return showCurrentDate();
            default:
                return executeRegularCommand(command);
        }
    }
    
    private String listFiles(String path) {
        String result = ShizukuManager.executeCommandWithOutput("ls -la " + path);
        if (result != null && !result.contains("Permission denied")) {
            return "📁 " + path + ":\n" + result;
        }
        return "❌ Cannot access: " + path + "\n💡 Enable Shizuku for file access";
    }
    
    private String readFile(String filePath) {
        if (filePath.isEmpty()) {
            return "❌ Usage: cat <filename>";
        }
        
        String result = ShizukuManager.executeCommandWithOutput("cat " + filePath);
        if (result != null && !result.contains("No such file")) {
            return "📄 " + filePath + ":\n" + result;
        }
        return "❌ Cannot read: " + filePath;
    }
    
    private String getCurrentDirectory() {
        String result = ShizukuManager.executeCommandWithOutput("pwd");
        return result != null ? "📂 " + result : "❌ Cannot get current directory";
    }
    
    private String showDiskUsage() {
        String result = ShizukuManager.executeCommandWithOutput("df -h");
        return result != null ? "💾 Disk Usage:\n" + result : "❌ Cannot get disk usage";
    }
    
    private String showProcesses() {
        String result = ShizukuManager.executeCommandWithOutput("ps");
        return result != null ? "🖥️ Running Processes:\n" + result : "❌ Cannot get process list";
    }
    
    private String showCurrentUser() {
        String result = ShizukuManager.executeCommandWithOutput("whoami");
        return result != null ? "👤 Current user: " + result.trim() : "❌ Cannot get user info";
    }
    
    private String showCurrentDate() {
        String result = ShizukuManager.executeCommandWithOutput("date");
        return result != null ? "📅 " + result : "❌ Cannot get date";
    }
    
    // Placeholder implementations for other commands
    private String changeDirectory(String path) {
        return "📂 Changed to: " + path + " (simulated)";
    }
    
    private String copyFile(String[] parts) {
        return "📋 Copy: " + String.join(" ", parts) + " (simulated)";
    }
    
    private String moveFile(String[] parts) {
        return "🚚 Move: " + String.join(" ", parts) + " (simulated)";
    }
    
    private String deleteFile(String path) {
        return "🗑️ Delete: " + path + " (simulated - use with caution)";
    }
    
    private String createDirectory(String path) {
        return "📁 Create: " + path + " (simulated)";
    }
    
    private String findFiles(String pattern) {
        return "🔍 Find: " + pattern + " (simulated)";
    }
    
    private String showDirectoryUsage(String path) {
        return "📊 Usage of " + path + " (simulated)";
    }
    
    private String executeSystemInfo(String command) {
        switch (command) {
            case "uname":
                return "🐧 Linux: Android (" + System.getProperty("os.version") + ")";
            case "free":
                return "💾 Memory: Use system settings for detailed info";
            case "top":
                return "📈 Process monitor: Use system task manager";
            default:
                return "💻 " + command + " output would appear here";
        }
    }
    
    private String executeRegularCommand(String command) {
        return "💻 Command: " + command + "\n" +
               "💡 Enable Shizuku for real file operations\n" +
               "💡 Install Termux from F-Droid for full Linux environment";
    }
    
    private boolean isFileOperation(String command) {
        String[] fileCommands = {"ls", "cat", "pwd", "cd", "cp", "mv", "rm", "mkdir", "find", "df", "du"};
        for (String cmd : fileCommands) {
            if (command.startsWith(cmd)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isSystemInfoCommand(String command) {
        String[] infoCommands = {"uname", "free", "top", "whoami", "date", "ps"};
        for (String cmd : infoCommands) {
            if (command.equals(cmd)) {
                return true;
            }
        }
        return false;
    }
}
