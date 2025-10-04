package com.cliui.modules;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import com.cliui.utils.PermissionManager;
import com.cliui.utils.Authentication;
import java.text.SimpleDateFormat;
import java.util.*;

public class WhatsAppModule implements CommandModule {
    private Context context;
    private PermissionManager permissionManager;
    
    // We CANNOT access real WhatsApp data, so we simulate the experience
    private List<SimulatedMessage> simulatedMessages = new ArrayList<>();
    private int currentPage = 0;
    private static final int MESSAGES_PER_PAGE = 5;
    private String selectedContact = null;
    private String draftMessage = null;
    
    public WhatsAppModule(Context context) {
        this.context = context;
        this.permissionManager = PermissionManager.getInstance(context);
        loadSimulatedData(); // This is ONLY demo data
    }
    
    @Override
    public String execute(String[] tokens) {
        if (tokens.length == 0) return getUsage();
        
        String command = tokens[0].toLowerCase();
        
        // Require biometric/pin authentication
        if (!Authentication.authenticate("whatsapp_access")) {
            return "🔒 Biometric/PIN authentication required for WhatsApp";
        }
        
        if (command.equals("wh")) {
            if (tokens.length == 1) {
                return showRecentMessages(0);
            } else if (tokens.length >= 2) {
                String subCommand = tokens[1].toLowerCase();
                switch (subCommand) {
                    case "more":
                    case "next":
                        return showRecentMessages(currentPage + 1);
                    case "all":
                        return showAllMessages();
                    case "send":
                        if (tokens.length >= 3) {
                            return startNewMessage(Arrays.copyOfRange(tokens, 2, tokens.length));
                        }
                        return "Usage: wh send [phone_number] [message]";
                    default:
                        // Try to treat as phone number
                        selectedContact = String.join(" ", Arrays.copyOfRange(tokens, 1, tokens.length));
                        return startNewMessage(new String[]{selectedContact});
                }
            }
        }
        
        // Handle simulated message ID selection
        try {
            int messageId = Integer.parseInt(command);
            return openSimulatedMessage(messageId);
        } catch (NumberFormatException e) {
            return "❌ Unknown command";
        }
    }
    
    private String showRecentMessages(int page) {
        currentPage = page;
        int start = page * MESSAGES_PER_PAGE;
        int end = Math.min(start + MESSAGES_PER_PAGE, simulatedMessages.size());
        
        if (start >= simulatedMessages.size()) {
            return "💚 No more demo messages\n💡 Real WhatsApp messages cannot be accessed due to privacy restrictions";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("💚 WhatsApp Demo (Page ").append(page + 1).append(")\n");
        sb.append("Showing ").append(start + 1).append("-").append(end).append(" of ").append(simulatedMessages.size()).append(" demo messages\n\n");
        
        for (int i = start; i < end; i++) {
            SimulatedMessage msg = simulatedMessages.get(i);
            String statusIcon = "📭";
            if ("read".equals(msg.status)) statusIcon = "📬";
            if ("replied".equals(msg.status)) statusIcon = "📤";
            
            sb.append(i + 1).append(". ").append(statusIcon).append(" ")
              .append(msg.sender).append(" - ")
              .append(msg.timestamp).append(" - ")
              .append(msg.status).append("\n   ")
              .append(msg.getPreview()).append("\n");
        }
        
        sb.append("\n💡 Type ID to view demo message");
        sb.append("\n💡 wh more - Next page");
        sb.append("\n💡 wh all - All demo messages"); 
        sb.append("\n💡 wh send [number] [message] - Send new message");
        sb.append("\n🔒 Real WhatsApp messages cannot be accessed");
        
        return sb.toString();
    }
    
    private String openSimulatedMessage(int messageId) {
        if (messageId < 1 || messageId > simulatedMessages.size()) {
            return "❌ Invalid message ID";
        }
        
        SimulatedMessage msg = simulatedMessages.get(messageId - 1);
        selectedContact = msg.sender;
        
        return "💚 Demo Message " + messageId + ":\n" +
               "From: " + msg.sender + "\n" +
               "Time: " + msg.timestamp + "\n" + 
               "Status: " + msg.status + "\n" +
               "Message: " + msg.content + "\n\n" +
               "💡 Commands:\n" +
               "• wh send " + msg.sender + " [message] - Reply to this contact\n" +
               "• wh - Return to message list\n" +
               "🔒 Real message reply not possible - opens new chat";
    }
    
    private String startNewMessage(String[] tokens) {
        if (tokens.length == 0) {
            return "❌ Usage: wh send [phone_number] [message]";
        }
        
        String phoneNumber = tokens[0];
        String message = tokens.length > 1 ? String.join(" ", Arrays.copyOfRange(tokens, 1, tokens.length)) : "";
        
        // Validate phone number format
        String cleanNumber = phoneNumber.replaceAll("[^0-9+]", "");
        if (cleanNumber.length() < 10) {
            return "❌ Invalid phone number: " + phoneNumber;
        }
        
        if (message.isEmpty()) {
            draftMessage = "";
            selectedContact = phoneNumber;
            return "💚 New message to: " + phoneNumber + "\n" +
                   "💡 Type your message and 'send' to open WhatsApp\n" +
                   "💡 Type 'exit' to cancel";
        }
        
        return sendWhatsAppMessage(cleanNumber, message);
    }
    
    private String sendWhatsAppMessage(String phoneNumber, String message) {
        try {
            // Use WhatsApp native app ONLY - no web fallback
            Intent intent = new Intent(Intent.ACTION_VIEW);
            String url = "https://api.whatsapp.com/send?phone=" + phoneNumber + "&text=" + Uri.encode(message);
            intent.setData(Uri.parse(url));
            intent.setPackage("com.whatsapp"); // Force native app
            
            // Verify WhatsApp can handle the intent
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
                
                return "✅ Opening WhatsApp to send message\n" +
                       "📞 To: " + phoneNumber + "\n" + 
                       "💬 Message: " + message + "\n\n" +
                       "💡 Complete sending in WhatsApp app";
            } else {
                return "❌ WhatsApp not available\n" +
                       "💡 Install WhatsApp from Play Store";
            }
            
        } catch (Exception e) {
            return "❌ Failed to open WhatsApp\n" +
                   "💡 Error: " + e.getMessage();
        }
    }
    
    private String showAllMessages() {
        StringBuilder sb = new StringBuilder();
        sb.append("💚 All Demo Messages (").append(simulatedMessages.size()).append(")\n");
        sb.append("🔒 Real WhatsApp access not available\n\n");
        
        for (int i = 0; i < simulatedMessages.size(); i++) {
            SimulatedMessage msg = simulatedMessages.get(i);
            String statusIcon = "📭";
            if ("read".equals(msg.status)) statusIcon = "📬";
            if ("replied".equals(msg.status)) statusIcon = "📤";
            
            sb.append(i + 1).append(". ").append(statusIcon).append(" ")
              .append(msg.sender).append(" - ")
              .append(msg.timestamp).append(" - ")
              .append(msg.status).append("\n   ")
              .append(msg.getPreview()).append("\n");
        }
        
        return sb.toString();
    }
    
    // ===== SIMULATED DATA ONLY =====
    // We cannot access real WhatsApp messages due to encryption and privacy policies
    
    private void loadSimulatedData() {
        simulatedMessages.clear();
        
        // Demo data to show how it would work
        simulatedMessages.add(new SimulatedMessage("+1234567890", "Hey, are we still meeting today?", "27-Sept-25;14:30", "unread"));
        simulatedMessages.add(new SimulatedMessage("Mom", "Don't forget to call me later!", "27-Sept-25;12:15", "read"));
        simulatedMessages.add(new SimulatedMessage("John", "Check out this photo I sent!", "27-Sept-25;10:45", "replied"));
        simulatedMessages.add(new SimulatedMessage("Sarah", "Running 15 minutes late, sorry!", "26-Sept-25;18:20", "read"));
        simulatedMessages.add(new SimulatedMessage("Work Group", "Meeting moved to 3 PM tomorrow", "26-Sept-25;16:30", "unread"));
        simulatedMessages.add(new SimulatedMessage("+1987654321", "Your package has been delivered", "26-Sept-25;14:15", "read"));
    }
    
    private String getUsage() {
        return "💚 WhatsApp Commands:\n" +
               "• wh - Show demo messages (paginated)\n" +
               "• wh more - Next page of demo messages\n" + 
               "• wh all - All demo messages\n" +
               "• wh send [number] [message] - Send message\n" +
               "• [id] - View demo message by ID\n" +
               "\n🔒 Limitations:\n" +
               "• Real WhatsApp messages cannot be accessed\n" +
               "• No message reading or deletion\n" +
               "• No conversation threads\n" +
               "• Native WhatsApp app only\n" +
               "\n💡 Works by opening WhatsApp with pre-filled messages";
    }
    
    // Demo data structure
    class SimulatedMessage {
        String sender;
        String content;
        String timestamp;
        String status; // unread, read, replied
        
        SimulatedMessage(String sender, String content, String timestamp, String status) {
            this.sender = sender;
            this.content = content;
            this.timestamp = timestamp;
            this.status = status;
        }
        
        String getPreview() {
            return content.length() > 40 ? content.substring(0, 40) + "..." : content;
        }
    }
}
