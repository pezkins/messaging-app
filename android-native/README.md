# Intok Android Native App

Native Android implementation of Intok messaging app using Kotlin and Jetpack Compose.

## Tech Stack

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose
- **Architecture:** MVVM + Clean Architecture
- **DI:** Hilt (Dagger)
- **Networking:** Retrofit + OkHttp
- **Image Loading:** Coil
- **Auth:** Google Sign-In SDK

## Requirements

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 35 (minimum SDK 26)

## Setup

1. Clone the repository
2. Open `android-native` folder in Android Studio
3. Sync Gradle files
4. Add `google-services.json` to `app/` folder (for Google Sign-In)
5. Build and run

## Project Structure

```
app/src/main/java/com/intokapp/app/
├── IntokApp.kt              # Application class
├── MainActivity.kt          # Main activity
├── ui/
│   ├── theme/               # Colors, Typography, Theme
│   ├── navigation/          # Navigation setup
│   └── screens/
│       ├── auth/            # Login, Setup screens
│       ├── conversations/   # Conversations list
│       ├── chat/            # Chat screen
│       └── settings/        # Settings screen
├── data/
│   ├── api/                 # API service, WebSocket
│   ├── models/              # Data models
│   └── repository/          # Repositories
└── di/                      # Dependency injection modules
```

## Building

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test
```

## Features Status

- [ ] Google Sign-In
- [ ] Email/Password Auth
- [ ] Conversations List
- [ ] Direct Messages
- [ ] Group Chats
- [ ] Real-time Messaging (WebSocket)
- [ ] Auto-translation
- [ ] Emoji Reactions
- [ ] Attachments (Images, Documents)
- [ ] GIF Picker + Native Keyboard GIF
- [ ] Voice Messages
- [ ] Push Notifications

