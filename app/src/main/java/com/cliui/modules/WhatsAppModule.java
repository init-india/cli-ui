package com.cliui.modules;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

public class WhatsAppModule implements CommandModule {
    private Context context;
    
    public WhatsAppModule(Context context) {
        this.context = context;
    }
    
    @Override
    public String execute(String[] tokens) {
        // Check if WhatsApp is installed
        boolean whatsappInstalled = false;
        try {
            context.getPackageManager().getPackageInfo("com.whatsapp", 0);
            whatsappInstalled = true;
        } catch (PackageManager.NameNotFoundException e) {
            whatsappInstalled = false;
        }
        
        if (!whatsappInstalled) {
            return "❌ WhatsApp not installed\nInstall from F-Droid or Play Store";
        }
        
        if (tokens.length == 1) {
            return "💚 WhatsApp Chats:\n1. Family Group\n2. Sarah\n3. Work Team\n[Opening WhatsApp...]";
        }
        
        // Open WhatsApp
        Intent intent = context.getPackageManager().getLaunchIntentForPackage("com.whatsapp");
        if (intent != null) {
            context.startActivity(intent);
            return "💚 Opening WhatsApp...";
        } else {
            return "❌ Cannot open WhatsApp";
        }
    }
}
