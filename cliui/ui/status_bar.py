"""
Status Bar - System information display
"""

import time
import psutil
import os

class StatusBar:
    def __init__(self):
        pass
    
    def get_battery_status(self) -> str:
        """Get battery information"""
        try:
            battery = psutil.sensors_battery()
            if battery:
                percent = int(battery.percent)
                status = "🔋" if battery.power_plugged else "🔌"
                return f"{status} {percent}%"
        except:
            pass
        return "🔋 100%"
    
    def get_memory_usage(self) -> str:
        """Get memory usage"""
        try:
            memory = psutil.virtual_memory()
            used_gb = memory.used / (1024**3)
            total_gb = memory.total / (1024**3)
            return f"💾 {used_gb:.1f}/{total_gb:.1f}GB"
        except:
            return "💾 ?/?GB"
    
    def get_cpu_usage(self) -> str:
        """Get CPU usage"""
        try:
            cpu_percent = psutil.cpu_percent(interval=0.1)
            return f"⚡ {cpu_percent:.0f}%"
        except:
            return "⚡ ?%"
    
    def get_time(self) -> str:
        """Get current time"""
        return time.strftime("🕒 %H:%M:%S")
    
    def get_current_dir(self) -> str:
        """Get current directory (shortened)"""
        try:
            cwd = os.getcwd()
            home = os.path.expanduser("~")
            if cwd.startswith(home):
                return f"📁 ~{cwd[len(home):]}"
            return f"📁 {cwd}"
        except:
            return "📁 /"
    
    def display(self):
        """Display status bar"""
        battery = self.get_battery_status()
        memory = self.get_memory_usage()
        cpu = self.get_cpu_usage()
        current_time = self.get_time()
        current_dir = self.get_current_dir()
        
        status_line = f"│ {current_dir} | {memory} | {cpu} | {battery} | {current_time}"
        
        # Pad to fit 78 characters
        padding = 78 - len(status_line) - 3  # -3 for the │ and spaces
        if padding > 0:
            status_line += " " * padding
        
        status_line += "│"
        print(status_line)
