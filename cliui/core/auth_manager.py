"""
Authentication Manager - Handle biometric/PIN authentication
"""

class AuthManager:
    def __init__(self):
        pass
    
    def authenticate(self) -> bool:
        """Authenticate user (skip for development)"""
        # TODO: Implement biometric/PIN authentication
        return True  # Skip for now
    
    def request_authentication(self, reason: str = "") -> bool:
        """Request authentication for sensitive operations"""
        print(f"🔒 Authentication required: {reason}")
        # TODO: Implement actual authentication
        return True
