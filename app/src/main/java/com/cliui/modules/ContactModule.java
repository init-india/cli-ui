package com.cliui.modules;

import android.content.Context;
import android.provider.ContactsContract;
import android.database.Cursor;
import com.cliui.utils.PermissionManager;
import com.cliui.utils.Authentication;
import java.util.*;

public class ContactModule implements CommandModule {
    private Context context;
    private PermissionManager permissionManager;
    
    private ContactState currentState = ContactState.LIST_VIEW;
    private List<Contact> contacts = new ArrayList<>();
    private List<Integer> deleteSelection = new ArrayList<>();
    private Contact newContact = null;
    private String editContactName = null;
    
    public ContactModule(Context context) {
        this.context = context;
        this.permissionManager = PermissionManager.getInstance(context);
        loadContacts();
    }
    
    @Override
    public String execute(String[] tokens) {
        if (!Authentication.authenticate()) {
            return "🔒 Authentication required for contacts";
        }
        
        if (!permissionManager.canExecute("contact")) {
            return "📱 Contact access requires permissions\nType 'contact' again to grant";
        }
        
        if (tokens.length == 0) return getUsage();
        String command = tokens[0].toLowerCase();
        
        if (command.equals("exit")) return handleExit();
        
        switch (currentState) {
            case LIST_VIEW: return handleListView(tokens);
            case ADD_MODE: return handleAddMode(tokens);
            case EDIT_MODE: return handleEditMode(tokens);
            case DELETE_MODE: return handleDeleteMode(tokens);
            default: return handleListView(tokens);
        }
    }
    
    private String handleListView(String[] tokens) {
        String command = tokens[0].toLowerCase();
        
        if (command.equals("contact")) {
            if (tokens.length == 1) {
                return showContacts();
            } else if (tokens.length >= 2) {
                String subCommand = tokens[1].toLowerCase();
                if (subCommand.equals("add")) {
                    return startAddContact();
                } else if (subCommand.equals("edit")) {
                    if (tokens.length >= 3) {
                        return startEditContact(String.join(" ", Arrays.copyOfRange(tokens, 2, tokens.length)));
                    }
                    return "Usage: contact edit [name]";
                } else if (subCommand.equals("del")) {
                    return enterDeleteMode();
                }
            }
        }
        
        return "Type 'contact' to view contacts or 'contact add' to add new";
    }
    
    private String handleAddMode(String[] tokens) {
        String command = tokens[0].toLowerCase();
        
        switch (command) {
            case "save":
                return saveContact();
            case "exit":
                currentState = ContactState.LIST_VIEW;
                return "Contact addition cancelled";
            case "name":
                if (tokens.length >= 2) {
                    newContact.name = String.join(" ", Arrays.copyOfRange(tokens, 1, tokens.length));
                    return "✅ Name: " + newContact.name + "\n" + getAddSuggestions();
                }
                return "Usage: name [full name]";
            case "number":
                if (tokens.length >= 2) {
                    newContact.number = tokens[1];
                    return "✅ Number: " + newContact.number + "\n" + getAddSuggestions();
                }
                return "Usage: number [phone number]";
            case "email":
                if (tokens.length >= 2) {
                    newContact.email = tokens[1];
                    return "✅ Email: " + newContact.email + "\n" + getAddSuggestions();
                }
                return "Usage: email [email address]";
            default:
                return "❌ Unknown command\n" + getAddSuggestions();
        }
    }
    
    private String showContacts() {
        // Sort alphabetically
        contacts.sort((c1, c2) -> c1.name.compareToIgnoreCase(c2.name));
        
        StringBuilder sb = new StringBuilder();
        sb.append("👥 Contacts (").append(contacts.size()).append(" total)\n\n");
        
        for (int i = 0; i < contacts.size(); i++) {
            Contact contact = contacts.get(i);
            sb.append(i + 1).append(". ").append(contact.name)
              .append(" - ").append(contact.number)
              .append(contact.email != null ? " - " + contact.email : "")
              .append("\n");
        }
        
        sb.append("\n💡 Type: contact add (add new) | contact edit [name] | contact del (delete) | exit");
        
        return sb.toString();
    }
    
    private String saveContact() {
        if (newContact == null || newContact.name.isEmpty()) {
            return "❌ Contact must have a name";
        }
        
        // Use Shizuku to add contact to system
        if (addContactToSystem(newContact)) {
            contacts.add(newContact);
            currentState = ContactState.LIST_VIEW;
            return "✅ Contact saved: " + newContact.name + "\n" + showContacts();
        } else {
            return "❌ Failed to save contact";
        }
    }
    
    private boolean addContactToSystem(Contact contact) {
        // Use Shizuku to execute contact addition commands
        return ShizukuManager.executeCommand(
            "content insert --uri content://contacts/people --bind name:s:" + contact.name +
            " --bind phone:s:" + contact.number
        );
    }
}
