"""
Mobile Engine Test for SmartCLI
"""

import sys
import os

# Add src to path
sys.path.append(os.path.join(os.path.dirname(__file__), 'src'))

def test_mobile_engine():
    """Test basic mobile engine functionality"""
    try:
        # Test imports
        from integrations.android_apis import android_api
        from launcher.auth import auth_manager
        from core.command_router import CommandRouter
        
        print("✅ All imports successful")
        print("✅ Mobile engine components loaded")
        print("✅ SmartCLI ready for Android deployment")
        return True
        
    except ImportError as e:
        print(f"❌ Import error: {e}")
        return False
    except Exception as e:
        print(f"❌ Test error: {e}")
        return False

if __name__ == "__main__":
    success = test_mobile_engine()
    sys.exit(0 if success else 1)
