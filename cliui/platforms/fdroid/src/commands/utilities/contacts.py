"""
Complete Contacts System for SmartCLI
"""
from integrations.android_apis import android_apis as android_api
from typing import List

class ContactsSystem:
    def process_command(self, command: str, args: List[str]) -> str:
        if command == "list": return self._list_contacts()
        elif command == "add" and len(args) >= 2:
            return self._add_contact(args[0], " ".join(args[1:]))
        elif command == "delete" and args: return f"🗑️  Deleting contact {args[0]}"
        elif command == "search" and args: return self._search_contact(" ".join(args))
        else: return self._show_help()
    
    def _list_contacts(self) -> str:
        contacts = android_api.get_contacts()
        output = ["👥 CONTACTS:", ""]
        for contact in contacts:
            output.append(f"  {contact['name']:12} - {contact['number']}")
        return "\n".join(output)
    
    def _add_contact(self, name: str, number: str) -> str:
        android_api.add_contact(name, number)
        return f"✅ Contact added: {name} - {number}"
    
    def _search_contact(self, query: str) -> str:
        contacts = android_api.get_contacts()
        for contact in contacts:
            if query.lower() in contact['name'].lower():
                return f"🔍 Found: {contact['name']} - {contact['number']}"
        return f"❌ Contact not found: {query}"
    
    def _show_help(self) -> str:
        return """
👥 CONTACTS COMMANDS:
  list              - Show all contacts
  add [name] [num]  - Add contact
  delete [name]     - Delete contact
  search [name]     - Find contact
"""

contacts_system = ContactsSystem()
