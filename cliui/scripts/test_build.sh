#!/bin/bash

echo "🧪 Testing Android Build Setup..."

cd platforms/android

# Check if gradlew exists
if [ ! -f "gradlew" ]; then
    echo "❌ gradlew file not found!"
    exit 1
fi

# Check if it's executable
if [ ! -x "gradlew" ]; then
    echo "❌ gradlew is not executable!"
    exit 1
fi

echo "✅ gradlew found and executable"

# Try to get Gradle version
./gradlew --version

echo "✅ Android build setup is ready!"
