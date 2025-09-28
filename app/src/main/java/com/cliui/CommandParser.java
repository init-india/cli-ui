package com.cliui;

import android.content.Context;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import com.cliui.modules.*;

public class CommandParser {
    private Context context;
    private Map<String, CommandModule> modules;
    private Stack<String> contextStack;
    
    public CommandParser(Context context) {
        this.context = context;
        this.modules = new HashMap<>();
        this.contextStack = new Stack<>();
        initializeModules();
    }
    
    private void initializeModules() {
        modules.put("call", new CallModule(context));
        modules.put("sms", new SMSModule(context));
        modules.put("map", new NavigationModule(context));
        modules.put("wifi", new ConnectivityModule(context));
        modules.put("bluetooth", new ConnectivityModule(context));
        modules.put("hotspot", new ConnectivityModule(context));
        modules.put("flash", new SystemModule(context));
        modules.put("location", new SystemModule(context));
        modules.put("mic", new SystemModule(context));
        modules.put("camera", new SystemModule(context));
        modules.put("mail", new EmailModule(context));
        modules.put("whatsapp", new WhatsAppModule(context));
        modules.put("notifications", new NotificationManager(context));
        modules.put("settings", new SettingsModule(context));
    }
    
    public String parse(String input) {
        try {
            String[] tokens = input.trim().split("\\s+");
            String command = tokens[0].toLowerCase();
            
            if (command.equals("exit")) {
                return handleExit();
            }
            
            if (command.equals("clear")) {
                return "CLEAR_SCREEN";
            }
            
            if (command.equals("help")) {
                return showHelp();
            }
            
            if (modules.containsKey(command)) {
                return modules.get(command).execute(tokens);
            }
            
            return "Command not found: " + command + "\nType 'help' for available commands.";
            
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    private String handleExit() {
        if (contextStack.isEmpty()) {
            return "Already at main context";
        }
        contextStack.pop();
        return "Returning to previous context...";
    }
    
    private String showHelp() {
        return "Available Commands:\n" +
               "• call [contact] - Make call\n" +
               "• sms [contact] [message] - Send SMS\n" +
               "• map [destination] - Navigation (OpenStreetMap)\n" +
               "• wifi - Toggle WiFi\n" +
               "• bluetooth - Toggle Bluetooth\n" +
               "• hotspot - Toggle hotspot\n" +
               "• flash - Toggle flashlight\n" +
               "• location - Toggle location\n" +
               "• camera - Open camera\n" +
               "• mail - Check email (IMAP)\n" +
               "• whatsapp - Open WhatsApp\n" +
               "• notifications - Show notifications\n" +
               "• settings - System settings\n" +
               "• clear - Clear screen\n" +
               "• exit - Return to previous context";
    }
}
