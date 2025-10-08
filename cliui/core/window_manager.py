"""
Window Manager - Handle multiple apps/windows
"""

from typing import Any

class WindowManager:
    def __init__(self):
        self.windows = []
        self.active_window = None
        self.window_id_counter = 1
        
        # Create some sample windows for demo
        self.create_window("Terminal", "CLI Interface")
        self.create_window("File Manager", "File browser")
    
    def create_window(self, name: str, content: Any) -> str:
        """Create new window"""
        window_id = f"win_{self.window_id_counter}"
        self.window_id_counter += 1
        
        window = {
            'id': window_id,
            'name': name,
            'content': content,
            'active': False
        }
        
        self.windows.append(window)
        self.focus_window(window_id)
        return window_id
    
    def focus_window(self, window_id: str) -> bool:
        """Focus on specific window"""
        found = False
        for window in self.windows:
            if window['id'] == window_id:
                window['active'] = True
                self.active_window = window_id
                found = True
            else:
                window['active'] = False
        
        return found
    
    def close_window(self, window_id: str) -> bool:
        """Close window"""
        initial_count = len(self.windows)
        self.windows = [w for w in self.windows if w['id'] != window_id]
        
        if self.active_window == window_id:
            if self.windows:
                self.focus_window(self.windows[0]['id'])
            else:
                self.active_window = None
        
        return len(self.windows) < initial_count
    
    def get_windows(self) -> list:
        """Get list of all windows"""
        return self.windows.copy()
    
    def get_active_window(self) -> dict:
        """Get currently active window"""
        for window in self.windows:
            if window['active']:
                return window
        return None
    
    def switch_to_next(self) -> str:
        """Switch to next window"""
        if not self.windows:
            return None
        
        current_index = 0
        for i, window in enumerate(self.windows):
            if window['active']:
                current_index = i
                break
        
        next_index = (current_index + 1) % len(self.windows)
        next_window = self.windows[next_index]
        self.focus_window(next_window['id'])
        return next_window['id']
