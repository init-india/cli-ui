package com.cliui.utils;

public class TUIFormatter {
    public static String formatSuccess(String text) {
        return "✅ " + text;
    }
    
    public static String formatError(String text) {
        return "❌ " + text;
    }
    
    public static String formatInfo(String text) {
        return "💡 " + text;
    }
}
