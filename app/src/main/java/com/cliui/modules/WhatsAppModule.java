package com.cliui.modules;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import com.cliui.utils.PermissionManager;
import com.cliui.utils.ShizukuManager;

import java.util.*;

public class WhatsAppModule implements CommandModule {
    private Context context;
    private PermissionManager permissionManager;
    
    // Demo data for educational purposes only
    private List<DemoContact> demoContacts = new ArrayList<>();
    private String selectedContact = null;
    
    public WhatsAppModule(Context context) {
        this.context = context;
        this.permissionManager = PermissionManager.getInstance(context);
        loadDemoData();
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

        // Require authentication for WhatsApp access
        if (!permissionManager.authenticate("whatsapp_access")) {
            return "🔒 Authentication required for WhatsApp access\n" +
                   "Please authenticate to use WhatsApp features";
        }
        
        switch (command) {
            case "wh":
                return handleWhatsAppCommand(tokens);
            case "whatsapp":
                return handleWhatsAppCommand(tokens);
            default:
                return "❌ Unknown command: " + command + "\n" + getUsage();
        }
    }
    
    private String handleWhatsAppCommand(String[] tokens) {
        if (tokens.length == 1) {
            return showDemoContacts();
        }
        
        String subCommand = tokens[1].toLowerCase();
        switch (subCommand) {
            case "send":
                if (tokens.length >= 4) {
                    String phoneNumber = tokens[2];
                    String message = String.join(" ", Arrays.copyOfRange(tokens, 3, tokens.length));
                    return openWhatsAppWithMessage(phoneNumber, message);
                }
                return "❌ Usage: wh send [phone_number] [message]";
                
            case "contact":
                if (tokens.length >= 3) {
                    String contactName = tokens[2];
                    return openWhatsAppContact(contactName);
                }
                return "❌ Usage: wh contact [name]";
                
            case "status":
                return getWhatsAppStatus();
                
            case "help":
                return getDetailedHelp();
                
            default:
                // Try to treat as phone number directly
                if (tokens.length >= 2) {
                    String possibleNumber = tokens[1];
                    if (isValidPhoneNumber(possibleNumber)) {
                        String message = tokens.length > 2 ? String.join(" ", Arrays.copyOfRange(tokens, 2, tokens.length)) : "";
                        return openWhatsAppWithMessage(possibleNumber, message);
                    }
                }
                return "❌ Unknown command: " + subCommand + "\n" + getUsage();
        }
    }
    
    /**
     * ONLY opens official WhatsApp app with pre-filled content
     * This is compliant with WhatsApp's terms as it uses their official intent system
     */
    private String openWhatsAppWithMessage(String phoneNumber, String message) {
        try {
            // Validate phone number format
            String cleanNumber = phoneNumber.replaceAll("[^0-9+]", "");
            if (!isValidPhoneNumber(cleanNumber)) {
                return "❌ Invalid phone number format\n" +
                       "💡 Use format: +1234567890 or 1234567890";
            }
            
            // Create intent for official WhatsApp app ONLY
            Intent intent = new Intent(Intent.ACTION_VIEW);
            
            // Use WhatsApp's official URL scheme
            String url = "https://wa.me/" + cleanNumber;
            if (!message.isEmpty()) {
                url += "?text=" + Uri.encode(message);
            }
            
            intent.setData(Uri.parse(url));
            intent.setPackage("com.whatsapp"); // Restrict to official app
            
            // Verify WhatsApp can handle the intent
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                
                return "✅ Opening WhatsApp...\n" +
                       "📞 To: " + cleanNumber + 
                       (message.isEmpty() ? "" : "\n💬 Message: " + message) + 
                       "\n\n🔒 Using official WhatsApp app only\n" +
                       "💡 Complete the action in WhatsApp";
            } else {
                return "❌ WhatsApp not installed\n" +
                       "💡 Install WhatsApp from official app store";
            }
            
        } catch (Exception e) {
            return "❌ Failed to open WhatsApp\n" +
                   "💡 Make sure WhatsApp is installed and updated";
        }
    }
    
    private String openWhatsAppContact(String contactName) {
        // This only opens WhatsApp main screen - no contact access
        try {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage("com.whatsapp");
            if (intent != null) {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return "✅ Opening WhatsApp...\n💡 Navigate to your contact manually";
            } else {
                return "❌ WhatsApp not installed";
            }
        } catch (Exception e) {
            return "❌ Failed to open WhatsApp";
        }
    }
    
    private String showDemoContacts() {
        StringBuilder sb = new StringBuilder();
        sb.append("💚 WhatsApp Quick Actions\n\n");
        sb.append("📋 Demo Contacts (Educational Purpose):\n");
        
        for (int i = 0; i < demoContacts.size(); i++) {
            DemoContact contact = demoContacts.get(i);
            sb.append(i + 1).append(". ").append(contact.name)
              .append(" - ").append(contact.phoneNumber).append("\n");
        }
        
        sb.append("\n💡 Commands:\n");
        sb.append("• wh send [number] [message] - Open WhatsApp with message\n");
        sb.append("• wh contact [name] - Open WhatsApp main screen\n");
        sb.append("• wh status - System information\n");
        sb.append("• wh help - Detailed guidance\n");
        
        sb.append("\n🔒 Compliance Notes:\n");
        sb.append("• Uses official WhatsApp app only\n");
        sb.append("• No message reading/access\n");
        sb.append("• No automation/bot behavior\n");
        sb.append("• No third-party APIs\n");
        
        return sb.toString();
    }
    
    private String getWhatsAppStatus() {
        StringBuilder status = new StringBuilder();
        status.append("💚 WhatsApp Status & Information\n\n");
        
        // Check if WhatsApp is installed (official app only)
        boolean whatsappInstalled = isWhatsAppInstalled();
        status.append("📱 Official WhatsApp: ").append(whatsappInstalled ? "Installed ✅" : "Not Installed ❌").append("\n");
        
        // System status
        status.append("🔧 Shizuku: ").append(ShizukuManager.getStatus()).append("\n");
        status.append("🔒 Permissions: Authenticated ✅\n");
        
        // Compliance information
        status.append("\n📜 Compliance Status:\n");
        status.append("✅ Uses official WhatsApp app only\n");
        status.append("✅ No message reading/access\n");
        status.append("✅ No automation/bot behavior\n");
        status.append("✅ No third-party APIs\n");
        status.append("✅ No WhatsApp Web usage\n");
        status.append("✅ Respects user privacy\n");
        
        // Educational note
        status.append("\n💡 Educational Purpose:\n");
        status.append("This module demonstrates how to properly\n");
        status.append("integrate with WhatsApp using official APIs\n");
        status.append("while respecting their terms of service.");
        
        return status.toString();
    }
    
    private String getDetailedHelp() {
        return "💚 WhatsApp Integration Guide\n\n" +
               "📋 What This Module Does:\n" +
               "• Opens official WhatsApp app with pre-filled messages\n" +
               "• Uses WhatsApp's official URL scheme (wa.me)\n" +
               "• Respects all WhatsApp terms and conditions\n\n" +
               
               "🚫 What This Module DOES NOT Do:\n" +
               "• Cannot read WhatsApp messages\n" +
               "• Cannot access conversations\n" +
               "• No automation or bot behavior\n" +
               "• No third-party APIs or WhatsApp Web\n" +
               "• No contact list access\n\n" +
               
               "🔒 Compliance Features:\n" +
               "• Uses only official WhatsApp app\n" +
               "• Requires user interaction for sending\n" +
               "• No background operations\n" +
               "• No data collection\n\n" +
               
               "💡 Usage Examples:\n" +
               "• wh send +1234567890 Hello - Opens WhatsApp with message\n" +
               "• wh contact - Opens WhatsApp main screen\n" +
               "• wh status - Shows system information\n\n" +
               
               "📞 Note: You need the phone number to message someone";
    }
    
    // ===== COMPLIANCE-CHECK METHODS =====
    
    private boolean isWhatsAppInstalled() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://wa.me/1234567890")); // Dummy number
            intent.setPackage("com.whatsapp");
            return intent.resolveActivity(context.getPackageManager()) != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isValidPhoneNumber(String number) {
        // Basic validation - should start with + or be 10+ digits
        return number.matches("^\\+[0-9]{10,15}$") || number.matches("^[0-9]{10,15}$");
    }
    
    // ===== DEMO DATA (Educational Purpose Only) =====
    
    private void loadDemoData() {
        demoContacts.clear();
        
        // These are example contacts for demonstration only
        // Users should replace with their actual contacts
        demoContacts.add(new DemoContact("Example Contact 1", "+1234567890"));
        demoContacts.add(new DemoContact("Example Contact 2", "+1987654321"));
        demoContacts.add(new DemoContact("Example Business", "+1122334455"));
    }
    
    private String getUsage() {
        return "💚 WhatsApp Commands (Compliant)\n\n" +
               "📋 Quick Actions:\n" +
               "• wh - Show demo contacts and options\n" +
               "• wh send [number] [message] - Open WhatsApp with message\n" +
               "• wh contact - Open WhatsApp main screen\n" +
               "• wh status - System and compliance info\n" +
               "• wh help - Detailed guidance\n\n" +
               
               "🔒 100% Compliant with WhatsApp Terms:\n" +
               "• Uses official app only\n" +
               "• No message reading\n" +
               "• No automation\n" +
               "• No third-party APIs\n\n" +
               
               "💡 Example: wh send +1234567890 Hello there!";
    }
    
    // Demo data structure
    class DemoContact {
        String name;
        String phoneNumber;
        
        DemoContact(String name, String phoneNumber) {
            this.name = name;
            this.phoneNumber = phoneNumber;
        }
    }
}
