"""
SmartCLI Authentication System - Biometric + PIN Support
"""

import getpass

class AuthManager:
    """Manages biometric and PIN authentication for SmartCLI"""
    
    def __init__(self):
        self.is_authenticated = False
        self.max_attempts = 3
        self.default_pin = "1234"  # Default PIN for development
    
    def authenticate(self, method: str = 'biometric') -> bool:
        """Authenticate user with specified method"""
        if method == 'biometric':
            return self._biometric_auth()
        elif method == 'pin':
            return self._pin_auth()
        return False
    
    def _biometric_auth(self) -> bool:
        """Handle biometric authentication with PIN fallback"""
        print("👆 Biometric authentication required...")
        print("   Place your finger on the sensor")
        
        # Simulate biometric check (50% success rate for testing)
        import random
        biometric_success = random.choice([True, False])
        
        if biometric_success:
            print("✅ Biometric authentication successful!")
            self.is_authenticated = True
            return True
        else:
            print("❌ Biometric not recognized")
            print("🔄 Falling back to PIN authentication...")
            return self._pin_auth()
    
    def _pin_auth(self) -> bool:
        """Handle PIN authentication with secure input"""
        attempts = 0
        
        print("\n🔢 PIN Authentication")
        print("   Enter your 4-digit PIN")
        
        while attempts < self.max_attempts:
            try:
                # Secure PIN input (hidden)
                pin_input = getpass.getpass("   PIN: ")
                
                if self._verify_pin(pin_input):
                    print("✅ PIN authentication successful!")
                    self.is_authenticated = True
                    return True
                else:
                    attempts += 1
                    remaining = self.max_attempts - attempts
                    if remaining > 0:
                        print(f"❌ Invalid PIN. Attempts remaining: {remaining}")
                    else:
                        print("🚫 Too many failed attempts")
                        
            except KeyboardInterrupt:
                print("\n↩️  Authentication cancelled")
                return False
            except Exception as e:
                print(f"⚠️  Input error: {e}")
                attempts += 1
        
        return False
    
    def _verify_pin(self, pin: str) -> bool:
        """Verify PIN against stored value"""
        # In production, this would verify against hashed PIN
        return pin == self.default_pin
    
    def logout(self):
        """Logout user (returns to auth screen)"""
        self.is_authenticated = False
        print("\n🔒 Session ended. Authentication required to continue.")
    
    def get_auth_status(self) -> bool:
        """Check if user is currently authenticated"""
        return self.is_authenticated
    
    def change_pin(self, new_pin: str) -> bool:
        """Change user PIN (for future implementation)"""
        if len(new_pin) >= 4 and new_pin.isdigit():
            self.default_pin = new_pin
            print("✅ PIN changed successfully")
            return True
        else:
            print("❌ PIN must be at least 4 digits")
            return False

# Global auth manager instance
auth_manager = AuthManager()
