# Developer Prompts - Notifications & Group Chat Management

## 🔔 Issue: Push Notifications Not Working

### Current State Analysis

**Backend (`server-serverless/src/handlers/websocket.ts`):**
- ✅ Push notifications are triggered correctly when users are offline (lines 413-442)
- ✅ Checks for active WebSocket connections before sending notifications
- ✅ Uses `sendPushNotification` from `notifications.ts` library

**iOS (`ios-native/Intok/App/IntokApp.swift`):**
- ✅ APNs token registration happens in `AppDelegate.didRegisterForRemoteNotifications`
- ✅ Token is sent to backend via `registerDeviceToken` API call
- ⚠️ **ISSUE**: Token registration only happens on app launch, not after login
- ⚠️ **ISSUE**: Need to verify token is registered after successful authentication

**Android (`android-native/app/src/main/java/com/intokapp/app/`):**
- ✅ FCM token registration happens in `MainActivity.getFCMToken()` (line 85-94)
- ⚠️ **ISSUE**: `MainActivity.getFCMToken()` has TODO comment: "Send token to backend via API"
- ✅ `AuthRepository.registerFCMToken()` exists (line 292-310) and is called after login
- ⚠️ **ISSUE**: `MainActivity` doesn't actually call the API to register the token

---

## 📋 Prompts for Developers

---

## ☁️ Backend Developer Prompt

**Task**: Fix push notification delivery issues

**Current Implementation:**
- Push notifications are sent in `server-serverless/src/handlers/websocket.ts` (lines 413-442)
- Uses `sendPushNotification` from `server-serverless/src/lib/notifications.ts`
- Checks if user has active WebSocket connections before sending

**Issues to Investigate:**

1. **Device Token Registration:**
   - Verify device tokens are being stored correctly in `DeviceTokensTable`
   - Check `server-serverless/src/handlers/devices.ts` endpoint is working
   - Ensure tokens are associated with correct `userId` and `platform` ('ios' or 'android')

2. **Notification Credentials:**
   - Verify APNs credentials are stored in AWS Secrets Manager at `intok/push/apns`
   - Verify FCM credentials are stored in AWS Secrets Manager at `intok/push/fcm`
   - Check if credentials are being fetched correctly in `notifications.ts`

3. **Notification Payload:**
   - Verify notification payload structure matches APNs/FCM requirements
   - Check if `conversationId` and `messageId` are included in notification data
   - Ensure notification title/body are properly formatted

4. **Error Handling:**
   - Add logging for failed notification sends
   - Check CloudWatch logs for notification errors
   - Verify invalid tokens are being cleaned up

**Action Items:**
1. Add comprehensive logging to `sendPushNotification` function
2. Verify Secrets Manager has correct credentials
3. Test notification sending with valid device tokens
4. Check CloudWatch logs for any errors during notification sends
5. Verify `DeviceTokensTable` has entries for test users

**Files to Review:**
- `server-serverless/src/lib/notifications.ts`
- `server-serverless/src/handlers/devices.ts`
- `server-serverless/src/handlers/websocket.ts` (lines 413-442)
- AWS Secrets Manager: `intok/push/apns` and `intok/push/fcm`
- DynamoDB Table: `DeviceTokensTable`

---

## 🤖 Android Developer Prompt

**Task**: Fix push notifications and add group chat management UI

### Part 1: Fix Push Notifications

**Current Issues:**
1. `MainActivity.getFCMToken()` (line 85-94) gets the token but doesn't send it to backend (has TODO comment)
2. Token registration only happens in `AuthRepository.registerFCMToken()` after login
3. Need to ensure token is registered on app launch AND after login

**Action Items:**

1. **Fix `MainActivity.getFCMToken()`:**
   - Remove TODO comment
   - Call `IntokFirebaseMessagingService.registerCurrentToken()` after getting token
   - Ensure this happens after user is authenticated (check `AuthRepository` state)

2. **Verify Token Registration Flow:**
   - Ensure `IntokFirebaseMessagingService.registerCurrentToken()` is called:
     - On app launch (if user is already logged in)
     - After successful login/registration
     - When FCM token refreshes (`onNewToken`)

3. **Check Notification Handling:**
   - Verify `IntokFirebaseMessagingService.onMessageReceived()` is working
   - Ensure notification channel is created (`createNotificationChannel()`)
   - Test notification tap navigation to conversation

**Files to Modify:**
- `android-native/app/src/main/java/com/intokapp/app/MainActivity.kt`
- `android-native/app/src/main/java/com/intokapp/app/data/repository/AuthRepository.kt`
- `android-native/app/src/main/java/com/intokapp/app/data/network/IntokFirebaseMessagingService.kt`

### Part 2: Add Group Chat Management UI

**Current State:**
- Chat screen has three-dot menu button (`MoreVert` icon) at line 242-244 in `ChatScreen.kt`
- Menu button currently has empty `onClick` handler

**Requirements:**
1. Add three-dot menu that shows options:
   - "Add People" (only for group chats)
   - "Remove People" (only for group chats)
   - "Group Info" (only for group chats)
   - "View Profile" (for direct chats)

2. Create "Add People" screen:
   - Show list of all users (excluding current participants)
   - Allow multi-select
   - Call backend API: `POST /api/conversations/:id/participants` with `{ userIds: [...] }`
   - Update conversation locally after successful add

3. Create "Remove People" screen:
   - Show list of current participants (excluding current user)
   - Allow single/multi-select
   - Call backend API: `DELETE /api/conversations/:id/participants/:userId`
   - Update conversation locally after successful remove

4. Handle WebSocket events:
   - Listen for `conversation:participants:added` event
   - Listen for `conversation:participants:removed` event
   - Update conversation participants list in real-time

**Files to Create/Modify:**
- `android-native/app/src/main/java/com/intokapp/app/ui/screens/chat/ChatScreen.kt` (add menu)
- `android-native/app/src/main/java/com/intokapp/app/ui/screens/chat/AddParticipantsScreen.kt` (new)
- `android-native/app/src/main/java/com/intokapp/app/ui/screens/chat/RemoveParticipantsScreen.kt` (new)
- `android-native/app/src/main/java/com/intokapp/app/data/repository/ChatRepository.kt` (add API methods)
- `android-native/app/src/main/java/com/intokapp/app/data/network/ApiService.kt` (add endpoints)
- `android-native/app/src/main/java/com/intokapp/app/data/network/WebSocketService.kt` (handle events)

**API Endpoints to Add:**
```kotlin
// In ApiService.kt
suspend fun addParticipants(conversationId: String, userIds: List<String>): ConversationResponse
suspend fun removeParticipant(conversationId: String, userIdId: String): ConversationResponse
```

---

## 🍎 iOS Developer Prompt

**Task**: Fix push notifications and add group chat management UI

### Part 1: Fix Push Notifications

**Current Issues:**
1. Token registration happens in `AppDelegate.didRegisterForRemoteNotifications` (line 45-52)
2. Token is sent to backend, but need to verify it happens after login
3. Token might not be registered if user logs in after app launch

**Action Items:**

1. **Ensure Token Registration After Login:**
   - In `AuthManager` after successful login/registration, check if APNs token exists
   - If token exists, call `APIService.shared.registerDeviceToken()` again
   - This ensures token is registered even if user logs in after app launch

2. **Verify Token Refresh:**
   - Ensure `didRegisterForRemoteNotifications` is called when token refreshes
   - Token should be re-registered with backend on refresh

3. **Check Notification Handling:**
   - Verify `userNotificationCenter(_:didReceive:)` handles notification taps correctly
   - Ensure navigation to conversation works when app is opened from notification
   - Test notification display when app is in foreground

**Files to Modify:**
- `ios-native/Intok/Core/Network/AuthManager.swift` (add token registration after login)
- `ios-native/Intok/App/IntokApp.swift` (verify notification handling)

### Part 2: Add Group Chat Management UI

**Current State:**
- Chat view has three-dot menu (`ellipsis.circle`) at line 123-134 in `ChatView.swift`
- Menu currently has placeholder items: "View Profile" and "Search in Chat"

**Requirements:**
1. Update three-dot menu to show:
   - "Add People" (only for group chats, `conversation.type == "group"`)
   - "Remove People" (only for group chats)
   - "Group Info" (only for group chats)
   - "View Profile" (for direct chats)

2. Create "Add People" view:
   - Show list of all users (filter out current participants)
   - Allow multi-select with checkmarks
   - Call backend API: `POST /api/conversations/:id/participants` with `{ userIds: [...] }`
   - Update `ChatStore` conversation after successful add

3. Create "Remove People" view:
   - Show list of current participants (exclude current user)
   - Allow selection
   - Call backend API: `DELETE /api/conversations/:id/participants/:userId`
   - Update `ChatStore` conversation after successful remove

4. Handle WebSocket events:
   - Listen for `conversation:participants:added` event in `ChatStore`
   - Listen for `conversation:participants:removed` event
   - Update conversation participants list in real-time

**Files to Create/Modify:**
- `ios-native/Intok/Features/Chat/ChatView.swift` (update menu)
- `ios-native/Intok/Features/Chat/AddParticipantsView.swift` (new)
- `ios-native/Intok/Features/Chat/RemoveParticipantsView.swift` (new)
- `ios-native/Intok/Core/Store/ChatStore.swift` (add API methods and WebSocket handlers)
- `ios-native/Intok/Core/Network/APIService.swift` (add endpoints)

**API Endpoints to Add:**
```swift
// In APIService.swift
func addParticipants(conversationId: String, userIds: [String]) async throws -> ConversationResponse
func removeParticipant(conversationId: String, userId: String) async throws -> ConversationResponse
```

---

## 🔧 Backend Developer Prompt (Group Chat Management)

**Task**: Add API endpoints for managing group chat participants

**Current State:**
- Conversations are stored in DynamoDB with `participantIds` array
- Each participant has their own conversation record (for querying via GSI)
- No endpoints exist for adding/removing participants

**Requirements:**

1. **Add Participant Endpoint:**
   - `POST /api/conversations/:conversationId/participants`
   - Request body: `{ userIds: string[] }`
   - Only allow if conversation type is "group"
   - Only allow if requester is already a participant
   - Add new participants to all existing participant records
   - Create new conversation records for new participants
   - Broadcast `conversation:participants:added` WebSocket event to all participants
   - Return updated conversation with new participants

2. **Remove Participant Endpoint:**
   - `DELETE /api/conversations/:conversationId/participants/:userId`
   - Only allow if conversation type is "group"
   - Only allow if requester is a participant (and can remove others) OR requester is removing themselves
   - Remove participant from all conversation records
   - Delete conversation record for removed participant
   - Broadcast `conversation:participants:removed` WebSocket event to remaining participants
   - Return success response

3. **WebSocket Events:**
   - `conversation:participants:added`: `{ conversationId, addedUserIds, participants }`
   - `conversation:participants:removed`: `{ conversationId, removedUserId, participants }`

**Files to Create/Modify:**
- `server-serverless/src/handlers/conversations.ts` (add new endpoints)
- `server-serverless/src/handlers/websocket.ts` (add broadcast functions)
- `server-serverless/template.yaml` (add new Lambda routes)
- `team/api-contracts.md` (document new endpoints)

**Implementation Notes:**
- Use DynamoDB `UpdateCommand` to add/remove from `participantIds` array
- Create/delete conversation records for new/removed participants
- Use `QueryCommand` with GSI to find all conversation records for a conversationId
- Broadcast WebSocket events to all active connections for remaining participants

---

## ✅ Testing Checklist

### Notifications:
- [ ] iOS: Token registered after app launch
- [ ] iOS: Token registered after login
- [ ] iOS: Notification received when app is closed
- [ ] iOS: Notification received when app is in background
- [ ] iOS: Tapping notification opens correct conversation
- [ ] Android: Token registered after app launch
- [ ] Android: Token registered after login
- [ ] Android: Notification received when app is closed
- [ ] Android: Notification received when app is in background
- [ ] Android: Tapping notification opens correct conversation
- [ ] Backend: Notifications sent only to offline users
- [ ] Backend: Notification payload includes conversationId and messageId

### Group Chat Management:
- [ ] Backend: Add participants endpoint works
- [ ] Backend: Remove participants endpoint works
- [ ] Backend: WebSocket events broadcast correctly
- [ ] iOS: Add people UI works
- [ ] iOS: Remove people UI works
- [ ] iOS: Real-time updates when participants added/removed
- [ ] Android: Add people UI works
- [ ] Android: Remove people UI works
- [ ] Android: Real-time updates when participants added/removed
