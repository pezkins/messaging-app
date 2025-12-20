#!/bin/bash

# Notification Diagnostic Script
# Run this while your app is running in the emulator/simulator

echo "============================================"
echo "📱 Push Notification Diagnostics"
echo "============================================"
echo ""

# Check which platform to diagnose
echo "Select platform:"
echo "1) iOS Simulator"
echo "2) Android Emulator"
read -p "Enter choice (1 or 2): " choice

case $choice in
  1)
    echo ""
    echo "⚠️  CRITICAL: iOS Simulator DOES NOT support APNs!"
    echo "   APNs (Apple Push Notification Service) only works on REAL DEVICES."
    echo "   You MUST test notifications on a physical iPhone/iPad."
    echo ""
    echo "📋 Checking iOS Simulator logs anyway..."
    echo ""
    
    # Get the booted simulator
    BOOTED_SIM=$(xcrun simctl list devices | grep Booted | head -1 | sed 's/.*(\([^)]*\)).*/\1/')
    
    if [ -z "$BOOTED_SIM" ]; then
      echo "❌ No booted iOS simulator found"
      echo "   Start a simulator first: xcrun simctl boot <device-id>"
      exit 1
    fi
    
    echo "📱 Booted Simulator: $BOOTED_SIM"
    echo ""
    echo "Streaming logs (filtered for notifications)..."
    echo "Look for:"
    echo "  - '📱 Push notification permission granted'"
    echo "  - '📱 APNs Token received'"
    echo "  - '❌ APNs registration failed'"
    echo "  - '✅ Device token registered with backend'"
    echo ""
    echo "Press Ctrl+C to stop"
    echo ""
    
    xcrun simctl spawn booted log stream --level=debug --predicate 'processImagePath contains "Intok" OR eventMessage contains "APNs" OR eventMessage contains "notification" OR eventMessage contains "📱" OR eventMessage contains "token" OR eventMessage contains "Device token"'
    ;;
    
  2)
    echo ""
    echo "📋 Checking Android Emulator logs..."
    echo ""
    
    # Check if adb is available
    if ! command -v adb &> /dev/null; then
      echo "❌ adb not found. Make sure Android SDK platform-tools is in PATH"
      exit 1
    fi
    
    # Check if device is connected
    DEVICES=$(adb devices | grep -v "List" | grep "device$" | wc -l | tr -d ' ')
    if [ "$DEVICES" -eq 0 ]; then
      echo "❌ No Android device/emulator connected"
      echo "   Start an emulator or connect a device"
      exit 1
    fi
    
    echo "📱 Connected devices:"
    adb devices
    echo ""
    echo "Streaming logs (filtered for notifications)..."
    echo "Look for:"
    echo "  - '✅ Notification permission granted'"
    echo "  - '📱 Got FCM token for registration'"
    echo "  - '✅ FCM token registered with backend'"
    echo "  - '📬 Message received from'"
    echo "  - '❌ Failed to' (errors)"
    echo ""
    echo "Press Ctrl+C to stop"
    echo ""
    
    adb logcat -c  # Clear old logs
    adb logcat -s MainActivity:D FCMService:D FirebaseMessaging:D | grep -E "📱|FCM|notification|token|MainActivity|FCMService|✅|❌|⚠️"
    ;;
    
  *)
    echo "Invalid choice"
    exit 1
    ;;
esac
