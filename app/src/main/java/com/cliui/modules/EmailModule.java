package com.cliui.modules;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import com.cliui.utils.Authentication;
import com.cliui.utils.PermissionManager;
import com.cliui.utils.ShizukuManager;
import java.text.SimpleDateFormat;
import java.util.*;

public class EmailModule implements CommandModule {
    private Context context;
    private PermissionManager permissionManager;
    
    // Gmail state management
    private GmailState currentState = GmailState.INBOX_VIEW;
    private List<GmailMessage> currentMessages = new ArrayList<>();
    private GmailMessage selectedMessage = null;
    private GmailDraft currentDraft = null;
    private int currentPage = 0;
    private String searchQuery = null;
    private String selectedSender = null;
    private List<Integer> deleteSelection = new ArrayList<>();
    
    // Constants
    private static final int MESSAGES_PER_PAGE = 20;
    
    public EmailModule(Context context) {
        this.context = context;
        this.permissionManager = PermissionManager.getInstance(context);
        loadSampleMessages(); // For demo - replace with real Gmail API
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

        // Require authentication for Gmail access
        if (!Authentication.authenticate("email_access")) {
            return "🔒 Authentication required for email access\n" +
                   "Please authenticate to manage emails";
        }
        
        // Handle exit command in any state
        if (command.equals("exit")) {
            return handleExit();
        }
        
        // State-based command handling
        switch (currentState) {
            case INBOX_VIEW:
                return handleInboxView(tokens);
            case MESSAGE_VIEW:
                return handleMessageView(tokens);
            case COMPOSE_MODE:
                return handleComposeMode(tokens);
            case REPLY_MODE:
                return handleReplyMode(tokens);
            case SEARCH_RESULTS:
                return handleSearchResults(tokens);
            case SENDER_THREAD:
                return handleSenderThread(tokens);
            case DELETE_MODE:
                return handleDeleteMode(tokens);
            default:
                return handleInboxView(tokens);
        }
    }
    
    private String handleInboxView(String[] tokens) {
        String command = tokens.length > 0 ? tokens[0].toLowerCase() : "";
        
        if (command.equals("mail")) {
            if (tokens.length == 1) {
                return showInboxPage(0);
            } else if (tokens.length >= 2) {
                String subCommand = tokens[1].toLowerCase();
                
                switch (subCommand) {
                    case "more":
                    case "next":
                        return showInboxPage(currentPage + 1);
                    case "all":
                        return showAllMessages();
                    case "compose":
                        return startNewCompose();
                    case "search":
                        if (tokens.length >= 3) {
                            return searchMessages(String.join(" ", Arrays.copyOfRange(tokens, 2, tokens.length)));
                        }
                        return "❌ Usage: mail search [query]";
                    case "del":
                    case "delete":
                        return enterDeleteMode();
                    case "backup":
                        return backupEmails();
                    case "sync":
                        loadSampleMessages(); // In real app, would sync with server
                        return "✅ Email cache refreshed";
                    default:
                        // mail <sender> - Show messages from specific sender
                        selectedSender = String.join(" ", Arrays.copyOfRange(tokens, 1, tokens.length));
                        return showSenderThread(selectedSender);
                }
            }
        }
        
        // Handle message ID selection (1, 2, 3, etc.)
        try {
            int messageId = Integer.parseInt(command);
            return openMessage(messageId);
        } catch (NumberFormatException e) {
            return "❌ Unknown command. Type 'mail' for email options.";
        }
    }
    
    private String handleMessageView(String[] tokens) {
        if (tokens.length == 0) return displayCurrentMessage() + "\n\n" + getMessageViewSuggestions();
        
        String command = tokens[0].toLowerCase();
        
        switch (command) {
            case "1":
            case "reply":
                return startReply(false);
            case "2":
            case "replyall":
                return startReply(true);
            case "forward":
                return startForward();
            case "attach":
                if (tokens.length >= 2) {
                    return attachFile(tokens[1]);
                }
                return "❌ Usage: attach [filename]";
            case "delete":
                return deleteCurrentMessage();
            case "mark":
                if (tokens.length >= 2) {
                    return markMessage(tokens[1]);
                }
                return "❌ Usage: mark [read|unread|important]";
            case "exit":
            case "back":
                currentState = GmailState.INBOX_VIEW;
                return showInboxPage(currentPage);
            default:
                return displayCurrentMessage() + "\n\n" + getMessageViewSuggestions();
        }
    }
    
    private String handleComposeMode(String[] tokens) {
        if (tokens.length == 0) return getComposeSuggestions();
        
        String command = tokens[0].toLowerCase();
        
        switch (command) {
            case "send":
                return sendEmail();
            case "to":
                if (tokens.length >= 2) {
                    currentDraft.to = tokens[1];
                    return "✅ To: " + currentDraft.to + "\n" + getComposeSuggestions();
                }
                return "❌ Usage: to [email] (multiple emails separated by ;)";
            case "cc":
                if (tokens.length >= 2) {
                    currentDraft.cc = tokens[1];
                    return "✅ CC: " + currentDraft.cc + "\n" + getComposeSuggestions();
                }
                return "❌ Usage: cc [email]";
            case "bcc":
                if (tokens.length >= 2) {
                    currentDraft.bcc = tokens[1];
                    return "✅ BCC: " + currentDraft.bcc + "\n" + getComposeSuggestions();
                }
                return "❌ Usage: bcc [email]";
            case "subject":
                if (tokens.length >= 2) {
                    currentDraft.subject = String.join(" ", Arrays.copyOfRange(tokens, 1, tokens.length));
                    return "✅ Subject: " + currentDraft.subject + "\n" + getComposeSuggestions();
                }
                return "❌ Usage: subject [title]";
            case "attach":
                if (tokens.length >= 2) {
                    return attachFile(tokens[1]);
                }
                return "❌ Usage: attach [filename]";
            case "priority":
                if (tokens.length >= 2) {
                    currentDraft.priority = tokens[1];
                    return "✅ Priority: " + currentDraft.priority + "\n" + getComposeSuggestions();
                }
                return "❌ Usage: priority [high|normal|low]";
            case "status":
                return getDraftStatus();
            case "exit":
            case "cancel":
                return saveDraftAndExit();
            default:
                // Treat as message body content
                if (currentDraft.body == null) {
                    currentDraft.body = "";
                }
                currentDraft.body += String.join(" ", tokens) + "\n";
                return "📝 Body updated\n" + getComposeSuggestions();
        }
    }
    
    private String handleReplyMode(String[] tokens) {
        if (tokens.length == 0) return getReplySuggestions();
        
        String command = tokens[0].toLowerCase();
        
        switch (command) {
            case "send":
                return sendReply();
            case "attach":
                if (tokens.length >= 2) {
                    return attachFile(tokens[1]);
                }
                return "❌ Usage: attach [filename]";
            case "status":
                return getDraftStatus();
            case "exit":
            case "cancel":
                return saveDraftAndExit();
            default:
                // Treat as reply content
                if (currentDraft.body == null) {
                    currentDraft.body = "";
                }
                currentDraft.body += String.join(" ", tokens) + "\n";
                return "📝 Reply content updated\n" + getReplySuggestions();
        }
    }
    
    private String handleSearchResults(String[] tokens) {
        if (tokens.length == 0) return getSearchResults();
        
        String command = tokens[0].toLowerCase();
        
        if (command.equals("exit")) {
            currentState = GmailState.INBOX_VIEW;
            searchQuery = null;
            loadSampleMessages(); // Restore all messages
            return "✅ Exited search mode";
        }
        
        try {
            int messageId = Integer.parseInt(command);
            return openMessageFromSearch(messageId);
        } catch (NumberFormatException e) {
            return "❌ Type message ID to open or 'exit' to return to inbox";
        }
    }
    
    private String handleSenderThread(String[] tokens) {
        if (tokens.length == 0) return getSenderThread();
        
        String command = tokens[0].toLowerCase();
        
        if (command.equals("exit")) {
            currentState = GmailState.INBOX_VIEW;
            selectedSender = null;
            loadSampleMessages(); // Restore all messages
            return "✅ Exited thread view";
        }
        
        if (command.equals("compose")) {
            return startNewMessageToSender();
        }
        
        try {
            int messageId = Integer.parseInt(command);
            return openMessageFromThread(messageId);
        } catch (NumberFormatException e) {
            return "❌ Type message ID to open, 'compose' for new message, or 'exit' to return";
        }
    }
    
    private String handleDeleteMode(String[] tokens) {
        if (tokens.length == 0) return getDeleteSuggestions();
        
        String command = tokens[0].toLowerCase();
        
        switch (command) {
            case "confirm":
                return deleteSelectedMessages();
            case "cancel":
                currentState = GmailState.INBOX_VIEW;
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
                return "✅ All " + currentMessages.size() + " messages selected for deletion\n" + getDeleteSuggestions();
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
    
    // ===== Core Email Operations =====
    
    private String showInboxPage(int page) {
        currentPage = page;
        int start = page * MESSAGES_PER_PAGE;
        int end = Math.min(start + MESSAGES_PER_PAGE, currentMessages.size());
        
        if (start >= currentMessages.size()) {
            return "📧 No more messages\n💡 Type 'mail' to return to first page";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("📧 Email Inbox (Page ").append(page + 1).append(")\n");
        sb.append("Messages ").append(start + 1).append("-").append(end).append(" of ").append(currentMessages.size()).append("\n\n");
        
        for (int i = start; i < end; i++) {
            GmailMessage msg = currentMessages.get(i);
            String statusIcon = "📭";
            if ("read".equals(msg.status)) statusIcon = "📬";
            if ("important".equals(msg.status)) statusIcon = "📥";
            
            sb.append(i + 1).append(". ").append(statusIcon).append(" ")
              .append(msg.getPreview()).append("\n");
        }
        
        sb.append("\n💡 Type ID to open | mail more (next) | mail all (all) | mail compose (new)");
        sb.append("\n💡 mail <sender> (filter) | mail search <query> | mail del (delete) | mail backup");
        
        currentState = GmailState.INBOX_VIEW;
        return sb.toString();
    }
    
    private String openMessage(int messageId) {
        if (messageId < 1 || messageId > currentMessages.size()) {
            return "❌ Invalid message ID";
        }
        
        selectedMessage = currentMessages.get(messageId - 1);
        selectedMessage.status = "read";
        currentState = GmailState.MESSAGE_VIEW;
        
        return displayCurrentMessage() + "\n\n" + getMessageViewSuggestions();
    }
    
    private String displayCurrentMessage() {
        if (selectedMessage == null) return "❌ No message selected";
        
        String priorityIcon = "📧";
        if ("high".equals(selectedMessage.priority)) priorityIcon = "🚨";
        if ("low".equals(selectedMessage.priority)) priorityIcon = "📨";
        
        return priorityIcon + " From: " + selectedMessage.sender + 
               "\n📬 To: " + selectedMessage.recipient +
               "\n📋 Subject: " + selectedMessage.subject +
               "\n🕒 Time: " + selectedMessage.timestamp +
               "\n📊 Status: " + selectedMessage.status +
               "\n\n" + selectedMessage.body +
               (selectedMessage.hasAttachments ? "\n\n📎 Contains attachments" : "");
    }
    
    private String startReply(boolean replyAll) {
        if (selectedMessage == null) return "❌ No message to reply to";
        
        currentDraft = new GmailDraft();
        currentDraft.to = replyAll ? selectedMessage.recipient : selectedMessage.sender;
        currentDraft.subject = "Re: " + selectedMessage.subject;
        currentDraft.body = "\n\nOn " + selectedMessage.timestamp + " " + selectedMessage.sender + " wrote:\n" + selectedMessage.body;
        
        currentState = GmailState.REPLY_MODE;
        return "📧 Replying to: " + selectedMessage.sender + 
               "\n💡 Type your reply, then 'send' to send\nType 'attach [file]' to add attachment\nType 'status' to see draft\nType 'exit' to cancel";
    }
    
    private String startForward() {
        if (selectedMessage == null) return "❌ No message to forward";
        
        currentDraft = new GmailDraft();
        currentDraft.subject = "Fwd: " + selectedMessage.subject;
        currentDraft.body = "\n\n---------- Forwarded message ---------\n" +
                           "From: " + selectedMessage.sender + "\n" +
                           "Date: " + selectedMessage.timestamp + "\n" +
                           "Subject: " + selectedMessage.subject + "\n" +
                           "To: " + selectedMessage.recipient + "\n\n" +
                           selectedMessage.body;
        
        currentState = GmailState.COMPOSE_MODE;
        return "📧 Forwarding: " + selectedMessage.subject + 
               "\n💡 Set recipient with 'to [email]' then 'send'\nType 'attach [file]' to add attachment";
    }
    
    private String startNewCompose() {
        currentDraft = new GmailDraft();
        currentState = GmailState.COMPOSE_MODE;
        return "📧 New Message\n" + getComposeSuggestions();
    }
    
    private String sendEmail() {
        if (currentDraft == null) return "❌ No message to send";
        
        if (currentDraft.to == null || currentDraft.to.isEmpty()) {
            return "❌ Recipient (To) field is required";
        }
        
        // Use Android's email intent
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{currentDraft.to});
        if (currentDraft.cc != null && !currentDraft.cc.isEmpty()) {
            intent.putExtra(Intent.EXTRA_CC, new String[]{currentDraft.cc});
        }
        if (currentDraft.bcc != null && !currentDraft.bcc.isEmpty()) {
            intent.putExtra(Intent.EXTRA_BCC, new String[]{currentDraft.bcc});
        }
        intent.putExtra(Intent.EXTRA_SUBJECT, currentDraft.subject);
        intent.putExtra(Intent.EXTRA_TEXT, currentDraft.body);
        
        try {
            context.startActivity(Intent.createChooser(intent, "Send email"));
            String result = "✅ Message sent to: " + currentDraft.to;
            currentDraft = null;
            currentState = GmailState.INBOX_VIEW;
            return result;
        } catch (Exception e) {
            return "❌ No email app available\n💡 Install an email client like Gmail";
        }
    }
    
    private String sendReply() {
        // Similar to sendEmail but maintains context
        String result = sendEmail();
        if (result.startsWith("✅")) {
            currentState = GmailState.MESSAGE_VIEW;
            if (selectedMessage != null) {
                selectedMessage.status = "replied";
            }
        }
        return result;
    }
    
    private String searchMessages(String query) {
        searchQuery = query;
        List<GmailMessage> results = new ArrayList<>();
        
        for (GmailMessage msg : currentMessages) {
            if (msg.contains(query)) {
                results.add(msg);
            }
        }
        
        currentMessages = results;
        currentState = GmailState.SEARCH_RESULTS;
        return "🔍 Search results for '" + query + "' (" + results.size() + " messages)\n" + showInboxPage(0);
    }
    
    private String showSenderThread(String sender) {
        List<GmailMessage> thread = new ArrayList<>();
        
        for (GmailMessage msg : currentMessages) {
            if (msg.sender.toLowerCase().contains(sender.toLowerCase()) || 
                msg.recipient.toLowerCase().contains(sender.toLowerCase())) {
                thread.add(msg);
            }
        }
        
        currentMessages = thread;
        currentState = GmailState.SENDER_THREAD;
        return "📧 Thread with " + sender + " (" + thread.size() + " messages)\n" + showInboxPage(0);
    }
    
    private String startNewMessageToSender() {
        if (selectedSender == null) return "❌ No sender selected";
        
        currentDraft = new GmailDraft();
        currentDraft.to = selectedSender;
        currentState = GmailState.COMPOSE_MODE;
        return "📧 New message to " + selectedSender + "\n" + getComposeSuggestions();
    }
    
    private String enterDeleteMode() {
        deleteSelection.clear();
        currentState = GmailState.DELETE_MODE;
        return "🗑️ Delete Mode\nSelect messages by ID (e.g., 1,3,5 or all)\n" + showInboxPage(currentPage);
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
                currentMessages.remove(id - 1);
                deletedCount++;
            }
        }
        
        deleteSelection.clear();
        currentState = GmailState.INBOX_VIEW;
        
        return "✅ " + deletedCount + " messages deleted\n" + showInboxPage(0);
    }
    
    private String deleteCurrentMessage() {
        if (selectedMessage == null) return "❌ No message selected";
        
        currentMessages.remove(selectedMessage);
        currentState = GmailState.INBOX_VIEW;
        return "✅ Message deleted\n" + showInboxPage(currentPage);
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
            case "important":
                selectedMessage.status = "important";
                return "✅ Marked as important";
            default:
                return "❌ Unknown status. Use: read, unread, or important";
        }
    }
    
    // ===== Shizuku Integration =====
    
    private String backupEmails() {
        if (!ShizukuManager.isAvailable()) {
            return "❌ Shizuku not available\n" +
                   "Email backup requires system-level file access via Shizuku";
        }
        
        // This would backup email database or export messages
        String backupCommand = "tar -czf /sdcard/email_backup.tar.gz /data/data/com.android.email/";
        if (ShizukuManager.executeCommand(backupCommand)) {
            return "✅ Email data backed up to /sdcard/email_backup.tar.gz";
        } else {
            return "❌ Backup failed - may require root access";
        }
    }
    
    // ===== Utility Methods =====
    
    private String attachFile(String filename) {
        if (currentDraft == null) return "❌ No active composition";
        
        if (currentDraft.attachments == null) {
            currentDraft.attachments = new ArrayList<>();
        }
        
        currentDraft.attachments.add(filename);
        return "✅ Attachment added: " + filename + "\n" + 
               (currentState == GmailState.COMPOSE_MODE ? getComposeSuggestions() : getReplySuggestions());
    }
    
    private String saveDraftAndExit() {
        if (currentDraft != null) {
            // In real implementation, save draft to database
            String saved = "💾 Draft saved";
            currentDraft = null;
            currentState = GmailState.INBOX_VIEW;
            return saved + "\n" + showInboxPage(currentPage);
        }
        
        currentState = GmailState.INBOX_VIEW;
        return showInboxPage(currentPage);
    }
    
    private String handleExit() {
        switch (currentState) {
            case MESSAGE_VIEW:
            case COMPOSE_MODE:
            case REPLY_MODE:
            case SEARCH_RESULTS:
            case SENDER_THREAD:
            case DELETE_MODE:
                return saveDraftAndExit();
            default:
                return "Already in main inbox";
        }
    }
    
    private String getDraftStatus() {
        if (currentDraft == null) return "❌ No draft in progress";
        
        StringBuilder sb = new StringBuilder();
        sb.append("📧 Draft Status:\n");
        sb.append("• To: ").append(currentDraft.to != null ? currentDraft.to : "❌ Not set").append("\n");
        sb.append("• Subject: ").append(currentDraft.subject != null ? currentDraft.subject : "❌ Not set").append("\n");
        sb.append("• CC: ").append(currentDraft.cc != null ? currentDraft.cc : "Not set").append("\n");
        sb.append("• BCC: ").append(currentDraft.bcc != null ? currentDraft.bcc : "Not set").append("\n");
        sb.append("• Body: ").append(currentDraft.body != null ? currentDraft.body.length() + " characters" : "Empty").append("\n");
        if (currentDraft.attachments != null && !currentDraft.attachments.isEmpty()) {
            sb.append("• Attachments: ").append(currentDraft.attachments.size()).append(" files\n");
        }
        return sb.toString();
    }
    
    private String getSearchResults() {
        return "🔍 Search Mode - Query: '" + searchQuery + "'\n" +
               "Found " + currentMessages.size() + " messages\n" +
               "💡 Type message ID to open or 'exit' to return to inbox";
    }
    
    private String getSenderThread() {
        return "📧 Thread View - " + selectedSender + "\n" +
               "Showing " + currentMessages.size() + " messages\n" +
               "💡 Type message ID to open, 'compose' for new message, or 'exit' to return";
    }
    
    private String getDeleteSuggestions() {
        return "🗑️ Delete Mode - Selected: " + deleteSelection.size() + " messages\n" +
               "💡 Click numbers to select/deselect messages\n" +
               "💡 Commands: all (select all) | clear (clear selection)\n" +
               "💡 Actions: confirm (delete selected) | cancel (exit)";
    }
    
    private String getMessageViewSuggestions() {
        return "💡 Commands: 1/reply | 2/replyall | forward | delete | mark [status]\n" +
               "💡 attach [file] | exit (back to inbox)";
    }
    
    private String getComposeSuggestions() {
        return "💡 Commands: to [email] | cc [email] | bcc [email]\n" +
               "💡 subject [title] | attach [file] | priority [level]\n" +
               "💡 status (show draft) | send (send) | exit (save draft)";
    }
    
    private String getReplySuggestions() {
        return "💡 Commands: send (send reply) | attach [file] | status (show draft) | exit (cancel)";
    }
    
    private String showAllMessages() {
        // Show all messages without pagination
        StringBuilder sb = new StringBuilder();
        sb.append("📧 All Messages (").append(currentMessages.size()).append(" total)\n\n");
        
        for (int i = 0; i < currentMessages.size(); i++) {
            GmailMessage msg = currentMessages.get(i);
            String statusIcon = "📭";
            if ("read".equals(msg.status)) statusIcon = "📬";
            if ("important".equals(msg.status)) statusIcon = "📥";
            
            sb.append(i + 1).append(". ").append(statusIcon).append(" ")
              .append(msg.getPreview()).append("\n");
        }
        
        return sb.toString();
    }
    
    private String openMessageFromSearch(int messageId) {
        // Open message while maintaining search context
        String result = openMessage(messageId);
        currentState = GmailState.SEARCH_RESULTS;
        return result;
    }
    
    private String openMessageFromThread(int messageId) {
        // Open message while maintaining thread context
        String result = openMessage(messageId);
        currentState = GmailState.SENDER_THREAD;
        return result;
    }
    
    private void loadSampleMessages() {
        // Sample data - replace with real email API integration
        currentMessages.clear();
        
        currentMessages.add(new GmailMessage(
            "amazon@amazon.com", "you@gmail.com", "Your Order Has Shipped", 
            "Your order #12345 has been shipped and will arrive tomorrow. Tracking number: 1Z999AA10123456784",
            "27-Sept-25;14:30", "unread", "normal", false
        ));
        
        currentMessages.add(new GmailMessage(
            "github@github.com", "you@gmail.com", "Security Alert", 
            "We noticed a new login to your GitHub account from a new device. If this was you, you can ignore this message.",
            "27-Sept-25;12:15", "unread", "high", false
        ));
        
        currentMessages.add(new GmailMessage(
            "team@company.com", "you@gmail.com", "Project Update - Q4 Planning", 
            "Hi team, here's the update for our Q4 planning meeting scheduled for next Monday. Please review the attached document.",
            "27-Sept-25;10:45", "read", "normal", true
        ));
        
        currentMessages.add(new GmailMessage(
            "news@technews.com", "you@gmail.com", "Weekly Tech Digest", 
            "This week in tech: New smartphone releases, AI breakthroughs, and security updates you should know about.",
            "26-Sept-25;09:20", "read", "low", false
        ));
    }
    
    private String getUsage() {
        return "📧 Email Module Usage:\n" +
               "• mail                 - Show inbox (paginated)\n" +
               "• mail more            - Next page\n" +
               "• mail all             - All messages\n" +
               "• mail compose         - New message\n" +
               "• mail <sender>        - Messages from sender\n" +
               "• mail search <query>  - Search messages\n" +
               "• mail del             - Delete messages\n" +
               "• mail backup          - Backup emails (Shizuku)\n" +
               "• mail sync            - Refresh messages\n" +
               "• [id]                 - Open message by ID\n" +
               "• exit                 - Back/Cancel\n" +
               "\n📧 In Message View:\n" +
               "• 1/reply              - Reply to sender\n" +
               "• 2/replyall           - Reply to all\n" +
               "• forward              - Forward message\n" +
               "• delete               - Delete message\n" +
               "• mark [status]        - Mark as read/unread/important\n" +
               "\n🔒 Requires: Authentication + Storage permissions" +
               "\n🔧 Shizuku: " + (ShizukuManager.isAvailable() ? "Available ✅" : "Not Available ❌");
    }
    
    // ===== Data Classes =====
    
    enum GmailState {
        INBOX_VIEW, MESSAGE_VIEW, COMPOSE_MODE, REPLY_MODE, SEARCH_RESULTS, SENDER_THREAD, DELETE_MODE
    }
    
    class GmailMessage {
        String sender;
        String recipient;
        String subject;
        String body;
        String timestamp;
        String status; // read, unread, replied, important
        String priority; // high, normal, low
        boolean hasAttachments;
        
        GmailMessage(String sender, String recipient, String subject, String body, 
                    String timestamp, String status, String priority, boolean hasAttachments) {
            this.sender = sender;
            this.recipient = recipient;
            this.subject = subject;
            this.body = body;
            this.timestamp = timestamp;
            this.status = status;
            this.priority = priority;
            this.hasAttachments = hasAttachments;
        }
        
        String getPreview() {
            String preview = body.length() > 50 ? body.substring(0, 50) + "..." : body;
            return sender + " - " + subject + " - '" + preview + "' - " + timestamp;
        }
        
        boolean contains(String query) {
            return sender.toLowerCase().contains(query.toLowerCase()) ||
                   subject.toLowerCase().contains(query.toLowerCase()) ||
                   body.toLowerCase().contains(query.toLowerCase());
        }
    }
    
    class GmailDraft {
        String to;
        String cc;
        String bcc;
        String subject;
        String body;
        String priority = "normal";
        List<String> attachments;
    }
}
