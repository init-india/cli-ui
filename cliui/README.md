# CLIUI - Touch Linux Mobile Interface

A revolutionary command-line interface launcher for Android that replaces traditional GUI with efficient CLI commands and touch support.

## 🎯 Features

- **Default Mobile Launcher** - Replace your home screen
- **Touch-Optimized CLI** - Numbered selections, gestures
- **90% Daily Tasks** - Calls, SMS, navigation, apps, media
- **Cross-Platform** - Android, iOS, F-Droid, Google Play
- **Open Source** - GPL-3.0 Licensed

## 🚀 Quick Start

```bash
# Desktop testing
python main.py

# Mobile (Termux on Android)
pkg install python
python main.py

📱 Commands
bash

call mom                    # Make phone call
sms john "Hello"           # Send SMS
map coffee shop            # Search location
music play                 # Control music
apps                       # List installed apps
launch camera              # Open camera app

🛠️ Development
bash

git clone <this-repo>
cd cliui
python test_mobile.py      # Test mobile commands
python test_android.py     # Test Android integration

📄 License

GPL-3.0 - See LICENSE file for details.
text


### 4. **Create LICENSE file**
**cliui/LICENSE**

GNU GENERAL PUBLIC LICENSE
Version 3, 29 June 2007

Copyright (C) 2024 CLIUI Team

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see https://www.gnu.org/licenses/.
text


### 5. **Initial Git Commit**
```bash
cd ~/Desktop/cli/cli-ui/cliui

# Add all files
git add .

# Initial commit
git commit -m "feat: Initial CLIUI mobile launcher implementation

- Core mobile engine with 20+ commands
- Touch-optimized interface
- Android bridge for real device integration
- Cross-platform architecture
- F-Droid ready structure

Ready for mobile testing and deployment."

# Check status
git status

6. Create GitHub Repository (Optional)

If you want to host on GitHub:
bash

# Create new repo on GitHub.com, then:
git remote add origin https://github.com/yourusername/cliui.git
git branch -M main
git push -u origin main

7. Development Workflow

Create development branch:
bash

git checkout -b development

Make changes and commit:
bash

# After making changes
git add .
git commit -m "feat: Add real Android calling integration

- Implement Android Intent for phone calls
- Add permission handling
- Update mobile engine with real device support
- Test on desktop and Android environments"

# Push to development branch
git push origin development

8. Useful Git Commands for Development
bash

# Check status
git status

# See commit history
git log --oneline

# Create feature branch
git checkout -b feature/android-calling

# Merge back to main
git checkout main
git merge feature/android-calling

# Update from remote
git pull origin main

# Tag a release
git tag v1.0.0
git push origin v1.0.0

9. Project Structure Overview
text

cliui/
├── .git/                 # Git repository
├── .gitignore           # Git ignore rules
├── README.md            # Project documentation
├── LICENSE              # GPL-3.0 license
├── main.py              # Entry point
├── core/                # Core engine
├── modules/             # Feature modules
├── ui/                  # User interface
├── platforms/           # Android/iOS specific
├── tests/               # Test suites
└── docs/                # Documentation
