# 🔔 Push Notification Diagnostics Guide

## ⚠️ CRITICAL: iOS Simulator Limitation

**iOS Simulator DOES NOT support APNs (Apple Push Notification Service).**

- APNs only works on **real iOS devices** (iPhone/iPad)
- Simulator will show: `❌ APNs registration failed: no valid 'aps-environment' entitlement`
- You **MUST test notifications on a physical device** for iOS

**Android Emulator CAN work** with FCM, but requires:
- Google Play Services installed on emulator
- Proper Firebase configuration

---

## 📋 Diagnostic Checklist

### iOS (Real Device Only)

**What to check in logs:**

1. **Permission Request:**
   ```
   📱 Push notification permission granted
   ```

2. **APNs Token Registration:**
   ```
   📱 APNs Token received: <token>...
   ```
   - If you see `❌ APNs registration failed`, check:
     - Device has internet connection
     - App has Push Notifications capability enabled in Xcode
     - Entitlements file has `aps-environment: production`

3. **Backend Registration:**
   ```
   ✅ Device token registered with backend
   ```
   - If you see `❌ Failed to register device token`, check:
     - User is logged in
     - Backend API is accessible
     - Token format is correct (64 hex characters)

4. **Notification Reception:**
   ```
   📱 Notification received in foreground
   📱 Notification tapped: <userInfo>
   ```

**Common Issues:**

| Error | Cause | Fix |
|-------|-------|-----|
| `no valid 'aps-environment' entitlement` | Missing capability | Enable Push Notifications in Xcode → Signing & Capabilities |
| `BadDeviceToken` | Invalid token format | Check token is 64 hex chars, not base64 |
| `DeviceTokenNotForTopic` | Wrong bundle ID | Verify `apns-topic` matches bundle ID (`com.pezkins.intok`) |
| `Unregistered` | Token expired/changed | Re-register token after app reinstall |

---

### Android (Emulator or Device)

**What to check in logs:**

1. **Permission Request (Android 13+):**
   ```
   ✅ Notification permission granted
   ```

2. **FCM Token Retrieval:**
   ```
   📱 Got FCM token for registration
   ```
   - If you see `❌ Failed to get FCM token`, check:
     - Google Play Services installed on emulator
     - `google-services.json` is in `app/` directory
     - Firebase project is configured correctly

3. **Backend Registration:**
   ```
   ✅ FCM token registered with backend
   ```
   - If you see `❌ Failed to register token`, check:
     - User is logged in
     - Backend API is accessible
     - Network connectivity

4. **Notification Reception:**
   ```
   📬 Message received from: <sender>
   📬 Notification shown: <title> (id: <id>, conv: <convId>)
   ```

**Common Issues:**

| Error | Cause | Fix |
|-------|-------|-----|
| `MISSING_INSTANCEID_SERVICE` | Google Play Services missing | Install Google Play Services on emulator |
| `SERVICE_NOT_AVAILABLE` | FCM not configured | Check `google-services.json` exists |
| `INVALID_SENDER` | Wrong sender ID | Verify Firebase project matches `google-services.json` |
| No notification shown | Channel not created | Check notification channel is created before showing |

---

## 🔍 How to Check Logs

### iOS Simulator/Device

**Method 1: Xcode Console**
- Run app from Xcode
- View logs in bottom console panel
- Filter by "APNs" or "notification"

**Method 2: Terminal (Simulator)**
```bash
# Stream logs
xcrun simctl spawn booted log stream --level=debug --predicate 'processImagePath contains "Intok"'

# View recent logs
xcrun simctl spawn booted log show --last 5m --predicate 'processImagePath contains "Intok"' | grep -i "apns\|notification\|token\|📱"
```

**Method 3: Console.app (Device)**
- Connect device via USB
- Open Console.app
- Select your device
- Filter by "Intok" or process name

### Android Emulator/Device

**Method 1: Android Studio Logcat**
- Open Logcat panel
- Filter by: `MainActivity|FCMService|📱`

**Method 2: Terminal (adb)**
```bash
# Stream filtered logs
adb logcat -s MainActivity:D FCMService:D FirebaseMessaging:D | grep -E "📱|FCM|notification|token"

# View recent logs
adb logcat -d -t 100 | grep -E "MainActivity|FCMService|📱|FCM|notification|token"

# Clear and start fresh
adb logcat -c && adb logcat | grep -E "MainActivity|FCMService|📱|FCM"
```

**Method 3: Use Diagnostic Script**
```bash
./scripts/check-notification-logs.sh
```

---

## 🧪 Testing Steps

### iOS (Real Device)

1. **Build and install on device:**
   ```bash
   # In Xcode: Product → Destination → Select your iPhone
   # Or use fastlane: fastlane ios build_device
   ```

2. **Check logs for:**
   - Permission granted ✅
   - APNs token received ✅
   - Token registered with backend ✅

3. **Send test notification:**
   - Have another user send you a message
   - Or use backend API to send test notification
   - Check CloudWatch logs for send attempts

4. **Verify notification appears:**
   - App closed: Notification should appear
   - App in background: Notification should appear
   - App in foreground: Notification banner should appear

### Android (Emulator/Device)

1. **Build and install:**
   ```bash
   ./gradlew installDebug
   ```

2. **Check logs for:**
   - Permission granted (Android 13+) ✅
   - FCM token received ✅
   - Token registered with backend ✅

3. **Send test notification:**
   - Have another user send you a message
   - Or use Firebase Console → Cloud Messaging → Send test message

4. **Verify notification appears:**
   - App closed: Notification should appear
   - App in background: Notification should appear
   - App in foreground: Notification should appear

---

## 🔧 Backend Verification

**Check CloudWatch Logs:**

1. Go to AWS Console → CloudWatch → Log Groups
2. Find your Lambda function logs (e.g., `WebSocketFunction`)
3. Look for:
   ```
   📱 [PUSH CHECK] Checking X participants for push notifications
   📱 [PUSH CHECK] User <userId>: X active connections
   📱 [PUSH] Sending push to offline user <userId>
   📱 [PUSH] Found X device(s) for user <userId>
   ✅ [APNs] Notification sent to <token>...
   ✅ [FCM] Notification sent: <name>
   ```

**Common Backend Issues:**

| Log Message | Issue | Fix |
|-------------|-------|-----|
| `No devices registered for user` | Token not registered | Check device registration endpoint is called |
| `APNs not configured` | Missing credentials | Check AWS Secrets Manager has `intok/push/apns` |
| `FCM not configured` | Missing credentials | Check AWS Secrets Manager has `intok/push/fcm` |
| `APNs error: 403` | Invalid credentials | Verify APNs key, teamId, keyId are correct |
| `FCM error: 401` | Invalid service account | Verify FCM service account JSON is correct |

---

## 📝 Quick Diagnostic Commands

**Check if device tokens are registered:**
```bash
# Query DynamoDB DeviceTokensTable
aws dynamodb query \
  --table-name lingualink-device-tokens \
  --key-condition-expression "userId = :userId" \
  --expression-attribute-values '{":userId":{"S":"YOUR_USER_ID"}}'
```

**Check backend notification logs:**
```bash
# View recent Lambda logs
aws logs tail /aws/lambda/YOUR_FUNCTION_NAME --follow
```

**Test notification manually (backend):**
```bash
# Call sendPushNotification function directly
# (Requires backend API endpoint or Lambda invocation)
```

---

## ✅ Success Indicators

**iOS:**
- ✅ Permission dialog appears and is granted
- ✅ APNs token is received (64 hex characters)
- ✅ Token is registered with backend (200 response)
- ✅ Notification appears when app is closed/background
- ✅ Tapping notification opens correct conversation

**Android:**
- ✅ Permission dialog appears (Android 13+) and is granted
- ✅ FCM token is received (long string)
- ✅ Token is registered with backend (200 response)
- ✅ Notification appears when app is closed/background
- ✅ Tapping notification opens correct conversation

---

## 🚨 If Still Not Working

1. **Verify credentials in AWS Secrets Manager:**
   - APNs: `intok/push/apns` (keyId, teamId, privateKey)
   - FCM: `intok/push/fcm` (projectId, serviceAccount)

2. **Check backend CloudWatch logs** for actual send attempts

3. **Verify device tokens exist in DynamoDB** `DeviceTokensTable`

4. **Test with real devices** (iOS requires physical device)

5. **Check network connectivity** - notifications require internet

6. **Verify app is not in Do Not Disturb mode**

7. **Check notification settings** in device Settings → Notifications → Intok
