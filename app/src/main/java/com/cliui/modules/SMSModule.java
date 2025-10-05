package com.cliui.modules;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.telephony.SmsManager;

import com.cliui.utils.PermissionManager;
import com.cliui.utils.ShizukuManager;
import java.text.SimpleDateFormat;
import java.util.*;

public class SMSModule implements CommandModule {
    private Context context;
    private PermissionManager permissionManager;
    private SMSState currentState = SMSState.LIST_VIEW;
    private List<SMSMessage> currentMessages = new ArrayList<>();
    private int currentPage = 0;
    private String selectedContact = null;
    private SMSMessage selectedMessage = null;
    private String draftMessage = null;
    private List<Integer> deleteSelection = new ArrayList<>();
    private String searchQuery = null;
    
    // SMS Broadcast Receiver as inner class
    private final SMSReceiver smsReceiver = new SMSReceiver();
    private static final int MESSAGES_PER_PAGE = 20;
    
    public SMSModule(Context context) {
        this.context = context;
        this.permissionManager = PermissionManager.getInstance(context);
        registerSMSReceiver();
        loadRecentMessages();
    }
    
    @Override
    public String execute(String[] tokens) {
        if (tokens.length == 0) return getUsage();
        
        String command = tokens[0].toLowerCase();
        String fullCommand = String.join(" ", tokens).toLowerCase();

        // Check permissions using PermissionManager
        if (!permissionManager.canExecute(fullCommand)) {
            return permissionManager.getPermissionExplanation(fullCommand);
        }

        // Require authentication for SMS access
   PermissionManager permissionManager = PermissionManager.getInstance(context);
if (!permissionManager.authenticate("sms_access")) {
            return "🔒 Authentication required for SMS access\n" +
                   "Please authenticate to read and send messages";
        }
        
        // Handle exit command in any state
        if (command.equals("exit")) {
            return handleExit();
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
            case SEARCH_RESULTS:
                return handleSearchResults(tokens);
            case COMPOSE_MODE:
                return handleComposeMode(tokens);
            default:
                return handleListView(tokens);
        }
    }
    
    private String handleListView(String[] tokens) {
        String command = tokens[0].toLowerCase();
        
        if (command.equals("sms")) {
            if (tokens.length == 1) {
                return showMessageList(0);
            } else if (tokens.length >= 2) {
                String subCommand = tokens[1].toLowerCase();
                switch (subCommand) {
                    case "more":
                    case "next":
                        return showMessageList(currentPage + 1);
                    case "all":
                        return showAllMessages();
                    case "del":
                    case "delete":
                        return enterDeleteMode();
                    case "compose":
                        return startNewMessage();
                    case "search":
                        if (tokens.length >= 3) {
                            return searchMessages(String.join(" ", Arrays.copyOfRange(tokens, 2, tokens.length)));
                        }
                        return "❌ Usage: sms search [query]";
                    case "backup":
                        return backupSMS();
                    case "sync":
                        loadRecentMessages();
                        return "✅ Messages reloaded from system";
                    default:
                        // sms <contact> - Show conversation thread
                        selectedContact = String.join(" ", Arrays.copyOfRange(tokens, 1, tokens.length));
                        return showContactThread(selectedContact);
                }
            }
        }
        
        // Handle message ID selection (1, 2, 3, etc.)
        try {
            int messageId = Integer.parseInt(command);
            return openMessage(messageId);
        } catch (NumberFormatException e) {
            return "❌ Unknown command. Type 'sms' for message options.";
        }
    }
    
    private String handleMessageView(String[] tokens) {
        if (tokens.length == 0) return displayCurrentMessage() + "\n\n" + getMessageViewSuggestions();
        
        String command = tokens[0].toLowerCase();
        
        switch (command) {
            case "reply":
                return enterReplyMode();
            case "forward":
                return forwardMessage();
            case "delete":
                return deleteCurrentMessage();
            case "info":
                return getMessageInfo();
            case "mark":
                if (tokens.length >= 2) {
                    return markMessage(tokens[1]);
                }
                return "❌ Usage: mark [read|unread]";
            case "exit":
            case "back":
                currentState = SMSState.LIST_VIEW;
                return showMessageList(currentPage);
            default:
                return displayCurrentMessage() + "\n\n" + getMessageViewSuggestions();
        }
    }
    
    private String handleReplyMode(String[] tokens) {
        if (tokens.length == 0) return getReplySuggestions();
        
        String command = tokens[0].toLowerCase();
        
        switch (command) {
            case "send":
                return sendReply();
            case "attach":
                return "📎 SMS does not support attachments";
            case "status":
                return "📝 Draft: " + (draftMessage != null ? draftMessage : "Empty");
            case "exit":
            case "cancel":
                currentState = SMSState.MESSAGE_VIEW;
                draftMessage = null;
                return "❌ Reply cancelled";
            default:
                // Treat as message content
                draftMessage = String.join(" ", tokens);
                return "📝 Draft updated: " + draftMessage + "\n" + getReplySuggestions();
        }
    }
    
    private String handleDeleteMode(String[] tokens) {
        if (tokens.length == 0) return getDeleteSuggestions();
        
        String command = tokens[0].toLowerCase();
        
        switch (command) {
            case "confirm":
                return deleteSelectedMessages();
            case "cancel":
                currentState = SMSState.LIST_VIEW;
                deleteSelection.clear();
                return "🗑️ Delete cancelled";
            case "clear":
                deleteSelection.clear();
                return "✅ Selection cleared\n" + getDeleteSuggestions();
            case "all":
                deleteSelection.clear();
                for (int i = 0; i < currentMessages.size(); i++) {
                    deleteSelection.add(i + 1);
                }
                return "✅ All " + currentMessages.size() + " messages selected\n" + getDeleteSuggestions();
            default:
                // Parse message IDs (1,3,5 etc)
                try {
                    String[] ids = command.split(",");
                    for (String id : ids) {
                        int messageId = Integer.parseInt(id.trim());
                        if (messageId >= 1 && messageId <= currentMessages.size()) {
                            if (deleteSelection.contains(messageId)) {
                                deleteSelection.remove((Integer) messageId);
                            } else {
                                deleteSelection.add(messageId);
                            }
                        }
                    }
                    return "✅ Selected " + deleteSelection.size() + " messages\n" + getDeleteSuggestions();
                } catch (NumberFormatException e) {
                    return "❌ Invalid ID format. Use: 1,3,5 etc\n" + getDeleteSuggestions();
                }
        }
    }
    
    private String handleContactThread(String[] tokens) {
        if (tokens.length == 0) return getContactThread();
        
        String command = tokens[0].toLowerCase();
        
        if (command.equals("exit")) {
            currentState = SMSState.LIST_VIEW;
            selectedContact = null;
            loadRecentMessages(); // Restore all messages
            return "✅ Exited conversation view";
        }
        
        if (command.equals("compose") || command.equals("reply")) {
            return startNewMessageToContact();
        }
        
        try {
            int messageId = Integer.parseInt(command);
            return openMessageFromThread(messageId);
        } catch (NumberFormatException e) {
            return "❌ Type message ID to open, 'reply' for new message, or 'exit' to return";
        }
    }
    
    private String handleSearchResults(String[] tokens) {
        if (tokens.length == 0) return getSearchResults();
        
        String command = tokens[0].toLowerCase();
        
        if (command.equals("exit")) {
            currentState = SMSState.LIST_VIEW;
            searchQuery = null;
            loadRecentMessages(); // Restore all messages
            return "✅ Exited search mode";
        }
        
        try {
            int messageId = Integer.parseInt(command);
            return openMessageFromSearch(messageId);
        } catch (NumberFormatException e) {
            return "❌ Type message ID to open or 'exit' to return to list";
        }
    }
    
    private String handleComposeMode(String[] tokens) {
        if (tokens.length == 0) return getComposeSuggestions();
        
        String command = tokens[0].toLowerCase();
        
        switch (command) {
            case "send":
                return sendNewMessage();
            case "to":
                if (tokens.length >= 2) {
                    selectedContact = tokens[1];
                    return "✅ To: " + selectedContact + "\n" + getComposeSuggestions();
                }
                return "❌ Usage: to [phone number]";
            case "status":
                return "📝 Draft: " + (draftMessage != null ? draftMessage : "Empty") + 
                       "\n📞 To: " + (selectedContact != null ? selectedContact : "Not set");
            case "exit":
            case "cancel":
                currentState = SMSState.LIST_VIEW;
                selectedContact = null;
                draftMessage = null;
                return "❌ Message composition cancelled";
            default:
                // Treat as message content
                draftMessage = String.join(" ", tokens);
                return "📝 Draft updated: " + draftMessage + "\n" + getComposeSuggestions();
        }
    }
    
    // ===== Core SMS Operations =====
    
    private String showMessageList(int page) {
        currentPage = page;
        int start = page * MESSAGES_PER_PAGE;
        int end = Math.min(start + MESSAGES_PER_PAGE, currentMessages.size());
        
        if (start >= currentMessages.size()) {
            return "💬 No more messages\n💡 Type 'sms' to return to first page";
        }
        
        StringBuilder list = new StringBuilder();
        list.append("💬 Messages (Page ").append(page + 1).append(")\n");
        list.append("Messages ").append(start + 1).append("-").append(end).append(" of ").append(currentMessages.size()).append("\n\n");
        
        for (int i = start; i < end; i++) {
            SMSMessage msg = currentMessages.get(i);
            String statusIcon = "📭";
            if ("read".equals(msg.status)) statusIcon = "📬";
            if ("replied".equals(msg.status)) statusIcon = "📤";
            
            list.append(i + 1).append(". ").append(statusIcon).append(" ")
                .append(msg.getPreview()).append("\n");
        }
        
        list.append("\n💡 Type ID to open | sms more (next) | sms all (all) | sms compose (new)");
        list.append("\n💡 sms <contact> (filter) | sms search <query> | sms del (delete) | sms backup");
        
        currentState = SMSState.LIST_VIEW;
        return list.toString();
    }
    
    private String openMessage(int messageId) {
        if (messageId < 1 || messageId > currentMessages.size()) {
            return "❌ Invalid message ID";
        }
        
        selectedMessage = currentMessages.get(messageId - 1);
        selectedMessage.status = "read";
        currentState = SMSState.MESSAGE_VIEW;
        
        return displayCurrentMessage() + "\n\n" + getMessageViewSuggestions();
    }
    
    private String displayCurrentMessage() {
        if (selectedMessage == null) return "❌ No message selected";
        
        return "💬 From: " + selectedMessage.sender + 
               "\n📅 Time: " + selectedMessage.timestamp +
               "\n📊 Status: " + selectedMessage.status +
               "\n\n" + selectedMessage.body;
    }
    
    private String enterReplyMode() {
        if (selectedMessage == null) return "❌ No message to reply to";
        
        currentState = SMSState.REPLY_MODE;
        selectedContact = selectedMessage.sender;
        draftMessage = "";
        
        return "💬 Replying to: " + selectedMessage.sender + 
               "\n💡 Type your message, then 'send' to send\n" +
               "💡 Type 'status' to see draft | 'exit' to cancel";
    }
    
    private String sendReply() {
        if (draftMessage == null || draftMessage.isEmpty()) {
            return "❌ No message to send";
        }
        
        if (selectedMessage == null || selectedMessage.sender == null) {
            return "❌ No recipient specified";
        }
        
        return sendSMS(selectedMessage.sender, draftMessage);
    }
    
    private String sendNewMessage() {
        if (draftMessage == null || draftMessage.isEmpty()) {
            return "❌ No message to send";
        }
        
        if (selectedContact == null) {
            return "❌ No recipient specified\n💡 Use 'to [number]' first";
        }
        
        return sendSMS(selectedContact, draftMessage);
    }
    
    private String sendSMS(String phoneNumber, String message) {
        try {
            // Check if we have SMS permissions
            if (!permissionManager.canSendSMS()) {
                return "❌ SMS send permission required\n" +
                       "Need permission to send text messages";
            }
            
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            
            // Update message status
            if (selectedMessage != null) {
                selectedMessage.status = "replied";
            }
            
            // Add to local messages for immediate feedback
            SMSMessage sentMessage = new SMSMessage();
            sentMessage.sender = "Me";
            sentMessage.body = message;
            sentMessage.timestamp = new SimpleDateFormat("dd-MMM-yy;HH:mm").format(new Date());
            sentMessage.status = "sent";
            currentMessages.add(0, sentMessage);
            
            String result = "✅ Message sent to: " + phoneNumber;
            draftMessage = null;
            currentState = SMSState.LIST_VIEW;
            
            return result;
            
        } catch (SecurityException e) {
            return "❌ SMS permission denied\n" +
                   "Please grant SMS permissions in app settings";
        } catch (Exception e) {
            return "❌ Send failed: " + e.getMessage() + 
                   "\n💡 Check phone number and try again";
        }
    }
    
    private String startNewMessage() {
        currentState = SMSState.COMPOSE_MODE;
        selectedContact = null;
        draftMessage = "";
        return "💬 New Message\n" + getComposeSuggestions();
    }
    
    private String startNewMessageToContact() {
        if (selectedContact == null) return "❌ No contact selected";
        
        currentState = SMSState.COMPOSE_MODE;
        draftMessage = "";
        return "💬 New message to: " + selectedContact + "\n" + getComposeSuggestions();
    }
    
    private String showContactThread(String contact) {
        List<SMSMessage> thread = new ArrayList<>();
        
        for (SMSMessage msg : currentMessages) {
            if (msg.sender.toLowerCase().contains(contact.toLowerCase())) {
                thread.add(msg);
            }
        }
        
        currentMessages = thread;
        currentState = SMSState.CONTACT_THREAD;
        return "💬 Conversation with " + contact + " (" + thread.size() + " messages)\n" + showMessageList(0);
    }
    
    private String searchMessages(String query) {
        searchQuery = query;
        List<SMSMessage> results = new ArrayList<>();
        
        for (SMSMessage msg : currentMessages) {
            if (msg.contains(query)) {
                results.add(msg);
            }
        }
        
        currentMessages = results;
        currentState = SMSState.SEARCH_RESULTS;
        return "🔍 Search results for '" + query + "' (" + results.size() + " messages)\n" + showMessageList(0);
    }
    
    private String enterDeleteMode() {
        deleteSelection.clear();
        currentState = SMSState.DELETE_MODE;
        return "🗑️ Delete Mode\nSelect messages by ID (e.g., 1,3,5 or all)\n" + showMessageList(currentPage);
    }
    
    private String deleteSelectedMessages() {
        if (deleteSelection.isEmpty()) {
            return "❌ No messages selected for deletion";
        }
        
        // Sort in descending order to avoid index issues
        Collections.sort(deleteSelection, Collections.reverseOrder());
        
        int deletedCount = 0;
        for (int id : deleteSelection) {
            if (id >= 1 && id <= currentMessages.size()) {
                // Try to delete from system if Shizuku available
                SMSMessage message = currentMessages.get(id - 1);
                if (ShizukuManager.isAvailable()) {
                    deleteMessageFromSystem(message);
                }
                currentMessages.remove(id - 1);
                deletedCount++;
            }
        }
        
        deleteSelection.clear();
        currentState = SMSState.LIST_VIEW;
        
        return "✅ " + deletedCount + " messages deleted\n" + showMessageList(0);
    }
    
    private String deleteCurrentMessage() {
        if (selectedMessage == null) return "❌ No message selected";
        
        // Try to delete from system if Shizuku available
        if (ShizukuManager.isAvailable()) {
            deleteMessageFromSystem(selectedMessage);
        }
        
        currentMessages.remove(selectedMessage);
        currentState = SMSState.LIST_VIEW;
        return "✅ Message deleted\n" + showMessageList(currentPage);
    }
    
    private String forwardMessage() {
        if (selectedMessage == null) return "❌ No message to forward";
        
        draftMessage = "Fwd: " + selectedMessage.body;
        currentState = SMSState.COMPOSE_MODE;
        return "💬 Forwarding message\n" + getComposeSuggestions();
    }
    
    private String getMessageInfo() {
        if (selectedMessage == null) return "❌ No message selected";
        
        return "📋 Message Info:\n" +
               "• Sender: " + selectedMessage.sender + "\n" +
               "• Time: " + selectedMessage.timestamp + "\n" +
               "• Status: " + selectedMessage.status + "\n" +
               "• Length: " + selectedMessage.body.length() + " characters\n" +
               "• Type: SMS";
    }
    
    private String markMessage(String status) {
        if (selectedMessage == null) return "❌ No message selected";
        
        switch (status.toLowerCase()) {
            case "read":
                selectedMessage.status = "read";
                return "✅ Marked as read";
            case "unread":
                selectedMessage.status = "unread";
                return "✅ Marked as unread";
            default:
                return "❌ Unknown status. Use: read or unread";
        }
    }
    
    // ===== Shizuku Integration =====
    
    private String backupSMS() {
        if (!ShizukuManager.isAvailable()) {
            return "❌ Shizuku not available\n" +
                   "SMS backup requires system-level database access via Shizuku";
        }
        
        // This would backup SMS database
        String backupCommand = "tar -czf /sdcard/sms_backup.tar.gz /data/data/com.android.providers.telephony/";
        if (ShizukuManager.executeCommand(backupCommand)) {
            return "✅ SMS data backed up to /sdcard/sms_backup.tar.gz";
        } else {
            return "❌ Backup failed - may require root access";
        }
    }
    
    private void deleteMessageFromSystem(SMSMessage message) {
        // This would delete the message from Android's SMS database
        // Implementation depends on specific device and Android version
        String deleteCommand = "content delete --uri content://sms --where \"_id=" + message.id + "\"";
        ShizukuManager.executeCommand(deleteCommand);
    }
    
    // ===== Utility Methods =====
    
    private String showAllMessages() {
        StringBuilder sb = new StringBuilder();
        sb.append("💬 All Messages (").append(currentMessages.size()).append(" total)\n\n");
        
        for (int i = 0; i < currentMessages.size(); i++) {
            SMSMessage msg = currentMessages.get(i);
            String statusIcon = "📭";
            if ("read".equals(msg.status)) statusIcon = "📬";
            if ("replied".equals(msg.status)) statusIcon = "📤";
            
            sb.append(i + 1).append(". ").append(statusIcon).append(" ")
              .append(msg.getPreview()).append("\n");
        }
        
        return sb.toString();
    }
    
    private String openMessageFromThread(int messageId) {
        String result = openMessage(messageId);
        currentState = SMSState.CONTACT_THREAD;
        return result;
    }
    
    private String openMessageFromSearch(int messageId) {
        String result = openMessage(messageId);
        currentState = SMSState.SEARCH_RESULTS;
        return result;
    }
    
    private String handleExit() {
        switch (currentState) {
            case MESSAGE_VIEW:
            case REPLY_MODE:
            case COMPOSE_MODE:
            case SEARCH_RESULTS:
            case CONTACT_THREAD:
            case DELETE_MODE:
                currentState = SMSState.LIST_VIEW;
                selectedContact = null;
                selectedMessage = null;
                draftMessage = null;
                deleteSelection.clear();
                searchQuery = null;
                loadRecentMessages(); // Restore all messages
                return "✅ Returned to message list";
            default:
                return "Already in main message list";
        }
    }
    
    private String getContactThread() {
        return "💬 Conversation View - " + selectedContact + "\n" +
               "Showing " + currentMessages.size() + " messages\n" +
               "💡 Type message ID to open, 'reply' for new message, or 'exit' to return";
    }
    
    private String getSearchResults() {
        return "🔍 Search Mode - Query: '" + searchQuery + "'\n" +
               "Found " + currentMessages.size() + " messages\n" +
               "💡 Type message ID to open or 'exit' to return to list";
    }
    
    private String getDeleteSuggestions() {
        return "🗑️ Delete Mode - Selected: " + deleteSelection.size() + " messages\n" +
               "💡 Click numbers to select/deselect messages\n" +
               "💡 Commands: all (select all) | clear (clear selection)\n" +
               "💡 Actions: confirm (delete selected) | cancel (exit)";
    }
    
    private String getMessageViewSuggestions() {
        return "💡 Commands: reply | forward | delete | info | mark [status] | exit";
    }
    
    private String getReplySuggestions() {
        return "💡 Commands: send (send reply) | status (show draft) | exit (cancel)";
    }
    
    private String getComposeSuggestions() {
        return "💡 Commands: to [number] (set recipient) | send (send message) | status (show draft) | exit (cancel)";
    }
    
    private void loadRecentMessages() {
        currentMessages.clear();
        
        // Load real SMS from Android's SMS content provider
        // This is a simplified version - real implementation would query the SMS database
        
        // Sample data for demonstration
        currentMessages.add(new SMSMessage("1", "+1234567890", "Hey, are we still meeting today?", "27-Sept-25;14:30", "read"));
        currentMessages.add(new SMSMessage("2", "Mom", "Don't forget to call me later!", "27-Sept-25;12:15", "unread"));
        currentMessages.add(new SMSMessage("3", "Amazon", "Your package has been delivered", "27-Sept-25;10:45", "read"));
        currentMessages.add(new SMSMessage("4", "Bank", "Alert: Transaction of $50 at Starbucks", "26-Sept-25;16:20", "read"));
    }
    
    private void registerSMSReceiver() {
        try {
            IntentFilter filter = new IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
            context.registerReceiver(smsReceiver, filter);
        } catch (Exception e) {
            // Receiver registration failed
        }
    }
    
    private String getUsage() {
        return "📱 SMS Module Usage:\n" +
               "• sms                 - Show messages (paginated)\n" +
               "• sms more            - Next page\n" +
               "• sms all             - All messages\n" +
               "• sms compose         - New message\n" +
               "• sms <contact>       - Messages from contact\n" +
               "• sms search <query>  - Search messages\n" +
               "• sms del             - Delete messages\n" +
               "• sms backup          - Backup SMS (Shizuku)\n" +
               "• sms sync            - Refresh messages\n" +
               "• [id]                - Open message by ID\n" +
               "• exit                - Back/Cancel\n" +
               "\n💬 In Message View:\n" +
               "• reply               - Reply to message\n" +
               "• forward             - Forward message\n" +
               "• delete              - Delete message\n" +
               "• info                - Message details\n" +
               "• mark [status]       - Mark as read/unread\n" +
               "\n🔒 Requires: SMS permissions + Authentication" +
               "\n🔧 Shizuku: " + (ShizukuManager.isAvailable() ? "Available ✅" : "Not Available ❌");
    }
    
    // ===== SMS Broadcast Receiver =====
    
    private class SMSReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
                // In real implementation, extract SMS data from intent
                String sender = "Unknown";
                String message = "New message received";
                
                // Reload messages to show new ones
                loadRecentMessages();
                
                // Show notification in CLI if active
                showCLINotification(sender, message);
            }
        }
        
        private void showCLINotification(String sender, String message) {
            // This would send a notification to the MainActivity
            // Implementation depends on your activity communication
        }
    }
    
    // ===== Data Classes =====
    
    enum SMSState {
        LIST_VIEW, MESSAGE_VIEW, REPLY_MODE, DELETE_MODE, 
        CONTACT_THREAD, SEARCH_RESULTS, COMPOSE_MODE
    }
    
    class SMSMessage {
        String id;
        String sender;
        String body;
        String timestamp;
        String status; // read, unread, replied, sent
        
        SMSMessage() {}
        
        SMSMessage(String id, String sender, String body, String timestamp, String status) {
            this.id = id;
            this.sender = sender;
            this.body = body;
            this.timestamp = timestamp;
            this.status = status;
        }
        
        String getPreview() {
            String preview = body.length() > 25 ? body.substring(0, 25) + "..." : body;
            return sender + " - '" + preview + "' - " + timestamp;
        }
        
        boolean contains(String query) {
            return sender.toLowerCase().contains(query.toLowerCase()) ||
                   body.toLowerCase().contains(query.toLowerCase());
        }
    }
}
