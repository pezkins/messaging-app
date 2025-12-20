# 🔍 Root Cause Analysis: Push Notifications Not Working

**Date:** December 8, 2025  
**Status:** Investigation Complete  
**Platforms:** iOS Simulator, Android Emulator

---

## 🎯 Executive Summary

After comprehensive code review and architecture analysis, I've identified **5 critical issues** preventing push notifications from working:

1. **iOS Simulator Limitation** (CRITICAL - Cannot be fixed)
2. **Android Emulator FCM Setup** (May require Google Play Services)
3. **Notification Logic: Online Users Skipped** (By design, but may be issue)
4. **Backend Secrets Configuration** (Needs verification)
5. **Device Token Registration Flow** (Needs verification)

---

## 🔴 Issue #1: iOS Simulator Limitation (CRITICAL)

### Problem
**iOS Simulator DOES NOT support APNs (Apple Push Notification Service).**

### Root Cause
Apple's iOS Simulator does not have the necessary entitlements or infrastructure to receive real APNs notifications. This is a **hard limitation** by Apple.

### Evidence
- Code shows proper APNs setup in `IntokApp.swift`
- `UIBackgroundModes` with `remote-notification` is correctly configured
- Token registration flow is correct
- **BUT**: Simulator will always fail with: `no valid 'aps-environment' entitlement`

### Impact
- **Cannot test iOS notifications on simulator**
- **Must use physical iOS device** for testing

### Solution
✅ **No code fix needed** - This is expected behavior  
✅ **Test on physical iPhone/iPad** - APNs will work on real devices

### Code Verification
```swift
// ios-native/Intok/App/IntokApp.swift
// ✅ Correctly configured:
- UNUserNotificationCenter setup
- Permission request
- Token registration
- Backend API call
```

---

## 🟡 Issue #2: Android Emulator FCM Setup

### Problem
Android emulator may not have Google Play Services installed, which is required for FCM.

### Root Cause
FCM requires Google Play Services. Some emulator images don't include it.

### Evidence
- Code shows proper FCM setup in `IntokFirebaseMessagingService.kt`
- `google-services.json` is configured
- Token retrieval uses `FirebaseMessaging.getInstance().token`
- **BUT**: If Google Play Services missing, token retrieval will fail

### Impact
- FCM token may not be retrievable
- Notifications won't work without token

### Solution
✅ **Verify Google Play Services** is installed on emulator  
✅ **Use emulator with Google APIs** (not AOSP)  
✅ **Check logs** for `❌ Failed to get FCM token`

### Code Verification
```kotlin
// android-native/app/src/main/java/com/intokapp/app/data/network/IntokFirebaseMessagingService.kt
// ✅ Correctly configured:
- FirebaseMessaging setup
- Token retrieval
- Backend registration
- Notification channel creation
```

---

## 🟡 Issue #3: Notification Logic - Online Users Skipped

### Problem
**Notifications are only sent to users who are OFFLINE** (no active WebSocket connections).

### Root Cause
This is **by design** - the backend checks if a user has active WebSocket connections before sending push notifications:

```typescript
// server-serverless/src/handlers/websocket.ts:429-430
if (!connections.Items?.length) {
  // Send push notification
} else {
  console.log(`📱 [PUSH] Skipping push for ${participantId} - user is online`);
}
```

### Impact
- If both users are online (WebSocket connected), **no push notification is sent**
- This is correct behavior for real-world usage
- **BUT**: For testing, if both emulators are connected, notifications won't trigger

### Solution
✅ **This is correct behavior** - notifications are for offline users  
✅ **For testing**: Close app or disconnect WebSocket to trigger notifications  
✅ **Alternative**: Add test endpoint to force send notifications

### Code Location
```typescript
// server-serverless/src/handlers/websocket.ts:413-455
// Line 430: Only sends if no active connections
```

---

## 🟡 Issue #4: Backend Secrets Configuration

### Problem
Backend may not have APNs/FCM credentials configured in AWS Secrets Manager.

### Root Cause
The backend checks for secrets:
- `intok/push/apns` (APNs credentials)
- `intok/push/fcm` (FCM credentials)

If these don't exist or are misconfigured, notifications will silently fail.

### Evidence
```typescript
// server-serverless/src/lib/notifications.ts:61-64
const secretName = process.env.APNS_SECRET_NAME;
if (!secretName) {
  console.log('📱 [APNs] APNS_SECRET_NAME not configured - iOS push disabled');
  return null;
}
```

### Impact
- Notifications won't be sent if secrets are missing
- No error thrown - just silently skipped

### Solution
✅ **Verify secrets exist** in AWS Secrets Manager:
```bash
aws secretsmanager get-secret-value --secret-id intok/push/apns
aws secretsmanager get-secret-value --secret-id intok/push/fcm
```

✅ **Check CloudWatch logs** for:
- `📱 [APNs] Not configured, skipping iOS push`
- `📱 [FCM] Not configured, skipping Android push`

### Required Secret Structure

**APNs Secret (`intok/push/apns`):**
```json
{
  "keyId": "ABC123XYZ",
  "teamId": "DEF456UVW",
  "privateKey": "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----"
}
```

**FCM Secret (`intok/push/fcm`):**
```json
{
  "projectId": "your-project-id",
  "serviceAccount": {
    "type": "service_account",
    "project_id": "your-project-id",
    "private_key_id": "...",
    "private_key": "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----",
    "client_email": "...",
    "client_id": "...",
    "auth_uri": "...",
    "token_uri": "...",
    "auth_provider_x509_cert_url": "...",
    "client_x509_cert_url": "..."
  }
}
```

---

## 🟡 Issue #5: Device Token Registration Flow

### Problem
Device tokens may not be registered with backend.

### Root Cause
Tokens are only registered:
- **iOS**: After login AND after APNs token is received
- **Android**: After login AND after FCM token is received

If login happens before token is received, token may not be registered.

### Evidence

**iOS Flow:**
```swift
// IntokApp.swift:48-58
func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
  // Store token
  AppDelegate.apnsToken = token
  // Try to register if logged in
  await registerDeviceTokenIfAuthenticated(token)
}

// AuthManager.swift:351-353
private func registerPushToken() async {
  await AppDelegate.registerStoredDeviceToken()
}
```

**Android Flow:**
```kotlin
// IntokFirebaseMessagingService.kt:40-62
fun registerCurrentToken(apiService: ApiService, tokenManager: TokenManager) {
  FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
    if (task.isSuccessful) {
      val authToken = tokenManager.getAccessToken()
      if (authToken != null) {
        apiService.registerDeviceToken(RegisterDeviceRequest(token = token))
      }
    }
  }
}
```

### Impact
- If token received before login → stored but not registered
- If login happens before token → token registered when received
- **Race condition** possible but handled correctly

### Solution
✅ **Code handles this correctly** - tokens are stored and registered when both conditions are met  
✅ **Check logs** to verify registration:
- iOS: `✅ Device token registered with backend`
- Android: `✅ FCM token registered with backend`

---

## 🔍 Diagnostic Checklist

### Step 1: Verify Device Tokens Are Registered

**Check DynamoDB:**
```bash
aws dynamodb query \
  --table-name lingualink-device-tokens \
  --key-condition-expression "userId = :userId" \
  --expression-attribute-values '{":userId":{"S":"YOUR_USER_ID"}}'
```

**Expected:** Should see device tokens with `platform: ios` or `platform: android`

### Step 2: Verify Backend Secrets

**Check AWS Secrets Manager:**
```bash
aws secretsmanager describe-secret --secret-id intok/push/apns
aws secretsmanager describe-secret --secret-id intok/push/fcm
```

**Expected:** Both secrets should exist

### Step 3: Check CloudWatch Logs

**Look for notification send attempts:**
```bash
aws logs tail /aws/lambda/WebSocketFunction --follow | grep "PUSH"
```

**Expected logs:**
```
📱 [PUSH CHECK] Checking X participants for push notifications
📱 [PUSH CHECK] User <userId>: X active connections
📱 [PUSH] Sending push to offline user <userId>
📱 [PUSH] Found X device(s) for user <userId>
✅ [APNs] Notification sent to <token>...
✅ [FCM] Notification sent: <name>
```

### Step 4: Verify User is Offline

**Notifications only sent to offline users:**
- Close the app completely
- Or disconnect WebSocket
- Send message from another user
- Check if notification is sent

### Step 5: Check Client Logs

**iOS (Real Device):**
- Look for: `📱 APNs Token received`
- Look for: `✅ Device token registered with backend`
- Look for: `📱 Notification received in foreground`

**Android (Emulator/Device):**
- Look for: `📱 Got FCM token for registration`
- Look for: `✅ FCM token registered with backend`
- Look for: `📬 Message received from`

---

## ✅ Verified Working Components

1. ✅ **iOS APNs Setup** - Correctly configured in code
2. ✅ **Android FCM Setup** - Correctly configured in code
3. ✅ **Backend Notification Service** - Properly structured
4. ✅ **Device Token Registration API** - Correctly implemented
5. ✅ **WebSocket Notification Logic** - Correctly checks online/offline status
6. ✅ **Notification Payload Structure** - Correct format for both platforms

---

## 🎯 Recommended Next Steps

1. **For iOS:**
   - ✅ Test on **physical iPhone/iPad** (simulator won't work)
   - ✅ Verify APNs credentials in AWS Secrets Manager
   - ✅ Check CloudWatch logs for send attempts

2. **For Android:**
   - ✅ Verify Google Play Services installed on emulator
   - ✅ Verify FCM credentials in AWS Secrets Manager
   - ✅ Check CloudWatch logs for send attempts
   - ✅ Verify device token is registered in DynamoDB

3. **For Both:**
   - ✅ Ensure **user is offline** (app closed) when testing
   - ✅ Verify device tokens exist in DynamoDB
   - ✅ Check CloudWatch logs for notification send attempts
   - ✅ Verify backend secrets are configured

---

## 📊 Summary Table

| Issue | Severity | Platform | Status | Fix Required |
|-------|----------|----------|--------|--------------|
| iOS Simulator Limitation | 🔴 CRITICAL | iOS | Expected | Use real device |
| Android Emulator FCM | 🟡 MEDIUM | Android | May need setup | Verify Google Play Services |
| Online Users Skipped | 🟡 INFO | Both | By design | Close app to test |
| Backend Secrets | 🟡 MEDIUM | Both | Needs verification | Configure secrets |
| Token Registration | 🟢 LOW | Both | Likely working | Verify logs |

---

## 🔧 Quick Fixes

### Fix #1: Test on Real iOS Device
```bash
# Build for device
cd ios-native
xcodebuild -workspace Intok.xcworkspace -scheme Intok -configuration Debug -destination 'platform=iOS,id=<DEVICE_ID>'
```

### Fix #2: Verify Android Emulator Has Google Play Services
```bash
# Check if Google Play Services is installed
adb shell pm list packages | grep "com.google.android.gms"
```

### Fix #3: Force Send Test Notification
Add a test endpoint to force send notifications (bypass online check):
```typescript
// In backend: Force send notification for testing
await sendPushNotification({
  userId: testUserId,
  title: "Test Notification",
  body: "This is a test",
  data: { conversationId: "test", messageId: "test", type: "test" }
});
```

---

## 📝 Conclusion

The code implementation is **correct**. The issues are:

1. **iOS Simulator limitation** (cannot be fixed - use real device)
2. **Configuration/Setup issues** (secrets, emulator setup)
3. **Testing methodology** (need offline users to trigger notifications)

**Next Action:** Verify backend secrets and test on real iOS device + properly configured Android emulator.
