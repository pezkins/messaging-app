# Intok 💬🌍

A multilingual mobile messaging app that automatically translates messages into your preferred language. Talk to anyone, anywhere!

> 📖 **New to coding?** Check out [SETUP.md](./SETUP.md) for a beginner-friendly step-by-step guide!

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                                INTOK ARCHITECTURE                                │
└─────────────────────────────────────────────────────────────────────────────────┘

┌──────────────────┐         ┌──────────────────┐         ┌──────────────────┐
│   📱 Mobile App  │         │   📱 Mobile App  │         │   🌐 Web App     │
│   (iOS/Android)  │         │   (iOS/Android)  │         │   (Browser)      │
│   User: Alice    │         │   User: Carlos   │         │   User: Yuki     │
│   Lang: English  │         │   Lang: Spanish  │         │   Lang: Japanese │
└────────┬─────────┘         └────────┬─────────┘         └────────┬─────────┘
         │                            │                            │
         │  REST API (HTTP)           │  REST API (HTTP)           │  REST API (HTTP)
         │  + Socket.IO (WS)          │  + Socket.IO (WS)          │  + Socket.IO (WS)
         │                            │                            │
         └────────────────────────────┼────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           🖥️  NODE.JS SERVER                                    │
│                              (Express.js)                                        │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │                         API ROUTES                                       │    │
│  │  /api/auth/*        - Login, Register, Token Refresh                    │    │
│  │  /api/users/*       - Profile, Language Settings, Search                │    │
│  │  /api/conversations - List, Create Conversations                        │    │
│  │  /api/messages/*    - Message History, Translation Preview              │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
│                                      │                                           │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │                      SOCKET.IO HANDLERS                                  │    │
│  │  message:send    → Receive message, detect language, translate, store   │    │
│  │  message:receive ← Send translated message to recipient                 │    │
│  │  message:typing  ↔ Typing indicators                                    │    │
│  │  message:read    ↔ Read receipts                                        │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
│                                      │                                           │
│  ┌───────────────────┐    ┌──────────┴──────────┐    ┌───────────────────┐      │
│  │ 🔐 Auth Service   │    │ 🌐 Translation Svc  │    │ 💬 Message Service│      │
│  │ - JWT tokens      │    │ - Language detect   │    │ - Store messages  │      │
│  │ - Password hash   │    │ - Translate text    │    │ - Fetch history   │      │
│  │ - Session mgmt    │    │ - Cache results     │    │ - Real-time sync  │      │
│  └───────────────────┘    └──────────┬──────────┘    └───────────────────┘      │
└──────────────────────────────────────┼──────────────────────────────────────────┘
                                       │
         ┌─────────────────────────────┼─────────────────────────────┐
         │                             │                             │
         ▼                             ▼                             ▼
┌─────────────────┐         ┌─────────────────────┐         ┌─────────────────┐
│  🐘 PostgreSQL  │         │   🤖 AI Provider    │         │   🔴 Redis      │
│                 │         │   (configurable)    │         │                 │
│  - Users        │         │                     │         │  - Translation  │
│  - Messages     │         │  ┌───────────────┐  │         │    cache        │
│  - Conversations│         │  │ OpenAI        │  │         │  - User sessions│
│  - Translations │         │  │ (gpt-4o-mini) │  │         │  - Socket IDs   │
│                 │         │  ├───────────────┤  │         │                 │
│                 │         │  │ Claude        │  │         │                 │
│                 │         │  │ (haiku)       │  │         │                 │
│                 │         │  ├───────────────┤  │         │                 │
│                 │         │  │ DeepSeek      │  │         │                 │
│                 │         │  │ (chat)        │  │         │                 │
│                 │         │  └───────────────┘  │         │                 │
└─────────────────┘         └─────────────────────┘         └─────────────────┘
```

### Message Flow Example

```
Alice (English) sends "Hello, how are you?" to Carlos (Spanish):

1. 📱 Alice's App
   └─► Socket.IO: message:send { content: "Hello, how are you?", conversationId: "xxx" }

2. 🖥️ Server receives message
   ├─► Detect language → "en" (English)
   ├─► Store original message in PostgreSQL
   └─► For each recipient, translate to their language:

3. 🤖 AI Provider (for Carlos - Spanish)
   ├─► Input: "Hello, how are you?" (en → es)
   └─► Output: "¡Hola! ¿Cómo estás?"

4. 🔴 Redis
   └─► Cache translation for future requests

5. 📱 Carlos's App
   └─◄ Socket.IO: message:receive { 
         originalContent: "Hello, how are you?",
         translatedContent: "¡Hola! ¿Cómo estás?",
         originalLanguage: "en",
         targetLanguage: "es"
       }

6. 📱 Carlos sees: "¡Hola! ¿Cómo estás?" 
   └─► (Can tap to view original English)
```

### Network Diagram (Development Setup)

```
┌─────────────────────────────────────────────────────────────────┐
│                     YOUR WINDOWS MACHINE                         │
│  ┌─────────────────────┐    ┌─────────────────────────────────┐ │
│  │   🌐 Web Browser    │    │      📱 Expo Go App             │ │
│  │   localhost:8081    │    │      (on your phone)            │ │
│  │                     │    │                                 │ │
│  │   React Native Web  │    │   Connects via WiFi to:         │ │
│  │   (Expo Web)        │    │   http://192.168.2.62:3001      │ │
│  └──────────┬──────────┘    └──────────┬──────────────────────┘ │
│             │                          │                         │
│             │ HTTP/WebSocket           │ HTTP/WebSocket          │
│             └──────────────────────────┼─────────────────────────┤
│                                        │                         │
│  ┌─────────────────────────────────────┼───────────────────────┐ │
│  │              WSL (Ubuntu)           │                       │ │
│  │  ┌─────────────────────────────┐    │                       │ │
│  │  │  📁 Source Code             │    │                       │ │
│  │  │  /home/ayepez/github/       │    │                       │ │
│  │  │  messaging-app/             │    │                       │ │
│  │  └─────────────────────────────┘    │                       │ │
│  └─────────────────────────────────────┼───────────────────────┘ │
└────────────────────────────────────────┼─────────────────────────┘
                                         │
                        Network (LAN)    │  192.168.2.x
                                         │
┌────────────────────────────────────────┼─────────────────────────┐
│              PROXMOX VM (Ubuntu)       │    IP: 192.168.2.62     │
│  ┌─────────────────────────────────────┴───────────────────────┐ │
│  │                    🖥️ Node.js Server                        │ │
│  │                    Port 3001                                 │ │
│  │                    (PM2 managed)                             │ │
│  └──────────────────────────┬──────────────────────────────────┘ │
│                             │                                    │
│       ┌─────────────────────┼─────────────────────┐              │
│       ▼                     ▼                     ▼              │
│  ┌─────────┐         ┌─────────────┐       ┌───────────┐        │
│  │🐘 Postgres│        │ 🔴 Redis    │       │ 🤖 Claude │        │
│  │Port 5432 │        │ Port 6379   │       │   API     │        │
│  │(Docker)  │        │ (Docker)    │       │ (Internet)│        │
│  └─────────┘         └─────────────┘       └───────────┘        │
└──────────────────────────────────────────────────────────────────┘
```

## Features

- **Real-time messaging** with WebSocket support
- **Automatic translation** powered by Claude AI (Anthropic)
- **Translation preview** before sending (1-to-1 chats)
- **Group chat support** with per-user translation
- **Voice messages** (Phase 2)
- **Message caching** with Redis to minimize API calls
- **16 supported languages** including English, Spanish, French, Chinese, Japanese, etc.

## Tech Stack

### Backend (Two Options)

**Option A: Traditional Server** (`server/`)
- **Node.js + TypeScript** - Server runtime
- **Express** - REST API framework
- **Socket.IO** - Real-time WebSocket communication
- **PostgreSQL + Prisma** - Database
- **Redis** - Translation caching

**Option B: Serverless (Recommended)** (`server-serverless/`)
- **AWS Lambda** - Serverless functions
- **API Gateway** - HTTP + WebSocket APIs
- **DynamoDB** - Serverless database (pay-per-request)
- **No Redis needed** - Translations cached in DynamoDB

### AI Translation (Multi-Provider Support)
- **OpenAI** - GPT-4o-mini model (default, fast & affordable)
- **Claude (Anthropic)** - Claude Haiku model (~$0.25/million input tokens)
- **DeepSeek** - DeepSeek Chat (~$0.14/million tokens, FREE tier available)

Switch providers via `AI_PROVIDER` environment variable.

### Mobile App (iOS & Android)
- **React Native + Expo** - Cross-platform mobile framework
- **Expo Router** - File-based navigation
- **Socket.IO Client** - Real-time communication
- **Zustand** - State management
- **AsyncStorage** - Token storage

## Project Structure

```
messaging-app/
├── server/                   # Traditional Express server (local dev)
│   ├── prisma/               # PostgreSQL schema
│   └── src/
│       ├── routes/           # REST API endpoints
│       ├── services/         # Translation service
│       └── socket/           # Socket.IO handlers
│
├── server-serverless/        # AWS Lambda serverless (production)
│   ├── template.yaml         # AWS SAM CloudFormation
│   └── src/
│       ├── handlers/         # Lambda functions
│       └── lib/
│           └── translation.ts # DeepSeek API + Ollama support
│
├── mobile/                   # React Native (Expo) app
│   ├── app/                  # Expo Router screens
│   │   ├── (auth)/           # Login & Register
│   │   └── (app)/            # Conversations, Chat, Settings
│   ├── src/
│   │   ├── services/         # API & WebSocket clients
│   │   ├── store/            # Zustand stores
│   │   └── constants/        # Theme & languages
│   └── assets/               # App icons & images
│
├── infrastructure/
│   ├── aws/                  # AWS deployment config
│   │   └── samconfig.toml
│   └── ollama/               # Self-hosted AI setup
│       ├── docker-compose.yml
│       └── setup.sh
│
├── shared/                   # Shared TypeScript types
├── DEPLOYMENT.md             # Detailed deployment guide
└── README.md                 # This file
```

## Getting Started

### Prerequisites

**For Traditional Server:**
- Node.js >= 18
- PostgreSQL database
- Redis server
- AI API key (OpenAI, Claude, or DeepSeek)

**For Serverless (Recommended):**
- Node.js >= 18
- AWS Account (free tier eligible)
- AWS CLI + SAM CLI installed
- AI API key (OpenAI, Claude, or DeepSeek)

### Backend Setup

```bash
cd messaging-app

# Install dependencies
npm install

# Create server/.env file
cat > server/.env << EOF
DATABASE_URL="postgresql://postgres:password@localhost:5432/lingualink"
REDIS_URL="redis://localhost:6379"
AI_PROVIDER="openai"
OPENAI_API_KEY="your-openai-api-key"
JWT_SECRET="your-super-secret-jwt-key"
JWT_REFRESH_SECRET="your-refresh-secret-key"
PORT=3001
CORS_ORIGIN="*"
EOF

# Generate Prisma client
npm run db:generate

# Run database migrations
npm run db:migrate

# Seed demo data (optional)
npm run db:seed

# Start the server
npm run dev:server
```

### Mobile App Setup

```bash
cd mobile

# Install dependencies
npm install

# Create .env file for mobile
cat > .env << EOF
EXPO_PUBLIC_API_URL=http://YOUR_SERVER_IP:3001
EXPO_PUBLIC_WS_URL=http://YOUR_SERVER_IP:3001
EOF

# Start Expo development server
npm start
```

Then scan the QR code with Expo Go app on your phone.

### Demo Accounts

After seeding, you can use these accounts:
- `alice@example.com` / `password123` (English)
- `carlos@example.com` / `password123` (Spanish)
- `marie@example.com` / `password123` (French)
- `yuki@example.com` / `password123` (Japanese)

## Deploying to App Stores

### Prerequisites

1. **Apple Developer Account** ($99/year) for iOS
2. **Google Play Developer Account** ($25 one-time) for Android
3. **EAS CLI** installed: `npm install -g eas-cli`

### Build & Submit

```bash
cd mobile

# Login to Expo
eas login

# Configure your project
eas build:configure

# Build for iOS
eas build --platform ios --profile production

# Build for Android
eas build --platform android --profile production

# Submit to App Store
eas submit --platform ios

# Submit to Google Play
eas submit --platform android
```

### App Store Configuration

1. **iOS (App Store Connect)**
   - Create app in App Store Connect
   - Update `app.json` with your bundle identifier
   - Configure push notification certificates

2. **Android (Google Play Console)**
   - Create app in Play Console
   - Generate upload key and configure signing
   - Set up Play App Signing

See [Expo EAS Submit docs](https://docs.expo.dev/submit/introduction/) for detailed instructions.

## API Documentation

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login user
- `POST /api/auth/refresh` - Refresh access token

### Users
- `GET /api/users/me` - Get current user
- `PATCH /api/users/me/language` - Update preferred language
- `GET /api/users/search?q=query` - Search users

### Conversations & Messages
- `GET /api/conversations` - Get all conversations
- `POST /api/conversations` - Create conversation
- `GET /api/conversations/:id/messages` - Get messages
- `POST /api/messages/preview-translation` - Preview translation

### WebSocket Events
- `message:send` - Send a message
- `message:receive` - Receive translated message
- `message:typing` - Typing indicator
- `message:read` - Read receipt

## Environment Variables - Complete Setup Checklist

### 🔑 API Keys & Accounts You Need

| Service | Purpose | How to Get | Cost |
|---------|---------|------------|------|
| **OpenAI API** | AI Translation (default) | https://platform.openai.com/api-keys | ~$0.15/M tokens |
| **Claude API** | AI Translation (alt) | https://console.anthropic.com/ | ~$0.25/M tokens |
| **DeepSeek API** | AI Translation (alt) | https://platform.deepseek.com/ | **FREE** tier |
| **AWS Account** | Serverless hosting | https://aws.amazon.com/free | Free tier available |
| **Apple Developer** | iOS App Store | https://developer.apple.com/programs/ | $99/year |
| **Google Play Developer** | Android Play Store | https://play.google.com/console | $25 one-time |
| **Expo Account** | Mobile builds | https://expo.dev/signup | **FREE** |

---

### 📋 Option 1: Traditional Server (`server/.env`)

Create file: `server/.env`

```bash
# ============================================
# DATABASE (Required)
# ============================================
# PostgreSQL connection string
# Format: postgresql://USER:PASSWORD@HOST:PORT/DATABASE
DATABASE_URL="postgresql://postgres:your_password@localhost:5432/lingualink"

# ============================================
# REDIS (Required for caching)
# ============================================
# Redis connection string
# Local: redis://localhost:6379
# Cloud (Upstash free tier): redis://default:xxx@xxx.upstash.io:6379
REDIS_URL="redis://localhost:6379"

# ============================================
# AI TRANSLATION (Required - Choose one provider)
# ============================================
# Set which AI provider to use: "openai" | "anthropic" | "deepseek"
AI_PROVIDER="openai"

# OpenAI API Key (if AI_PROVIDER=openai)
# Get it at: https://platform.openai.com/api-keys
OPENAI_API_KEY="sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"

# Anthropic/Claude API Key (if AI_PROVIDER=anthropic)
# Get it at: https://console.anthropic.com/
ANTHROPIC_API_KEY="sk-ant-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"

# DeepSeek API Key (if AI_PROVIDER=deepseek)
# Get it at: https://platform.deepseek.com/api_keys (FREE tier!)
DEEPSEEK_API_KEY="sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"

# ============================================
# AUTHENTICATION (Required)
# ============================================
# JWT secrets - generate random strings (32+ characters)
# Generate with: openssl rand -base64 32
JWT_SECRET="your-super-secret-jwt-key-min-32-chars"
JWT_REFRESH_SECRET="your-refresh-secret-key-min-32-chars"

# Token expiration (optional)
JWT_EXPIRES_IN="15m"
JWT_REFRESH_EXPIRES_IN="7d"

# ============================================
# SERVER CONFIG (Optional)
# ============================================
PORT=3001
NODE_ENV="development"
CORS_ORIGIN="*"
```

---

### 📋 Option 2: Serverless AWS (`infrastructure/aws/samconfig.toml`)

These are set during `sam deploy --guided`:

```bash
# ============================================
# REQUIRED PARAMETERS
# ============================================

# JWT Secret for authentication
# Generate with: openssl rand -base64 32
JwtSecret=your-jwt-secret-min-32-characters

# Translation mode: "api" or "ollama"
TranslationMode=api

# ============================================
# IF TranslationMode=api (DeepSeek Cloud)
# ============================================
# Get free API key at: https://platform.deepseek.com/api_keys
DeepSeekApiKey=sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# ============================================
# IF TranslationMode=ollama (Self-hosted AI)
# ============================================
# URL of your Ollama server
OllamaUrl=http://your-server-ip:11434
```

---

### 📱 Mobile App (`mobile/.env`)

Create file: `mobile/.env`

```bash
# ============================================
# API ENDPOINTS (Required)
# ============================================

# For LOCAL development:
EXPO_PUBLIC_API_URL=http://localhost:3001
EXPO_PUBLIC_WS_URL=ws://localhost:3001

# For AWS SERVERLESS (after deployment):
# Get these URLs from: sam deploy output or AWS Console
EXPO_PUBLIC_API_URL=https://xxxxxxxx.execute-api.us-east-1.amazonaws.com/prod
EXPO_PUBLIC_WS_URL=wss://xxxxxxxx.execute-api.us-east-1.amazonaws.com/prod

# For PRODUCTION with custom domain:
# EXPO_PUBLIC_API_URL=https://api.lingualink.app
# EXPO_PUBLIC_WS_URL=wss://ws.lingualink.app
```

---

### 📲 App Store Submission (`mobile/app.json` & `mobile/eas.json`)

Update these in `mobile/app.json`:

```json
{
  "expo": {
    "name": "LinguaLink",
    "slug": "lingualink",
    "ios": {
      "bundleIdentifier": "com.yourcompany.lingualink"  // Change this!
    },
    "android": {
      "package": "com.yourcompany.lingualink"  // Change this!
    },
    "extra": {
      "eas": {
        "projectId": "your-expo-project-id"  // From expo.dev
      }
    }
  }
}
```

Update in `mobile/eas.json` for app store submission:

```json
{
  "submit": {
    "production": {
      "ios": {
        "appleId": "your-apple-id@email.com",
        "ascAppId": "1234567890"  // From App Store Connect
      },
      "android": {
        "serviceAccountKeyPath": "./google-service-account.json"
      }
    }
  }
}
```

---

### 🚀 Quick Start Commands

```bash
# 1. Generate secure JWT secrets
openssl rand -base64 32  # Run twice, use for JWT_SECRET and JWT_REFRESH_SECRET

# 2. Get an AI API key (choose one):
# - OpenAI (default): https://platform.openai.com/api-keys
# - Claude: https://console.anthropic.com/
# - DeepSeek (FREE): https://platform.deepseek.com/

# 3. Create server/.env with all values above

# 4. Start the app
cd messaging-app
npm install
npm run db:generate
npm run db:migrate
npm run dev:server

# 5. In another terminal, start mobile
cd mobile
npm install
npm start
```

---

### 🔐 Security Notes

- **Never commit `.env` files** to git (already in `.gitignore`)
- **Rotate JWT secrets** periodically in production
- **Use AWS Secrets Manager** for serverless production deployments
- **Claude API** - monitor usage at console.anthropic.com

## Roadmap

- [x] MVP: Text messaging + auto-translation
- [ ] Phase 2: Voice messages
- [ ] Phase 3: Group translation sync
- [ ] Phase 4: Message reactions & emojis
- [ ] Phase 5: Push notifications
- [ ] Phase 6: Offline mode with local cache

## License

MIT
