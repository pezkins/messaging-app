# 🚀 Intok - Google Play Store Submission Guide

This guide walks you through deploying Intok to the Google Play Store.

## Prerequisites

- [ ] Google Play Developer Account ($25 one-time fee)
- [ ] Expo Account (free)
- [ ] EAS CLI installed
- [ ] Privacy Policy hosted online
- [ ] App icons ready

---

## Step 1: Create Accounts

### 1.1 Google Play Developer Account
1. Go to [Google Play Console](https://play.google.com/console/signup)
2. Sign in with your Google account
3. Pay the $25 registration fee
4. Complete your developer profile

### 1.2 Expo Account
1. Go to [expo.dev/signup](https://expo.dev/signup)
2. Create a free account
3. Remember your username (needed for app.json)

---

## Step 2: Install EAS CLI

```bash
npm install -g eas-cli
eas login
```

---

## Step 3: Configure Your Project

### 3.1 Update app.json

Edit `mobile/app.json` and replace these placeholders:

```json
{
  "expo": {
    "owner": "YOUR_EXPO_USERNAME",  // ← Replace with your Expo username
    "extra": {
      "eas": {
        "projectId": "YOUR_PROJECT_ID"  // ← Will be auto-filled in Step 4
      }
    }
  }
}
```

### 3.2 Link to EAS

```bash
cd mobile
eas init
```

This will:
- Create your project on Expo servers
- Auto-fill the `projectId` in app.json

---

## Step 4: Host Your Privacy Policy

Your privacy policy needs to be accessible via a public URL. Options:

### Option A: GitHub Pages (Free)
1. Push the `privacy-policy.html` to your repo
2. Enable GitHub Pages in repo settings
3. URL will be: `https://YOUR_USERNAME.github.io/messaging-app/privacy-policy.html`

### Option B: AWS S3 (You already have AWS)
```bash
# Create public bucket
aws s3 mb s3://lingualink-public --region us-east-1

# Upload privacy policy
aws s3 cp privacy-policy.html s3://lingualink-public/ --acl public-read

# Enable static website hosting
aws s3 website s3://lingualink-public --index-document privacy-policy.html
```
URL: `http://lingualink-public.s3-website-us-east-1.amazonaws.com/privacy-policy.html`

---

## Step 5: Build Your App

### 5.1 Create Production Build

```bash
cd mobile
eas build --platform android --profile production
```

This will:
- Build your app in the cloud (takes ~15-20 minutes)
- Generate an `.aab` file (Android App Bundle)
- Store it on Expo servers

### 5.2 Download the Build

After build completes:
```bash
eas build:list
# or download from the Expo dashboard
```

---

## Step 6: Google Play Console Setup

### 6.1 Create Your App

1. Go to [Google Play Console](https://play.google.com/console)
2. Click **"Create app"**
3. Fill in:
   - **App name**: LinguaLink
   - **Default language**: English (US)
   - **App or game**: App
   - **Free or paid**: Free
4. Accept declarations

### 6.2 Complete Store Listing

Navigate to **"Main store listing"** and fill in:

| Field | Value |
|-------|-------|
| **App name** | Intok |
| **Short description** | Talk to anyone, anywhere with AI translation |
| **Full description** | See below |

**Full Description** (copy this):
```
💬 Intok - Talk to Anyone, Anywhere

Chat with anyone in the world! Intok automatically translates your messages in real-time using AI, so you can communicate naturally in your own language.

✨ KEY FEATURES:

• 🗣️ Real-time Translation - Messages are instantly translated to each person's preferred language
• 🌎 50+ Languages - Support for major world languages with regional variants
• 🇵🇪 Regional Dialects - Choose your country for vocabulary that feels natural (Spanish from Peru vs Spain!)
• 🔒 Secure Messaging - Your conversations are private and encrypted
• 🚀 Fast & Reliable - Powered by OpenAI for accurate, contextual translations

📱 HOW IT WORKS:

1. Sign up and choose your preferred language & country
2. Find friends and start chatting
3. Type in your language - they receive it in theirs!

Perfect for:
• International friendships & relationships
• Business communication across borders
• Language learners wanting to practice
• Travelers connecting with locals
• Multicultural families staying in touch

Download Intok today and start connecting without language barriers! 🌐
```

### 6.3 Upload Graphics

You need these assets:

| Asset | Size | Required |
|-------|------|----------|
| **App icon** | 512x512 PNG | ✅ Yes |
| **Feature graphic** | 1024x500 PNG | ✅ Yes |
| **Screenshots** | Various sizes | ✅ Yes (min 2) |

**Screenshot sizes for phone:**
- Minimum: 320px
- Maximum: 3840px
- Aspect ratio: 16:9 or 9:16

### 6.4 Content Rating

1. Go to **"Content rating"**
2. Start questionnaire
3. Answer honestly (for a messaging app):
   - Violence: No
   - Sexual content: No
   - User-generated content: Yes (messages)
   - etc.

### 6.5 Privacy Policy

1. Go to **"App content"** → **"Privacy policy"**
2. Enter your privacy policy URL:
   ```
   https://YOUR_URL/privacy-policy.html
   ```

### 6.6 Target Audience

1. Go to **"App content"** → **"Target audience"**
2. Select age group: **18+** or **13+** (recommended for messaging apps)

---

## Step 7: Upload Your App

### 7.1 Create Release

1. Go to **"Production"** (or **"Internal testing"** for first test)
2. Click **"Create new release"**
3. Upload your `.aab` file from Step 5
4. Add release notes:
   ```
   Version 1.0.0
   - Initial release
   - Real-time message translation
   - 50+ language support
   - Google Sign-In
   - Regional dialect support
   ```

### 7.2 Review and Submit

1. Click **"Review release"**
2. Fix any warnings (usually about missing assets)
3. Click **"Start rollout to Production"**

---

## Step 8: Wait for Review

Google typically reviews apps within:
- **First submission**: 3-7 days
- **Updates**: 1-3 days

You'll receive an email when approved or if changes are needed.

---

## Recommended: Start with Internal Testing

Before going to production, test with a small group:

1. Go to **"Internal testing"** instead of Production
2. Add tester emails (up to 100)
3. Testers get a link to install

This lets you catch bugs before public release!

---

## Troubleshooting

### Build Fails
```bash
# Check build logs
eas build:list
eas build:view

# Clean and rebuild
cd mobile
rm -rf node_modules
npm install
eas build --platform android --profile production --clear-cache
```

### App Rejected
Common reasons:
- Missing privacy policy
- Incomplete content rating
- Low-quality screenshots
- Misleading description

---

## Quick Commands Reference

```bash
# Login to EAS
eas login

# Initialize project
eas init

# Build for Android
eas build --platform android --profile production

# Build for testing (APK)
eas build --platform android --profile preview

# List builds
eas build:list

# Submit to Play Store (after setup)
eas submit --platform android
```

---

## Checklist Before Submission

- [ ] Privacy policy hosted and accessible
- [ ] App icon (512x512)
- [ ] Feature graphic (1024x500)
- [ ] At least 2 screenshots
- [ ] Short description (80 chars max)
- [ ] Full description (4000 chars max)
- [ ] Content rating completed
- [ ] Target audience selected
- [ ] App category selected (Communication)
- [ ] Contact email provided
- [ ] `.aab` file uploaded

---

## Need Help?

- [Expo EAS Docs](https://docs.expo.dev/eas/)
- [Google Play Console Help](https://support.google.com/googleplay/android-developer)
- [Android App Bundle Guide](https://developer.android.com/guide/app-bundle)

Good luck with your launch! 🚀

