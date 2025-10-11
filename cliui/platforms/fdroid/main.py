"""
COMPLETE SmartCLI F-Droid - All Systems Integrated
"""

import os
import sys

# Add ALL imports
sys.path.append(os.path.join(os.path.dirname(__file__), 'src'))

from launcher.auth import auth_manager
from launcher.main_screen import MainScreen
from core.command_router import CommandRouter

class SmartCLIApp:
    """Complete SmartCLI application with ALL features"""
    
    def __init__(self):
        self.main_screen = MainScreen()
        self.command_router = CommandRouter()
        self.current_mode = "home"
    
    def run(self):
        """Main application loop"""
        print("🏠 SmartCLI Launcher Starting...")
        
        while True:
            # Authentication
            if not auth_manager.get_auth_status():
                if not self._authenticate():
                    continue
            
            # Main session
            self._main_session()
    
    def _authenticate(self) -> bool:
        print("=" * 50)
        print("         🔐 SMARTCLI AUTHENTICATION")
        print("=" * 50)
        return auth_manager.authenticate('biometric') or auth_manager.authenticate('pin')
    
    def _main_session(self):
        self.current_mode = "home"
        print("\n🎉 Authentication successful! SmartCLI ready.\n")
        
        try:
            while auth_manager.get_auth_status():
                self.main_screen.display(self.current_mode)
                
                try:
                    command = input("$ ").strip()
                except (EOFError, KeyboardInterrupt):
                    print("\n🔒 Locking phone...")
                    auth_manager.logout()
                    continue
                
                self._process_command(command)
                
        except Exception as e:
            print(f"\n⚠️  Error: {e}")
            auth_manager.logout()
    
    def _process_command(self, command: str):
        """Process command through complete router"""
        output, new_mode = self.command_router.execute(command, self.current_mode)
        
        # Handle special commands
        if output == "EXIT":
            if self.current_mode == "home":
                print("🔒 Locking phone...")
                auth_manager.logout()
            else:
                print(f"↩️  Returning to home from {self.current_mode}...")
                self.current_mode = "home"
        elif output == "LOCK":
            print("🔒 Immediate lock...")
            auth_manager.logout()
        elif output == "AUTH":
            print("🔄 Re-authenticating...")
            auth_manager.logout()
        elif output.endswith("_MODE"):
            self.current_mode = output.replace("_MODE", "").lower()
        elif output:
            print(output)

def main():
    """Entry point"""
    app = SmartCLIApp()
    app.run()

if __name__ == "__main__":
    main()
