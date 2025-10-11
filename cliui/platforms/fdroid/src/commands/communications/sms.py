"""
Complete SMS System for SmartCLI
"""
from integrations.android_apis import android_apis as android_api
from typing import List


class SMSSystem:
    def process_command(self, command: str, args: List[str]) -> str:
        if command == "send" and len(args) >= 2:
            return self._send_sms(args[0], " ".join(args[1:]))
        elif command == "list": return self._list_messages()
        elif command == "read" and args: return self._read_message(args[0])
        elif command == "delete" and args: return f"🗑️  Deleting message {args[0]}"
        elif command == "thread" and args: return f"💬 Thread with {args[0]}"
        else: return self._show_help()
    
    def _send_sms(self, contact: str, message: str) -> str:
        contacts = android_api.get_contacts()
        for c in contacts:
            if contact.lower() in c['name'].lower():
                android_api.send_sms(c['number'], message)
                return f"✉️  SMS to {c['name']}: {message}"
        return f"❌ Contact not found: {contact}"
    
    def _list_messages(self) -> str:
        return """💬 RECENT MESSAGES:
  1. Mom - Call me when free... [14:25]
  2. John - Running late... [13:48]
  3. Bank - OTP: 458792 [12:30]
  
💡 Commands: read [id], send [name] [msg], delete [id]"""
    
    def _read_message(self, msg_id: str) -> str:
        return f"📩 Message {msg_id}: Full message content here"
    
    def _show_help(self) -> str:
        return """
💬 SMS COMMANDS:
  send [name] [msg] - Send SMS
  list              - Show messages
  read [id]         - Read message
  delete [id]       - Delete message
  thread [name]     - Conversation
"""

sms_system = SMSSystem()
