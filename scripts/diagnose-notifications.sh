#!/bin/bash

# Comprehensive Notification Diagnostic Script
# Checks all potential issues with push notifications

echo "============================================"
echo "🔍 Push Notification Diagnostic Tool"
echo "============================================"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if AWS CLI is available
if ! command -v aws &> /dev/null; then
    echo -e "${RED}❌ AWS CLI not found. Install it to check backend configuration.${NC}"
    echo ""
fi

# Check iOS Simulator
echo "📱 iOS Simulator Check:"
BOOTED_IOS=$(xcrun simctl list devices | grep Booted | head -1)
if [ -n "$BOOTED_IOS" ]; then
    echo -e "${YELLOW}⚠️  iOS Simulator detected:${NC}"
    echo "   $BOOTED_IOS"
    echo -e "${RED}   ⚠️  CRITICAL: iOS Simulator DOES NOT support APNs!${NC}"
    echo -e "${RED}   ⚠️  You MUST test on a physical iPhone/iPad${NC}"
else
    echo -e "${GREEN}✅ No iOS Simulator running${NC}"
fi
echo ""

# Check Android Emulator
echo "🤖 Android Emulator Check:"
ANDROID_DEVICES=$(adb devices | grep -v "List" | grep "device$" | wc -l | tr -d ' ')
if [ "$ANDROID_DEVICES" -gt 0 ]; then
    echo -e "${GREEN}✅ Android device/emulator connected${NC}"
    adb devices | grep "device$"
    
    # Check for Google Play Services
    echo ""
    echo "   Checking Google Play Services..."
    GPS_CHECK=$(adb shell pm list packages | grep "com.google.android.gms" | head -1)
    if [ -n "$GPS_CHECK" ]; then
        echo -e "${GREEN}   ✅ Google Play Services installed${NC}"
    else
        echo -e "${YELLOW}   ⚠️  Google Play Services NOT found${NC}"
        echo -e "${YELLOW}   ⚠️  FCM requires Google Play Services${NC}"
        echo -e "${YELLOW}   ⚠️  Use an emulator with Google APIs${NC}"
    fi
    
    # Check for Firebase
    FIREBASE_CHECK=$(adb shell pm list packages | grep "com.google.firebase" | head -1)
    if [ -n "$FIREBASE_CHECK" ]; then
        echo -e "${GREEN}   ✅ Firebase services detected${NC}"
    else
        echo -e "${YELLOW}   ⚠️  Firebase services not detected (may be normal)${NC}"
    fi
else
    echo -e "${YELLOW}⚠️  No Android device/emulator connected${NC}"
fi
echo ""

# Check backend secrets (if AWS CLI available)
if command -v aws &> /dev/null; then
    echo "☁️  Backend Configuration Check:"
    
    # Check APNs secret
    echo "   Checking APNs credentials..."
    APNS_SECRET=$(aws secretsmanager describe-secret --secret-id intok/push/apns 2>&1)
    if echo "$APNS_SECRET" | grep -q "ResourceNotFoundException"; then
        echo -e "${RED}   ❌ APNs secret NOT found: intok/push/apns${NC}"
        echo -e "${RED}   ❌ iOS push notifications will be disabled${NC}"
    else
        echo -e "${GREEN}   ✅ APNs secret exists${NC}"
    fi
    
    # Check FCM secret
    echo "   Checking FCM credentials..."
    FCM_SECRET=$(aws secretsmanager describe-secret --secret-id intok/push/fcm 2>&1)
    if echo "$FCM_SECRET" | grep -q "ResourceNotFoundException"; then
        echo -e "${RED}   ❌ FCM secret NOT found: intok/push/fcm${NC}"
        echo -e "${RED}   ❌ Android push notifications will be disabled${NC}"
    else
        echo -e "${GREEN}   ✅ FCM secret exists${NC}"
    fi
    echo ""
else
    echo -e "${YELLOW}⚠️  AWS CLI not available - skipping backend checks${NC}"
    echo "   Install AWS CLI to check backend configuration"
    echo ""
fi

# Check recent logs
echo "📋 Recent Logs Check:"
echo ""

# iOS logs
if [ -n "$BOOTED_IOS" ]; then
    echo "   iOS Simulator logs (last 2 minutes):"
    IOS_LOGS=$(xcrun simctl spawn booted log show --last 2m --predicate 'processImagePath contains "Intok"' 2>/dev/null | grep -E "📱|APNs|notification|token|❌|✅" | tail -5)
    if [ -n "$IOS_LOGS" ]; then
        echo "$IOS_LOGS" | sed 's/^/      /'
    else
        echo -e "${YELLOW}      No recent notification logs found${NC}"
    fi
    echo ""
fi

# Android logs
if [ "$ANDROID_DEVICES" -gt 0 ]; then
    echo "   Android logs (last 50 lines):"
    ANDROID_LOGS=$(adb logcat -d -t 50 | grep -E "MainActivity|FCMService|📱|FCM|notification|token|✅|❌" | tail -5)
    if [ -n "$ANDROID_LOGS" ]; then
        echo "$ANDROID_LOGS" | sed 's/^/      /'
    else
        echo -e "${YELLOW}      No recent notification logs found${NC}"
    fi
    echo ""
fi

# Summary
echo "============================================"
echo "📊 Summary"
echo "============================================"
echo ""

ISSUES=0

if [ -n "$BOOTED_IOS" ]; then
    echo -e "${RED}❌ Issue #1: iOS Simulator cannot receive APNs${NC}"
    echo "   → Test on physical iPhone/iPad"
    ISSUES=$((ISSUES + 1))
fi

if [ "$ANDROID_DEVICES" -gt 0 ] && [ -z "$GPS_CHECK" ]; then
    echo -e "${YELLOW}⚠️  Issue #2: Google Play Services not found on Android${NC}"
    echo "   → Use emulator with Google APIs"
    ISSUES=$((ISSUES + 1))
fi

if command -v aws &> /dev/null; then
    if echo "$APNS_SECRET" | grep -q "ResourceNotFoundException"; then
        echo -e "${RED}❌ Issue #3: APNs secret not configured${NC}"
        echo "   → Configure intok/push/apns in AWS Secrets Manager"
        ISSUES=$((ISSUES + 1))
    fi
    
    if echo "$FCM_SECRET" | grep -q "ResourceNotFoundException"; then
        echo -e "${RED}❌ Issue #4: FCM secret not configured${NC}"
        echo "   → Configure intok/push/fcm in AWS Secrets Manager"
        ISSUES=$((ISSUES + 1))
    fi
fi

if [ $ISSUES -eq 0 ]; then
    echo -e "${GREEN}✅ No obvious issues detected${NC}"
    echo ""
    echo "Next steps:"
    echo "1. Ensure user is OFFLINE (app closed) when testing"
    echo "2. Check CloudWatch logs for notification send attempts"
    echo "3. Verify device tokens are registered in DynamoDB"
else
    echo ""
    echo -e "${YELLOW}⚠️  Found $ISSUES potential issue(s)${NC}"
    echo ""
    echo "See team/ROOT_CAUSE_ANALYSIS_NOTIFICATIONS.md for details"
fi

echo ""
echo "============================================"
echo "📚 Documentation"
echo "============================================"
echo "Full analysis: team/ROOT_CAUSE_ANALYSIS_NOTIFICATIONS.md"
echo "Diagnostics guide: team/NOTIFICATION_DIAGNOSTICS.md"
echo ""
