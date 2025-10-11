"""
Main SmartCLI Screen - Enhanced with all modes
"""

import os
from datetime import datetime

class MainScreen:
    """Enhanced TUI with all SmartCLI modes"""
    
    def display(self, current_mode: str = "home"):
        """Display main screen with current mode"""
        self._clear_screen()
        self._show_status_bar(current_mode)
        self._show_mode_content(current_mode)
    
    def _clear_screen(self):
        """Clear terminal screen"""
        os.system('clear')
    
    def _show_status_bar(self, mode: str):
        """Display enhanced status bar"""
        mode_icons = {
            "home": "🏠",
            "sms": "💬", 
            "mail": "📧",
            "maps": "🗺️",
            "call": "📞",
            "whatsapp": "💚",
            "contacts": "👥"
        }
        
        icon = mode_icons.get(mode, "🏠")
        time_now = datetime.now().strftime("%d-%b-%Y;%H:%M")
        
        print("=" * 60)
        print(f"{icon} {mode.upper():<12} | {time_now} | RAM:2.1/4GB | 💾:32/64GB | 🔋92%")
        print("📱(3) 📞(2) ✉️(5) 💚(4) 🔔(7) 📍(1)")
        print("=" * 60)
        print()
    
    def _show_mode_content(self, mode: str):
        """Show content specific to current mode"""
        mode_handlers = {
            "home": self._show_home_content,
            "sms": self._show_sms_content,
            "mail": self._show_mail_content,
            "maps": self._show_maps_content,
            "call": self._show_call_content,
            "whatsapp": self._show_whatsapp_content,
            "contacts": self._show_contacts_content,
        }
        
        handler = mode_handlers.get(mode, self._show_home_content)
        handler()
    
    def _show_home_content(self):
        """Home screen content"""
        print("🤖 Welcome to SMARTCLI - Your Mobile Linux Terminal")
        print()
        print("💡 Quick commands:")
        print("  sms mom 'hello'    - Send quick SMS")
        print("  call john          - Call contact")
        print("  map cafe           - Find nearby cafes")
        print("  mail               - Check email")
        print("  wh                 - WhatsApp messages")
        print()
        print("🔧 System: apps, wifi, bluetooth, settings")
        print("📞 Communication: sms, call, mail, wh")
        print("🗺️  Navigation: map, location")
        print("⚙️  Hardware: flash, camera, mic")
        print()
        print("Type 'help' for complete command reference")
        print()
    
    def _show_sms_content(self):
        """SMS mode content"""
        print("💬 SMS MESSAGE MANAGEMENT")
        print("-" * 40)
        print("Recent conversations:")
        print("  1. Mom (2) 📍       - Call me when free... [14:25]")
        print("  2. John (1) 🏢      - Running 15min late... [13:48]")
        print("  3. Bank 🔒          - OTP: 458792 [12:30]")
        print("  4. Alice 💼         - Meeting confirmed... [11:15]")
        print()
        print("Commands: list, read [id], send [name] [msg], del [id], exit")
        print("Quick: sms mom 'message' - send directly")
        print()
    
    def _show_mail_content(self):
        """Mail mode content"""
        print("📧 EMAIL MANAGEMENT")
        print("-" * 40)
        print("Unread emails:")
        print("  1. Amazon 🛒        - Order #4582 confirmed [14:20]")
        print("  2. GitHub 💻        - Repository update [13:45]")
        print("  3. News 📰          - Daily digest [12:30]")
        print("  4. Work 💼          - Project update [11:15]")
        print()
        print("Commands: list, read [id], compose, reply [id], exit")
        print()
    
    def _show_maps_content(self):
        """Maps mode content"""
        print("🗺️  MAPS & NAVIGATION")
        print("-" * 40)
        print("Recent locations:")
        print("  1. 🏠 Home                 - 2.1km")
        print("  2. 🏢 Office               - 8.7km") 
        print("  3. ☕ Starbucks            - 0.5km")
        print("  4. ⛽ Gas Station          - 1.2km")
        print("  5. 🏪 Supermarket         - 0.8km")
        print()
        print("Commands: search [place], nav [dest], route [src] [dest]")
        print("Quick: map cafe - search nearby cafes")
        print()
    
    def _show_call_content(self):
        """Call mode content"""
        print("📞 CALL MANAGEMENT")
        print("-" * 40)
        print("Recent calls:")
        print("  1. Mom 📍          14:25  ✅ 5:23")
        print("  2. John 🏢         13:48  ❌ Missed")
        print("  3. Office 💼       12:30  ✅ 2:15")
        print("  4. Alice 📍        11:15  ❌ Missed")
        print()
        print("Commands: call [name], history, contacts, exit")
        print("Quick: call mom - call immediately")
        print()
    
    def _show_whatsapp_content(self):
        """WhatsApp mode content"""
        print("💚 WHATSAPP MESSAGES")
        print("-" * 40)
        print("Recent chats:")
        print("  1. Family 👨‍👩‍👧‍👦     - Mom: Dinner at 7? [14:22]")
        print("  2. Work Team 💼    - John: Files uploaded [13:40]")
        print("  3. Friends 🎉      - Alice: Party tonight! [12:15]")
        print("  4. Group 📱        - 5 new messages [11:30]")
        print()
        print("Commands: wh [name], send [msg], list, exit")
        print("Quick: wh mom - open chat with mom")
        print()
    
    def _show_contacts_content(self):
        """Contacts mode content"""
        print("👥 CONTACT MANAGEMENT")
        print("-" * 40)
        print("Recent contacts:")
        print("  1. Mom 📍           - +91 XXXXX X8901")
        print("  2. John 🏢          - +91 XXXXX X6723")
        print("  3. Alice 📍         - +91 XXXXX X4456")
        print("  4. Office 💼        - +91 XXXXX X3345")
        print()
        print("Commands: contact [name], add, edit, del, list, exit")
        print()
