[app]
title = SmartCLI
package.name = smartcli
package.domain = org.smartcli

source.dir = .
source.include_exts = py,png,jpg,kv,atlas,txt

version = 0.1
requirements = python3,kivy

[buildozer]
log_level = 2

[android]
api = 33
minapi = 21
ndk = 25b
sdk = 33

# Android permissions
android.permissions = INTERNET,ACCESS_NETWORK_STATE,READ_CONTACTS,WRITE_CONTACTS,READ_CALL_LOG,CALL_PHONE,READ_SMS,SEND_SMS,RECEIVE_SMS,ACCESS_FINE_LOCATION,ACCESS_COARSE_LOCATION,CAMERA,FLASHLIGHT,ACCESS_WIFI_STATE,CHANGE_WIFI_STATE,BLUETOOTH,BLUETOOTH_ADMIN

# Orientation
android.orientation = portrait

# Presplash screen
presplash.filename = %(source.dir)s/assets/presplash.png

# App icon
icon.filename = %(source.dir)s/assets/icon.png

[loggers]
root = WARNING
