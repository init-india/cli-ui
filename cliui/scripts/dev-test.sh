#!/bin/bash
echo "🚀 CLIUI Development Testing"
cd cliui

echo "1. Testing new commands..."
python -c "
from core.mobile_engine import MobileCLIEngine
engine = MobileCLIEngine()
print(engine.execute_command('notes test note'))
print(engine.execute_command('calc 15 + 25'))
print(engine.execute_command('timer 5 test'))
print(engine.execute_command('calendar'))
"

echo "2. Testing core functionality..."
python test_mobile.py

echo "✅ Development environment ready!"
