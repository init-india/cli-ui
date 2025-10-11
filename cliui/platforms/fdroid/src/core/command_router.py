"""
COMPLETE Command Router for SmartCLI - All Systems Integrated
"""
import os
import subprocess
from typing import Dict, List, Tuple

# Import ALL systems - FIXED IMPORTS
from integrations.android_apis import android_apis as android_api
from commands.communications.calls import call_system
from commands.communications.sms import sms_system
from commands.communications.email import email_system
from commands.communications.whatsapp import whatsapp_system
from commands.navigation.maps import maps_system
from commands.system.hardware import hardware_system
from commands.utilities.contacts import contacts_system
from commands.utilities.alarms import alarms_system


class CommandRouter:
    """Complete command router with ALL systems"""
    
    def __init__(self):
        self.command_handlers = {}
        self._register_builtin_commands()
    
    def _register_builtin_commands(self):
        """Register ALL built-in commands"""
        self.command_handlers = {
            # System commands
            'help': self._cmd_help, 'clear': self._cmd_clear, 'exit': self._cmd_exit,
            'lock': self._cmd_lock, 'auth': self._cmd_auth, 'ps': self._cmd_ps,
            'pwd': self._cmd_pwd, 'whoami': self._cmd_whoami, 'date': self._cmd_date,
            'echo': self._cmd_echo, 'apps': self._cmd_apps,
            
            # Communication modes
            'sms': self._cmd_sms, 'mail': self._cmd_mail, 'call': self._cmd_call,
            'wh': self._cmd_whatsapp,
            
            # Navigation
            'map': self._cmd_map,
            
            # Hardware controls
            'wifi': self._cmd_wifi, 'bluetooth': self._cmd_bluetooth, 'hotspot': self._cmd_hotspot,
            'location': self._cmd_location, 'flash': self._cmd_flash, 'mic': self._cmd_mic,
            'camera': self._cmd_camera,
            
            # Utilities
            'contact': self._cmd_contact, 'alarm': self._cmd_alarm, 'time': self._cmd_time,
        }
    
    def execute(self, command: str, current_mode: str) -> Tuple[str, str]:
        """Execute command with ALL systems"""
        parts = command.strip().split()
        if not parts: return "", current_mode
        
        cmd, args = parts[0], parts[1:]
        
        # Handle ALL mode-specific systems
        mode_handlers = {
            "call": call_system.process_command,
            "sms": sms_system.process_command,
            "mail": email_system.process_command,
            "whatsapp": whatsapp_system.process_command,
            "maps": maps_system.process_command,
            "contacts": contacts_system.process_command,
        }
        
        if current_mode in mode_handlers:
            result = mode_handlers[current_mode](cmd, args)
            return result, current_mode
        
        # Handle mode switching for ALL systems
        mode_switchers = {
            "call": ("📞 Call mode", call_system),
            "sms": ("💬 SMS mode", sms_system),
            "mail": ("📧 Mail mode", email_system),
            "wh": ("💚 WhatsApp mode", whatsapp_system),
            "map": ("🗺️  Maps mode", maps_system),
            "contact": ("👥 Contacts mode", contacts_system),
        }
        
        if cmd in mode_switchers:
            msg, system = mode_switchers[cmd]
            if args:
                result = system.process_command(cmd, args)
                return result, cmd
            return f"{msg}. Type 'help' for commands.", cmd
        
        # System commands
        if cmd in self.command_handlers:
            return self.command_handlers[cmd](args), current_mode
        
        # App launching
        if self._is_app_name(cmd):
            return self._launch_app(cmd), current_mode
        
        # Linux commands
        return self._try_linux_command(command), current_mode
    
    def _is_app_name(self, command: str) -> bool:
        apps = ['firefox', 'calculator', 'maps', 'contacts', 'messages', 'camera', 'settings', 'phone']
        return command in apps
    
    def _launch_app(self, app_name: str) -> str:
        success = android_api.launch_app(app_name)
        return f"🚀 Launching {app_name}..." if success else f"❌ Failed to launch {app_name}"
    
    def _try_linux_command(self, command: str) -> str:
        safe_commands = ['ls', 'pwd', 'whoami', 'date', 'echo']
        cmd_base = command.split()[0]
        if cmd_base in safe_commands:
            try:
                result = subprocess.run(command, shell=True, capture_output=True, text=True, timeout=5)
                return result.stdout if result.returncode == 0 else f"Error: {result.stderr}"
            except: return f"❌ Command failed: {command}"
        return f"❌ Command not found: {command}"
    
    # ===== ALL COMMAND HANDLERS =====
    
    def _cmd_help(self, args: List[str]) -> str: return self._get_help_text()
    def _cmd_clear(self, args: List[str]) -> str: os.system('clear'); return ""
    def _cmd_exit(self, args: List[str]) -> str: return "EXIT"
    def _cmd_lock(self, args: List[str]) -> str: return "LOCK"
    def _cmd_auth(self, args: List[str]) -> str: return "AUTH"
    def _cmd_ps(self, args: List[str]) -> str: return "🖥️  Processes: SmartCLI, System UI"
    def _cmd_pwd(self, args: List[str]) -> str: return "/home/mobile-user"
    def _cmd_whoami(self, args: List[str]) -> str: return "mobile-user"
    def _cmd_date(self, args: List[str]) -> str: from datetime import datetime; return datetime.now().strftime("%d-%b-%Y;%H:%M")
    def _cmd_echo(self, args: List[str]) -> str: return " ".join(args)
    def _cmd_apps(self, args: List[str]) -> str: return "📱 Apps: firefox, calculator, maps, contacts, messages, camera, settings"
    
    def _cmd_sms(self, args: List[str]) -> str: return "SMS_MODE"
    def _cmd_mail(self, args: List[str]) -> str: return "MAIL_MODE"
    def _cmd_call(self, args: List[str]) -> str: return "CALL_MODE"
    def _cmd_whatsapp(self, args: List[str]) -> str: return "WHATSAPP_MODE"
    def _cmd_map(self, args: List[str]) -> str: return "MAPS_MODE"
    def _cmd_contact(self, args: List[str]) -> str: return "CONTACTS_MODE"
    
    def _cmd_wifi(self, args: List[str]) -> str: return hardware_system.process_command("wifi", args)
    def _cmd_bluetooth(self, args: List[str]) -> str: return hardware_system.process_command("bluetooth", args)
    def _cmd_hotspot(self, args: List[str]) -> str: return hardware_system.process_command("hotspot", args)
    def _cmd_location(self, args: List[str]) -> str: return hardware_system.process_command("location", args)
    def _cmd_flash(self, args: List[str]) -> str: return hardware_system.process_command("flash", args)
    def _cmd_mic(self, args: List[str]) -> str: return hardware_system.process_command("mic", args)
    def _cmd_camera(self, args: List[str]) -> str: return hardware_system.process_command("camera", args)
    
    def _cmd_alarm(self, args: List[str]) -> str: return alarms_system.process_command("list" if not args else args[0], args[1:] if args else [])
    def _cmd_time(self, args: List[str]) -> str: from datetime import datetime; return datetime.now().strftime("🕐 %H:%M:%S")
    
    def _get_help_text(self) -> str:
        return """
🤖 SMARTCLI - COMPLETE COMMAND REFERENCE
==================================================
🔐 AUTHENTICATION: auth, lock
🏠 SYSTEM: help, clear, exit, ps, apps, pwd, whoami, date, echo
📱 APPS: firefox, calculator, maps, contacts, messages, camera, settings
💬 COMMUNICATION: sms, call, mail, wh
🗺️  NAVIGATION: map
⚙️  HARDWARE: wifi, bluetooth, hotspot, location, flash, mic, camera
👥 UTILITIES: contact, alarm, time
🐧 LINUX: ls, pwd, whoami, date, echo
==================================================
"""
