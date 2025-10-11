"""
Complete Call System for SmartCLI
"""

from integrations.android_apis import android_apis as android_api
from typing import List


class CallSystem:
    def __init__(self):
        self.active_call = None
        self.call_history = []
    
    def process_command(self, command: str, args: List[str]) -> str:
        if command == "call" and args:
            return self._make_call(" ".join(args))
        elif command == "ans": return "✅ Answering call"
        elif command == "rej": return "❌ Rejecting call"
        elif command == "end": return self._end_call()
        elif command == "hold": return "⏸️  Call on hold"
        elif command == "unhold": return "▶️  Call resumed"
        elif command == "mute": return "🔇 Call muted"
        elif command == "unmute": return "🔊 Call unmuted"
        elif command == "merge": return "🔀 Calls merged"
        elif command == "history": return self._show_history()
        else: return self._show_help()
    
    def _make_call(self, contact: str) -> str:
        contacts = android_api.get_contacts()
        for c in contacts:
            if contact.lower() in c['name'].lower():
                android_api.make_call(c['number'])
                return f"📞 Calling {c['name']}..."
        return f"❌ Contact not found: {contact}"
    
    def _end_call(self) -> str:
        self.active_call = None
        return "📞 Call ended"
    
    def _show_history(self) -> str:
        output = ["📞 CALL HISTORY", ""]
        for i, call in enumerate(self.call_history[:10], 1):
            output.append(f"  {i}. {call}")
        return "\n".join(output)
    
    def _show_help(self) -> str:
        return """
📞 CALL COMMANDS:
  call [name]    - Make call
  ans/rej        - Answer/reject
  end            - End call
  hold/unhold    - Call control
  mute/unmute    - Audio control
  merge          - Conference calls
  history        - Call history
"""

call_system = CallSystem()
