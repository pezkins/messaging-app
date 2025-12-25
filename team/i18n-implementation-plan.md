# 🌐 App Internationalization (i18n) Implementation Plan

**Feature:** Full UI Localization  
**Priority:** High  
**Created:** December 2024  
**Status:** 📋 Planning

---

## 📋 Overview

### Problem Statement
The Intok app currently displays all UI elements (buttons, labels, navigation, error messages, etc.) in English only. International users who don't speak English have difficulty navigating the app, even though message translation works perfectly.

### Solution
Implement full app internationalization (i18n) with a **Hybrid Approach**:
1. **Auto-detect** device language on first launch and use it as the default UI language
2. **Allow manual override** in Settings (user can change UI language independently of device settings)
3. **Leverage existing infrastructure** - use the same `preferredLanguage` field that users already set for message translation

### Supported Languages
**All 120+ languages** currently supported for message translation will be supported for UI localization. The language list is already defined in:
- iOS: `Intok/Core/Utils/Languages.swift` (LANGUAGES array)
- Android: `data/constants/Languages.kt` (LANGUAGES list)
- Backend: `src/lib/translation.ts` (LANGUAGE_NAMES map)

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                        USER EXPERIENCE FLOW                         │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   First Launch                     Settings                         │
│   ┌──────────────┐                ┌──────────────────────────┐     │
│   │ Detect device│                │ Language Settings         │     │
│   │ locale (e.g. │───────────────▶│                          │     │
│   │ Japanese)    │                │ • Message Language: 日本語 │     │
│   └──────────────┘                │ • App UI Language: Auto   │     │
│          │                        │   ↳ (Using: Japanese)     │     │
│          ▼                        │                          │     │
│   ┌──────────────┐                │ [Change App Language]     │     │
│   │ Show app in  │                │   → Opens language picker │     │
│   │ Japanese UI  │                └──────────────────────────┘     │
│   └──────────────┘                                                  │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                        TECHNICAL FLOW                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   ┌─────────────────────────────────────────────────────────────┐  │
│   │                    Localization Layer                        │  │
│   │  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐    │  │
│   │  │ String Keys  │──▶│ Translation  │──▶│ Display Text │    │  │
│   │  │ "login_btn"  │   │ Lookup       │   │ "登録する"    │    │  │
│   │  └──────────────┘   └──────────────┘   └──────────────┘    │  │
│   └─────────────────────────────────────────────────────────────┘  │
│                              │                                      │
│   ┌──────────────────────────┼──────────────────────────────────┐  │
│   │                          ▼                                   │  │
│   │              Translation Source                              │  │
│   │   ┌──────────────────────────────────────────────────────┐  │  │
│   │   │  Option A: Static Translation Files (RECOMMENDED)     │  │  │
│   │   │  • iOS: Localizable.strings per language             │  │  │
│   │   │  • Android: strings.xml in res/values-{lang}/        │  │  │
│   │   │  • Bundled with app, works offline                   │  │  │
│   │   └──────────────────────────────────────────────────────┘  │  │
│   │                          OR                                  │  │
│   │   ┌──────────────────────────────────────────────────────┐  │  │
│   │   │  Option B: AI Translation (runtime)                   │  │  │
│   │   │  • Use existing translation service                  │  │  │
│   │   │  • Translate strings on first launch                 │  │  │
│   │   │  • Cache locally for offline use                     │  │  │
│   │   │  • Higher initial load time                          │  │  │
│   │   └──────────────────────────────────────────────────────┘  │  │
│   └──────────────────────────────────────────────────────────────┘  │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### Translation Approach Decision

Given that Intok supports **120+ languages**, we recommend a **Hybrid Translation Approach**:

1. **Tier 1 Languages (Static Files)** - Top 20 most common languages get professionally translated static files for best performance
2. **Tier 2+ Languages (AI Translation)** - Remaining languages use AI translation at runtime with caching

---

## 🍎 iOS Engineer Brief

### Summary
Implement full UI localization for the iOS app with support for 120+ languages, auto-detecting device language and allowing manual override.

### Why This Change
International users struggle to navigate the app because all UI text is in English. This creates friction during onboarding and daily use, contradicting the app's core value proposition of breaking language barriers.

### Technical Requirements

#### 1. Create Localization Infrastructure
```
ios-native/Intok/
├── Resources/
│   └── Localizable/
│       ├── en.lproj/
│       │   └── Localizable.strings      # English (base)
│       ├── es.lproj/
│       │   └── Localizable.strings      # Spanish
│       ├── fr.lproj/
│       │   └── Localizable.strings      # French
│       ├── de.lproj/
│       │   └── Localizable.strings      # German
│       ├── ja.lproj/
│       │   └── Localizable.strings      # Japanese
│       ├── zh-Hans.lproj/
│       │   └── Localizable.strings      # Chinese Simplified
│       ├── ar.lproj/
│       │   └── Localizable.strings      # Arabic (RTL)
│       └── ... (all 120+ languages)
```

#### 2. Create LocalizationManager
Create a new service to manage app language:
- Detect device language on first launch
- Store user's UI language preference (separate from message `preferredLanguage`)
- Provide methods to change language at runtime
- Handle fallback to English if translation unavailable

**Key considerations:**
- Store `appLanguage` preference in UserDefaults (key: `app_language`)
- Value can be: `"auto"` (use device language) or specific language code (e.g., `"ja"`)
- When "auto", resolve to actual device language at runtime
- Subscribe to locale change notifications for dynamic updates

#### 3. Extract All UI Strings
Audit and extract ALL hardcoded strings from:
- **Auth screens:** LoginView, EmailAuthView, RegisterView
- **Setup screens:** SetupView (language/country selection)
- **Chat screens:** ConversationsView, ChatView, NewChatView
- **Settings screens:** SettingsView, ProfileView, WhatsNewSheet
- **Common components:** Error messages, alerts, buttons, labels
- **System text:** Navigation titles, tab labels, action sheets

**Estimated strings to extract:** ~200-300 unique strings

#### 4. Update Settings UI
Add new "App Language" setting in SettingsView:
- Display current language with flag emoji
- Open language picker (same component used in Setup)
- Show "Auto (Device Language)" as first option
- Instant language switch without app restart

#### 5. RTL (Right-to-Left) Support
Languages like Arabic, Hebrew, Urdu, Persian require RTL layout:
- Use SwiftUI's built-in RTL support (`.environment(\.layoutDirection, .rightToLeft)`)
- Test all screens with RTL languages
- Ensure icons/images that imply direction are flipped appropriately

#### 6. Translation Generation Strategy
For 120+ languages:
1. Create English base file with all strings
2. Use AI translation service to generate initial translations
3. Store generated files in app bundle
4. Allow future updates via app updates or remote fetch

### Files to Create/Modify
| File | Action | Description |
|------|--------|-------------|
| `Core/Services/LocalizationManager.swift` | Create | Manage app language state |
| `Resources/Localizable/*.strings` | Create | Translation files per language |
| `Core/Extensions/String+Localization.swift` | Create | Helper for localized strings |
| `Features/Settings/SettingsView.swift` | Modify | Add App Language setting |
| `Features/Auth/LoginView.swift` | Modify | Use localized strings |
| `Features/Setup/SetupView.swift` | Modify | Use localized strings |
| `Features/Chat/*.swift` | Modify | Use localized strings |
| All UI files | Modify | Replace hardcoded strings |

### Acceptance Criteria
- [ ] App detects device language on first launch
- [ ] All UI text displays in user's selected language
- [ ] User can change app language in Settings without restart
- [ ] RTL languages display correctly
- [ ] Offline functionality preserved (translations bundled)
- [ ] All 120+ supported languages available

---

## 🤖 Android Engineer Brief

### Summary
Implement full UI localization for the Android app with support for 120+ languages, auto-detecting device language and allowing manual override.

### Why This Change
International users struggle to navigate the app because all UI text is in English. This creates friction during onboarding and daily use, contradicting the app's core value proposition of breaking language barriers.

### Technical Requirements

#### 1. Create Localization Infrastructure
```
android-native/app/src/main/res/
├── values/
│   └── strings.xml                    # English (default)
├── values-es/
│   └── strings.xml                    # Spanish
├── values-fr/
│   └── strings.xml                    # French
├── values-de/
│   └── strings.xml                    # German
├── values-ja/
│   └── strings.xml                    # Japanese
├── values-zh-rCN/
│   └── strings.xml                    # Chinese Simplified
├── values-ar/
│   └── strings.xml                    # Arabic (RTL)
└── ... (all 120+ languages)
```

#### 2. Create LocalizationManager
Create a new service to manage app language:
- Use Hilt for dependency injection
- Detect device locale on first launch
- Store user's UI language preference in SharedPreferences
- Provide methods to change language at runtime using `AppCompatDelegate.setApplicationLocales()`

**Key considerations:**
- Store `app_language` preference in SharedPreferences
- Value can be: `"auto"` (use device language) or specific language code (e.g., `"ja"`)
- Use Android 13+ per-app language preferences API
- For older Android versions, use AppCompat's `setApplicationLocales()`

#### 3. Extract All UI Strings
Audit and extract ALL hardcoded strings from:
- **Auth screens:** LoginScreen, EmailAuthScreen, RegisterScreen
- **Setup screens:** SetupScreen (language/country selection)
- **Chat screens:** ConversationsScreen, ChatScreen, NewChatScreen
- **Settings screens:** SettingsScreen, ProfileScreen, WhatsNewDialog
- **Common components:** Error messages, dialogs, buttons, labels
- **System text:** Navigation titles, bottom nav labels, action dialogs

**Estimated strings to extract:** ~200-300 unique strings

#### 4. Update Settings UI
Add new "App Language" setting in SettingsScreen:
- Display current language with flag emoji
- Open language picker (same component used in Setup)
- Show "Auto (Device Language)" as first option
- Handle configuration change or activity recreation

#### 5. RTL (Right-to-Left) Support
Languages like Arabic, Hebrew, Urdu, Persian require RTL layout:
- Ensure `android:supportsRtl="true"` in AndroidManifest.xml
- Use `start`/`end` instead of `left`/`right` in layouts
- Test all Composables with RTL locales
- Use `LayoutDirection` in Compose for RTL-aware layouts

#### 6. Translation Generation Strategy
For 120+ languages:
1. Create English base file with all strings
2. Use AI translation service to generate initial translations
3. Store generated files in app resources
4. Allow future updates via app updates or remote fetch

### Files to Create/Modify
| File | Action | Description |
|------|--------|-------------|
| `data/repository/LocalizationManager.kt` | Create | Manage app language state |
| `res/values-*/strings.xml` | Create | Translation files per language |
| `ui/screens/settings/SettingsScreen.kt` | Modify | Add App Language setting |
| `ui/screens/auth/LoginScreen.kt` | Modify | Use string resources |
| `ui/screens/setup/SetupScreen.kt` | Modify | Use string resources |
| `ui/screens/chat/*.kt` | Modify | Use string resources |
| `AndroidManifest.xml` | Modify | Ensure RTL support enabled |
| All UI files | Modify | Replace hardcoded strings |

### Acceptance Criteria
- [ ] App detects device language on first launch
- [ ] All UI text displays in user's selected language
- [ ] User can change app language in Settings (persists across sessions)
- [ ] RTL languages display correctly
- [ ] Offline functionality preserved (translations bundled)
- [ ] All 120+ supported languages available

---

## ☁️ Backend Engineer Brief

### Summary
Create an endpoint to generate UI translations using the existing AI translation service, enabling the mobile apps to fetch translations for any of the 120+ supported languages.

### Why This Change
Supporting 120+ languages with static translation files would require significant manual effort. By leveraging the existing AI translation service, we can:
1. Generate accurate translations for any language on-demand
2. Ensure consistency between message translations and UI translations
3. Allow easy updates without app store releases

### Technical Requirements

#### 1. Create Translation Generation Endpoint
```
POST /api/translate/ui-strings
```

**Request Body:**
```json
{
  "targetLanguage": "ja",
  "targetCountry": "JP",
  "strings": {
    "login_button": "Sign In",
    "register_button": "Create Account",
    "settings_title": "Settings",
    // ... all UI strings (200-300 keys)
  }
}
```

**Response:**
```json
{
  "language": "ja",
  "translations": {
    "login_button": "ログイン",
    "register_button": "アカウント作成",
    "settings_title": "設定",
    // ... all translated strings
  },
  "version": "1.0.0",
  "generatedAt": "2024-12-08T10:00:00Z"
}
```

#### 2. Caching Strategy
- Cache translations in DynamoDB with TTL of 30 days
- Key: `ui-translations#{languageCode}#{version}`
- Mobile apps should cache locally and only refresh when version changes

#### 3. Base Strings Management
Create a master English strings file that serves as the source of truth:
```
server-serverless/src/data/ui-strings.json
```

This file should be version-controlled and updated when new UI strings are added.

#### 4. Batch Translation Optimization
For 200-300 strings, consider:
- Batch strings into groups of 20-30 per API call to AI service
- Use parallel requests to speed up generation
- Total generation time target: < 30 seconds for full translation

### Files to Create/Modify
| File | Action | Description |
|------|--------|-------------|
| `src/handlers/translate.ts` | Create/Modify | Add UI translation endpoint |
| `src/data/ui-strings.json` | Create | Master English strings file |
| `template.yaml` | Modify | Add new Lambda function |

### Acceptance Criteria
- [ ] Endpoint generates accurate translations for any supported language
- [ ] Response time < 30 seconds for full translation set
- [ ] Translations cached in DynamoDB
- [ ] Mobile apps can fetch and cache translations
- [ ] Version tracking for cache invalidation

---

## 📊 String Categories to Extract

### Authentication (~30 strings)
- Login screen labels and buttons
- Registration form fields and validation
- OAuth provider buttons
- Error messages

### Setup/Onboarding (~25 strings)
- Language selection instructions
- Country selection labels
- Profile setup fields
- Welcome messages

### Conversations (~40 strings)
- List headers and empty states
- Search placeholder
- New chat/group buttons
- Conversation actions (delete, archive)

### Chat (~50 strings)
- Message input placeholder
- Send button accessibility
- Media picker labels
- Typing indicator text
- Read receipts text
- Error messages
- Reply/React/Delete actions

### Settings (~40 strings)
- Section headers
- Profile fields
- Language settings
- Notification settings
- About/Legal links
- Logout/Delete account

### Common (~20 strings)
- OK/Cancel/Done buttons
- Loading states
- Error states
- Network error messages
- Generic confirmations

### Accessibility (~15 strings)
- VoiceOver/TalkBack labels
- Button descriptions
- Image descriptions

---

## 📅 Implementation Timeline

| Phase | Duration | Tasks |
|-------|----------|-------|
| Phase 1 | 3 days | Extract all English strings, create base files |
| Phase 2 | 2 days | Implement LocalizationManager on iOS & Android |
| Phase 3 | 3 days | Generate translations for Tier 1 languages (20) |
| Phase 4 | 2 days | Update Settings UI with language selector |
| Phase 5 | 2 days | RTL support and testing |
| Phase 6 | 3 days | Generate remaining languages, testing |
| **Total** | **~15 days** | Full implementation |

---

## 🧪 Testing Requirements

### Functional Testing
- [ ] Language detection on fresh install
- [ ] Language change without app restart
- [ ] Settings persistence across sessions
- [ ] Fallback to English when translation unavailable

### Visual Testing
- [ ] All screens in top 10 languages
- [ ] RTL layout (Arabic, Hebrew)
- [ ] Long text handling (German tends to be 30% longer)
- [ ] Special characters rendering

### Edge Cases
- [ ] Device language not in supported list
- [ ] Language change mid-conversation
- [ ] Offline language switching
- [ ] Memory usage with 120+ language files

---

## 📝 Notes

### Translation Quality
- AI translations should be reviewed for critical strings (legal, auth)
- Consider community contributions for quality improvement
- Plan for periodic translation updates

### App Size Impact
- Each language adds ~50-100KB to app size
- 120 languages could add 6-12MB
- Consider on-demand download for less common languages

### Coordination
- iOS and Android must maintain string key parity
- Backend provides single source of truth for translations
- All three teams should sync on string key naming conventions

---

*This plan was created for the Intok development team. Questions or concerns should be raised in team standup.*
