# Intok iOS Native App

Native iOS implementation of Intok messaging app using Swift and SwiftUI.

## Tech Stack

- **Language:** Swift 5.9+
- **UI Framework:** SwiftUI
- **Architecture:** MVVM + Combine
- **Networking:** URLSession
- **Auth:** Google Sign-In SDK

## Requirements

- Xcode 15.0 or newer
- iOS 17.0+ deployment target
- macOS Sonoma or newer (for development)

## Setup

### 1. Create Xcode Project

Since the project structure was created without Xcode, you'll need to create the Xcode project:

1. Open Xcode
2. Create new project: **File > New > Project**
3. Select **iOS > App**
4. Configure:
   - Product Name: `Intok`
   - Bundle Identifier: `com.intokapp.app`
   - Interface: `SwiftUI`
   - Language: `Swift`
5. Save in the `ios-native` folder
6. Delete the auto-generated files and use the existing Swift files

### 2. Add Dependencies

Add the following via Swift Package Manager (SPM):

```
File > Add Package Dependencies...
```

- **Google Sign-In SDK**: `https://github.com/google/GoogleSignIn-iOS`
- **SDWebImage**: `https://github.com/SDWebImage/SDWebImage`
- **Alamofire** (optional): `https://github.com/Alamofire/Alamofire`

### 3. Configure Google Sign-In

1. Get `GoogleService-Info.plist` from Firebase Console
2. Add to project
3. Add URL scheme in Info.plist

### 4. Build and Run

```bash
# From Xcode
Cmd + R

# Or via command line
xcodebuild -project Intok.xcodeproj -scheme Intok -destination 'platform=iOS Simulator,name=iPhone 15'
```

## Project Structure

```
Intok/
├── App/
│   ├── IntokApp.swift         # App entry point
│   └── ContentView.swift      # Root view
├── Features/
│   ├── Auth/
│   │   ├── LoginView.swift
│   │   └── SetupView.swift
│   ├── Conversations/
│   │   └── ConversationsView.swift
│   ├── Chat/
│   │   └── ChatView.swift
│   └── Settings/
│       └── SettingsView.swift
├── Core/
│   ├── Network/
│   │   └── AuthManager.swift
│   ├── Models/
│   │   └── Models.swift
│   └── Utils/
│       └── Theme.swift
└── Resources/
    └── Assets.xcassets/
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

## Design System

The app uses a custom design system with:

- **Colors:** Purple primary palette matching Intok branding
- **Typography:** System fonts with custom sizing
- **Components:** Custom button styles, text fields, etc.

See `Core/Utils/Theme.swift` for details.

