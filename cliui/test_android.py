#!/usr/bin/env python3
"""
Test real Android functionality
"""

import sys
import os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from core.mobile_engine import MobileCLIEngine
from core.android_bridge import AndroidBridge

def test_android_integration():
    """Test Android-specific functionality"""
    print("🤖 Testing Android Integration...")
    print("=" * 50)
    
    android = AndroidBridge()
    print(f"Platform detected: {android.platform}")
    
    engine = MobileCLIEngine(android)
    
    # Test commands that will use real Android intents if available
    test_commands = [
        "call mom",
        "sms Dad Test message",
        "map pizza shop",
        "launch camera",
        "apps"
    ]
    
    for i, cmd in enumerate(test_commands, 1):
        print(f"\n{i}. Testing: {cmd}")
        print("-" * 40)
        result = engine.execute_command(cmd)
        print(result)
        print(f"Platform: {android.platform}")

if __name__ == "__main__":
    test_android_integration()
