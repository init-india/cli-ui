"""
Touch Interface - Real touch handling for mobile
"""

class TouchInterface:
    def __init__(self):
        self.touch_zones = {
            'status_bar': (0, 0, 80, 2),
            'quick_actions': (0, 3, 80, 1),
            'output_area': (0, 5, 80, 10),
            'number_pad': (0, 16, 80, 8)
        }
    
    def handle_touch(self, x, y):
        """Handle touch coordinates"""
        if self.is_in_zone(x, y, 'quick_actions'):
            return self.handle_quick_action(x, y)
        elif self.is_in_zone(x, y, 'number_pad'):
            return self.handle_number_pad(x, y)
        elif self.is_in_zone(x, y, 'output_area'):
            return self.handle_output_touch(x, y)
    
    def handle_quick_action(self, x, y):
        """Handle quick action buttons"""
        actions = ['call', 'sms', 'maps', 'music', 'files']
        button_width = 16
        button_index = x // button_width
        
        if 0 <= button_index < len(actions):
            return f"quick_{actions[button_index]}"
        return None
    
    def handle_number_pad(self, x, y):
        """Handle virtual number pad"""
        # Convert coordinates to keypad position
        row = (y - 16) // 2
        col = x // 8
        
        keypad = [
            ['1', '2', '3', 'call'],
            ['4', '5', '6', 'sms'],
            ['7', '8', '9', 'maps'],
            ['*', '0', '#', 'enter']
        ]
        
        if 0 <= row < 4 and 0 <= col < 4:
            return f"key_{keypad[row][col]}"
        return None
