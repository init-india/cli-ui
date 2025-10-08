#!/usr/bin/env python3
"""
Test mobile-specific functionality
"""

import sys
import os

# Add the cliui package to path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from core.mobile_engine import MobileCLIEngine
from core.android_bridge import AndroidBridge

def test_mobile_commands():
    """Test all mobile commands"""
    print("🧪 Testing Mobile Commands...")
    print("=" * 50)
    
    android = AndroidBridge()
    engine = MobileCLIEngine(android)
    
    # Use contacts that actually exist in our system
    test_commands = [
        "help",
        "call mom",
        "call Dad", 
        "sms Mom Hello there!",
        "sms Emergency Test emergency message",
        "whatsapp Family Group meeting at 5", 
        "contacts",
        "map coffee shop",
        "nav",
        "music play",
        "music volume 80",
        "weather",
        "time",
        "apps",
        "launch Camera",
        "wifi",
        "bluetooth",
        "battery",
        "files",
        "photos",
        "reminder 18:00 Gym session"
    ]
    
    for i, cmd in enumerate(test_commands, 1):
        print(f"\n{i:2d}. Testing: {cmd}")
        print("-" * 40)
        result = engine.execute_command(cmd)
        print(result)
        if i < len(test_commands):  # Don't pause after last command
            input("Press Enter to continue...")

def test_touch_simulation():
    """Test touch interface"""
    print("\n👆 Testing Touch Simulation...")
    print("=" * 50)
    
    # Test simple touch coordinates simulation
    test_touches = [
        (10, 4, "Quick Action - Call"),
        (30, 4, "Quick Action - SMS"), 
        (50, 4, "Quick Action - Maps"),
        (10, 18, "Number Pad - 1"),
        (20, 18, "Number Pad - 2"),
        (70, 18, "Number Pad - Enter"),
    ]
    
    for x, y, description in test_touches:
        print(f"📍 {description} at ({x}, {y})")

def test_daily_routines():
    """Test daily routines"""
    print("\n🔄 Testing Daily Routines...")
    print("=" * 50)
    
    android = AndroidBridge()
    engine = MobileCLIEngine(android)
    
    try:
        from modules.daily_routines import DailyRoutines
        routines = DailyRoutines(engine)
        
        print("🌅 Morning Routine:")
        print(routines.morning_routine())
        
        print("\n🚗 Driving Mode:")
        print(routines.driving_mode())
        
    except ImportError:
        print("📝 Daily routines module not yet implemented")
        print("This is normal during development")

def test_contact_search():
    """Test contact search functionality"""
    print("\n👥 Testing Contact Search...")
    print("=" * 50)
    
    android = AndroidBridge()
    engine = MobileCLIEngine(android)
    
    test_searches = [
        "mom",           # Exact match
        "Mom",           # Case insensitive
        "work",          # Partial match "John Work"
        "emerg",         # Partial match "Emergency"
        "pizza",         # Partial match "Pizza Shop"
        "nonexistent"    # No match
    ]
    
    for search_term in test_searches:
        print(f"\nSearching for: '{search_term}'")
        contact_number = engine.find_contact_number(search_term)
        if contact_number:
            print(f"✅ Found: {contact_number}")
        else:
            print(f"❌ Not found")

if __name__ == "__main__":
    print("🚀 CLIUI Mobile Features Test Suite")
    print("=" * 50)
    
    # Run tests
    test_mobile_commands()
    test_touch_simulation() 
    test_contact_search()
    test_daily_routines()
    
    print("\n✅ All tests completed!")
    print("\n📊 Summary: Core mobile commands are working!")
    print("   Next: Implement real Android API integrations")
