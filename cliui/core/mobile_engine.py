"""
Mobile CLI Engine - Optimized for mobile daily tasks
"""

import os
import datetime
from typing import List, Dict
from .history_manager import HistoryManager

class MobileCLIEngine:
    def __init__(self, android_bridge=None):
        self.history = HistoryManager()
        self.android = android_bridge
        self.contacts = []  
        self.messages = []  
        self.apps = []      
        
        # Mobile-specific commands
        self.init_mobile_commands()
        self.load_mobile_data()
    
    def init_mobile_commands(self):
        """Initialize mobile-optimized commands"""
        self.commands = {
            # Communication
            'call': self.make_call,
            'sms': self.send_sms,
            'whatsapp': self.whatsapp_message,
            'contacts': self.list_contacts,
            
            # Navigation & Travel
            'map': self.open_maps,
            'nav': self.start_navigation,
            'uber': self.call_uber,
            
            # Media & Entertainment
            'music': self.control_music,
            'camera': self.open_camera,
            'gallery': self.open_gallery,
            
            # System & Utilities
            'wifi': self.toggle_wifi,
            'bluetooth': self.toggle_bluetooth,
            'hotspot': self.toggle_hotspot,
            'battery': self.show_battery,
            
            # Apps & Launcher
            'apps': self.list_apps,
            'launch': self.launch_app,
            'close': self.close_app,
            
            # Information
            'weather': self.get_weather,
            'time': self.get_time,
            'reminder': self.set_reminder,
            
            # File Management (Mobile-optimized)
            'files': self.list_files,
            'photos': self.list_photos,
            'downloads': self.list_downloads,
            
            # Help
            'help': self.show_help,
            'clear': self.clear_screen,
        }
    
    def load_mobile_data(self):
        """Load mobile-specific data"""
        # Use Android bridge if available, else use simulation
        if self.android:
            try:
                self.contacts = self.android.get_contacts()
            except:
                self.contacts = self.get_simulated_contacts()
        else:
            self.contacts = self.get_simulated_contacts()
        
        self.apps = [
            {"name": "WhatsApp", "package": "com.whatsapp", "category": "communication"},
            {"name": "Google Maps", "package": "com.google.android.apps.maps", "category": "navigation"},
            {"name": "Spotify", "package": "com.spotify.music", "category": "music"},
            {"name": "Camera", "package": "com.android.camera", "category": "media"},
            {"name": "Gallery", "package": "com.android.gallery", "category": "media"},
            {"name": "Files", "package": "com.android.filemanager", "category": "tools"},
        ]
    
    def get_simulated_contacts(self):
        """Fallback simulated contacts"""
        return [
            {"name": "Mom", "number": "+1234567890", "favorite": True},
            {"name": "Dad", "number": "+1234567891", "favorite": True},
            {"name": "John Work", "number": "+1234567892", "favorite": False},
            {"name": "Emergency", "number": "911", "favorite": True},
            {"name": "Pizza Shop", "number": "+1234567893", "favorite": False},
        ]
    
    def execute_command(self, command_input: str) -> str:
        """Execute mobile-optimized command"""
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
                return f"❌ Error: {str(e)}"
        else:
            return f"❌ Command not found: {command}\nType 'help' for mobile commands"
    
    def find_contact_number(self, contact_name: str) -> str:
        """Find contact number by name with flexible matching"""
        contact_name = contact_name.lower().strip()
        
        # Exact match first
        for contact in self.contacts:
            if contact_name == contact['name'].lower():
                return contact['number']
        
        # Partial match
        for contact in self.contacts:
            if contact_name in contact['name'].lower():
                return contact['number']
        
        # Word-by-word match (for "john work" matching "John Work")
        search_words = contact_name.split()
        for contact in self.contacts:
            contact_words = contact['name'].lower().split()
            if all(word in contact_words for word in search_words):
                return contact['number']
        
        return None

def quick_notes(self, args: List[str]) -> str:
    """Quick note taking"""
    if not args:
        return "📝 Recent Notes:\n[1] Meeting notes (today)\n[2] Shopping list (yesterday)\n\nUsage: notes <your note>"
    
    note = " ".join(args)
    return f"📝 Note saved: {note}\n[Edit] [Share] [Delete]"

def calculator(self, args: List[str]) -> str:
    """Simple calculator"""
    if not args:
        return "🧮 Calculator\nUsage: calc <expression>\nExample: calc 15 + 27 * 2"
    
    try:
        # Safe evaluation
        expression = " ".join(args)
        result = eval(expression, {"__builtins__": None}, {})
        return f"🧮 {expression} = {result}"
    except:
        return "❌ Invalid expression"

def set_timer(self, args: List[str]) -> str:
    """Set a timer"""
    if not args:
        return "⏰ Usage: timer <minutes> <message>\nExample: timer 5 'Tea ready'"
    
    return f"⏰ Timer set: {' '.join(args)}\n[Start] [Cancel] [Snooze]"

def check_calendar(self, args: List[str]) -> str:
    """Check calendar events"""
    return "📅 Today's Events:\n[1] 10:00 Team Meeting\n[2] 14:00 Doctor Appointment\n[3] 18:00 Gym\n\n[Add Event] [View Week]"




    
    # Communication Commands
    def make_call(self, args: List[str]) -> str:
        """Make a phone call"""
        if not args:
            return "📞 Recent Calls:\n[1] Mom (2 min ago)\n[2] John (1 hr ago)\n\nUsage: call <name/number>"
        
        target = " ".join(args)
        
        # Use Android bridge if available
        if self.android:
            # Find contact number
            contact_number = self.find_contact_number(target)
            if contact_number:
                return self.android.make_call(contact_number)
            else:
                return self.android.make_call(target)
        else:
            contact_number = self.find_contact_number(target)
            if contact_number:
                return f"📞 [SIMULATION] Calling {target} ({contact_number})...\n[Speaker] [Mute] [Add Call] [End]"
            else:
                return f"📞 [SIMULATION] Calling {target}...\n[Speaker] [Mute] [Add Call] [End]"
    
    def send_sms(self, args: List[str]) -> str:
        """Send SMS message"""
        if not args:
            return "💬 Recent Messages:\n[1] Mom: Dinner at 7? (5 min)\n[2] John: Meeting notes (1 hr)\n\nUsage: sms <contact> <message>"
        
        if len(args) < 2:
            return "💬 Usage: sms <contact> <message>"
        
        contact = args[0]
        message = " ".join(args[1:])
        
        # Use Android bridge if available
        if self.android:
            contact_number = self.find_contact_number(contact)
            if contact_number:
                return self.android.send_sms(contact_number, message)
            else:
                return f"❌ Contact not found: {contact}"
        else:
            contact_number = self.find_contact_number(contact)
            if contact_number:
                return f"💬 [SIMULATION] SMS to {contact} ({contact_number}): '{message}'\n[Send] [Edit] [Cancel]"
            else:
                return f"💬 [SIMULATION] SMS to {contact}: '{message}'\n[Send] [Edit] [Cancel]"
    
    def whatsapp_message(self, args: List[str]) -> str:
        """Send WhatsApp message"""
        if not args:
            return "💚 WhatsApp Chats:\n[1] Family Group (3 new)\n[2] Mom (online)\n[3] Work Team\n\nUsage: whatsapp <contact> <message>"
        
        if len(args) < 2:
            return "💚 Usage: whatsapp <contact> <message>"
        
        contact = args[0]
        message = " ".join(args[1:])
        return f"💚 WhatsApp to {contact}: '{message}'\n[Send] [Attach] [Cancel]"
    
    def list_contacts(self, args: List[str]) -> str:
        """List contacts"""
        output = "👥 Contacts:\n"
        output += "─" * 40 + "\n"
        
        for i, contact in enumerate(self.contacts, 1):
            favorite = "⭐ " if contact['favorite'] else "  "
            output += f"[{i}] {favorite}{contact['name']} - {contact['number']}\n"
        
        output += "─" * 40 + "\n"
        output += "Touch number to call, or use 'call <name>'"
        return output
    
    # Navigation Commands
    def open_maps(self, args: List[str]) -> str:
        """Open maps with location"""
        if not args:
            return "🗺️ Frequent Locations:\n[1] Home\n[2] Work\n[3] Gym\n[4] Coffee Shop\n\nUsage: map <location>"
        
        location = " ".join(args)
        return f"🗺️ Searching: {location}\n[1] {location} City Center (2km)\n[2] {location} Mall (3km)\n[3] {location} Station (1km)\n\nType 'nav' to start navigation"
    
    def start_navigation(self, args: List[str]) -> str:
        """Start voice navigation"""
        return "🚗 Navigation Started!\n🎤 'In 200m, turn left onto Main Street'\n\n[Volume] [Mute] [Stop] [Alternative Routes]"
    
    def call_uber(self, args: List[str]) -> str:
        """Call ride service"""
        return "🚗 Uber Options:\n[1] UberX - 5min - $12\n[2] Uber Comfort - 3min - $18\n[3] Uber Pool - 7min - $8\n\nSelect number to book"
    
    # Media Commands
    def control_music(self, args: List[str]) -> str:
        """Control music playback"""
        if not args:
            return "🎵 Now Playing: Jazz Playlist\n⏸️  [Play] [Pause] [Next] [Volume] [Playlists]"
        
        action = args[0].lower()
        if action == "play":
            return "🎵 Playing: Current Playlist"
        elif action == "pause":
            return "⏸️ Music Paused"
        elif action == "next":
            return "⏭️ Next Track: Song Name"
        elif action == "volume" and len(args) > 1:
            return f"🔊 Volume set to {args[1]}%"
        else:
            return "🎵 Music Controls: play, pause, next, volume <1-100>"
    
    def open_camera(self, args: List[str]) -> str:
        """Open camera app"""
        return "📸 Camera Launched\n[Photo] [Video] [Switch Camera] [Flash] [Settings]"
    
    def open_gallery(self, args: List[str]) -> str:
        """Open photo gallery"""
        return "🖼️ Gallery:\n[1] Recent Photos (45)\n[2] Albums (12)\n[3] Favorites (8)\n\nSelect number to view"
    
    # System Commands
    def toggle_wifi(self, args: List[str]) -> str:
        """Toggle WiFi"""
        return "📶 WiFi: Connected to HomeNetwork\n[Disconnect] [Scan] [Settings]"
    
    def toggle_bluetooth(self, args: List[str]) -> str:
        """Toggle Bluetooth"""
        return "🔵 Bluetooth: Connected to Speaker\n[Disconnect] [Pair New] [Settings]"
    
    def toggle_hotspot(self, args: List[str]) -> str:
        """Toggle mobile hotspot"""
        return "📱 Hotspot: Off\n[Turn On] [Configure]"
    
    def show_battery(self, args: List[str]) -> str:
        """Show battery status"""
        return "🔋 Battery: 78%\n⏱️  Estimated: 5h 23m remaining\n[Power Saving] [Battery Health]"
    
    # App Management
    def list_apps(self, args: List[str]) -> str:
        """List installed apps"""
        output = "📱 Installed Apps:\n"
        output += "─" * 40 + "\n"
        
        for i, app in enumerate(self.apps, 1):
            output += f"[{i}] {app['name']} ({app['category']})\n"
        
        output += "─" * 40 + "\n"
        output += "Type 'launch <number/name>' to open app"
        return output
    
    def launch_app(self, args: List[str]) -> str:
        """Launch an app"""
        if not args:
            return "🚀 Usage: launch <app_name> or launch <number>"
        
        target = " ".join(args)
        if target.isdigit():
            idx = int(target) - 1
            if 0 <= idx < len(self.apps):
                target = self.apps[idx]['name']
        
        return f"🚀 Launching {target}...\nType 'home' to return to CLI"
    
    def close_app(self, args: List[str]) -> str:
        """Close current app"""
        return "📴 Closing current app...\nReturning to CLI home"
    
    # Information Commands
    def get_weather(self, args: List[str]) -> str:
        """Get weather information"""
        return "🌤️  Weather: Sunny, 25°C\n💧 Humidity: 60%\n💨 Wind: 15 km/h\n📅 Forecast: Clear skies next 3 days"
    
    def get_time(self, args: List[str]) -> str:
        """Get current time"""
        now = datetime.datetime.now()
        return f"🕒 {now.strftime('%A, %B %d')}\n⏰ {now.strftime('%H:%M:%S')}\n📅 Next event: Meeting at 15:00"
    
    def set_reminder(self, args: List[str]) -> str:
        """Set a reminder"""
        if not args:
            return "⏰ Usage: reminder <time> <message>\nExample: reminder 15:00 Team meeting"
        
        return f"✅ Reminder set for {' '.join(args)}\n[Edit] [Delete] [Snooze]"
    
    # File Management (Mobile-optimized)
    def list_files(self, args: List[str]) -> str:
        """List mobile files"""
        return "📁 Mobile Storage:\n[1] Downloads (45 files)\n[2] Documents (12 files)\n[3] Photos (234 files)\n[4] Music (89 files)\n\nSelect category to browse"
    
    def list_photos(self, args: List[str]) -> str:
        """List photos"""
        return "🖼️ Recent Photos:\n[1] vacation.jpg (2MB)\n[2] screenshot.png (1MB)\n[3] document.pdf (3MB)\n\n[Share] [Delete] [View All]"
    
    def list_downloads(self, args: List[str]) -> str:
        """List downloads"""
        return "📥 Downloads:\n[1] report.pdf (2MB)\n[2] image.jpg (1MB)\n[3] music.mp3 (5MB)\n\n[Open] [Share] [Delete]"
    
    # System Commands
    def show_help(self, args: List[str]) -> str:
        """Show mobile-optimized help"""
        help_text = """
📱 CLIUI Mobile Commands:

Communication:
  call <name/number>    - Make phone call
  sms <contact> <msg>   - Send SMS
  whatsapp <contact>    - WhatsApp message
  contacts              - List contacts

Navigation:
  map <location>        - Search location
  nav                   - Start navigation
  uber                  - Book ride

Media:
  music [play/pause]    - Control music
  camera                - Open camera
  gallery               - Open photos

System:
  wifi                  - WiFi controls
  bluetooth             - Bluetooth settings
  battery               - Battery status
  apps                  - List apps
  launch <app>          - Open app

Information:
  weather               - Weather forecast
  time                  - Current time
  reminder <time> <msg> - Set reminder

Files:
  files                 - Browse files
  photos                - View photos
  downloads             - Downloads folder

Touch: Use numbers [1], [2], etc. to select items
Voice: Say commands naturally
"""
        return help_text
    
    def clear_screen(self, args: List[str]) -> str:
        """Clear screen"""
        return "CLEAR_SCREEN"
