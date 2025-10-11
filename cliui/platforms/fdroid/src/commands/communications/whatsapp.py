"""
Complete WhatsApp System for SmartCLI
"""

from integrations.android_apis import android_apis as android_api
from typing import List


class WhatsAppSystem:
    def process_command(self, command: str, args: List[str]) -> str:
        if command == "send" and len(args) >= 2:
            return self._send_message(args[0], " ".join(args[1:]))
        elif command == "list": return self._list_chats()
        elif command == "call" and args: return f"📞 WhatsApp call to {args[0]}"
        else: return self._show_help()
    
    def _send_message(self, contact: str, message: str) -> str:
        contacts = android_api.get_contacts()
        for c in contacts:
            if contact.lower() in c['name'].lower():
                android_api.send_whatsapp(c['number'], message)
                return f"💚 WhatsApp to {c['name']}: {message}"
        return f"❌ Contact not found: {contact}"
    
    def _list_chats(self) -> str:
        chats = android_api.get_whatsapp_chats()
        output = ["💚 WHATSAPP CHATS:", ""]
        for chat in chats:
            status = "🔵" if chat['unread'] > 0 else "⚪"
            output.append(f"  {chat['name']:12} - {chat['last_message']} [{chat['time']}] {status}")
        return "\n".join(output)
    
    def _show_help(self) -> str:
        return """
💚 WHATSAPP COMMANDS:
  send [name] [msg] - Send message
  list              - Show chats
  call [name]       - Voice/Video call
"""

whatsapp_system = WhatsAppSystem()
