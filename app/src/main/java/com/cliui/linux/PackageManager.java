package com.cliui.linux;

import android.content.Context;
import com.cliui.utils.PermissionManager;
import java.util.*;

public class PackageManager {
    private Context context;
    private PermissionManager permissionManager;
    
    public PackageManager(Context context) {
        this.context = context;
        this.permissionManager = PermissionManager.getInstance(context);
    }
    
    public String installPackage(String pkg) {
        if (!permissionManager.isShizukuAvailable()) {
            return "📦 " + pkg + "\n❌ Shizuku required for package management\n💡 Enable Shizuku or install Termux from F-Droid";
        }
        
        // Try to install via Shizuku
        if (pkg.startsWith("http") || pkg.endsWith(".apk")) {
            return installApk(pkg);
        } else {
            return installSystemPackage(pkg);
        }
    }
    
    public String removePackage(String pkg) {
        if (!permissionManager.isShizukuAvailable()) {
            return "🗑️ " + pkg + "\n❌ Shizuku required for package removal";
        }
        
        String result = ShizukuManager.executeCommandWithOutput("pm uninstall " + pkg);
        return result != null ? "✅ Removed: " + pkg + "\n" + result : "❌ Failed to remove: " + pkg;
    }
    
    public List<String> getInstalledPackages() {
        List<String> packages = new ArrayList<>();
        
        if (permissionManager.isShizukuAvailable()) {
            String result = ShizukuManager.executeCommandWithOutput("pm list packages");
            if (result != null) {
                String[] lines = result.split("\n");
                for (String line : lines) {
                    if (line.startsWith("package:")) {
                        packages.add(line.substring(8)); // Remove "package:" prefix
                    }
                }
            }
        }
        
        // Add some common packages for demo
        if (packages.isEmpty()) {
            packages.add("com.android.settings");
            packages.add("com.android.dialer");
            packages.add("com.android.mms");
            packages.add("com.google.android.gms");
        }
        
        return packages;
    }
    
    public String getPackageInfo(String pkg) {
        if (!permissionManager.isShizukuAvailable()) {
            return "📋 " + pkg + "\n❌ Shizuku required for package info";
        }
        
        String result = ShizukuManager.executeCommandWithOutput("dumpsys package " + pkg);
        if (result != null) {
            // Extract basic info from dumpsys output
            String[] lines = result.split("\n");
            StringBuilder info = new StringBuilder();
            info.append("📦 ").append(pkg).append("\n\n");
            
            for (String line : lines) {
                if (line.contains("versionName") || line.contains("userId") || 
                    line.contains("installTime") || line.contains("updateTime")) {
                    info.append(line.trim()).append("\n");
                }
                if (info.length() > 500) break; // Limit output
            }
            return info.toString();
        }
        return "❌ Cannot get info for: " + pkg;
    }
    
    public String enablePackage(String pkg) {
        if (!permissionManager.isShizukuAvailable()) {
            return "🔓 " + pkg + "\n❌ Shizuku required for package control";
        }
        
        String result = ShizukuManager.executeCommandWithOutput("pm enable " + pkg);
        return result != null ? "✅ Enabled: " + pkg : "❌ Failed to enable: " + pkg;
    }
    
    public String disablePackage(String pkg) {
        if (!permissionManager.isShizukuAvailable()) {
            return "🔒 " + pkg + "\n❌ Shizuku required for package control";
        }
        
        String result = ShizukuManager.executeCommandWithOutput("pm disable " + pkg);
        return result != null ? "✅ Disabled: " + pkg : "❌ Failed to disable: " + pkg;
    }
    
    private String installApk(String apkPath) {
        String result = ShizukuManager.executeCommandWithOutput("pm install " + apkPath);
        return result != null ? "✅ Installing: " + apkPath + "\n" + result : "❌ Failed to install APK";
    }
    
    private String installSystemPackage(String pkg) {
        return "📦 " + pkg + "\n💡 System package installation requires root access\n💡 Use: pkg install " + pkg + " in Termux";
    }
}
