#!/system/bin/sh
# SmartCLI Linux Bootstrap Script - F-Droid Compatible

echo "SmartCLI Linux Environment"
echo "For full Linux environment, install Termux from F-Droid"
echo "Then use: termux-setup-storage"
echo "And: pkg install python git curl"

export SMARTCLI_HOME=/data/data/com.cliui/files
mkdir -p $SMARTCLI_HOME

echo "SmartCLI ready for basic Linux commands"
