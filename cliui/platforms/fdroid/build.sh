#!/bin/bash
echo "🚀 Building SmartCLI APK..."

# Activate virtual environment
source ../../smartcli-env/bin/activate

# Build the APK
buildozer android debug

if [ -f "bin/smartcli-0.1-debug.apk" ]; then
    echo "✅ Build successful! APK: bin/smartcli-0.1-debug.apk"
    echo "📱 Install with: adb install bin/smartcli-0.1-debug.apk"
else
    echo "❌ Build failed. Check logs above."
fi
