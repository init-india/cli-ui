package com.cliui.modules;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.ContactsContract;
import android.database.Cursor;
import com.cliui.utils.PermissionManager;
import com.cliui.utils.Authentication;
import java.text.SimpleDateFormat;
import java.util.*;

public class WhatsAppModule implements CommandModule {
    private Context context;
    private PermissionManager permissionManager;
    
    // State management
    private WhatsAppState currentState = WhatsAppState.IDLE;
    private List<WhatsAppContact> recentContacts = new ArrayList<>();
    private String selectedContact = null;
    private String draftMessage = null;
    private String callType = "audio";
    
    public WhatsAppModule(Context context) {
        this.context = context;
        this.permissionManager = PermissionManager.getInstance(context);
        loadRecentContacts();
    }
    
    @Override
    public String execute(String[] tokens) {
        if (tokens.length == 0) return getUsage();
        
        String command = tokens[0].toLowerCase();
        
        // Require authentication for WhatsApp access
        if (!Authentication.authenticate()) {
            return "🔒 Authentication required for WhatsApp access";
        }
        
        // Check if WhatsApp is installed
        if (!isWhatsAppInstalled()) {
            return "❌ WhatsApp not installed\nInstall from Play Store or official website";
        }
        
        // Check permissions
        if (!permissionManager.canExecute("whatsapp")) {
            return "📱 WhatsApp access requires permissions\nType 'wh' again to grant";
        }
        
        // Handle exit command in any state
        if (command.equals("exit")) {
            return handleExit();
        }
        
        // State-based handling
        switch (currentState) {
            case CONTACT_SELECTION:
                return handleContactSelection(tokens);
            case MESSAGE_DRAFT:
                return handleMessageDraft(tokens);
            case CALL_TYPE_SELECTION:
                return handleCallTypeSelection(tokens);
            default:
                return handleIdleState(tokens);
        }
    }
    
    private String handleIdleState(String[] tokens) {
        String command = tokens[0].toLowerCase();
        
        if (command.equals("wh")) {
            if (tokens.length == 1) {
                return showQuickActions();
            } else if (tokens.length >= 2) {
                String subCommand = tokens[1].toLowerCase();
                
                switch (subCommand) {
                    case "call":
                        return handleCallCommand(tokens);
                    case "del":
                        return "🗑️ WhatsApp message deletion not available\nDue to WhatsApp restrictions, use the WhatsApp app directly";
                    default:
                        // wh <contact> - Start messaging
                        selectedContact = String.join(" ", Arrays.copyOfRange(tokens, 1, tokens.length));
                        return startMessagingWithContact(selectedContact);
                }
            }
        }
        
        if (command.equals("whcall")) {
            if (tokens.length == 1) {
                return showCallHistory();
            } else if (tokens.length >= 2) {
                selectedContact = String.join(" ", Arrays.copyOfRange(tokens, 1, tokens.length));
                return initiateCallWithContact(selectedContact);
            }
        }
        
        return "Unknown command. Type 'wh' for WhatsApp options.";
    }
    
    private String handleContactSelection(String[] tokens) {
        try {
            int selection = Integer.parseInt(tokens[0]);
            if (selection < 1 || selection > recentContacts.size()) {
                return "❌ Invalid selection. Choose 1-" + recentContacts.size();
            }
            
            selectedContact = recentContacts.get(selection - 1).name;
            return startMessagingWithContact(selectedContact);
            
        } catch (NumberFormatException e) {
            return "❌ Please enter a number 1-" + recentContacts.size();
        }
    }
    
    private String handleMessageDraft(String[] tokens) {
        String command = tokens[0].toLowerCase();
        
        switch (command) {
            case "send":
                return sendWhatsAppMessage();
            case "exit":
                currentState = WhatsAppState.IDLE;
                return "💚 Draft cancelled\n" + showQuickActions();
            default:
                // Treat as message content
                draftMessage = String.join(" ", tokens);
                return "📝 Draft for " + selectedContact + ":\n" + draftMessage + 
                       "\n\n💡 Type: send (send message) | exit (cancel)";
        }
    }
    
    private String handleCallTypeSelection(String[] tokens) {
        String command = tokens[0].toLowerCase();
        
        switch (command) {
            case "1":
                callType = "audio";
                return startWhatsAppCall();
            case "2":
                callType = "video";
                return startWhatsAppCall();
            case "exit":
                currentState = WhatsAppState.IDLE;
                return "📞 Call cancelled";
            default:
                return "❌ Please choose: 1 (audio) or 2 (video)\nType: exit to cancel";
        }
    }
    
    private String handleCallCommand(String[] tokens) {
        if (tokens.length >= 3) {
            selectedContact = String.join(" ", Arrays.copyOfRange(tokens, 2, tokens.length));
            return initiateCallWithContact(selectedContact);
        }
        return "Usage: wh call [contact]";
    }
    
    private String showQuickActions() {
        StringBuilder sb = new StringBuilder();
        sb.append("💚 WhatsApp Quick Access\n");
        sb.append("Recent Contacts:\n");
        
        if (recentContacts.isEmpty()) {
            sb.append("No recent contacts found\n");
        } else {
            for (int i = 0; i < recentContacts.size(); i++) {
                sb.append(i + 1).append(". ").append(recentContacts.get(i).name).append("\n");
            }
        }
        
        sb.append("\n💡 Quick Commands:\n");
        sb.append("• wh [number] - Message contact\n");
        sb.append("• wh call [contact] - Call contact\n");
        sb.append("• whcall - Call history & options\n");
        sb.append("• Type number to select contact");
        
        currentState = WhatsAppState.CONTACT_SELECTION;
        return sb.toString();
    }
    
    private String startMessagingWithContact(String contact) {
        String phoneNumber = findContactPhoneNumber(contact);
        
        if (phoneNumber != null) {
            currentState = WhatsAppState.MESSAGE_DRAFT;
            return "💚 Ready to message: " + contact + 
                   "\n\n💡 Type your message, then 'send' to send\nType 'exit' to cancel";
        } else {
            return "❌ Contact not found: " + contact + 
                   "\n💡 Try: wh [phone number] to message directly";
        }
    }
    
    private String sendWhatsAppMessage() {
        if (draftMessage == null || draftMessage.isEmpty()) {
            return "❌ No message to send";
        }
        
        String phoneNumber = findContactPhoneNumber(selectedContact);
        if (phoneNumber == null) {
            // If no contact found, treat as direct number
            phoneNumber = selectedContact.replaceAll("[^0-9+]", "");
        }
        
        try {
            // Use WhatsApp's official URL scheme
            String url = "https://wa.me/" + phoneNumber + "?text=" + Uri.encode(draftMessage);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            intent.setPackage("com.whatsapp");
            context.startActivity(intent);
            
            // Add to recent contacts
            addToRecentContacts(selectedContact);
            
            String result = "✅ Opening WhatsApp to send message to " + selectedContact;
            draftMessage = null;
            currentState = WhatsAppState.IDLE;
            
            return result;
            
        } catch (Exception e) {
            return "❌ Failed to open WhatsApp: " + e.getMessage();
        }
    }
    
    private String showCallHistory() {
        return "📞 Recent WhatsApp Calls:\n" +
               "1. Sarah (audio) - 27-Sept-25;14:30 - answered\n" +
               "2. John (video) - 27-Sept-25;12:15 - missed\n" +
               "3. Mom (audio) - 26-Sept-25;18:45 - answered\n\n" +
               "💡 Type: whcall [contact] to call";
    }
    
    private String initiateCallWithContact(String contact) {
        String phoneNumber = findContactPhoneNumber(contact);
        
        if (phoneNumber != null) {
            currentState = WhatsAppState.CALL_TYPE_SELECTION;
            return "📞 Call " + contact + " via WhatsApp\n" +
                   "Choose call type:\n" +
                   "1. Audio Call\n" +
                   "2. Video Call\n\n" +
                   "💡 Type: 1 or 2 | exit to cancel";
        } else {
            return "❌ Contact not found: " + contact;
        }
    }
    
    private String startWhatsAppCall() {
        String phoneNumber = findContactPhoneNumber(selectedContact);
        if (phoneNumber == null) {
            phoneNumber = selectedContact.replaceAll("[^0-9+]", "");
        }
        
        try {
            // Use WhatsApp's call URL scheme
            String url = "whatsapp://call/" + callType + "/" + phoneNumber;
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            intent.setPackage("com.whatsapp");
            context.startActivity(intent);
            
            // Add to recent contacts
            addToRecentContacts(selectedContact);
            
            String result = "✅ Starting WhatsApp " + callType + " call with " + selectedContact;
            currentState = WhatsAppState.IDLE;
            
            return result;
            
        } catch (Exception e) {
            return "❌ Failed to start WhatsApp call: " + e.getMessage() +
                   "\n💡 Make sure WhatsApp is installed";
        }
    }
    
    private String handleExit() {
        switch (currentState) {
            case CONTACT_SELECTION:
            case MESSAGE_DRAFT:
            case CALL_TYPE_SELECTION:
                currentState = WhatsAppState.IDLE;
                selectedContact = null;
                draftMessage = null;
                return "💚 Returning to main menu";
            default:
                return "Already in main menu";
        }
    }
    
    // Contact management methods
    private String findContactPhoneNumber(String contactName) {
        // Search device contacts for WhatsApp-registered numbers
        if (contactName.matches(".*\\d.*")) {
            // Contains numbers, likely a phone number
            return contactName.replaceAll("[^0-9+]", "");
        }
        
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                },
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?",
                new String[]{"%" + contactName + "%"},
                null
            );
            
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndex(
                    ContactsContract.CommonDataKinds.Phone.NUMBER));
            }
        } catch (SecurityException e) {
            // Contact permission not granted
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return null;
    }
    
    private void loadRecentContacts() {
        // Load from shared preferences
        // Mock data for demonstration
        recentContacts.add(new WhatsAppContact("Sarah", "27-Sept-25;14:30"));
        recentContacts.add(new WhatsAppContact("John", "27-Sept-25;12:15"));
        recentContacts.add(new WhatsAppContact("Mom", "26-Sept-25;18:45"));
    }
    
    private void addToRecentContacts(String contactName) {
        // Remove if already exists
        recentContacts.removeIf(contact -> contact.name.equalsIgnoreCase(contactName));
        
        // Add to beginning
        WhatsAppContact contact = new WhatsAppContact(contactName, 
            new SimpleDateFormat("dd-MMM-yy;HH:mm").format(new Date()));
        recentContacts.add(0, contact);
        
        // Keep only 5 recent contacts
        if (recentContacts.size() > 5) {
            recentContacts = recentContacts.subList(0, 5);
        }
    }
    
    private boolean isWhatsAppInstalled() {
        try {
            context.getPackageManager().getPackageInfo("com.whatsapp", 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
    
    private String getUsage() {
        return "💚 WhatsApp Commands:\n" +
               "• wh - Show quick actions & recent contacts\n" +
               "• wh [contact] - Start messaging contact\n" +
               "• wh call [contact] - Call contact\n" +
               "• whcall - Show call history\n" +
               "• whcall [contact] - Call contact directly\n" +
               "• exit - Cancel current operation\n\n" +
               "🔒 Requires authentication & permissions";
    }
    
    // Data classes
    enum WhatsAppState {
        IDLE, CONTACT_SELECTION, MESSAGE_DRAFT, CALL_TYPE_SELECTION
    }
    
    class WhatsAppContact {
        String name;
        String lastInteraction;
        
        WhatsAppContact(String name, String lastInteraction) {
            this.name = name;
            this.lastInteraction = lastInteraction;
        }
    }
}
