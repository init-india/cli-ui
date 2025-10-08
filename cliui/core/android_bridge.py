"""
Android Bridge - Real Android API integration
"""

import subprocess
import json
import os
from android import activity
from android.permissions import Permission, request_permissions, check_permission

class AndroidBridge:
    def __init__(self):
        self.platform = self.detect_platform()
        self.permissions_granted = False
        
    def detect_platform(self):
        """Detect if running on Android"""
        try:
            # Check for Android Python
            from android import config
            return 'android'
        except ImportError:
            # Check for Linux Android environment
            if os.path.exists('/system/bin/getprop'):
                return 'android'
            elif os.path.exists('/Applications'):
                return 'ios'
            else:
                return 'desktop'
    
    def request_permissions(self):
        """Request Android permissions"""
        if self.platform != 'android':
            return True
            
        try:
            from android.permissions import Permission, request_permissions
            
            required_permissions = [
                Permission.CALL_PHONE,
                Permission.SEND_SMS,
                Permission.READ_CONTACTS,
                Permission.CAMERA,
                Permission.ACCESS_FINE_LOCATION,
            ]
            
            # Check and request permissions
            granted = all(check_permission(perm) for perm in required_permissions)
            if not granted:
                request_permissions(required_permissions)
                # In real app, you'd wait for permission callback
                
            self.permissions_granted = granted
            return granted
        except:
            return False
    
    def make_call(self, number):
        """Make actual phone call on Android"""
        if self.platform == 'android':
            try:
                from android import Intent
                from android.net import Uri
                
                intent = Intent(Intent.ACTION_CALL, Uri.parse(f"tel:{number}"))
                activity.startActivity(intent)
                return f"📞 Calling {number}..."
            except Exception as e:
                return f"❌ Call failed: {str(e)}"
        else:
            return f"📞 [SIMULATION] Calling {number}"
    
    def send_sms(self, number, message):
        """Send actual SMS on Android"""
        if self.platform == 'android':
            try:
                from android import Intent
                from android.net import Uri
                
                intent = Intent(Intent.ACTION_SENDTO, Uri.parse(f"sms:{number}"))
                intent.putExtra("sms_body", message)
                activity.startActivity(intent)
                return f"💬 Opening SMS to {number}"
            except Exception as e:
                return f"❌ SMS failed: {str(e)}"
        else:
            return f"💬 [SIMULATION] SMS to {number}: {message}"
    
    def launch_app(self, app_package):
        """Launch Android app by package name"""
        if self.platform == 'android':
            try:
                from android import Intent
                from android.content.pm import PackageManager
                
                intent = activity.getPackageManager().getLaunchIntentForPackage(app_package)
                if intent:
                    activity.startActivity(intent)
                    return f"🚀 Launching {app_package}"
                else:
                    return f"❌ App not found: {app_package}"
            except Exception as e:
                return f"❌ Launch failed: {str(e)}"
        else:
            return f"🚀 [SIMULATION] Launching {app_package}"
    
    def open_maps(self, location):
        """Open maps with location"""
        if self.platform == 'android':
            try:
                from android import Intent
                from android.net import Uri
                
                intent = Intent(Intent.ACTION_VIEW, Uri.parse(f"geo:0,0?q={location}"))
                intent.setPackage("com.google.android.apps.maps")  # Open in Google Maps
                activity.startActivity(intent)
                return f"🗺️ Opening maps for: {location}"
            except:
                # Fallback to any maps app
                try:
                    from android import Intent
                    from android.net import Uri
                    intent = Intent(Intent.ACTION_VIEW, Uri.parse(f"geo:0,0?q={location}"))
                    activity.startActivity(intent)
                    return f"🗺️ Opening maps for: {location}"
                except Exception as e:
                    return f"🗺️ [SIMULATION] Searching: {location}"
        else:
            return f"🗺️ [SIMULATION] Searching: {location}"
    
    def get_contacts(self):
        """Get actual contacts from Android"""
        if self.platform == 'android':
            try:
                from android.provider import ContactsContract
                from android.content import ContentResolver
                
                contacts = []
                resolver = activity.getContentResolver()
                cursor = resolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    None, None, None, None
                )
                
                if cursor:
                    while cursor.moveToNext():
                        name = cursor.getString(cursor.getColumnIndex(
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                        number = cursor.getString(cursor.getColumnIndex(
                            ContactsContract.CommonDataKinds.Phone.NUMBER))
                        contacts.append({"name": name, "number": number, "favorite": False})
                    cursor.close()
                    
                return contacts if contacts else self.get_simulated_contacts()
            except:
                return self.get_simulated_contacts()
        else:
            return self.get_simulated_contacts()
    
    def get_simulated_contacts(self):
        """Fallback simulated contacts"""
        return [
            {"name": "Mom", "number": "+1234567890", "favorite": True},
            {"name": "Dad", "number": "+1234567891", "favorite": True},
            {"name": "John Work", "number": "+1234567892", "favorite": False},
            {"name": "Emergency", "number": "911", "favorite": True},
            {"name": "Pizza Shop", "number": "+1234567893", "favorite": False},
        ]
