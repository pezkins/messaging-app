# 📱 Intok Native App Migration Guide

This document outlines the migration from React Native (Expo) to native Android (Kotlin) and iOS (Swift) apps.

## 📋 Table of Contents

- [Branch Strategy](#branch-strategy)
- [CI/CD Pipeline Overview](#cicd-pipeline-overview)
- [Required GitHub Secrets](#required-github-secrets)
- [Android Setup](#android-setup)
- [iOS Setup](#ios-setup)
- [Migration Checklist](#migration-checklist)
- [Deployment Workflow](#deployment-workflow)

---

## 🌿 Branch Strategy

We use a three-branch strategy for the native apps:

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│    dev ──────► stage ──────► main                              │
│     │           │             │                                 │
│     ▼           ▼             ▼                                 │
│  Simulators  Internal      Production                          │
│  & Debug     Testing       Release                             │
│              (TestFlight   (App Store                          │
│               & Play       & Play Store)                       │
│               Internal)                                         │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Branch Purposes

| Branch | Purpose | Deployment Target | Trigger |
|--------|---------|-------------------|---------|
| `dev` | Development & testing | Debug builds (artifacts) | Automatic on push |
| `stage` | Internal testing | Play Store Internal + TestFlight | Manual merge from `dev` |
| `main` | Production release | Play Store Production + App Store | Manual merge from `stage` |

### Creating the Branches

```bash
# From the native development branch
git checkout native/v0.1.0-development

# Create the branch structure
git checkout -b dev
git push -u origin dev

git checkout -b stage
git push -u origin stage

# main branch should already exist or:
git checkout -b main
git push -u origin main
```

---

## ⚙️ CI/CD Pipeline Overview

The pipeline (`native-ci.yml`) runs different jobs based on the branch:

### Dev Branch (Automatic)
1. ✅ Code quality checks (lint, tests)
2. 🏗️ Build debug APK (Android)
3. 🏗️ Build debug app (iOS Simulator)
4. 📤 Upload artifacts for testing

### Stage Branch (Manual Merge)
1. ✅ Code quality checks
2. 🏗️ Build signed release AAB (Android)
3. 🏗️ Build signed IPA (iOS)
4. 🚀 Deploy to Play Store Internal Testing
5. 🚀 Deploy to TestFlight

### Main Branch (Manual Merge)
1. ✅ Code quality checks
2. 🏗️ Build signed release AAB (Android)
3. 🏗️ Build signed IPA (iOS)
4. 🚀 Deploy to Play Store Production
5. 🚀 Deploy to App Store

---

## 🔐 Required GitHub Secrets

### Already Configured (from React Native pipeline)

| Secret | Description | Status |
|--------|-------------|--------|
| `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` | Google Play Console service account | ✅ Configured |
| `AWS_ACCESS_KEY_ID` | AWS credentials for backend | ✅ Configured |
| `AWS_SECRET_ACCESS_KEY` | AWS credentials for backend | ✅ Configured |
| `OPENAI_API_KEY` | Translation API key | ✅ Configured |

### New Secrets Required for Android

| Secret | Description | How to Get |
|--------|-------------|------------|
| `ANDROID_KEYSTORE_BASE64` | Release keystore (base64 encoded) | See [Generating Android Keystore](#generating-android-keystore) |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password | Your chosen password |
| `ANDROID_KEY_ALIAS` | Key alias in keystore | Usually `release` or `intok` |
| `ANDROID_KEY_PASSWORD` | Key password | Your chosen password |

### New Secrets Required for iOS

| Secret | Description | How to Get |
|--------|-------------|------------|
| `APPLE_CERTIFICATE_BASE64` | Distribution certificate (base64) | Apple Developer Portal |
| `APPLE_CERTIFICATE_PASSWORD` | Certificate password | Set when exporting |
| `APPLE_PROVISIONING_PROFILE_BASE64` | App Store provisioning profile | Apple Developer Portal |
| `KEYCHAIN_PASSWORD` | Temporary keychain password | Any random string |
| `APP_STORE_CONNECT_API_KEY_ID` | App Store Connect API Key ID | App Store Connect |
| `APP_STORE_CONNECT_API_ISSUER_ID` | API Issuer ID | App Store Connect |
| `APP_STORE_CONNECT_API_KEY` | API Private Key (.p8 contents) | App Store Connect |
| `APPLE_TEAM_ID` | Apple Developer Team ID | developer.apple.com |

---

## 🤖 Android Setup

### Generating Android Keystore

```bash
# Generate a new release keystore
keytool -genkey -v -keystore intok-release.keystore \
  -alias intok \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000

# Convert to base64 for GitHub secret
base64 -i intok-release.keystore -o intok-release.keystore.base64
cat intok-release.keystore.base64
# Copy this output to ANDROID_KEYSTORE_BASE64 secret
```

### Local Development

For local release builds, create `android-native/keystore.properties`:

```properties
storeFile=release.keystore
storePassword=your_store_password
keyAlias=intok
keyPassword=your_key_password
```

**⚠️ IMPORTANT:** Never commit `keystore.properties` or `*.keystore` files!

### Version Management

Update version in `android-native/app/build.gradle.kts`:

```kotlin
defaultConfig {
    versionCode = 2          // Increment for each release
    versionName = "0.2.0"    // Semantic version
}
```

---

## 🍎 iOS Setup

### Apple Developer Account Setup

1. **Create App ID:**
   - Go to [Apple Developer Portal](https://developer.apple.com/account/resources/identifiers/list)
   - Click "+" to add new identifier
   - Select "App IDs" → "App"
   - Bundle ID: `com.intokapp.app`
   - Enable capabilities: Push Notifications, Sign In with Apple

2. **Create Distribution Certificate:**
   - Go to Certificates → "+"
   - Select "Apple Distribution"
   - Follow CSR process
   - Download and install in Keychain
   - Export as .p12 with password

3. **Create Provisioning Profile:**
   - Go to Profiles → "+"
   - Select "App Store" distribution
   - Select your App ID
   - Select your Distribution Certificate
   - Download and save

4. **Create App Store Connect API Key:**
   - Go to [App Store Connect](https://appstoreconnect.apple.com) → Users and Access → Keys
   - Click "+" to generate new key
   - Save the Key ID, Issuer ID, and download the .p8 file

### Converting for GitHub Secrets

```bash
# Convert certificate to base64
base64 -i Certificates.p12 -o certificate.base64
cat certificate.base64
# Copy to APPLE_CERTIFICATE_BASE64

# Convert provisioning profile to base64
base64 -i Intok_AppStore.mobileprovision -o profile.base64
cat profile.base64
# Copy to APPLE_PROVISIONING_PROFILE_BASE64

# API Key (.p8) - copy contents directly
cat AuthKey_XXXXXXXX.p8
# Copy to APP_STORE_CONNECT_API_KEY
```

### Local Fastlane Setup

```bash
cd ios-native

# Install dependencies
bundle install

# Configure Fastlane (first time only)
bundle exec fastlane init

# Run builds
bundle exec fastlane build_debug      # Debug for simulator
bundle exec fastlane deploy_testflight # TestFlight
bundle exec fastlane deploy_appstore   # App Store
```

---

## ✅ Migration Checklist

### Phase 1: Setup (Current)
- [x] Create native Android project structure
- [x] Create native iOS project structure
- [x] Implement UI screens (Login, Setup, Conversations, Chat, Settings)
- [x] Configure CI/CD pipeline
- [ ] **Generate Android release keystore**
- [ ] **Add Android secrets to GitHub**
- [ ] **Create iOS certificates and profiles**
- [ ] **Add iOS secrets to GitHub**

### Phase 2: Core Features
- [ ] Implement API service (REST client)
- [ ] Implement WebSocket service (real-time messaging)
- [ ] Implement authentication manager
- [ ] Implement local storage (DataStore/Keychain)
- [ ] Implement Google Sign-In
- [ ] Implement Apple Sign-In (iOS)

### Phase 3: Advanced Features
- [ ] Implement attachment picker (camera, gallery, documents)
- [ ] Implement GIF picker (Tenor API)
- [ ] Implement emoji reaction picker
- [ ] Implement typing indicators
- [ ] Implement push notifications

### Phase 4: Testing & Release
- [ ] Deploy to internal testing (stage branch)
- [ ] Bug fixes and QA
- [ ] Performance optimization
- [ ] Deploy to production (main branch)
- [ ] Deprecate React Native app

---

## 🚀 Deployment Workflow

### Daily Development

```bash
# Work on dev branch
git checkout dev
git pull origin dev

# Make changes, commit
git add .
git commit -m "feat: add new feature"
git push origin dev
# → Pipeline builds debug APK/app automatically
```

### Ready for Internal Testing

```bash
# Create PR from dev to stage
# On GitHub: New Pull Request → dev → stage
# Review and merge

# Or via command line:
git checkout stage
git merge dev
git push origin stage
# → Pipeline builds and deploys to Internal Testing
```

### Ready for Production

```bash
# Create PR from stage to main
# On GitHub: New Pull Request → stage → main
# Review and merge (CAREFUL: This releases to production!)

# Or via command line:
git checkout main
git merge stage
git push origin main
# → Pipeline builds and deploys to Production
```

### Manual Deployment

You can also trigger deployments manually:

1. Go to GitHub Actions
2. Select "📱 Native App CI/CD"
3. Click "Run workflow"
4. Select:
   - **Branch:** dev, stage, or main
   - **Platform:** android, ios, or both
   - **Environment:** dev, stage, or production

---

## 🔧 Troubleshooting

### Android Build Fails

1. **Missing keystore:** Ensure `ANDROID_KEYSTORE_BASE64` secret is set
2. **Wrong password:** Verify `ANDROID_KEYSTORE_PASSWORD` and `ANDROID_KEY_PASSWORD`
3. **Version code conflict:** Increment `versionCode` in build.gradle.kts

### iOS Build Fails

1. **No Xcode project:** Run `xcodegen generate` locally and commit
2. **Signing error:** Verify certificates and provisioning profiles
3. **API key error:** Check App Store Connect API configuration

### Play Store Upload Fails

1. **Version code exists:** Increment `versionCode` (must be higher than last upload)
2. **Service account error:** Verify `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`
3. **App not found:** Ensure app is created in Play Console with package `com.intokapp.app`

### App Store Upload Fails

1. **Invalid IPA:** Check provisioning profile matches bundle ID
2. **API auth error:** Verify App Store Connect API keys
3. **App not found:** Create app in App Store Connect first

---

## 📚 Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Fastlane Documentation](https://docs.fastlane.tools/)
- [Google Play Console](https://play.google.com/console)
- [App Store Connect](https://appstoreconnect.apple.com)
- [Apple Developer Portal](https://developer.apple.com)

---

## 📞 Support

For issues with the CI/CD pipeline or deployment, check:
1. GitHub Actions logs for detailed error messages
2. Play Console / App Store Connect for upload issues
3. Create an issue in the repository for persistent problems


