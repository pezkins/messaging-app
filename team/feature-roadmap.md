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
**Focus:** Bug fixes, polish, and push notification deep linking

### Recent Completions (v0.1.28)
- ✅ Cross-platform GIF support fix
- ✅ Push notification deep linking (navigate to specific chat)
- ✅ iOS GIPHY API configuration
- ✅ Group Info screen (view/edit name, picture, participants)

---

## Phase 1: Core Messaging ✅ COMPLETE

### Authentication
| Feature | iOS | Android | Backend |
|---------|-----|---------|---------|
| Email/Password Login | ✅ | ✅ | ✅ |
| Google Sign-In | ✅ | ✅ | ✅ |
| Apple Sign-In | ✅ | N/A | ✅ |
| Token Refresh | ✅ | ✅ | ✅ |
| Secure Token Storage | ✅ | ✅ | N/A |
| OAuth Flow | ✅ | ✅ | ✅ |

### Conversations
| Feature | iOS | Android | Backend |
|---------|-----|---------|---------|
| List Conversations | ✅ | ✅ | ✅ |
| Create Direct Chat | ✅ | ✅ | ✅ |
| Create Group Chat | ✅ | ✅ | ✅ |
| User Search | ✅ | ✅ | ✅ |
| Unread Counts | ✅ | ✅ | ✅ |
| Delete Conversation | ✅ | ✅ | ✅ |

### Messaging
| Feature | iOS | Android | Backend |
|---------|-----|---------|---------|
| Send Text Message | ✅ | ✅ | ✅ |
| Receive Message (WebSocket) | ✅ | ✅ | ✅ |
| Auto Translation | ✅ | ✅ | ✅ |
| Show Original Toggle | ✅ | ✅ | ✅ |
| Message History | ✅ | ✅ | ✅ |
| Typing Indicators | ✅ | ✅ | ✅ |
| Read Receipts | ✅ | ✅ | ✅ |
| Message Pagination | ✅ | ✅ | ✅ |
| Offline Cache | ✅ | ✅ | N/A |

### Settings
| Feature | iOS | Android | Backend |
|---------|-----|---------|---------|
| Language Selection | ✅ | ✅ | ✅ |
| Country/Region Selection | ✅ | ✅ | ✅ |
| Profile Picture | ✅ | ✅ | ✅ |
| Username Update | ✅ | ✅ | ✅ |
| Logout | ✅ | ✅ | ✅ |
| Delete Account | 💭 | 💭 | 💭 |

---

## Phase 2: Rich Media ✅ COMPLETE

| Feature | iOS | Android | Backend | Notes |
|---------|-----|---------|---------|-------|
| Image Attachments | ✅ | ✅ | ✅ | S3 presigned URLs |
| Document Attachments | ✅ | ✅ | ✅ | PDF, TXT support |
| Document Translation | ✅ | ✅ | ✅ | Optional translate toggle |
| GIF Picker (GIPHY) | ✅ | ✅ | N/A | Cross-platform support |
| Camera Capture | ✅ | ✅ | N/A | Take photo to send |
| Download/Save Media | ✅ | ✅ | ✅ | Save to Photos/Gallery |
| Voice Messages | 💭 | 💭 | 💭 | Backlog |
| Video Messages | 💭 | 💭 | 💭 | Backlog |

---

## Phase 3: Engagement Features ✅ MOSTLY COMPLETE

| Feature | iOS | Android | Backend | Notes |
|---------|-----|---------|---------|-------|
| Emoji Reactions | ✅ | ✅ | ✅ | Quick reactions + full picker |
| Frequent Emojis | ✅ | ✅ | N/A | Track most used |
| Push Notifications | ✅ | ✅ | ✅ | APNs + FCM |
| Notification Deep Linking | ✅ | ✅ | ✅ | Open specific chat |
| Translated Notifications | ✅ | ✅ | ✅ | Show in recipient's language |
| Message Replies | ✅ | ✅ | ✅ | Quote reply UI |
| Delete Messages | ✅ | ✅ | ✅ | Delete for me / everyone |
| Message Forwarding | 💭 | 💭 | 💭 | Backlog |
| Pin Messages | 💭 | 💭 | 💭 | Backlog |
| Message Search | 💭 | 💭 | 💭 | Backlog |

---

## Phase 4: Group Management ✅ COMPLETE

| Feature | iOS | Android | Backend | Notes |
|---------|-----|---------|---------|-------|
| Add Participants | ✅ | ✅ | ✅ | Search & add users |
| Remove Participants | ✅ | ✅ | ✅ | Admin only |
| Group Info Screen | ✅ | ✅ | ✅ | View all members |
| Edit Group Name | ✅ | ✅ | ✅ | Admin only |
| Edit Group Picture | ✅ | ✅ | ✅ | S3 upload |
| Leave Group | ✅ | ✅ | ✅ | Self-remove |

---

## Phase 5: Internationalization ✅ COMPLETE

| Feature | iOS | Android | Backend | Priority |
|---------|-----|---------|---------|----------|
| UI Localization (120+ langs) | ✅ | ✅ | ✅ | **Critical** |
| Auto-detect device language | ✅ | ✅ | N/A | **Critical** |
| In-app language selector | ✅ | ✅ | N/A | **Critical** |
| RTL Support (Arabic, Hebrew) | ✅ | ✅ | N/A | High |
| AI Translation for UI strings | N/A | N/A | ✅ | High |

**See:** `team/i18n-implementation-plan.md` for full details

---

## Phase 6: Advanced Features

| Feature | iOS | Android | Backend | Priority |
|---------|-----|---------|---------|----------|
| Offline Mode | 💭 | 💭 | 💭 | High |
| Message Sync | 💭 | 💭 | 💭 | High |
| End-to-End Encryption | 💭 | 💭 | 💭 | Medium |
| Video Calling | 💭 | 💭 | 💭 | Low |
| Screen Sharing | 💭 | 💭 | 💭 | Low |
| Message Scheduling | 💭 | 💭 | 💭 | Low |
| Custom Themes | 💭 | 💭 | N/A | Low |

---

## Technical Debt & Improvements

| Item | Area | Priority | Status |
|------|------|----------|--------|
| Add unit tests | iOS | High | 📋 |
| Add unit tests | Android | High | 📋 |
| API error handling | Backend | Medium | ✅ |
| Accessibility audit | iOS/Android | Medium | 📋 |
| Performance optimization | iOS/Android | Medium | 📋 |
| Memory leak audit | iOS/Android | High | 📋 |
| WebSocket reconnection | iOS/Android | High | ✅ |

---

## Completed Versions

### v0.1.28 (December 2024) - Current
- Cross-platform GIF display fix
- Push notification deep linking
- iOS GIPHY API key configuration
- Group Info screen improvements

### v0.1.27 (December 2024)
- Group Info screen (iOS + Android)
- View/Edit group name and picture
- Participant list management

### v0.1.26 (December 2024)
- Translated push notifications
- Backend notification improvements

### v0.1.25 (December 2024)
- Push notifications (APNs + FCM)
- Device token registration fixes
- FCM V1 API migration

### v0.1.24 (December 2024)
- Add/Remove participants from groups
- Group management APIs

### v0.1.20-0.1.23 (December 2024)
- Message replies
- Delete messages
- Emoji reactions
- Document translation
- Image attachments

### v0.1.0-0.1.19 (November-December 2024)
- Core messaging functionality
- Authentication (Email + OAuth)
- Real-time WebSocket messaging
- Auto-translation
- Conversation management
- Settings & preferences

---

## Feature Requests

| Request | Source | Status | Notes |
|---------|--------|--------|-------|
| Dark mode | User feedback | ✅ | Default theme |
| Message reactions | User feedback | ✅ | Implemented |
| Voice messages | User feedback | 💭 | Phase 5 |
| Custom themes | User feedback | 💭 | Backlog |
| Message search | User feedback | 💭 | Phase 5 |
| Video calls | User feedback | 💭 | Phase 5 |

---

## Coordination Notes

### Frontend-Backend Sync

When a frontend feature requires backend changes:
1. Frontend dev documents requirements in this file
2. Backend dev reviews and confirms API changes
3. API contract updated in `api-contracts.md`
4. Both teams align on timeline

### Feature Parity

iOS and Android maintain feature parity. All features are implemented on both platforms simultaneously.

### CI/CD Pipeline

- `dev` → Debug builds (simulators)
- `stage` → Internal testing (TestFlight + Play Internal)
- `main` → Production (App Store + Play Store)

---

*Last Updated: December 20, 2024*
