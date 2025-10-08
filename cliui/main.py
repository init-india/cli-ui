#!/usr/bin/env python3
"""
CLIUI Mobile Launcher - With Real Mobile APIs
"""

import os
import sys
from core.mobile_engine import MobileCLIEngine
from core.android_bridge import AndroidBridge
from ui.mobile_interface import MobileCLIInterface
from modules.daily_routines import DailyRoutines

class CLIUILauncher:
    def __init__(self):
        self.mode = "locked"
        self.android = AndroidBridge()
        self.cli_engine = MobileCLIEngine(self.android)
        self.interface = MobileCLIInterface()
        self.routines = DailyRoutines(self.cli_engine)
        
    def start(self):
        """Start with mobile optimizations"""
        print("📱 CLIUI Mobile Launcher v1.0")
        print(f"Platform: {self.android.platform}")
        
        if self.authenticate():
            self.run_launcher()
        else:
            print("Authentication failed")
    
    def authenticate(self):
        """Mobile-optimized authentication"""
        # For now, simple PIN check
        print("Enter PIN (1234 for demo):")
        pin = input("> ")
        return pin == "1234"
    
    def run_launcher(self):
        """Main mobile interface loop"""
        while True:
            self.interface.display_home_screen()
            command = self.interface.get_input()
            
            if command == "exit":
                break
            elif command == "routine morning":
                print(self.routines.morning_routine())
            elif command == "mode driving":
                print(self.routines.driving_mode())
            elif command.startswith("touch:"):
                self.handle_touch_command(command)
            else:
                result = self.cli_engine.execute_command(command)
                self.interface.display_output(result)

    def handle_touch_command(self, command):
        """Handle touch simulation"""
        try:
            coords = command[6:].split(',')
            if len(coords) == 2:
                x, y = int(coords[0]), int(coords[1])
                action = self.interface.touch_handler.handle_touch(x, y)
                if action:
                    self.process_touch_action(action)
        except Exception as e:
            print(f"Touch error: {e}")

    def process_touch_action(self, action):
        """Process touch actions"""
        if action == "quick_call":
            self.cli_engine.execute_command("call mom")
        elif action == "quick_sms":
            self.cli_engine.execute_command("sms")
        elif action.startswith("key_"):
            key = action[4:]
            if key.isdigit():
                print(f"Number pressed: {key}")
            elif key == "call":
                self.cli_engine.execute_command("call")

if __name__ == "__main__":
    launcher = CLIUILauncher()
    launcher.start()
