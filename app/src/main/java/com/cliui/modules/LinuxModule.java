package com.cliui.modules;

import android.content.Context;
import com.cliui.utils.PermissionManager;
import com.cliui.linux.TerminalSession;
import com.cliui.linux.PackageManager;
import java.util.*;

public class LinuxModule implements CommandModule {
    private Context context;
    private PermissionManager permissionManager;
    private TerminalSession terminalSession;
    private PackageManager packageManager;
    private boolean inShellMode = false;
    
    public LinuxModule(Context context) {
        this.context = context;
        this.permissionManager = PermissionManager.getInstance(context);
        this.terminalSession = new TerminalSession(context);
        this.packageManager = new PackageManager(context);
    }
    
    @Override
    public String execute(String[] tokens) {
        if (tokens.length == 0) return getUsage();
        
        String command = tokens[0].toLowerCase();
        
        // Handle shell mode
        if (inShellMode && !command.equals("exit")) {
            return executeLinuxCommand(tokens);
        }
        
        // Special commands
        switch (command) {
            case "linux":
                return showLinuxInfo();
            case "shell":
                inShellMode = true;
                return "💻 Shell Mode Activated\nType Linux commands directly\nType 'exit' to return to main CLI";
            case "pkg":
            case "package":
                return handlePackageCommand(tokens);
            case "files":
                return showFileOperations();
            case "sysinfo":
                return showSystemInfo();
            case "exit":
                inShellMode = false;
                return "↩️ Exited shell mode";
            default:
                // Direct Linux command execution
                return executeLinuxCommand(tokens);
        }
    }
    
    private String executeLinuxCommand(String[] tokens) {
        String fullCommand = String.join(" ", tokens);
        return terminalSession.executeCommand(fullCommand);
    }
    
    private String handlePackageCommand(String[] tokens) {
        if (tokens.length < 2) {
            return "📦 Package Management\n\n" +
                   "• pkg list - List installed packages\n" +
                   "• pkg info <package> - Package information\n" +
                   "• pkg install <package> - Install package\n" +
                   "• pkg remove <package> - Remove package\n" +
                   "• pkg enable <package> - Enable package\n" +
                   "• pkg disable <package> - Disable package\n\n" +
                   "💡 Shizuku required for package operations";
        }
        
        String action = tokens[1].toLowerCase();
        String packageName = tokens.length > 2 ? tokens[2] : "";
        
        switch (action) {
            case "list":
                return listPackages();
            case "info":
                return packageName.isEmpty() ? "Usage: pkg info <package>" : packageManager.getPackageInfo(packageName);
            case "install":
                return packageName.isEmpty() ? "Usage: pkg install <package>" : packageManager.installPackage(packageName);
            case "remove":
            case "uninstall":
                return packageName.isEmpty() ? "Usage: pkg remove <package>" : packageManager.removePackage(packageName);
            case "enable":
                return packageName.isEmpty() ? "Usage: pkg enable <package>" : packageManager.enablePackage(packageName);
            case "disable":
                return packageName.isEmpty() ? "Usage: pkg disable <package>" : packageManager.disablePackage(packageName);
            default:
                return "❌ Unknown package action: " + action;
        }
    }
    
    private String listPackages() {
        List<String> packages = packageManager.getInstalledPackages();
        StringBuilder sb = new StringBuilder();
        sb.append("📦 Installed Packages (").append(packages.size()).append(")\n\n");
        
        for (int i = 0; i < Math.min(packages.size(), 20); i++) {
            sb.append(i + 1).append(". ").append(packages.get(i)).append("\n");
        }
        
        if (packages.size() > 20) {
            sb.append("\n... and ").append(packages.size() - 20).append(" more packages");
        }
        
        sb.append("\n\n💡 Use 'pkg info <package>' for details");
        return sb.toString();
    }
    
    private String showLinuxInfo() {
        return "🐧 Linux Environment\n\n" +
               "Available Operations:\n" +
               "• File Operations: ls, cat, pwd, df, du, find\n" +
               "• System Info: ps, whoami, date, uname\n" +
               "• Package Management: pkg list/info/install/remove\n" +
               "• Shell Mode: shell (interactive command line)\n\n" +
               "💡 Enable Shizuku for real file system access\n" +
               "💡 Install Termux from F-Droid for full Linux environment";
    }
    
    private String showFileOperations() {
        return "📁 File Operations (with Shizuku):\n\n" +
               "• ls [path] - List directory contents\n" +
               "• cat <file> - Display file content\n" +
               "• pwd - Show current directory\n" +
               "• df - Show disk usage\n" +
               "• du [path] - Show directory usage\n" +
               "• ps - Show running processes\n" +
               "• whoami - Show current user\n" +
               "• date - Show current date/time\n\n" +
               "💡 Example: ls /sdcard/Download";
    }
    
    private String showSystemInfo() {
        return executeLinuxCommand(new String[]{"uname"}) + "\n" +
               executeLinuxCommand(new String[]{"whoami"}) + "\n" +
               executeLinuxCommand(new String[]{"date"}) + "\n" +
               executeLinuxCommand(new String[]{"df"});
    }
    
    private String getUsage() {
        return "🐧 Linux Commands:\n\n" +
               "• [command] - Execute Linux command directly\n" +
               "• linux - Show Linux environment info\n" +
               "• shell - Enter interactive shell mode\n" +
               "• pkg - Package management\n" +
               "• files - Available file operations\n" +
               "• sysinfo - System information\n" +
               "• exit - Exit shell mode\n\n" +
               "💡 Type any Linux command directly";
    }
}
