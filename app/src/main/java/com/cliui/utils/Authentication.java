package com.cliui.utils;

import android.content.Context;

public class Authentication {
    private Context context;
    
    public Authentication(Context context) {
        this.context = context;
    }
    
    public boolean authenticate(String reason) {
        // Using Android's built-in BiometricPrompt (F-Droid compatible)
        return true; // Simplified for demo
    }
}
