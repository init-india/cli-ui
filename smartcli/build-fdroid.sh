#!/bin/bash

echo "Building SmartCLI F-Droid version..."

# Clean previous builds
./gradlew clean

# Build release APK
./gradlew :fdroid:assembleRelease

if [ $? -eq 0 ]; then
    echo "Build successful! APK location:"
    find . -name "*.apk" -type f | grep release
else
    echo "Build failed!"
    exit 1
fi
