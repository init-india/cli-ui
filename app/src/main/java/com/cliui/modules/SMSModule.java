package com.cliui.modules;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.telephony.SmsManager;
import com.cliui.utils.Authentication;
import com.cliui.utils.PermissionManager;  // ADD THIS
import java.text.SimpleDateFormat;
import java.util.*;

public class SMSModule implements CommandModule {
    private Context context;
    private PermissionManager permissionManager;  // ADD THIS
    private SMSState currentState = SMSState.LIST_VIEW;
    private List<SMSMessage> currentMessages = new ArrayList<>();
    private int currentPage = 0;
    private String selectedContact = null;
    private SMSMessage selectedMessage = null;
    private String draftMessage = null;
    private List<Integer> deleteSelection = new ArrayList<>();
    
    // SMS Broadcast Receiver as inner class
    private final SMSReceiver smsReceiver = new SMSReceiver();
    
    public SMSModule(Context context) {
        this.context = context;
        this.permissionManager = PermissionManager.getInstance(context);  // ADD THIS
        registerSMSReceiver();
        loadRecentMessages();
    }
    
    @Override
    public String execute(String[] tokens) {
        if (tokens.length == 0) return getUsage();
        
        String command = tokens[0].toLowerCase();
        
        // Check SMS permissions first
        if (!permissionManager.canExecute("sms")) {
            return "📱 SMS permission required\nType 'sms' again to grant permission";
        }
        
        // Handle different SMS states
        switch (currentState) {
            case LIST_VIEW:
                return handleListView(tokens);
            case MESSAGE_VIEW:
                return handleMessageView(tokens);
            case REPLY_MODE:
                return handleReplyMode(tokens);
            case DELETE_MODE:
                return handleDeleteMode(tokens);
            case CONTACT_THREAD:
                return handleContactThread(tokens);
            default:
                return handleListView(tokens);
        }
    }
    
    private String handleListView(String[] tokens) {
        String command = tokens[0].toLowerCase();
        
        switch (command) {
            case "sms":
                if (tokens.length == 1) {
                    return showMessageList(0); // First 20 messages
                } else if (tokens.length == 2) {
                    String param = tokens[1];
                    if (param.equals("more")) {
                        return showMessageList(currentPage + 1);
                    } else if (param.equals("all")) {
                        return showAllMessages();
                    } else if (param.equals("del")) {
                        return enterDeleteMode();
                    } else {
                        // sms <contact> - Show conversation thread
                        selectedContact = param;
                        return showContactThread(param);
                    }
                } else if (tokens.length >= 3) {
                    // sms <contact> <message> - Direct send
                    return sendDirectMessage(tokens[1], tokens);
                }
                break;
                
            case "exit":
                resetState();
                return "Returning to main CLI...";
        }
        
        return "Unknown command. Type 'sms' for message list.";
    }
    
    private String handleMessageView(String[] tokens) {
        String command = tokens[0].toLowerCase();
        
        switch (command) {
            case "reply":
                return enterReplyMode();
            case "exit":
                currentState = SMSState.LIST_VIEW;
                return showMessageList(currentPage);
            default:
                return "💬 Message: " + selectedMessage.body + 
                       "\n\nType: reply (respond) | exit (back to list)";
        }
    }
    
    private String handleReplyMode(String[] tokens) {
        String command = tokens[0].toLowerCase();
        
        switch (command) {
            case "send":
                return sendReply();
            case "exit":
                currentState = SMSState.MESSAGE_VIEW;
                return "💬 Draft saved.\n" + getMessageViewSuggestions();
            default:
                // Treat as message content
                draftMessage = String.join(" ", tokens);
                return "📝 Draft: " + draftMessage + 
                       "\n\nType: send (send message) | exit (cancel)";
        }
    }
    
    private String handleDeleteMode(String[] tokens) {
        String command = tokens[0].toLowerCase();
        
        switch (command) {
            case "confirm":
                return deleteSelectedMessages();
            case "cancel":
                currentState = SMSState.LIST_VIEW;
                return "Delete cancelled.";
            default:
                // Parse message IDs (1,3,5 etc)
                try {
                    String[] ids = command.split(",");
                    for (String id : ids) {
                        deleteSelection.add(Integer.parseInt(id.trim()));
                    }
                    return "Selected messages: " + deleteSelection + 
                           "\nType: confirm (delete) | cancel (abort)";
                } catch (NumberFormatException e) {
                    return "Invalid ID format. Use: 1,3,5 etc\nType: confirm | cancel";
                }
        }
    }
    
    private String handleContactThread(String[] tokens) {
        String command = tokens[0].toLowerCase();
        
        switch (command) {
            case "reply":
                return enterReplyMode();
            case "exit":
                currentState = SMSState.LIST_VIEW;
                return showMessageList(currentPage);
            default:
                try {
                    int messageId = Integer.parseInt(command);
                    return openMessageFromThread(messageId);
                } catch (NumberFormatException e) {
                    return "💬 Conversation with " + selectedContact + 
                           "\n\nType [id] (open message) | reply (new message) | exit (back)";
                }
        }
    }
    
    // Core SMS functionality
    private String showMessageList(int page) {
        currentPage = page;
        loadRecentMessages();
        
        if (currentMessages.isEmpty()) {
            return "📱 No messages found.";
        }
        
        StringBuilder list = new StringBuilder();
        list.append("💬 Messages (Page ").append(page + 1).append("):\n");
        
        int start = page * 20;
        int end = Math.min(start + 20, currentMessages.size());
        
        for (int i = start; i < end; i++) {
            SMSMessage msg = currentMessages.get(i);
            list.append(i + 1).append(". ").append(msg.getPreview()).append("\n");
        }
        
        list.append("\n💡 Type [id] (open) | sms more (next) | sms all (all) | sms del (delete)");
        
        return list.toString();
    }
    
    private String openMessage(int messageId) {
        if (messageId < 1 || messageId > currentMessages.size()) {
            return "❌ Invalid message ID";
        }
        
        // Authenticate before showing full message
        if (!Authentication.authenticate()) {
            return "🔒 Authentication failed";
        }
        
        selectedMessage = currentMessages.get(messageId - 1);
        currentState = SMSState.MESSAGE_VIEW;
        
        return "💬 From: " + selectedMessage.sender + 
               "\nTime: " + selectedMessage.timestamp +
               "\nMessage: " + selectedMessage.body +
               "\n\nType: reply (respond) | exit (back to list)";
    }
    
    private String sendReply() {
        if (draftMessage == null || draftMessage.isEmpty()) {
            return "❌ No message to send";
        }
        
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(selectedMessage.sender, null, draftMessage, null, null);
            
            // Update message status to replied
            selectedMessage.status = "replied";
            
            draftMessage = null;
            currentState = SMSState.MESSAGE_VIEW;
            
            return "✅ Message sent!\n" + getMessageViewSuggestions();
            
        } catch (Exception e) {
            return "❌ Send failed: " + e.getMessage();
        }
    }
    
    // SMS Broadcast Receiver as inner class
    private class SMSReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
                // Extract SMS data
                String sender = "Unknown";
                String message = "";
                
                // Play beep sound
                playNotificationSound();
                
                // Update CLI if active
                if (isCLIActive()) {
                    showCLINotification(sender, message);
                }
                
                // Reload messages
                loadRecentMessages();
            }
        }
        
        private void playNotificationSound() {
            // Implement beep sound
        }
        
        private void showCLINotification(String sender, String message) {
            // Show notification in CLI interface
            String preview = message.length() > 30 ? message.substring(0, 30) + "..." : message;
            String notification = "📱 New SMS from " + sender + ": " + preview;
            // Send to MainActivity to display
        }
    }
    
    // Utility methods
    private void registerSMSReceiver() {
        IntentFilter filter = new IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
        context.registerReceiver(smsReceiver, filter);
    }
    
    private void loadRecentMessages() {
        // Load real SMS from Android's SMS content provider
        currentMessages.clear();
        // Implementation for real SMS database access
    }
    
    private void resetState() {
        currentState = SMSState.LIST_VIEW;
        selectedContact = null;
        selectedMessage = null;
        draftMessage = null;
        deleteSelection.clear();
    }
    
    private String getUsage() {
        return "📱 SMS Commands:\n" +
               "• sms - Show recent messages\n" + 
               "• sms more - Show next 20 messages\n" +
               "• sms all - Show all messages\n" +
               "• sms <contact> - Show conversation\n" +
               "• sms <contact> <message> - Send message\n" +
               "• sms del - Delete messages\n" +
               "• exit - Return to main CLI";
    }
    
    // SMS State Enum
    enum SMSState {
        LIST_VIEW, MESSAGE_VIEW, REPLY_MODE, DELETE_MODE, CONTACT_THREAD
    }
    
    // SMS Message Class
    class SMSMessage {
        String id;
        String sender;
        String body;
        String timestamp;
        String status; // read, unread, replied
        
        String getPreview() {
            String preview = body.length() > 25 ? body.substring(0, 25) + "..." : body;
            return sender + " - '" + preview + "' - " + timestamp + " - " + status;
        }
    }
    
    // Placeholder methods for incomplete functionality
    private String showAllMessages() { return "All messages view - Implement later"; }
    private String showContactThread(String contact) { return "Thread view for " + contact; }
    private String sendDirectMessage(String contact, String[] tokens) { return "Direct send to " + contact; }
    private String enterDeleteMode() { return "Delete mode - Implement later"; }
    private String deleteSelectedMessages() { return "Delete confirmed - Implement later"; }
    private String enterReplyMode() { return "Reply mode entered"; }
    private String getMessageViewSuggestions() { return "Message view suggestions"; }
    private String openMessageFromThread(int messageId) { return "Open message " + messageId; }
    private boolean isCLIActive() { return true; }
}
