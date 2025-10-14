#!/bin/bash
# smartcli/build-fdroid.sh

cd "$(dirname "$0")"

# Make gradlew executable
chmod +x ./gradlew

# Build core module
./gradlew :core:build

# Build F-Droid app
./gradlew :fdroid:assembleRelease

echo "Build complete! APK location:"
find . -name "*.apk" -type f
