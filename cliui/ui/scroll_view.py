from typing import List




"""
Scroll View - Touch-enabled scrolling for output
"""

class ScrollView:
    def __init__(self, height: int = 15):
        self.height = height
        self.scroll_position = 0
        self.total_lines = 0
        self.content = []
    
    def set_content(self, content: List[str]):
        """Set content to display"""
        self.content = content
        self.total_lines = len(content)
        self.ensure_valid_scroll()
    
    def scroll_up(self, lines: int = 1):
        """Scroll up"""
        self.scroll_position = max(0, self.scroll_position - lines)
    
    def scroll_down(self, lines: int = 1):
        """Scroll down"""
        self.scroll_position = min(
            max(0, self.total_lines - self.height),
            self.scroll_position + lines
        )
    
    def scroll_to_bottom(self):
        """Scroll to bottom"""
        self.scroll_position = max(0, self.total_lines - self.height)
    
    def handle_touch_scroll(self, start_y: int, end_y: int):
        """Handle touch scrolling gesture"""
        delta = start_y - end_y  # Positive = scroll up, Negative = scroll down
        scroll_lines = abs(delta) // 2  # Convert pixel delta to lines
        
        if delta > 0:
            self.scroll_up(scroll_lines)
        else:
            self.scroll_down(scroll_lines)
    
    def get_visible_content(self) -> List[str]:
        """Get currently visible content"""
        start = self.scroll_position
        end = min(start + self.height, self.total_lines)
        return self.content[start:end]
    
    def display(self, content: List[str]):
        """Display content with scroll indicators"""
        self.set_content(content)
        visible = self.get_visible_content()
        
        # Show scroll indicators
        if self.scroll_position > 0:
            print("↑ (more above)")
        
        for line in visible:
            # Truncate long lines for display
            if len(line) > 78:
                line = line[:75] + "..."
            print(line)
        
        if self.scroll_position + self.height < self.total_lines:
            print("↓ (more below)")
        
        # Scroll position indicator
        if self.total_lines > self.height:
            percent = (self.scroll_position / max(1, self.total_lines - self.height)) * 100
            bar_length = 20
            filled = int(bar_length * percent / 100)
            bar = "█" * filled + "░" * (bar_length - filled)
            print(f"╞{bar}╡ {percent:.0f}%")
    
    def ensure_valid_scroll(self):
        """Ensure scroll position is valid"""
        max_scroll = max(0, self.total_lines - self.height)
        self.scroll_position = min(self.scroll_position, max_scroll)
        self.scroll_position = max(0, self.scroll_position)
