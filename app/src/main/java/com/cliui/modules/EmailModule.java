package com.cliui.modules;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import com.cliui.utils.Authentication;
import com.cliui.utils.PermissionManager;
import java.text.SimpleDateFormat;
import java.util.*;

public class GmailModule implements CommandModule {
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
    
    // Constants
    private static final int MESSAGES_PER_PAGE = 20;
    
    public GmailModule(Context context) {
        this.context = context;
        this.permissionManager = PermissionManager.getInstance(context);
        loadSampleMessages(); // For demo - replace with real Gmail API
    }
    
    @Override
    public String execute(String[] tokens) {
        if (tokens.length == 0) return getUsage();
        
        String command = tokens[0].toLowerCase();
        
        // Require authentication for Gmail access
        if (!Authentication.authenticate()) {
            return "🔒 Biometric authentication required for Gmail access";
        }
        
        // Check permissions
        if (!permissionManager.canExecute("mail")) {
            return "📧 Gmail access requires permissions\nType 'mail' again to grant";
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
                        return "Usage: mail search [query]";
                    case "del":
                        return enterDeleteMode();
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
            return "Unknown command. Type 'mail' for Gmail options.";
        }
    }
    
    private String handleMessageView(String[] tokens) {
        String command = tokens[0].toLowerCase();
        
        switch (command) {
            case "1":
            case "reply":
                return startReply(false);
            case "2":
            case "replyall":
                return startReply(true);
            case "attach":
                if (tokens.length >= 2) {
                    return attachFile(tokens[1]);
                }
                return "Usage: attach [filename]";
            case "exit":
                currentState = GmailState.INBOX_VIEW;
                return showInboxPage(currentPage);
            default:
                return displayCurrentMessage() + "\n\n" + getMessageViewSuggestions();
        }
    }
    
    private String handleComposeMode(String[] tokens) {
        String command = tokens[0].toLowerCase();
        
        switch (command) {
            case "send":
                return sendEmail();
            case "to":
                if (tokens.length >= 2) {
                    currentDraft.to = tokens[1];
                    return "✅ To: " + currentDraft.to + "\n" + getComposeSuggestions();
                }
                return "Usage: to [email] (multiple emails separated by ;)";
            case "cc":
                if (tokens.length >= 2) {
                    currentDraft.cc = tokens[1];
                    return "✅ CC: " + currentDraft.cc + "\n" + getComposeSuggestions();
                }
                return "Usage: cc [email]";
            case "bcc":
                if (tokens.length >= 2) {
                    currentDraft.bcc = tokens[1];
                    return "✅ BCC: " + currentDraft.bcc + "\n" + getComposeSuggestions();
                }
                return "Usage: bcc [email]";
            case "subject":
                if (tokens.length >= 2) {
                    currentDraft.subject = String.join(" ", Arrays.copyOfRange(tokens, 1, tokens.length));
                    return "✅ Subject: " + currentDraft.subject + "\n" + getComposeSuggestions();
                }
                return "Usage: subject [title]";
            case "attach":
                if (tokens.length >= 2) {
                    return attachFile(tokens[1]);
                }
                return "Usage: attach [filename]";
            case "exit":
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
        String command = tokens[0].toLowerCase();
        
        switch (command) {
            case "send":
                return sendReply();
            case "attach":
                if (tokens.length >= 2) {
                    return attachFile(tokens[1]);
                }
                return "Usage: attach [filename]";
            case "exit":
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
        try {
            int messageId = Integer.parseInt(tokens[0]);
            return openMessageFromSearch(messageId);
        } catch (NumberFormatException e) {
            return "Type message ID to open or 'exit' to return to inbox";
        }
    }
    
    private String handleSenderThread(String[] tokens) {
        try {
            int messageId = Integer.parseInt(tokens[0]);
            return openMessageFromThread(messageId);
        } catch (NumberFormatException e) {
            if (tokens[0].equalsIgnoreCase("compose")) {
                return startNewMessageToSender();
            }
            return "Type message ID to open, 'compose' for new message, or 'exit' to return";
        }
    }
    
    private String handleDeleteMode(String[] tokens) {
        String command = tokens[0].toLowerCase();
        
        switch (command) {
            case "confirm":
                return deleteSelectedMessages();
            case "cancel":
                currentState = GmailState.INBOX_VIEW;
                return "🗑️ Delete cancelled";
            case "all":
                deleteSelection.clear();
                for (int i = 0; i < currentMessages.size(); i++) {
                    deleteSelection.add(i + 1);
                }
                return "✅ All messages selected for deletion\nType: confirm (delete) | cancel (abort)";
            default:
                // Parse message IDs (1,3,5 etc)
                try {
                    String[] ids = command.split(",");
                    for (String id : ids) {
                        deleteSelection.add(Integer.parseInt(id.trim()));
                    }
                    return "✅ Selected messages: " + deleteSelection + 
                           "\nType: confirm (delete) | cancel (abort) | all (select all)";
                } catch (NumberFormatException e) {
                    return "❌ Invalid ID format. Use: 1,3,5 etc\nType: confirm | cancel | all";
                }
        }
    }
    
    // Core Gmail functionality
    private String showInboxPage(int page) {
        currentPage = page;
        int start = page * MESSAGES_PER_PAGE;
        int end = Math.min(start + MESSAGES_PER_PAGE, currentMessages.size());
        
        if (start >= currentMessages.size()) {
            return "📧 No more messages\nType 'mail' to return to first page";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("📧 Gmail Inbox (Page ").append(page + 1).append(")\n");
        sb.append("Messages ").append(start + 1).append("-").append(end).append(" of ").append(currentMessages.size()).append("\n\n");
        
        for (int i = start; i < end; i++) {
            GmailMessage msg = currentMessages.get(i);
            sb.append(i + 1).append(". ").append(msg.getPreview()).append("\n");
        }
        
        sb.append("\n💡 Type ID to open | mail more (next) | mail all (all) | mail compose (new)");
        sb.append("\n💡 mail <sender> (filter) | mail search <query> | mail del (delete)");
        
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
        
        return "📧 From: " + selectedMessage.sender + 
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
               "\n💡 Type your reply, then 'send' to send\nType 'attach [file]' to add attachment\nType 'exit' to save draft";
    }
    
    private String startNewCompose() {
        currentDraft = new GmailDraft();
        currentState = GmailState.COMPOSE_MODE;
        return "📧 New Message\n" + getComposeSuggestions();
    }
    
    private String sendEmail() {
        if (currentDraft == null) return "❌ No message to send";
        
        // Use Android's email intent
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{currentDraft.to});
        intent.putExtra(Intent.EXTRA_CC, new String[]{currentDraft.cc});
        intent.putExtra(Intent.EXTRA_BCC, new String[]{currentDraft.bcc});
        intent.putExtra(Intent.EXTRA_SUBJECT, currentDraft.subject);
        intent.putExtra(Intent.EXTRA_TEXT, currentDraft.body);
        
        context.startActivity(Intent.createChooser(intent, "Send email"));
        
        String result = "✅ Message sent to: " + currentDraft.to;
        currentDraft = null;
        currentState = GmailState.INBOX_VIEW;
        
        return result;
    }
    
    private String sendReply() {
        // Similar to sendEmail but maintains context
        String result = sendEmail();
        currentState = GmailState.MESSAGE_VIEW;
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
        // Sort in descending order to avoid index issues
        Collections.sort(deleteSelection, Collections.reverseOrder());
        
        for (int id : deleteSelection) {
            if (id >= 1 && id <= currentMessages.size()) {
                currentMessages.remove(id - 1);
            }
        }
        
        int deletedCount = deleteSelection.size();
        deleteSelection.clear();
        currentState = GmailState.INBOX_VIEW;
        
        return "✅ " + deletedCount + " messages deleted\n" + showInboxPage(0);
    }
    
    // Utility methods
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
            // Save draft logic would go here
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
    
    private String getMessageViewSuggestions() {
        return "💡 Type: 1/reply (reply) | 2/replyall (reply all) | attach [file] (add attachment) | exit (back to inbox)";
    }
    
    private String getComposeSuggestions() {
        return "💡 Type: to [email] | cc [email] | bcc [email] | subject [title] | attach [file] | send (send) | exit (save draft)";
    }
    
    private String getReplySuggestions() {
        return "💡 Type: send (send reply) | attach [file] (add attachment) | exit (save draft)";
    }
    
    private String showAllMessages() {
        // Show all messages without pagination
        StringBuilder sb = new StringBuilder();
        sb.append("📧 All Messages (").append(currentMessages.size()).append(" total)\n\n");
        
        for (int i = 0; i < currentMessages.size(); i++) {
            GmailMessage msg = currentMessages.get(i);
            sb.append(i + 1).append(". ").append(msg.getPreview()).append("\n");
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
        // Sample data - replace with real Gmail API integration
        currentMessages.add(new GmailMessage(
            "amazon@amazon.com", "you@gmail.com", "Your Order Has Shipped", 
            "Your order #12345 has been shipped and will arrive tomorrow...",
            "27-Sept-25;14:30", "unread", false
        ));
        
        currentMessages.add(new GmailMessage(
            "github@github.com", "you@gmail.com", "Security Alert", 
            "We noticed a new login to your GitHub account from a new device...",
            "27-Sept-25;12:15", "unread", false
        ));
        
        currentMessages.add(new GmailMessage(
            "team@company.com", "you@gmail.com", "Project Update - Q4 Planning", 
            "Hi team, here's the update for our Q4 planning meeting scheduled for...",
            "27-Sept-25;10:45", "read", true
        ));
        
        // Add more sample messages...
    }
    
    private String getUsage() {
        return "📧 Gmail Commands:\n" +
               "• mail - Show inbox (20 messages)\n" +
               "• mail more - Show next 20 messages\n" +
               "• mail all - Show all messages\n" +
               "• mail compose - New message\n" +
               "• mail <sender> - Messages from sender\n" +
               "• mail search <query> - Search messages\n" +
               "• mail del - Delete messages\n" +
               "• [id] - Open message by ID\n" +
               "• exit - Back/Cancel current operation\n\n" +
               "🔒 Requires biometric authentication";
    }
    
    // Data classes
    enum GmailState {
        INBOX_VIEW, MESSAGE_VIEW, COMPOSE_MODE, REPLY_MODE, SEARCH_RESULTS, SENDER_THREAD, DELETE_MODE
    }
    
    class GmailMessage {
        String sender;
        String recipient;
        String subject;
        String body;
        String timestamp;
        String status; // read, unread, replied
        boolean hasAttachments;
        
        GmailMessage(String sender, String recipient, String subject, String body, 
                    String timestamp, String status, boolean hasAttachments) {
            this.sender = sender;
            this.recipient = recipient;
            this.subject = subject;
            this.body = body;
            this.timestamp = timestamp;
            this.status = status;
            this.hasAttachments = hasAttachments;
        }
        
        String getPreview() {
            String preview = body.length() > 50 ? body.substring(0, 50) + "..." : body;
            return sender + " - " + subject + " - '" + preview + "' - " + timestamp + " - " + status;
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
        List<String> attachments;
    }
    
    // Delete mode selection
    private List<Integer> deleteSelection = new ArrayList<>();
}
