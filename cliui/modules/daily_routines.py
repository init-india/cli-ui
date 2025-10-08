"""
Daily Routines - Handle common mobile workflows
"""

class DailyRoutines:
    def __init__(self, cli_engine):
        self.engine = cli_engine
    
    def morning_routine(self):
        """Execute morning routine"""
        routine = [
            "weather",
            "time",
            "reminder Check today's schedule",
            "music play morning playlist",
            "map traffic to work"
        ]
        
        results = []
        for command in routine:
            results.append(f"> {command}")
            results.append(self.engine.execute_command(command))
        
        return "\n".join(results)
    
    def driving_mode(self):
        """Enter driving-optimized mode"""
        return """🚗 Driving Mode Activated
[1] Navigate Home
[2] Navigate to Work  
[3] Call Emergency Contact
[4] Music - Driving Playlist
[5] Exit Driving Mode

Voice: "Hey CLI, navigate to nearest gas station"
"""
    
    def meeting_mode(self):
        """Enter meeting mode"""
        return """🤝 Meeting Mode
[1] Silence Phone
[2] Set 'In Meeting' Auto-Reply
[3] Quick Note
[4] Share Contact
[5] Exit Meeting Mode
"""
