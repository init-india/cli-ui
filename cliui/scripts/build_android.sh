#!/bin/bash

echo "🚀 Building CLIUI Android APK..."

cd platforms/android

# Clean previous builds
./gradlew clean

# Build debug APK
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo "✅ Build successful!"
    echo "📦 APK location: app/build/outputs/apk/debug/app-debug.apk"
    
    # List the APK
    ls -la app/build/outputs/apk/debug/app-debug.apk
else
    echo "❌ Build failed!"
    exit 1
fi
