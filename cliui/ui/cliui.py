"""
Main CLIUI Interface - Touch-enabled TUI
"""

import os
import sys
from typing import List, Dict
from .scroll_view import ScrollView
from .status_bar import StatusBar

class CLIUI:
    def __init__(self, cli_engine):
        self.cli_engine = cli_engine
        self.scroll_view = ScrollView()
        self.status_bar = StatusBar()
        self.current_input = ""
        self.output_buffer = ["🚀 CLIUI Started! Type 'help' for commands"]
        
    def clear_screen(self):
        """Clear the terminal screen"""
        os.system('clear' if os.name == 'posix' else 'cls')
    
    def display_interface(self):
        """Display the complete CLI interface"""
        self.clear_screen()
        
        # Status bar
        print("╭" + "─" * 78 + "╮")
        self.status_bar.display()
        print("├" + "─" * 78 + "┤")
        
        # Output area with scrolling
        self.scroll_view.display(self.output_buffer)
        
        # Input area
        print("├" + "─" * 78 + "┤")
        print(f"> {self.current_input}_")
        print("╰" + "─" * 78 + "╯")
        print("Touch: [1-9] Select | [↑↓] History | [↔] Scroll | [⏎] Execute")
    
    def handle_touch(self, x: int, y: int):
        """Handle touch input at coordinates"""
        # Map screen coordinates to actions
        if y < 3:  # Status bar area
            self.handle_status_bar_touch(x, y)
        elif y < 20:  # Output area
            self.handle_output_touch(x, y)
        else:  # Input area
            self.focus_input()
    
    def handle_output_touch(self, x: int, y: int):
        """Handle touch in output area for number selection"""
        # Simple simulation - in real app, use proper coordinate mapping
        visible_content = self.scroll_view.get_visible_content()
        
        # Simple number detection
        line_index = y - 3  # Adjust for header
        if 0 <= line_index < len(visible_content):
            line = visible_content[line_index]
            # Look for [number] pattern
            import re
            matches = re.findall(r'\[(\d+)\]', line)
            if matches:
                number = matches[0]
                self.execute_touch_command(number)
    
    def execute_touch_command(self, number: str):
        """Execute command based on touch number"""
        last_output = self.output_buffer[-1] if self.output_buffer else ""
        
        if "📁" in last_output:  # File listing context
            self.current_input = f"cd {number}"
        elif "🪟" in last_output:  # Window listing context
            self.current_input = f"focus {number}"
        elif "📜" in last_output:  # History context
            self.current_input = f"!{number}"
        elif "🗺️" in last_output:  # Map context
            self.current_input = f"nav"
        
        self.execute_command()
    
    def execute_command(self):
        """Execute current input command"""
        if not self.current_input.strip():
            return
        
        result = self.cli_engine.execute_command(self.current_input)
        
        # Handle special commands
        if result == "CLEAR_SCREEN":
            self.output_buffer.clear()
            self.output_buffer.append("Screen cleared")
        elif result == "EXIT_APP":
            print("Goodbye!")
            sys.exit(0)
        else:
            self.output_buffer.append(f"> {self.current_input}")
            if result:  # Only add if there's actual output
                self.output_buffer.extend(result.split('\n'))
        
        # Auto-scroll to bottom
        self.scroll_view.scroll_to_bottom()
        
        self.current_input = ""
    
    def run(self):
        """Main application loop"""
        try:
            while True:
                self.display_interface()
                
                # Get user input
                try:
                    user_input = input().strip()
                except (KeyboardInterrupt, EOFError):
                    break
                
                if user_input.lower() in ['exit', 'quit']:
                    break
                elif user_input == '':
                    self.current_input = ""
                elif user_input.startswith('touch:'):
                    # Simulate touch input (for testing)
                    coords = user_input[6:].split(',')
                    if len(coords) == 2:
                        x, y = int(coords[0]), int(coords[1])
                        self.handle_touch(x, y)
                else:
                    self.current_input = user_input
                    self.execute_command()
        
        except Exception as e:
            print(f"Error: {e}")
        
        print("Goodbye!")
