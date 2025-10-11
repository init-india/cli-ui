[app]
title = SmartCLI
package.name = smartcli
package.domain = org.smartcli

[buildozer]
log_level = 2

[requirements]
python3 = 3.8
kivy = 2.1.0

# Android permissions
android.permissions = INTERNET,READ_CONTACTS,WRITE_CONTACTS,READ_CALL_LOG,CALL_PHONE,READ_SMS,SEND_SMS,RECEIVE_SMS,ACCESS_FINE_LOCATION,ACCESS_COARSE_LOCATION,CAMERA,FLASHLIGHT,ACCESS_WIFI_STATE,CHANGE_WIFI_STATE,BLUETOOTH,BLUETOOTH_ADMIN,ACCESS_NETWORK_STATE

[app]
source.dir = platforms/fdroid
source.include_exts = py,png,jpg,kv,atlas

# Orientation
orientation = portrait

# Presplash
presplash.filename = %(source.dir)s/assets/presplash.png

# Icon
icon.filename = %(source.dir)s/assets/icon.png
