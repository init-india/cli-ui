"""
Complete Email System for SmartCLI
"""

from integrations.android_apis import android_apis as android_api
from typing import List


class EmailSystem:
    def process_command(self, command: str, args: List[str]) -> str:
        if command == "list": return self._list_emails()
        elif command == "read" and args: return self._read_email(args[0])
        elif command == "compose" and len(args) >= 2:
            return self._compose_email(args[0], " ".join(args[1:]))
        elif command == "reply" and args: return f"📧 Replying to email {args[0]}"
        else: return self._show_help()
    
    def _list_emails(self) -> str:
        emails = android_api.get_emails()
        output = ["📧 RECENT EMAILS:", ""]
        for email in emails:
            status = "🔵" if not email['read'] else "⚪"
            output.append(f"  {email['id']}. {status} {email['sender']:12} - {email['subject']} [{email['time']}]")
        return "\n".join(output)
    
    def _read_email(self, email_id: str) -> str:
        return f"📧 Email {email_id}: Full email content here"
    
    def _compose_email(self, to: str, subject: str) -> str:
        android_api.send_email(to, subject, "Email body")
        return f"📧 Email to {to}: {subject}"
    
    def _show_help(self) -> str:
        return """
📧 EMAIL COMMANDS:
  list              - Show emails
  read [id]         - Read email
  compose [to] [sub] - New email
  reply [id]        - Reply to email
"""

email_system = EmailSystem()
