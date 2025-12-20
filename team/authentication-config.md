# 🔐 Authentication Configuration

## ⛔ DO NOT MODIFY - CRITICAL SETTINGS ⛔

This document contains critical authentication configuration that **MUST NOT BE CHANGED** without explicit approval from the tech lead.

---

## 🍎 iOS - Sign in with Apple

### Critical Files (DO NOT TOUCH)

| File | Setting | Value | Purpose |
|------|---------|-------|---------|
| `ios-native/Intok.xcodeproj/project.pbxproj` | `DEVELOPMENT_TEAM` | `LW7QG2H5ST` | Apple Developer Team ID |
| `ios-native/Intok.xcodeproj/project.pbxproj` | `CODE_SIGN_ENTITLEMENTS` | `Intok/Intok.entitlements` | Links entitlements file |
| `ios-native/Intok.xcodeproj/project.pbxproj` | `CODE_SIGN_STYLE` | `Automatic` | Automatic signing |
| `ios-native/Intok/Intok.entitlements` | `com.apple.developer.applesignin` | `Default` | Apple Sign-In capability |

### Required Entitlements (ios-native/Intok/Intok.entitlements)

```xml
<!-- ⛔ DO NOT MODIFY THIS FILE ⛔ -->
<key>com.apple.developer.applesignin</key>
<array>
    <string>Default</string>
</array>
```

### Key Swift Files

| File | Purpose |
|------|---------|
| `ios-native/Intok/Core/Network/AppleAuthManager.swift` | Handles Apple Sign-In flow |
| `ios-native/Intok/Features/Auth/LoginView.swift` | Login UI with Apple button |

### How It Works

1. User taps "Continue with Apple" button
2. `AppleAuthManager.signIn()` creates authorization request with nonce
3. iOS presents the Apple Sign-In sheet
4. On success, credentials sent to backend via `AuthManager.signInWithAppleOAuth()`
5. Backend validates with Apple and returns JWT token

---

## 🤖 Android - Google Sign-In

### Critical Files (DO NOT TOUCH)

| File | Setting | Value | Purpose |
|------|---------|-------|---------|
| `android-native/app/google-services.json` | `client_id` | OAuth client IDs | Google OAuth configuration |
| `android-native/app/build.gradle.kts` | Google Play Services Auth | Latest version | Google Sign-In SDK |

### Required in google-services.json

```json
// ⛔ DO NOT MODIFY THIS FILE ⛔
{
  "client": [
    {
      "oauth_client": [
        {
          "client_type": 1,  // Android
          "client_id": "..."
        },
        {
          "client_type": 3,  // Web (required for ID token)
          "client_id": "..."
        }
      ]
    }
  ]
}
```

### Key Kotlin Files

| File | Purpose |
|------|---------|
| `android-native/app/src/main/java/com/intokapp/app/data/repository/AuthRepository.kt` | Google Sign-In implementation |
| `android-native/app/src/main/java/com/intokapp/app/ui/screens/auth/LoginScreen.kt` | Login UI with Google button |

---

## ☁️ Backend - OAuth Endpoints

### Critical Files (DO NOT TOUCH)

| File | Endpoint | Purpose |
|------|----------|---------|
| `server-serverless/src/handlers/auth.ts` | `POST /auth/oauth/apple` | Apple OAuth handler |
| `server-serverless/src/handlers/auth.ts` | `POST /auth/oauth/google` | Google OAuth handler |

### Environment Variables (AWS)

| Variable | Purpose |
|----------|---------|
| `APPLE_TEAM_ID` | Apple Developer Team ID (LW7QG2H5ST) |
| `APPLE_KEY_ID` | App Store Connect API Key ID |
| `APPLE_PRIVATE_KEY` | App Store Connect private key |
| `GOOGLE_CLIENT_ID` | Google OAuth client ID |

---

## 🚫 What NOT To Do

1. **NEVER** change `DEVELOPMENT_TEAM` in project.pbxproj
2. **NEVER** remove or modify entitlements in `Intok.entitlements`
3. **NEVER** delete or regenerate `google-services.json` without updating all environments
4. **NEVER** modify OAuth endpoints without updating ALL clients
5. **NEVER** change bundle ID / package name without reconfiguring OAuth

---

## 🔧 Troubleshooting

### iOS Sign in with Apple fails (Error 1000)

1. Verify `DEVELOPMENT_TEAM = LW7QG2H5ST` in project.pbxproj
2. Verify `CODE_SIGN_ENTITLEMENTS = Intok/Intok.entitlements` is set
3. Check entitlements file has `com.apple.developer.applesignin`
4. For simulator: must be signed into Apple ID in Settings
5. For real device: should work if above are correct

### Android Google Sign-In fails

1. Verify `google-services.json` exists and is valid
2. Check SHA-1 fingerprint matches in Firebase Console
3. Verify Web client ID is present in `google-services.json`

---

## 📋 Configuration Checklist

Before ANY release, verify:

- [ ] `DEVELOPMENT_TEAM` = `LW7QG2H5ST`
- [ ] `CODE_SIGN_ENTITLEMENTS` = `Intok/Intok.entitlements`
- [ ] `Intok.entitlements` contains `com.apple.developer.applesignin`
- [ ] `google-services.json` is present and unchanged
- [ ] Backend OAuth endpoints are deployed
- [ ] All environment variables are set in AWS

---

## 👥 Contacts

For authentication issues, contact:
- **iOS Auth**: Tech Lead
- **Android Auth**: Tech Lead
- **Backend OAuth**: Tech Lead

---

*⚠️ This configuration was working as of December 20, 2024. DO NOT MODIFY without explicit approval.*

*Last Updated: December 2024*
