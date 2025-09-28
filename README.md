SmartCLI transforms your Android smartphone into a command-line interface. Control your phone using Linux-style commands with a beautiful TUI (Terminal User Interface).
🚀 Quick Start

    Install SmartCLI from F-Droid

    Set as default launcher when prompted

    Type help to see available commands

    Start controlling your phone via CLI!

📋 Available Commands
📞 PHONE & CALLS
bash

call                    # Show call history
call [contact]          # Call a contact/number
call [number]           # Call specific number

Examples:
bash

call                    # View recent calls
call john               # Call John
call +1555123456        # Call specific number

💬 SMS & MESSAGING
bash

sms                     # Show recent messages
sms [contact]           # View conversation with contact
sms [contact] [message] # Send SMS to contact

Examples:
bash

sms                     # View message inbox
sms john                # Open conversation with John
sms john "Hello there!" # Send message to John

🗺️ NAVIGATION & MAPS
bash

map                     # Show recent locations
map [destination]       # Navigate to destination
map [source];[dest]     # Route from source to destination
nav                     # Start navigation

Examples:
bash

map                     # View location history
map starbucks           # Navigate to Starbucks
map home;office         # Route from home to office
nav                     # Start active navigation

📶 CONNECTIVITY
bash

wifi                    # Toggle WiFi on/off
bluetooth               # Toggle Bluetooth on/off  
hotspot                 # Toggle hotspot on/off

Examples:
bash

wifi                    # Turn WiFi on (if off) or show status
bluetooth               # Toggle Bluetooth
hotspot                 # Toggle mobile hotspot

⚡ SYSTEM CONTROLS
bash

flash                   # Toggle flashlight on/off
location                # Show location status
mic                     # Toggle microphone on/off
camera                  # Open camera app

Examples:
bash

flash                   # Turn flashlight on/off
location                # Check location services status
camera                  # Launch camera application

📧 EMAIL & COMMUNICATION
bash

mail                    # Show recent emails
mail compose            # Compose new email
mail open [1-3]         # Read specific email
whatsapp                # Open WhatsApp

Examples:
bash

mail                    # View email inbox
mail compose            # Write new email
mail open 1             # Read first email
whatsapp                # Launch WhatsApp

⚙️ SYSTEM SETTINGS
bash

settings                # Show all settings
settings display        # Display settings
settings sound          # Sound settings  
settings security       # Security settings
settings brightness [0-100]  # Change brightness
settings media [0-100]       # Change media volume
settings ringtone [name]     # Change ringtone

Examples:
bash

settings                # View all system settings
settings display        # Show display options
settings brightness 75  # Set brightness to 75%
settings media 80       # Set media volume to 80%

🔔 NOTIFICATIONS
bash

notifications           # Show notification log

🐧 LINUX ENVIRONMENT
bash

# Basic Linux commands available
ls, cd, pwd, cat, grep, find, ps, top, etc.

🛠️ UTILITY COMMANDS
bash

help                    # Show this help menu
clear                   # Clear terminal screen
exit                    # Return to previous context

🎨 TUI FEATURES
Color Coding:

    🟢 Green - Command prompts and success messages

    ⚪ White - Command output and information

    🔴 Red - Error messages and warnings

    🟡 Yellow - Important notifications

Symbols:

    ✅ Success actions

    ❌ Errors and failures

    💡 Information and tips

    📱 Phone operations

    💬 Messaging

    🗺️ Navigation

    📶 Connectivity

    ⚙️ Settings

🔐 SECURITY FEATURES

    Biometric authentication for sensitive operations

    PIN fallback support

    App-specific access controls

    Privacy-focused design

🌐 OPEN SOURCE FEATURES

    ✅ F-Droid compatible - No Google dependencies

    ✅ OpenStreetMap integration - No Google Maps

    ✅ IMAP email support - No Gmail API required

    ✅ Pure FOSS dependencies only

🚨 PERMISSIONS EXPLAINED

SmartCLI requests these permissions for full functionality:

    READ_SMS - View your messages

    SEND_SMS - Send messages

    CALL_PHONE - Make phone calls

    LOCATION - Navigation and maps

    CAMERA - Flashlight control and camera access

    CONTACTS - Access your contact list

🔄 NAVIGATION TIPS
Context Management:
bash

cli> call john
📞 Calling John...
call> mute              # In-call commands
call> exit              # Return to main CLI

cli> sms john
💬 Conversation with John...
sms> reply "Hello"      # Reply to message
sms> exit               # Back to message list
sms> exit               # Back to main CLI

Dynamic Suggestions:

    Type partial commands for auto-suggestions

    Use Tab-completion where available

    Context-aware help in each module

🐛 TROUBLESHOOTING
Common Issues:
bash

# Command not found
❌ Command not found: [command]
💡 Type 'help' for available commands

# Permission denied  
❌ Permission required for [feature]
💡 Grant the requested permission in Android Settings

# Feature not available
❌ Feature requires [app/service]
💡 Install the required application

Connectivity Notes:

    Some connectivity commands show status only

    Actual toggling may require system permissions

    Uses Android intents for system integration

📖 ADVANCED USAGE
Batch Operations:
bash

# Chain commands with semicolons
sms john "Meeting at 3"; call sarah; map office

# Use in scripts for automation

Customization:

    Theme colors in settings

    Command aliases support

    Notification preferences

🤝 CONTRIBUTING

SmartCLI is open source! Contribute at:
[GitHub Repository URL]
📄 LICENSE

GPL-3.0 - Free and open source software

Enjoy controlling your smartphone the Linux way! 🐧📱

