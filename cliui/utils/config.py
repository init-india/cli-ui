"""
Configuration Manager - Load and save settings
"""

import json
import os

class Config:
    def __init__(self):
        self.config_path = "config/preferences.json"
        self.settings = self.load_settings()
    
    def load_settings(self) -> dict:
        """Load settings from file"""
        default_settings = {
            "theme": "dark",
            "font_size": 14,
            "touch_sensitivity": 5,
            "max_history": 1000
        }
        
        try:
            if os.path.exists(self.config_path):
                with open(self.config_path, 'r') as f:
                    return json.load(f)
        except:
            pass
        
        return default_settings
    
    def save_settings(self):
        """Save settings to file"""
        try:
            os.makedirs(os.path.dirname(self.config_path), exist_ok=True)
            with open(self.config_path, 'w') as f:
                json.dump(self.settings, f, indent=2)
        except:
            pass
