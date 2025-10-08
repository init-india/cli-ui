"""
Core CLI Engine - Command processing and execution
"""

import os
import subprocess
from typing import List, Dict, Any
from .history_manager import HistoryManager
from .window_manager import WindowManager

class CLIEngine:
    def __init__(self):
        self.history = HistoryManager()
        self.windows = WindowManager()
        self.current_dir = os.path.expanduser("~")
        self.modules = {}
        self.last_listing = []
        self.last_listing_path = ""
        self.init_commands()
    
    def init_commands(self):
        """Initialize all available commands"""
        self.commands = {
            # System commands
            'help': self.show_help,
            'clear': self.clear_screen,
            'exit': self.exit_app,
            'history': self.show_history,
            
            # File operations
            'ls': self.list_files,
            'cd': self.change_dir,
            'pwd': self.show_pwd,
            'cat': self.read_file,
            'mkdir': self.make_dir,
            'rm': self.remove_file,
            
            # Window management
            'windows': self.list_windows,
            'focus': self.focus_window,
            'close': self.close_window,
            'ps': self.show_processes,
            
            # System controls
            'wifi': self.toggle_wifi,
            'bluetooth': self.toggle_bluetooth,
            'volume': self.set_volume,
            'brightness': self.set_brightness,
            
            # Navigation
            'map': self.show_map,
            'nav': self.start_navigation,
        }
    
    def execute_command(self, command_input: str) -> str:
        """Execute command and return output"""
        # Add to history
        self.history.add(command_input)
        
        parts = command_input.strip().split()
        if not parts:
            return ""
        
        command = parts[0]
        args = parts[1:]
        
        if command in self.commands:
            try:
                return self.commands[command](args)
            except Exception as e:
                return f"Error executing {command}: {str(e)}"
        else:
            return f"Command not found: {command}\nType 'help' for available commands"
    
    def show_help(self, args: List[str]) -> str:
        """Display help information"""
        help_text = """
CLIUI - Touch Linux Mobile Interface

Basic Commands:
  ls, cd, pwd, cat, mkdir, rm    - File operations
  windows, focus, close, ps      - Window management
  wifi, bluetooth, volume        - System controls
  history, clear, help, exit     - System commands
  map, nav                       - Navigation

Touch Features:
  - Tap numbers to execute commands
  - Scroll with touch gestures
  - Swipe between windows
  - Long press for context menus

Type 'help <command>' for detailed information
"""
        return help_text
    
    def list_files(self, args: List[str]) -> str:
        """List directory contents with touch numbers"""
        path = self.current_dir
        if args:
            path = os.path.join(self.current_dir, args[0])
        
        try:
            items = os.listdir(path)
            output = f"📁 {path}\n"
            output += "─" * 40 + "\n"
            
            for i, item in enumerate(items, 1):
                full_path = os.path.join(path, item)
                icon = "📁" if os.path.isdir(full_path) else "📄"
                output += f"[{i}] {icon} {item}\n"
            
            output += "─" * 40 + "\n"
            output += "Touch number to open, or use cd/cat commands"
            
            # Store for touch handling
            self.last_listing = items
            self.last_listing_path = path
            
            return output
        except Exception as e:
            return f"Error: {str(e)}"
    
    def change_dir(self, args: List[str]) -> str:
        """Change directory with touch number support"""
        if not args:
            return "Usage: cd <directory> or cd <number>"
        
        target = args[0]
        
        # Handle touch number selection
        if target.isdigit() and hasattr(self, 'last_listing'):
            idx = int(target) - 1
            if 0 <= idx < len(self.last_listing):
                target = self.last_listing[idx]
        
        if target == "..":
            new_path = os.path.dirname(self.current_dir)
        else:
            new_path = os.path.join(self.current_dir, target)
        
        if os.path.exists(new_path) and os.path.isdir(new_path):
            self.current_dir = new_path
            return f"Changed to: {new_path}"
        else:
            return f"Directory not found: {target}"
    
    def show_pwd(self, args: List[str]) -> str:
        """Show current directory"""
        return f"Current directory: {self.current_dir}"
    
    def read_file(self, args: List[str]) -> str:
        """Read file content"""
        if not args:
            return "Usage: cat <filename> or cat <number>"
        
        filename = args[0]
        
        # Handle touch number selection
        if filename.isdigit() and hasattr(self, 'last_listing'):
            idx = int(filename) - 1
            if 0 <= idx < len(self.last_listing):
                filename = self.last_listing[idx]
        
        filepath = os.path.join(self.current_dir, filename)
        
        try:
            with open(filepath, 'r') as f:
                content = f.read()
            return f"📖 {filename}\n─" * 20 + f"\n{content}"
        except Exception as e:
            return f"Error reading file: {str(e)}"
    
    def make_dir(self, args: List[str]) -> str:
        """Create directory"""
        if not args:
            return "Usage: mkdir <directory_name>"
        
        dirpath = os.path.join(self.current_dir, args[0])
        os.makedirs(dirpath, exist_ok=True)
        return f"✅ Created directory: {args[0]}"
    
    def list_windows(self, args: List[str]) -> str:
        """List open windows/apps with touch numbers"""
        windows = self.windows.get_windows()
        output = "🪟 Open Windows\n"
        output += "─" * 40 + "\n"
        
        if not windows:
            output += "No windows open\n"
        else:
            for i, window in enumerate(windows, 1):
                status = "●" if window['active'] else "○"
                output += f"[{i}] {status} {window['name']} ({window['id']})\n"
        
        output += "─" * 40 + "\n"
        output += "Touch number to focus, or use 'focus <id>'"
        
        self.last_window_listing = windows
        return output
    
    def focus_window(self, args: List[str]) -> str:
        """Focus on a window by ID or touch number"""
        if not args:
            return "Usage: focus <window_id> or focus <number>"
        
        target = args[0]
        
        # Handle touch number selection
        if target.isdigit() and hasattr(self, 'last_window_listing'):
            idx = int(target) - 1
            if 0 <= idx < len(self.last_window_listing):
                target = self.last_window_listing[idx]['id']
        
        if self.windows.focus_window(target):
            return f"Focused window: {target}"
        else:
            return f"Window not found: {target}"
    
    def show_history(self, args: List[str]) -> str:
        """Show command history"""
        return self.history.get_history_display()
    
    def clear_screen(self, args: List[str]) -> str:
        """Clear screen"""
        return "CLEAR_SCREEN"
    
    def exit_app(self, args: List[str]) -> str:
        """Exit application"""
        return "EXIT_APP"
    
    def toggle_wifi(self, args: List[str]) -> str:
        """Toggle WiFi"""
        return "📶 WiFi toggle would be implemented here"
    
    def toggle_bluetooth(self, args: List[str]) -> str:
        """Toggle Bluetooth"""
        return "🔵 Bluetooth toggle would be implemented here"
    
    def set_volume(self, args: List[str]) -> str:
        """Set volume"""
        if args and args[0].isdigit():
            return f"🔊 Volume set to {args[0]}%"
        return "Usage: volume <0-100>"
    
    def set_brightness(self, args: List[str]) -> str:
        """Set brightness"""
        if args and args[0].isdigit():
            return f"💡 Brightness set to {args[0]}%"
        return "Usage: brightness <0-100>"
    
    def show_processes(self, args: List[str]) -> str:
        """Show running processes"""
        return "🔄 Process list would be implemented here"
    
    def close_window(self, args: List[str]) -> str:
        """Close window"""
        if not args:
            return "Usage: close <window_id> or close <number>"
        
        target = args[0]
        if target.isdigit() and hasattr(self, 'last_window_listing'):
            idx = int(target) - 1
            if 0 <= idx < len(self.last_window_listing):
                target = self.last_window_listing[idx]['id']
        
        if self.windows.close_window(target):
            return f"✅ Closed window: {target}"
        else:
            return f"Window not found: {target}"
    
    def remove_file(self, args: List[str]) -> str:
        """Remove file or directory"""
        if not args:
            return "Usage: rm <filename> or rm <number>"
        
        target = args[0]
        
        # Handle touch number selection
        if target.isdigit() and hasattr(self, 'last_listing'):
            idx = int(target) - 1
            if 0 <= idx < len(self.last_listing):
                target = self.last_listing[idx]
        
        target_path = os.path.join(self.current_dir, target)
        
        try:
            if os.path.isdir(target_path):
                return "Use 'rm -r' to remove directories"
            else:
                os.remove(target_path)
                return f"✅ Removed: {target}"
        except Exception as e:
            return f"Error removing: {str(e)}"
    
    def show_map(self, args: List[str]) -> str:
        """Show map functionality"""
        if not args:
            return "Usage: map <location> or map <source> <destination>"
        
        if len(args) == 1:
            return f"🗺️  Showing map for: {args[0]}\nType 'nav' to start navigation"
        else:
            return f"🗺️  Route: {args[0]} → {args[1]}\nType 'nav' to start navigation"
    
    def start_navigation(self, args: List[str]) -> str:
        """Start navigation"""
        return "🚗 Navigation started!\nVoice: 'In 200m, turn left'\nType 'exit' to stop navigation"
