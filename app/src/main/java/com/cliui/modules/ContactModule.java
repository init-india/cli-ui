package com.cliui.modules;

import android.content.Context;
import android.provider.ContactsContract;
import android.database.Cursor;
import android.net.Uri;
import android.content.ContentValues;
import com.cliui.utils.PermissionManager;

import com.cliui.utils.ShizukuManager;
import java.util.*;

public class ContactModule implements CommandModule {
    private Context context;
    private PermissionManager permissionManager;
    
    private ContactState currentState = ContactState.LIST_VIEW;
    private List<Contact> contacts = new ArrayList<>();
    private List<Integer> deleteSelection = new ArrayList<>();
    private Contact newContact = null;
    private Contact editContact = null;
    private String searchQuery = "";
    private int currentEditIndex = -1;
    
    public ContactModule(Context context) {
        this.context = context;
        this.permissionManager = PermissionManager.getInstance(context);
        loadContacts();
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

        // Require authentication for contact operations
        PermissionManager permissionManager = PermissionManager.getInstance(context);
if (!permissionManager.authenticate("contact_access")) {
            return "🔒 Authentication required for contact access\n" +
                   "Please authenticate to manage contacts";
        }
        
        if (command.equals("exit")) return handleExit();
        
        switch (currentState) {
            case LIST_VIEW: return handleListView(tokens);
            case ADD_MODE: return handleAddMode(tokens);
            case EDIT_MODE: return handleEditMode(tokens);
            case DELETE_MODE: return handleDeleteMode(tokens);
            case SEARCH_MODE: return handleSearchMode(tokens);
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
                switch (subCommand) {
                    case "add":
                        return startAddContact();
                    case "edit":
                        if (tokens.length >= 3) {
                            return startEditContact(String.join(" ", Arrays.copyOfRange(tokens, 2, tokens.length)));
                        }
                        return "❌ Usage: contact edit [name or number]";
                    case "del":
                    case "delete":
                        return enterDeleteMode();
                    case "search":
                        if (tokens.length >= 3) {
                            return startSearch(String.join(" ", Arrays.copyOfRange(tokens, 2, tokens.length)));
                        }
                        return "❌ Usage: contact search [query]";
                    case "backup":
                        return backupContacts();
                    case "restore":
                        return "🔧 Contact restore not implemented yet";
                    case "sync":
                        loadContacts();
                        return "✅ Contacts reloaded from system";
                    default:
                        return "❌ Unknown contact command: " + subCommand;
                }
            }
        }
        
        return getUsage();
    }
    
    private String handleAddMode(String[] tokens) {
        if (tokens.length == 0) return getAddSuggestions();
        
        String command = tokens[0].toLowerCase();
        
        switch (command) {
            case "save":
                return saveContact();
            case "cancel":
            case "exit":
                currentState = ContactState.LIST_VIEW;
                newContact = null;
                return "❌ Contact addition cancelled";
            case "name":
                if (tokens.length >= 2) {
                    newContact.name = String.join(" ", Arrays.copyOfRange(tokens, 1, tokens.length));
                    return "✅ Name: " + newContact.name + "\n" + getAddSuggestions();
                }
                return "❌ Usage: name [full name]";
            case "number":
                if (tokens.length >= 2) {
                    newContact.number = tokens[1];
                    return "✅ Number: " + newContact.number + "\n" + getAddSuggestions();
                }
                return "❌ Usage: number [phone number]";
            case "email":
                if (tokens.length >= 2) {
                    newContact.email = tokens[1];
                    return "✅ Email: " + newContact.email + "\n" + getAddSuggestions();
                }
                return "❌ Usage: email [email address]";
            case "company":
                if (tokens.length >= 2) {
                    newContact.company = String.join(" ", Arrays.copyOfRange(tokens, 1, tokens.length));
                    return "✅ Company: " + newContact.company + "\n" + getAddSuggestions();
                }
                return "❌ Usage: company [company name]";
            case "title":
                if (tokens.length >= 2) {
                    newContact.title = String.join(" ", Arrays.copyOfRange(tokens, 1, tokens.length));
                    return "✅ Title: " + newContact.title + "\n" + getAddSuggestions();
                }
                return "❌ Usage: title [job title]";
            case "status":
                return getCurrentContactStatus();
            default:
                return "❌ Unknown command\n" + getAddSuggestions();
        }
    }
    
    private String handleEditMode(String[] tokens) {
        if (editContact == null) {
            currentState = ContactState.LIST_VIEW;
            return "❌ No contact selected for editing";
        }
        
        if (tokens.length == 0) return getEditSuggestions();
        
        String command = tokens[0].toLowerCase();
        
        switch (command) {
            case "save":
                return updateContact();
            case "cancel":
            case "exit":
                currentState = ContactState.LIST_VIEW;
                editContact = null;
                return "❌ Contact editing cancelled";
            case "name":
                if (tokens.length >= 2) {
                    editContact.name = String.join(" ", Arrays.copyOfRange(tokens, 1, tokens.length));
                    return "✅ Name: " + editContact.name + "\n" + getEditSuggestions();
                }
                return "❌ Usage: name [full name]";
            case "number":
                if (tokens.length >= 2) {
                    editContact.number = tokens[1];
                    return "✅ Number: " + editContact.number + "\n" + getEditSuggestions();
                }
                return "❌ Usage: number [phone number]";
            case "email":
                if (tokens.length >= 2) {
                    editContact.email = tokens[1];
                    return "✅ Email: " + editContact.email + "\n" + getEditSuggestions();
                }
                return "❌ Usage: email [email address]";
            case "company":
                if (tokens.length >= 2) {
                    editContact.company = String.join(" ", Arrays.copyOfRange(tokens, 1, tokens.length));
                    return "✅ Company: " + editContact.company + "\n" + getEditSuggestions();
                }
                return "❌ Usage: company [company name]";
            case "title":
                if (tokens.length >= 2) {
                    editContact.title = String.join(" ", Arrays.copyOfRange(tokens, 1, tokens.length));
                    return "✅ Title: " + editContact.title + "\n" + getEditSuggestions();
                }
                return "❌ Usage: title [job title]";
            case "delete":
                return deleteCurrentContact();
            case "status":
                return getEditContactStatus();
            default:
                return "❌ Unknown command\n" + getEditSuggestions();
        }
    }
    
    private String handleDeleteMode(String[] tokens) {
        if (tokens.length == 0) return getDeleteSuggestions();
        
        String command = tokens[0].toLowerCase();
        
        switch (command) {
            case "done":
            case "save":
                return executeDeletion();
            case "cancel":
            case "exit":
                currentState = ContactState.LIST_VIEW;
                deleteSelection.clear();
                return "❌ Deletion cancelled";
            case "clear":
                deleteSelection.clear();
                return "✅ Selection cleared\n" + getDeleteSuggestions();
            case "all":
                deleteSelection.clear();
                for (int i = 0; i < contacts.size(); i++) {
                    deleteSelection.add(i);
                }
                return "✅ All " + contacts.size() + " contacts selected for deletion\n" + getDeleteSuggestions();
            default:
                // Handle number selection
                try {
                    int index = Integer.parseInt(command) - 1;
                    if (index >= 0 && index < contacts.size()) {
                        if (deleteSelection.contains(index)) {
                            deleteSelection.remove((Integer) index);
                            return "❌ Removed from selection: " + contacts.get(index).name + "\n" + getDeleteSuggestions();
                        } else {
                            deleteSelection.add(index);
                            return "✅ Added to selection: " + contacts.get(index).name + "\n" + getDeleteSuggestions();
                        }
                    }
                } catch (NumberFormatException e) {
                    // Not a number, continue to error
                }
                return "❌ Unknown command\n" + getDeleteSuggestions();
        }
    }
    
    private String handleSearchMode(String[] tokens) {
        if (tokens.length == 0) return getSearchResults();
        
        String command = tokens[0].toLowerCase();
        
        switch (command) {
            case "exit":
            case "back":
                currentState = ContactState.LIST_VIEW;
                searchQuery = "";
                return "✅ Exited search mode";
            case "clear":
                searchQuery = "";
                return getSearchResults();
            default:
                // New search query
                searchQuery = String.join(" ", tokens);
                return getSearchResults();
        }
    }
    
    // ===== Core Contact Operations =====
    
    private String startAddContact() {
        if (!permissionManager.areAllPermissionsGranted(PermissionManager.CONTACT_PERMISSIONS)) {
            return "❌ Contact write permission required\n" +
                   "Need permission to add new contacts to system";
        }
        
        newContact = new Contact();
        currentState = ContactState.ADD_MODE;
        return "👥 Adding New Contact\n" +
               "💡 Available commands:\n" +
               "name [full name] | number [phone] | email [address]\n" +
               "company [name] | title [job title]\n" +
               "status (show progress) | save (save contact) | cancel (exit)";
    }
    
    private String startEditContact(String query) {
        if (!permissionManager.areAllPermissionsGranted(PermissionManager.CONTACT_PERMISSIONS)) {
            return "❌ Contact write permission required\n" +
                   "Need permission to edit contacts in system";
        }
        
        // Find contact by name or number
        for (int i = 0; i < contacts.size(); i++) {
            Contact contact = contacts.get(i);
            if (contact.name.toLowerCase().contains(query.toLowerCase()) ||
                (contact.number != null && contact.number.contains(query))) {
                editContact = new Contact(contact); // Create copy for editing
                currentEditIndex = i;
                currentState = ContactState.EDIT_MODE;
                return "✏️ Editing Contact: " + contact.name + "\n" + getEditSuggestions();
            }
        }
        return "❌ Contact not found: " + query;
    }
    
    private String enterDeleteMode() {
        if (!permissionManager.areAllPermissionsGranted(PermissionManager.CONTACT_PERMISSIONS)) {
            return "❌ Contact write permission required\n" +
                   "Need permission to delete contacts from system";
        }
        
        currentState = ContactState.DELETE_MODE;
        deleteSelection.clear();
        return showContacts() + "\n\n" + getDeleteSuggestions();
    }
    
    private String startSearch(String query) {
        searchQuery = query;
        currentState = ContactState.SEARCH_MODE;
        return getSearchResults();
    }
    
    private String saveContact() {
        if (newContact == null || newContact.name.isEmpty()) {
            return "❌ Contact must have a name";
        }
        
        // Try Shizuku first for system-level contact addition
        if (ShizukuManager.isAvailable()) {
            if (addContactViaShizuku(newContact)) {
                contacts.add(new Contact(newContact));
                currentState = ContactState.LIST_VIEW;
                newContact = null;
                return "✅ Contact saved via Shizuku: " + newContact.name + "\n" + showContacts();
            }
        }
        
        // Fallback to Android ContactsContract
        if (addContactViaAndroid(newContact)) {
            contacts.add(new Contact(newContact));
            currentState = ContactState.LIST_VIEW;
            newContact = null;
            return "✅ Contact saved: " + newContact.name + "\n" + showContacts();
        } else {
            return "❌ Failed to save contact to system";
        }
    }
    
    private String updateContact() {
        if (editContact == null || currentEditIndex == -1) {
            return "❌ No contact to update";
        }
        
        // Try Shizuku first
        if (ShizukuManager.isAvailable()) {
            if (updateContactViaShizuku(editContact)) {
                contacts.set(currentEditIndex, new Contact(editContact));
                currentState = ContactState.LIST_VIEW;
                editContact = null;
                currentEditIndex = -1;
                return "✅ Contact updated via Shizuku: " + editContact.name;
            }
        }
        
        // Fallback to Android ContactsContract
        if (updateContactViaAndroid(editContact)) {
            contacts.set(currentEditIndex, new Contact(editContact));
            currentState = ContactState.LIST_VIEW;
            editContact = null;
            currentEditIndex = -1;
            return "✅ Contact updated: " + editContact.name;
        } else {
            return "❌ Failed to update contact in system";
        }
    }
    
    private String deleteCurrentContact() {
        if (editContact == null || currentEditIndex == -1) {
            return "❌ No contact to delete";
        }
        
        String contactName = editContact.name;
        
        // Try Shizuku first
        if (ShizukuManager.isAvailable()) {
            if (deleteContactViaShizuku(editContact)) {
                contacts.remove(currentEditIndex);
                currentState = ContactState.LIST_VIEW;
                editContact = null;
                currentEditIndex = -1;
                return "✅ Contact deleted via Shizuku: " + contactName;
            }
        }
        
        // Fallback to Android ContactsContract
        if (deleteContactViaAndroid(editContact)) {
            contacts.remove(currentEditIndex);
            currentState = ContactState.LIST_VIEW;
            editContact = null;
            currentEditIndex = -1;
            return "✅ Contact deleted: " + contactName;
        } else {
            return "❌ Failed to delete contact from system";
        }
    }
    
    private String executeDeletion() {
        if (deleteSelection.isEmpty()) {
            return "❌ No contacts selected for deletion";
        }
        
        int successCount = 0;
        List<Contact> toRemove = new ArrayList<>();
        
        // Sort in descending order to maintain indices during removal
        deleteSelection.sort(Collections.reverseOrder());
        
        for (int index : deleteSelection) {
            if (index < contacts.size()) {
                Contact contact = contacts.get(index);
                
                // Try to delete from system
                boolean deleted = ShizukuManager.isAvailable() ? 
                    deleteContactViaShizuku(contact) : deleteContactViaAndroid(contact);
                
                if (deleted) {
                    toRemove.add(contact);
                    successCount++;
                }
            }
        }
        
        // Remove from local list
        contacts.removeAll(toRemove);
        deleteSelection.clear();
        currentState = ContactState.LIST_VIEW;
        
        return "✅ Deleted " + successCount + " contacts\n" + showContacts();
    }
    
    // ===== Shizuku Integration =====
    
    private boolean addContactViaShizuku(Contact contact) {
        String command = String.format(
            "content insert --uri content://com.android.contacts/raw_contacts --bind account_type:s:com.google --bind account_name:s:null && " +
            "content insert --uri content://com.android.contacts/data --bind raw_contact_id:i:$RAW_CONTACT_ID --bind mimetype:s:vnd.android.cursor.item/name --bind data1:s:\"%s\" && " +
            "content insert --uri content://com.android.contacts/data --bind raw_contact_id:i:$RAW_CONTACT_ID --bind mimetype:s:vnd.android.cursor.item/phone_v2 --bind data1:s:\"%s\" --bind data2:i:2",
            contact.name.replace("\"", "\\\""),
            contact.number != null ? contact.number : ""
        );
        
        return ShizukuManager.executeCommand(command);
    }
    
    private boolean updateContactViaShizuku(Contact contact) {
        // This is simplified - real implementation would need contact ID
        String command = String.format(
            "content update --uri content://com.android.contacts/data --bind data1:s:\"%s\" --where \"mimetype='vnd.android.cursor.item/name'\"",
            contact.name.replace("\"", "\\\"")
        );
        
        return ShizukuManager.executeCommand(command);
    }
    
    private boolean deleteContactViaShizuku(Contact contact) {
        // Simplified - would need contact lookup by name/number
        String command = String.format(
            "content delete --uri content://com.android.contacts/raw_contacts --where \"display_name='%s'\"",
            contact.name.replace("'", "\\'")
        );
        
        return ShizukuManager.executeCommand(command);
    }
    
    // ===== Android ContactsContract Integration =====
    
    private boolean addContactViaAndroid(Contact contact) {
        try {
            ArrayList<ContentValues> data = new ArrayList<>();
            
            // Name
            if (contact.name != null && !contact.name.isEmpty()) {
                ContentValues name = new ContentValues();
                name.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
                name.put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.name);
                data.add(name);
            }
            
            // Phone
            if (contact.number != null && !contact.number.isEmpty()) {
                ContentValues phone = new ContentValues();
                phone.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
                phone.put(ContactsContract.CommonDataKinds.Phone.NUMBER, contact.number);
                phone.put(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
                data.add(phone);
            }
            
            // Email
            if (contact.email != null && !contact.email.isEmpty()) {
                ContentValues email = new ContentValues();
                email.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);
                email.put(ContactsContract.CommonDataKinds.Email.ADDRESS, contact.email);
                email.put(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK);
                data.add(email);
            }
            
            // Insert the contact
            Uri rawContactUri = context.getContentResolver().insert(ContactsContract.RawContacts.CONTENT_URI, new ContentValues());
            if (rawContactUri != null) {
                long rawContactId = Long.parseLong(rawContactUri.getLastPathSegment());
                
                for (ContentValues values : data) {
                    values.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
                    context.getContentResolver().insert(ContactsContract.Data.CONTENT_URI, values);
                }
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    
    private boolean updateContactViaAndroid(Contact contact) {
        // Implementation would query and update existing contact
        // Simplified for this example
        return false;
    }
    
    private boolean deleteContactViaAndroid(Contact contact) {
        try {
            // Query for the contact
            Cursor cursor = context.getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI,
                new String[]{ContactsContract.Contacts._ID},
                ContactsContract.Contacts.DISPLAY_NAME + " = ?",
                new String[]{contact.name},
                null
            );
            
            if (cursor != null && cursor.moveToFirst()) {
                long contactId = cursor.getLong(0);
                cursor.close();
                
                // Delete the contact
                Uri contactUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, String.valueOf(contactId));
                int deleted = context.getContentResolver().delete(contactUri, null, null);
                return deleted > 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    
    // ===== UI/Display Methods =====
    
    private String showContacts() {
        if (contacts.isEmpty()) {
            return "📱 No contacts found\n💡 Type 'contact add' to create your first contact";
        }
        
        // Sort alphabetically
        contacts.sort((c1, c2) -> c1.name.compareToIgnoreCase(c2.name));
        
        StringBuilder sb = new StringBuilder();
        sb.append("👥 Contacts (").append(contacts.size()).append(" total)\n\n");
        
        for (int i = 0; i < contacts.size(); i++) {
            Contact contact = contacts.get(i);
            sb.append(i + 1).append(". ").append(contact.getDisplayString()).append("\n");
        }
        
        sb.append("\n💡 Commands: contact add | contact edit [name] | contact del | contact search [query]");
        
        return sb.toString();
    }
    
    private String getSearchResults() {
        if (searchQuery.isEmpty()) {
            return "🔍 Search Mode\n💡 Type search terms or 'exit' to return";
        }
        
        List<Contact> results = new ArrayList<>();
        for (Contact contact : contacts) {
            if (contact.matchesSearch(searchQuery)) {
                results.add(contact);
            }
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("🔍 Search Results for \"").append(searchQuery).append("\" (").append(results.size()).append(" found)\n\n");
        
        for (int i = 0; i < results.size(); i++) {
            Contact contact = results.get(i);
            sb.append(i + 1).append(". ").append(contact.getDisplayString()).append("\n");
        }
        
        sb.append("\n💡 Type new search terms or 'exit' to return to list");
        
        return sb.toString();
    }
    
    private String backupContacts() {
        if (!ShizukuManager.isAvailable()) {
            return "❌ Shizuku required for contact backup\n" +
                   "Backup feature needs system-level file access";
        }
        
        String backupCommand = "cp /data/data/com.android.providers.contacts/databases/contacts2.db /sdcard/contacts_backup.db";
        if (ShizukuManager.executeCommand(backupCommand)) {
            return "✅ Contacts backed up to /sdcard/contacts_backup.db";
        } else {
            return "❌ Backup failed - may require root access";
        }
    }
    
    private void loadContacts() {
        contacts.clear();
        
        if (!permissionManager.areAllPermissionsGranted(PermissionManager.CONTACT_PERMISSIONS)) {
            return;
        }
        
        try {
            Cursor cursor = context.getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI,
                new String[]{
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME
                },
                null, null, ContactsContract.Contacts.DISPLAY_NAME + " ASC"
            );
            
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                    String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                    
                    Contact contact = new Contact();
                    contact.name = name;
                    
                    // Get phone numbers
                    Cursor phoneCursor = context.getContentResolver().query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        new String[]{id},
                        null
                    );
                    
                    if (phoneCursor != null && phoneCursor.moveToFirst()) {
                        contact.number = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        phoneCursor.close();
                    }
                    
                    // Get email
                    Cursor emailCursor = context.getContentResolver().query(
                        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                        new String[]{id},
                        null
                    );
                    
                    if (emailCursor != null && emailCursor.moveToFirst()) {
                        contact.email = emailCursor.getString(emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS));
                        emailCursor.close();
                    }
                    
                    contacts.add(contact);
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // ===== Suggestion Methods =====
    
    private String getUsage() {
        return "👥 Contact Module Usage:\n" +
               "• contact                 - Show all contacts\n" +
               "• contact add             - Add new contact\n" +
               "• contact edit [name]     - Edit specific contact\n" +
               "• contact del             - Delete contacts (multi-select)\n" +
               "• contact search [query]  - Search contacts\n" +
               "• contact backup          - Backup contacts (Shizuku)\n" +
               "• contact sync            - Reload from system\n" +
               "• exit                    - Exit current mode\n" +
               "\n🔒 Requires: Contact permissions + Authentication" +
               "\n🔧 Shizuku: " + (ShizukuManager.isAvailable() ? "Available ✅" : "Not Available ❌");
    }
    
    private String getAddSuggestions() {
        StringBuilder sb = new StringBuilder();
        sb.append("👥 Adding Contact - Current Fields:\n");
        sb.append(newContact != null ? newContact.getFieldStatus() : "No contact data");
        sb.append("\n\n💡 Commands: name | number | email | company | title\n");
        sb.append("💡 Actions: save (save contact) | status (show progress) | cancel (exit)");
        return sb.toString();
    }
    
    private String getEditSuggestions() {
        if (editContact == null) return "No contact selected for editing";
        
        StringBuilder sb = new StringBuilder();
        sb.append("✏️ Editing: ").append(editContact.name).append("\n");
        sb.append("Current Fields:\n");
        sb.append(editContact.getFieldStatus());
        sb.append("\n\n💡 Commands: name | number | email | company | title\n");
        sb.append("💡 Actions: save (update) | delete (remove) | status (show) | cancel (exit)");
        return sb.toString();
    }
    
    private String getDeleteSuggestions() {
        return "🗑️ Delete Mode - Selected: " + deleteSelection.size() + " contacts\n" +
               "💡 Click numbers to select/deselect contacts\n" +
               "💡 Commands: all (select all) | clear (clear selection)\n" +
               "💡 Actions: done (delete selected) | cancel (exit)";
    }
    
    private String getCurrentContactStatus() {
        return newContact != null ? newContact.getFieldStatus() : "No contact in progress";
    }
    
    private String getEditContactStatus() {
        return editContact != null ? editContact.getFieldStatus() : "No contact being edited";
    }
    
    private String handleExit() {
        switch (currentState) {
            case ADD_MODE:
                newContact = null;
                break;
            case EDIT_MODE:
                editContact = null;
                currentEditIndex = -1;
                break;
            case DELETE_MODE:
                deleteSelection.clear();
                break;
            case SEARCH_MODE:
                searchQuery = "";
                break;
        }
        currentState = ContactState.LIST_VIEW;
        return "✅ Returned to contact list";
    }
    
    // ===== Inner Classes =====
    
    enum ContactState {
        LIST_VIEW, ADD_MODE, EDIT_MODE, DELETE_MODE, SEARCH_MODE
    }
    
    class Contact {
        String name = "";
        String number = "";
        String email = "";
        String company = "";
        String title = "";
        String address = "";
        String notes = "";
        
        Contact() {}
        
        Contact(Contact other) {
            this.name = other.name;
            this.number = other.number;
            this.email = other.email;
            this.company = other.company;
            this.title = other.title;
            this.address = other.address;
            this.notes = other.notes;
        }
        
        String getDisplayString() {
            StringBuilder sb = new StringBuilder();
            sb.append(name);
            if (number != null && !number.isEmpty()) {
                sb.append(" 📞 ").append(number);
            }
            if (email != null && !email.isEmpty()) {
                sb.append(" 📧 ").append(email);
            }
            if (company != null && !company.isEmpty()) {
                sb.append(" 🏢 ").append(company);
            }
            return sb.toString();
        }
        
        String getFieldStatus() {
            StringBuilder sb = new StringBuilder();
            sb.append("• Name: ").append(name.isEmpty() ? "❌ Not set" : "✅ " + name).append("\n");
            sb.append("• Number: ").append(number.isEmpty() ? "❌ Not set" : "✅ " + number).append("\n");
            sb.append("• Email: ").append(email.isEmpty() ? "❌ Not set" : "✅ " + email).append("\n");
            if (!company.isEmpty()) sb.append("• Company: ").append(company).append("\n");
            if (!title.isEmpty()) sb.append("• Title: ").append(title).append("\n");
            return sb.toString();
        }
        
        boolean matchesSearch(String query) {
            String lowerQuery = query.toLowerCase();
            return name.toLowerCase().contains(lowerQuery) ||
                   (number != null && number.contains(query)) ||
                   (email != null && email.toLowerCase().contains(lowerQuery)) ||
                   (company != null && company.toLowerCase().contains(lowerQuery));
        }
    }
}
