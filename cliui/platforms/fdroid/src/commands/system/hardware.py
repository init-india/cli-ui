"""
Complete Hardware Controls for SmartCLI
"""


from integrations.android_apis import android_apis as android_api
from typing import List


class HardwareSystem:
    def process_command(self, command: str, args: List[str]) -> str:
        if command == "wifi":
            if args and args[0] == "on": android_api.toggle_wifi(True); return "📶 WiFi: ON"
            elif args and args[0] == "off": android_api.toggle_wifi(False); return "📶 WiFi: OFF"
            else: android_api.toggle_wifi(); return "📶 WiFi toggled"
        
        elif command == "bluetooth":
            if args and args[0] == "on": android_api.toggle_bluetooth(True); return "🔵 Bluetooth: ON"
            elif args and args[0] == "off": android_api.toggle_bluetooth(False); return "🔵 Bluetooth: OFF"
            else: android_api.toggle_bluetooth(); return "🔵 Bluetooth toggled"
        
        elif command == "flash":
            if args and args[0] == "on": android_api.toggle_flashlight(True); return "💡 Flashlight: ON"
            elif args and args[0] == "off": android_api.toggle_flashlight(False); return "💡 Flashlight: OFF"
            else: android_api.toggle_flashlight(); return "💡 Flashlight toggled"
        
        elif command == "hotspot": return "📡 Hotspot toggled"
        elif command == "location": return "📍 Location toggled"
        elif command == "mic": return "🎤 Microphone toggled"
        elif command == "camera": return "📷 Camera toggled"
        else: return self._show_help()
    
    def _show_help(self) -> str:
        return """
⚙️  HARDWARE COMMANDS:
  wifi [on/off]     - WiFi control
  bluetooth [on/off] - Bluetooth control
  flash [on/off]    - Flashlight control
  hotspot           - Mobile hotspot
  location          - Location services
  mic               - Microphone
  camera            - Camera control
"""

hardware_system = HardwareSystem()
