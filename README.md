SmartCLI Phone - Complete Command Reference
📱 Overview

SmartCLI Phone transforms your Android device into a command-line interface smartphone. Control everything through CLI commands - calls, messages, navigation, settings, and more.
📞 PHONE & CALLS
Basic Call Commands
text

call [number]           - Make phone call to number
call [contact]          - Make call to contact name
call                    - Show call history

Advanced Call Controls (During Active Calls)
text

ans                     - Answer incoming call
rej                     - Reject incoming call
end                     - End current call
hold                    - Put call on hold
unhold                  - Resume held call
mute                    - Mute microphone
unmute                  - Unmute microphone
speakeron               - Enable speakerphone
speakeroff              - Disable speakerphone

Conference Call Features
text

call [number]           - Add another call during active call
merge                   - Merge active calls into conference
drop [contact]          - Remove participant from conference
status                  - Show current call status

💬 SMS & MESSAGING
SMS Commands
text

sms                     - Show recent messages (20 per page)
sms more                - Show next 20 messages
sms all                 - Show all messages
sms [contact]           - Show conversation with contact
sms [contact] [message] - Send message directly
sms del                 - Enter delete mode

Interactive SMS Workflow
text

[message_id]            - Open specific message (with authentication)
reply                   - Reply to opened message
send                    - Send composed message
exit                    - Return to message list

🗺️ NAVIGATION & MAPS
Map Commands
text

map                     - Show recent location searches
map [destination]       - Search and navigate to destination
map [source];[dest]     - Route with specific source

Navigation Flow
text

[1-5]                   - Select location from suggestions
nav                     - Start navigation with voice guidance
alt                     - Show alternative routes
mute/unmute             - Toggle voice guidance
pause/resume            - Pause/resume navigation
status                  - Show navigation status

⚙️ SYSTEM SETTINGS
Display Settings
text

settings display brightness <0-100>    - Set screen brightness
settings display dark on/off           - Toggle dark mode
settings display rotation on/off       - Toggle auto-rotate
settings display timeout <seconds>     - Set screen timeout

Sound Settings
text

settings sound media <0-100>           - Set media volume
settings sound call <0-100>            - Set call volume
settings sound alarm <0-100>           - Set alarm volume
settings sound vibrate on/off          - Toggle vibration

Quick Settings
text

settings                                - Show all settings categories
settings display                       - Show display settings
settings sound                         - Show sound settings
settings security                      - Show security settings

🔗 CONNECTIVITY
WiFi Control
text

wifi                    - Toggle WiFi on/off
wifi                    - Show available networks (when on)
[1-10]                  - Connect to selected network

Bluetooth Control
text

bluetooth               - Toggle Bluetooth on/off
bluetooth               - Show paired devices (when on)
[1-10]                  - Connect to selected device

Other Connectivity
text

hotspot                 - Toggle mobile hotspot
location                - Toggle location services

🖥️ SYSTEM CONTROLS
Hardware Controls
text

flash                   - Toggle flashlight
camera                  - Open camera app
mic                     - Toggle microphone access

System Information
text

telephony               - Show phone information
telephony network       - Show network info
telephony sim           - Show SIM status
telephony signal        - Show signal strength

📧 EMAIL & GMAIL
Email Commands
text

mail                    - Show inbox (20 emails per page)
mail more               - Show next 20 emails
mail all                - Show all emails
mail compose            - Compose new email
mail [sender]           - Filter by sender
mail search [query]     - Search emails
mail del                - Delete emails

Email Workflow
text

[email_id]              - Open specific email
1/reply                 - Reply to email
2/replyall              - Reply all to email
send                    - Send composed email
to [email]              - Set recipient
cc [email]              - Set CC recipient
bcc [email]             - Set BCC recipient
subject [title]         - Set email subject
attach [file]           - Add attachment

💚 WHATSAPP INTEGRATION
WhatsApp Commands
text

wh                      - Show quick actions & recent contacts
wh [contact]            - Start messaging contact
wh call [contact]       - Call contact via WhatsApp
whcall                  - Show call history

WhatsApp Workflow
text

[1-5]                   - Select contact from list
[message]               - Type message content
send                    - Send WhatsApp message
1                       - Audio call
2                       - Video call

👥 CONTACTS MANAGEMENT
Contact Commands
text

contact                 - Show all contacts (alphabetical)
contact add             - Add new contact
contact edit [name]     - Edit existing contact
contact del             - Delete contacts

Contact Workflow
text

name [full_name]        - Set contact name
number [phone]          - Set phone number
email [address]         - Set email address
save                    - Save contact
[1,3,5]                 - Select contacts to delete (comma separated)
confirm                 - Confirm deletion

🐧 LINUX ENVIRONMENT
Direct Linux Commands
text

[any_linux_command]     - Execute Linux command directly

File Operations
text

ls [path]               - List directory contents
cat [file]              - Display file content
pwd                     - Show current directory
df                      - Show disk usage
ps                      - Show running processes

Package Management
text

pkg list                - List installed packages
pkg info [package]      - Show package information
pkg install [package]   - Install package
pkg remove [package]    - Remove package
pkg enable [package]    - Enable package
pkg disable [package]   - Disable package

Linux Environment
text

linux                   - Show Linux environment info
shell                   - Enter interactive shell mode
files                   - Show available file operations
sysinfo                 - Show system information

🛠️ UTILITY COMMANDS
System Utilities
text

clear                   - Clear terminal screen
help                    - Show help information
exit                    - Exit current mode/return to main CLI
apps                    - Show installed applications

Authentication
text

[Biometric/PIN]         - Required for sensitive operations:
                          - Reading messages/emails
                          - Contact management
                          - Security settings

🔐 PERMISSION REQUIREMENTS
Required Permissions

    Phone: CALL_PHONE, READ_PHONE_STATE

    SMS: SEND_SMS, READ_SMS, RECEIVE_SMS

    Contacts: READ_CONTACTS, WRITE_CONTACTS

    Location: ACCESS_FINE_LOCATION

    Storage: READ_EXTERNAL_STORAGE

    Camera/Mic: CAMERA, RECORD_AUDIO

Shizuku Integration

    System controls: WiFi, Bluetooth, Hotspot, Settings

    File operations: Linux file system access

    Package management: App installation/removal

    Root commands: Advanced system operations

💡 TIPS & BEST PRACTICES
Command Patterns

    Space-separated: call 1234567890

    Semicolon for pairs: map home;work

    Numbers for selection: 1, 2, 3 for lists

    Keywords for actions: reply, send, exit

State Management

    Use exit to return to previous state

    Commands change behavior based on context

    Authentication required for private data

    Real-time feedback for all operations

Enhanced Features

    Voice guidance during navigation

    Biometric authentication for security

    Dynamic suggestions while typing

    Pagination for long lists

    Draft saving for unfinished messages

🚀 GETTING STARTED

    Install the app from F-Droid or Google Play

    Grant permissions when prompted for each feature

    Enable Shizuku for advanced system control (optional)

    Start typing commands - the CLI will guide you

    Use help anytime for command reference

🔧 TROUBLESHOOTING
Common Issues

    Permission denied: Type command again to grant permissions

    Feature not available: Enable Shizuku for system controls

    Authentication failed: Use biometric or PIN authentication

    Command not found: Check spelling or use help

Enhanced Capabilities

    Install Termux from F-Droid for full Linux environment

    Enable Shizuku for root-level system control

    Use Google Play Services for enhanced location/maps

SmartCLI Phone - Your smartphone, reimagined for the command line. 🚀📱
