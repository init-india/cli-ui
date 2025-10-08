"""
History Manager - Scrollable command history
"""

import time
from typing import List, Dict

class HistoryManager:
    def __init__(self, max_size: int = 1000):
        self.history = []
        self.max_size = max_size
        self.current_position = 0
    
    def add(self, command: str):
        """Add command to history"""
        entry = {
            'timestamp': time.time(),
            'command': command,
            'id': len(self.history) + 1
        }
        
        self.history.append(entry)
        
        # Maintain max size
        if len(self.history) > self.max_size:
            self.history.pop(0)
        
        # Reset navigation position
        self.current_position = len(self.history)
    
    def get_previous(self) -> str:
        """Get previous command in history"""
        if not self.history:
            return ""
        
        self.current_position = max(0, self.current_position - 1)
        if self.current_position < len(self.history):
            return self.history[self.current_position]['command']
        return ""
    
    def get_next(self) -> str:
        """Get next command in history"""
        if not self.history:
            return ""
        
        self.current_position = min(len(self.history), self.current_position + 1)
        if self.current_position < len(self.history):
            return self.history[self.current_position]['command']
        return ""
    
    def search(self, query: str) -> List[Dict]:
        """Search history for commands"""
        results = []
        for entry in self.history:
            if query.lower() in entry['command'].lower():
                results.append(entry)
        return results
    
    def get_history_display(self, limit: int = 20) -> str:
        """Get formatted history for display"""
        if not self.history:
            return "No command history"
        
        display_history = self.history[-limit:]  # Most recent first
        output = "📜 Command History\n"
        output += "─" * 50 + "\n"
        
        for entry in reversed(display_history):
            time_str = time.strftime('%H:%M:%S', time.localtime(entry['timestamp']))
            output += f"[{entry['id']}] {time_str} > {entry['command']}\n"
        
        output += "─" * 50 + "\n"
        output += f"Showing {len(display_history)} of {len(self.history)} commands\n"
        output += "Touch number to execute, scroll for more"
        
        return output
    
    def clear(self):
        """Clear command history"""
        self.history.clear()
        self.current_position = 0
