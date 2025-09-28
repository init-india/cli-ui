package com.cliui.modules;

import android.content.Context;
import android.content.Intent;

public class EmailModule implements CommandModule {
    private Context context;
    
    public EmailModule(Context context) {
        this.context = context;
    }
    
    @Override
    public String execute(String[] tokens) {
        if (tokens.length == 1) {
            return "📧 Recent Emails:\n1. Amazon - Order shipped\n2. GitHub - Security alert\n3. Team - Project update\n\n" +
                   "Use: mail open [1-3] to read\nOr: mail compose to write new email";
        }
        
        if (tokens.length == 2) {
            if (tokens[1].equals("compose")) {
                // Open email compose intent
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("message/rfc822");
                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{""});
                intent.putExtra(Intent.EXTRA_SUBJECT, "");
                intent.putExtra(Intent.EXTRA_TEXT, "");
                context.startActivity(Intent.createChooser(intent, "Send email"));
                return "📧 Opening email composer...";
            }
            return "📧 Emails from " + tokens[1] + ":\n1. Order confirmation\n2. Shipping update";
        }
        
        if (tokens.length == 3 && tokens[1].equals("open")) {
            try {
                int emailId = Integer.parseInt(tokens[2]);
                if (emailId >= 1 && emailId <= 3) {
                    return "📧 Email #" + emailId + " content:\nThis is a sample email content.\n[Use your email app for full access]";
                }
            } catch (NumberFormatException e) {
                return "❌ Invalid email ID";
            }
        }
        
        return "Usage: mail | mail compose | mail open [1-3]";
    }
}
