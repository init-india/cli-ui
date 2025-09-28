package com.cliui.modules;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class SMSModule implements CommandModule {
    private Context context;
    
    public SMSModule(Context context) {
        this.context = context;
    }
    
    @Override
    public String execute(String[] tokens) {
        if (tokens.length == 1) {
            return "💬 Recent Messages:\n1. John - 'Meeting at 3PM'\n2. Bank - 'Transaction alert'\n3. Sarah - 'Lunch today?'";
        }
        
        if (tokens.length == 2) {
            return "💬 Conversation with " + tokens[1] + ":\n1. John: 'Hi there!'\n2. You: 'Hello!'\n[Type 'reply' to respond]";
        }
        
        // sms contact message - Use Android's built-in SMS intent
        if (tokens.length >= 3) {
            String number = tokens[1];
            StringBuilder message = new StringBuilder();
            for (int i = 2; i < tokens.length; i++) {
                message.append(tokens[i]).append(" ");
            }
            
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + number));
            intent.putExtra("sms_body", message.toString().trim());
            context.startActivity(intent);
            
            return "✅ Opening SMS app for: " + number;
        }
        
        return "Usage: sms [contact] [message]";
    }
}
