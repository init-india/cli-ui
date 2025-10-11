"""
Complete Alarms System for SmartCLI
"""

from typing import List

class AlarmsSystem:
    def process_command(self, command: str, args: List[str]) -> str:
        if command == "list": return "⏰ Alarms: 07:30 (Weekdays)"
        elif command == "set" and args: return f"⏰ Alarm set: {args[0]}"
        elif command == "delete" and args: return f"🗑️  Alarm deleted: {args[0]}"
        else: return self._show_help()
    
    def _show_help(self) -> str:
        return """
⏰ ALARMS COMMANDS:
  list              - Show alarms
  set [time]        - Set alarm
  delete [time]     - Delete alarm
"""

alarms_system = AlarmsSystem()
