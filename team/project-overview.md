# 🌍 Intok Project Overview

> A multilingual mobile messaging app that automatically translates messages into your preferred language. Talk to anyone, anywhere!

## Quick Reference

| Component | Technology | Directory |
|-----------|------------|-----------|
| iOS App | Swift + SwiftUI | `ios-native/` |
| Android App | Kotlin + Jetpack Compose | `android-native/` |
| Backend | AWS Lambda + DynamoDB | `server-serverless/` |
| CI/CD | GitHub Actions | `.github/workflows/` |
| Docs | HTML/Markdown | `docs/`, `team/` |

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                                INTOK ARCHITECTURE                                │
└─────────────────────────────────────────────────────────────────────────────────┘

┌──────────────────┐         ┌──────────────────┐         
│   📱 iOS App     │         │   📱 Android App │         
│   (Swift/SwiftUI)│         │   (Kotlin/Compose)│         
│   User: Alice    │         │   User: Carlos   │         
│   Lang: English  │         │   Lang: Spanish  │         
└────────┬─────────┘         └────────┬─────────┘         
         │                            │                    
         │  REST API (HTTP)           │  REST API (HTTP)   
         │  + WebSocket               │  + WebSocket       
         │                            │                    
         └────────────────────────────┼────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                          ☁️  AWS SERVERLESS BACKEND                             │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │                         API GATEWAY                                      │    │
│  │  HTTP API     - REST endpoints for auth, users, conversations           │    │
│  │  WebSocket API - Real-time messaging                                    │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
│                                      │                                           │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │                      LAMBDA FUNCTIONS                                    │    │
│  │  auth.ts        - Login, Register, Token Refresh                        │    │
│  │  users.ts       - Profile, Language Settings, Search                    │    │
│  │  conversations.ts - List, Create Conversations                          │    │
│  │  messages.ts    - Message History, Translation Preview                  │    │
│  │  websocket.ts   - Real-time message handling                            │    │
│  │  attachments.ts - File uploads and downloads                            │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
│                                      │                                           │
│       ┌──────────────────────────────┼──────────────────────────────┐           │
│       ▼                              ▼                              ▼           │
│  ┌─────────────┐             ┌─────────────┐             ┌─────────────────┐    │
│  │  DynamoDB   │             │ Translation │             │   S3 Bucket     │    │
│  │  - Users    │             │   Service   │             │   (Attachments) │    │
│  │  - Messages │             │  (DeepSeek/ │             │                 │    │
│  │  - Convos   │             │   OpenAI)   │             │                 │    │
│  └─────────────┘             └─────────────┘             └─────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## Message Flow Example

```
Alice (English) sends "Hello, how are you?" to Carlos (Spanish):

1. 📱 Alice's iOS App
   └─► WebSocket: message:send { content: "Hello, how are you?", conversationId: "xxx" }

2. ☁️ Lambda (websocket.ts)
   ├─► Detect language → "en" (English)
   ├─► Store original message in DynamoDB
   └─► Translate for each recipient's language

3. 🤖 Translation Service (DeepSeek API)
   ├─► Input: "Hello, how are you?" (en → es)
   └─► Output: "¡Hola! ¿Cómo estás?"

4. 📱 Carlos's Android App
   └─◄ WebSocket: message:receive { 
         originalContent: "Hello, how are you?",
         translatedContent: "¡Hola! ¿Cómo estás?",
         originalLanguage: "en",
         targetLanguage: "es"
       }
```

## Supported Languages

| Code | Language | Code | Language |
|------|----------|------|----------|
| en | English | ja | Japanese |
| es | Spanish | ko | Korean |
| fr | French | ar | Arabic |
| de | German | hi | Hindi |
| it | Italian | pt | Portuguese |
| zh | Chinese | ru | Russian |
| nl | Dutch | tr | Turkish |
| pl | Polish | vi | Vietnamese |

## Branch Strategy

```
┌─────────────────────────────────────────────────────────────────┐
│    dev ──────► stage ──────► main                              │
│     │           │             │                                 │
│     ▼           ▼             ▼                                 │
│  Simulators  Internal      Production                          │
│  & Debug     Testing       Release                             │
│              (TestFlight   (App Store                          │
│               & Play       & Play Store)                       │
│               Internal)                                         │
└─────────────────────────────────────────────────────────────────┘
```

## Deprecated Components

> ⚠️ **DO NOT MODIFY** the following directories - they are being phased out:

| Directory | Status | Replacement |
|-----------|--------|-------------|
| `server/` | Deprecated | `server-serverless/` |
| `shared/` | Deprecated | Inline types in each app |
| `mobile/` | Deprecated | `ios-native/` + `android-native/` |
| `infrastructure/` | Deprecated | SAM template in `server-serverless/` |

## ⛔ Critical Configuration - DO NOT TOUCH

> ⚠️ **NEVER MODIFY** the following authentication configuration without explicit approval:

| File | Critical Settings |
|------|-------------------|
| `ios-native/Intok.xcodeproj/project.pbxproj` | `DEVELOPMENT_TEAM = LW7QG2H5ST`, `CODE_SIGN_ENTITLEMENTS` |
| `ios-native/Intok/Intok.entitlements` | `com.apple.developer.applesignin` |
| `android-native/app/google-services.json` | OAuth client IDs |

**See `team/authentication-config.md` for complete documentation.**

## Key Features

### Implemented ✅
- User authentication (JWT)
- Conversation management
- Real-time messaging (WebSocket)
- Automatic translation
- Language preference settings

### In Progress 🚧
- Push notifications
- Attachment support (images, documents)
- GIF picker integration
- Emoji reactions

### Planned 📋
- Voice messages
- Group translation sync
- Offline mode
- End-to-end encryption

## Technology Decisions

### Why Native Apps?
- Better performance than React Native
- Access to latest platform APIs
- Improved user experience
- Easier debugging and profiling

### Why Serverless?
- Pay-per-use pricing
- Automatic scaling
- No server management
- Global availability

### Why DeepSeek for Translation?
- Cost-effective (~$0.14/1M tokens)
- Free tier available
- High-quality translations
- Low latency

---

*Last Updated: December 2024*

