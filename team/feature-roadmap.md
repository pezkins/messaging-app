# 🗺️ Feature Roadmap

This document tracks the planned features, current development, and completed work for Intok.

---

## Status Legend

| Status | Icon | Description |
|--------|------|-------------|
| Completed | ✅ | Feature is live in production |
| In Progress | 🚧 | Currently being developed |
| Planned | 📋 | Scheduled for development |
| Backlog | 💭 | Future consideration |
| Blocked | 🚫 | Waiting on dependency |

---

## Current Sprint

**Sprint:** December 2024
**Focus:** Native app core functionality

### iOS 🍎

| Feature | Status | Assignee | Notes |
|---------|--------|----------|-------|
| Google Sign-In | 🚧 | iOS Dev | SDK integrated, testing auth flow |
| Apple Sign-In | 📋 | iOS Dev | After Google Sign-In |
| WebSocket Connection | 🚧 | iOS Dev | Real-time messaging |
| Message Send/Receive | 📋 | iOS Dev | Depends on WebSocket |

### Android 🤖

| Feature | Status | Assignee | Notes |
|---------|--------|----------|-------|
| Google Sign-In | 🚧 | Android Dev | SDK integrated |
| WebSocket Connection | 🚧 | Android Dev | OkHttp implementation |
| Message Send/Receive | 📋 | Android Dev | Depends on WebSocket |
| DataStore Migration | 📋 | Android Dev | Secure token storage |

### Backend ☁️

| Feature | Status | Assignee | Notes |
|---------|--------|----------|-------|
| Google OAuth Verification | ✅ | Backend Dev | Token validation ready |
| WebSocket Improvements | 🚧 | Backend Dev | Connection stability |
| Rate Limiting | 📋 | Backend Dev | Security enhancement |

---

## Phase 1: Core Messaging (Current)

### Authentication
| Feature | iOS | Android | Backend |
|---------|-----|---------|---------|
| Email/Password Login | ✅ | ✅ | ✅ |
| Google Sign-In | 🚧 | 🚧 | ✅ |
| Apple Sign-In | 📋 | N/A | 📋 |
| Token Refresh | ✅ | ✅ | ✅ |
| Secure Token Storage | 🚧 | 🚧 | N/A |

### Conversations
| Feature | iOS | Android | Backend |
|---------|-----|---------|---------|
| List Conversations | ✅ | ✅ | ✅ |
| Create Direct Chat | ✅ | ✅ | ✅ |
| Create Group Chat | 📋 | 📋 | ✅ |
| User Search | ✅ | ✅ | ✅ |
| Unread Counts | 📋 | 📋 | ✅ |

### Messaging
| Feature | iOS | Android | Backend |
|---------|-----|---------|---------|
| Send Text Message | 🚧 | 🚧 | ✅ |
| Receive Message | 🚧 | 🚧 | ✅ |
| Auto Translation | 📋 | 📋 | ✅ |
| Translation Preview | 📋 | 📋 | ✅ |
| Message History | ✅ | ✅ | ✅ |
| Typing Indicators | 📋 | 📋 | ✅ |
| Read Receipts | 📋 | 📋 | ✅ |

### Settings
| Feature | iOS | Android | Backend |
|---------|-----|---------|---------|
| Language Selection | ✅ | ✅ | ✅ |
| Profile Update | 📋 | 📋 | 📋 |
| Logout | ✅ | ✅ | ✅ |
| Delete Account | 💭 | 💭 | 💭 |

---

## Phase 2: Rich Media

| Feature | iOS | Android | Backend | Priority |
|---------|-----|---------|---------|----------|
| Image Attachments | 📋 | 📋 | 📋 | High |
| Document Attachments | 📋 | 📋 | 📋 | Medium |
| GIF Picker (Tenor) | 📋 | 📋 | 📋 | Medium |
| Native Keyboard GIFs | 📋 | 📋 | N/A | Low |
| Voice Messages | 💭 | 💭 | 💭 | Low |
| Video Messages | 💭 | 💭 | 💭 | Low |

### Implementation Notes

**Image Attachments:**
- Use S3 presigned URLs for upload/download
- Support JPEG, PNG, HEIC
- Max size: 10MB
- Generate thumbnails server-side

**GIF Picker:**
- Integrate Tenor API
- Cache frequently used GIFs
- Support search and trending

---

## Phase 3: Engagement Features

| Feature | iOS | Android | Backend | Priority |
|---------|-----|---------|---------|----------|
| Emoji Reactions | 📋 | 📋 | 📋 | High |
| Push Notifications | 📋 | 📋 | 📋 | High |
| Message Forwarding | 💭 | 💭 | 💭 | Medium |
| Message Replies | 💭 | 💭 | 💭 | Medium |
| Pin Messages | 💭 | 💭 | 💭 | Low |
| Message Search | 💭 | 💭 | 💭 | Low |

### Push Notifications Requirements

**iOS:**
- APNs integration
- Notification Service Extension for rich notifications
- Handle notification tap to open specific chat

**Android:**
- FCM integration
- Notification channels
- Handle notification tap

**Backend:**
- Store device tokens in DynamoDB
- Lambda for sending notifications
- Support silent notifications for data sync

---

## Phase 4: Advanced Features

| Feature | iOS | Android | Backend | Priority |
|---------|-----|---------|---------|----------|
| Offline Mode | 💭 | 💭 | 💭 | High |
| Message Sync | 💭 | 💭 | 💭 | High |
| End-to-End Encryption | 💭 | 💭 | 💭 | Medium |
| Video Calling | 💭 | 💭 | 💭 | Low |
| Screen Sharing | 💭 | 💭 | 💭 | Low |

---

## Technical Debt & Improvements

| Item | Area | Priority | Assignee |
|------|------|----------|----------|
| Add unit tests | iOS | High | iOS Dev |
| Add unit tests | Android | High | Android Dev |
| API error handling improvements | Backend | Medium | Backend Dev |
| Accessibility audit | iOS/Android | Medium | Architect |
| Performance optimization | iOS/Android | Medium | All |
| Memory leak audit | iOS/Android | High | All |

---

## Completed Features ✅

### v0.1.0 - Initial Release
- [x] Basic authentication (email/password)
- [x] Conversation list UI
- [x] Chat UI
- [x] Settings UI
- [x] Language selection
- [x] REST API integration
- [x] Basic navigation

---

## Feature Requests

Track feature requests and their status:

| Request | Source | Status | Notes |
|---------|--------|--------|-------|
| Dark mode | User feedback | ✅ | Implemented |
| Message reactions | User feedback | 📋 | Phase 3 |
| Voice messages | User feedback | 💭 | Phase 2 |
| Custom themes | User feedback | 💭 | Backlog |

---

## Coordination Notes

### Frontend-Backend Sync

When a frontend feature requires backend changes:
1. Frontend dev documents requirements in this file
2. Backend dev reviews and confirms API changes
3. API contract updated in `api-contracts.md`
4. Both teams align on timeline

### Feature Parity

iOS and Android should maintain feature parity. If one platform implements a feature, the other should follow in the same sprint when possible.

### Breaking Changes

Before introducing breaking changes:
1. Document in this roadmap
2. Update API contracts
3. Plan migration path
4. Coordinate release timing

---

*Last Updated: December 2024*

