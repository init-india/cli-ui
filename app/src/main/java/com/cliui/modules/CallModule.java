package com.cliui.modules;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class CallModule implements CommandModule {
    private Context context;
    
    public CallModule(Context context) {
        this.context = context;
    }
    
    @Override
    public String execute(String[] tokens) {
        if (tokens.length == 1) {
            return "📞 Call History:\n1. John (missed) - 09:30\n2. Sarah (answered) - 09:25\n3. Boss (outgoing) - 09:15";
        }
        
        if (tokens.length == 2) {
            String number = tokens[1];
            // Use Android's built-in dialer intent
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + number));
            context.startActivity(intent);
            return "📞 Opening dialer for: " + number;
        }
        
        return "Usage: call [contact]";
    }
}
