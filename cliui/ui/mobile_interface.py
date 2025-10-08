"""
Mobile Interface - Optimized for touch mobile devices
"""

import os
import time

class MobileCLIInterface:
    def __init__(self):
        self.screen_width = 80
        self.notifications = [
            "📱 2 new messages",
            "🔔 Meeting in 15 minutes", 
            "📸 5 new photos"
        ]
    
    def display_home_screen(self):
        """Display mobile-optimized home screen"""
        os.system('clear')
        
        # Status Bar (like mobile status bar)
        print("┌─────────────────────────────────────┐")
        print("│ 📶 4G 🔋 78% 🕒 14:30              │")
        print("├─────────────────────────────────────┤")
        
        # Quick Actions Bar
        print("│ [📞] [💬] [🗺️] [🎵] [📁]           │")
        print("├─────────────────────────────────────┤")
        
        # Notifications
        if self.notifications:
            print("│ 🔔 Notifications:                   │")
            for note in self.notifications[:3]:
                print(f"│   {note:<30} │")
            print("├─────────────────────────────────────┤")
        
        # Command Output Area
        print("│ > _                                 │")
        print("│                                     │")
        print("│                                     │")
        print("│                                     │")
        print("├─────────────────────────────────────┤")
        
        # Virtual Keyboard Area
        print("│ [1][2][3][4][5][6][7][8][9][0]     │")
        print("│ [Q][W][E][R][T][Y][U][I][O][P]     │")
        print("│ [A][S][D][F][G][H][J][K][L][ENT]   │")
        print("│ [Z][X][C][V][B][N][M][,][.][?]     │")
        print("│ [VOICE] [SPACE] [DEL] [CLEAR]      │")
        print("└─────────────────────────────────────┘")
    
    def display_app_screen(self, app_name):
        """Display app with CLI overlay"""
        os.system('clear')
        
        print("┌─────────────────────────────────────┐")
        print(f"│ 📱 {app_name:<35} │")
        print("├─────────────────────────────────────┤")
        print("│                                     │")
        print("│          APP CONTENT AREA           │")
        print("│                                     │")
        print("│                                     │")
        print("├─────────────────────────────────────┤")
        print("│ 🏠 [Home] [Back] [Close] [CLI]      │")
        print("└─────────────────────────────────────┘")
    
    def get_input(self):
        """Get input from user (simulated)"""
        return input("> ").strip()
    
    def get_overlay_input(self):
        """Get input from overlay"""
        return input("CLI Overlay > ").strip()
    
    def display_output(self, output):
        """Display command output"""
        print(output)
        input("\nPress Enter to continue...")
    
    def show_app_with_overlay(self, app_name):
        """Show app launch animation"""
        print(f"🚀 Launching {app_name}...")
        time.sleep(1)
        print(f"📱 {app_name} is now running with CLI overlay")
        print("Type 'home' to return to CLI launcher")
