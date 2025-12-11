# 📡 API Contracts

This document defines all API endpoints and data schemas used by Intok.

## Base URLs

| Environment | HTTP API | WebSocket API |
|-------------|----------|---------------|
| Development | `http://localhost:3001` | `ws://localhost:3001` |
| Staging | `https://stage-api.intokapp.com` | `wss://stage-ws.intokapp.com` |
| Production | `https://api.intokapp.com` | `wss://ws.intokapp.com` |

---

## Authentication

All authenticated endpoints require the following header:
```
Authorization: Bearer <accessToken>
```

### POST /api/auth/register

Create a new user account.

**Request:**
```json
{
  "email": "user@example.com",
  "password": "securePassword123",
  "username": "johndoe",
  "preferredLanguage": "en",
  "preferredCountry": "US"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| email | string | Yes | Valid email address |
| password | string | Yes | Minimum 6 characters |
| username | string | Yes | 3-30 characters |
| preferredLanguage | string | No | Language code (default: "en") |
| preferredCountry | string | No | 2-letter country code (default: "US") |

**Response (201 Created):**
```json
{
  "user": {
    "id": "user_abc123",
    "email": "user@example.com",
    "username": "johndoe",
    "preferredLanguage": "en",
    "preferredCountry": "US",
    "createdAt": "2024-12-08T00:00:00.000Z"
  },
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

**Errors:**
- `400` - Validation error (invalid email, weak password, username too short/long)
- `400` - `EMAIL_EXISTS` - Email already registered

---

### POST /api/auth/login

Authenticate user with email and password.

**Request:**
```json
{
  "email": "user@example.com",
  "password": "securePassword123"
}
```

**Response (200 OK):**
```json
{
  "user": {
    "id": "user_abc123",
    "email": "user@example.com",
    "username": "johndoe",
    "preferredLanguage": "en",
    "preferredCountry": "US",
    "createdAt": "2024-12-08T00:00:00.000Z"
  },
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

**Errors:**
- `400` - Validation error
- `401` - `INVALID_CREDENTIALS` - Invalid email or password

---

### POST /api/auth/oauth

Authenticate with OAuth provider (Google or Apple).

**Request:**
```json
{
  "provider": "google",
  "providerId": "oauth_provider_user_id",
  "email": "user@example.com",
  "name": "John Doe",
  "avatarUrl": "https://example.com/avatar.jpg"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| provider | string | Yes | Either "google" or "apple" |
| providerId | string | Yes | Unique user ID from the OAuth provider |
| email | string | Yes | Email from OAuth provider |
| name | string | No | Display name (nullable) |
| avatarUrl | string | No | Profile photo URL (nullable) |

**Response (200 OK):**
```json
{
  "user": {
    "id": "user_abc123",
    "email": "user@example.com",
    "username": "johndoe123",
    "preferredLanguage": "en",
    "preferredCountry": "US",
    "avatarUrl": "https://example.com/avatar.jpg",
    "createdAt": "2024-12-08T00:00:00.000Z"
  },
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

**Notes:**
- If user doesn't exist, a new account is created automatically
- Username is auto-generated from name or email prefix
- If user exists with email but no OAuth, their account is linked

**Errors:**
- `400` - Validation error

---

### POST /api/auth/check-email

Check if an email is already registered.

**Request:**
```json
{
  "email": "user@example.com"
}
```

**Response (200 OK):**
```json
{
  "exists": true,
  "email": "user@example.com"
}
```

**Use Case:** 
- Check email availability before registration
- Determine if user should login vs register

**Errors:**
- `400` - Invalid email format

---

### GET /api/auth/me

Get current authenticated user's profile.

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Response (200 OK):**
```json
{
  "user": {
    "id": "user_abc123",
    "email": "user@example.com",
    "username": "johndoe",
    "preferredLanguage": "en",
    "preferredCountry": "US",
    "createdAt": "2024-12-08T00:00:00.000Z"
  }
}
```

**Errors:**
- `401` - Authentication required
- `404` - User not found

---

## Users

### GET /api/users/search

Search for users by username or email.

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| q | string | Yes | Search query (minimum 2 characters) |

**Example:** `GET /api/users/search?q=john`

**Response (200 OK):**
```json
{
  "users": [
    {
      "id": "user_xyz789",
      "username": "johnsmith",
      "email": "john.smith@example.com",
      "avatarUrl": "https://...",
      "preferredLanguage": "es"
    }
  ]
}
```

**Notes:**
- Searches both username and email fields
- Results limited to 20 users
- Current user is excluded from results

**Errors:**
- `401` - Unauthorized

---

### PATCH /api/users/me

Update current user's profile.

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Request:**
```json
{
  "username": "newusername",
  "avatarUrl": "https://example.com/new-avatar.jpg"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| username | string | No | 3-30 characters |
| avatarUrl | string | No | Valid URL |

**Response (200 OK):**
```json
{
  "user": {
    "id": "user_abc123",
    "email": "user@example.com",
    "username": "newusername",
    "preferredLanguage": "en",
    "preferredCountry": "US",
    "avatarUrl": "https://example.com/new-avatar.jpg",
    "createdAt": "2024-12-08T00:00:00.000Z"
  }
}
```

**Errors:**
- `400` - Validation error
- `401` - Unauthorized

---

### PATCH /api/users/me/language

Update user's preferred language for message translations.

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Request:**
```json
{
  "preferredLanguage": "es"
}
```

**Response (200 OK):**
```json
{
  "user": {
    "id": "user_abc123",
    "email": "user@example.com",
    "username": "johndoe",
    "preferredLanguage": "es",
    "preferredCountry": "US",
    "createdAt": "2024-12-08T00:00:00.000Z"
  }
}
```

**Valid Languages:** `en`, `es`, `fr`, `de`, `it`, `zh`, `ja`, `ko`, `ar`, `hi`, `pt`, `ru`, `nl`, `pl`, `tr`, `vi`

**Errors:**
- `400` - Validation error
- `401` - Unauthorized

---

### PATCH /api/users/me/country

Update user's preferred country (used for regional translation context).

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Request:**
```json
{
  "preferredCountry": "MX"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| preferredCountry | string | Yes | 2-letter ISO country code |

**Response (200 OK):**
```json
{
  "user": {
    "id": "user_abc123",
    "email": "user@example.com",
    "username": "johndoe",
    "preferredLanguage": "es",
    "preferredCountry": "MX",
    "createdAt": "2024-12-08T00:00:00.000Z"
  }
}
```

**Use Case:**
- Helps translation service use regional variations (e.g., Spanish for Mexico vs Spain)

**Errors:**
- `400` - Validation error (must be exactly 2 characters)
- `401` - Unauthorized

---

### POST /api/users/profile-picture/upload-url

Get a presigned URL to upload a profile picture.

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Request:**
```json
{
  "contentType": "image/jpeg",
  "fileSize": 512000
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| contentType | string | Yes | MIME type (image/jpeg, image/png, image/webp) |
| fileSize | number | Yes | File size in bytes (max 5MB) |

**Response (200 OK):**
```json
{
  "uploadUrl": "https://lingualink-attachments-xxx.s3.us-east-1.amazonaws.com/profiles/user_abc123/uuid.jpg?...",
  "key": "profiles/user_abc123/uuid.jpg",
  "expiresIn": 300
}
```

**Upload Flow:**
1. Call this endpoint to get presigned URL
2. Upload image directly to S3 using HTTP PUT with the presigned URL
3. Call `PUT /api/users/profile-picture` with the returned `key`

**Errors:**
- `400` - Missing required fields
- `400` - Invalid file type (allowed: JPEG, PNG, WebP)
- `400` - File too large (max 5MB)
- `401` - Unauthorized

---

### PUT /api/users/profile-picture

Update user's profile picture after successful upload to S3.

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Request:**
```json
{
  "key": "profiles/user_abc123/uuid.jpg"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| key | string | Yes | S3 key from upload-url response |

**Response (200 OK):**
```json
{
  "user": {
    "id": "user_abc123",
    "email": "user@example.com",
    "username": "johndoe",
    "preferredLanguage": "en",
    "preferredCountry": "US",
    "profilePicture": "https://lingualink-attachments-xxx.s3.us-east-1.amazonaws.com/profiles/user_abc123/uuid.jpg",
    "createdAt": "2024-12-08T00:00:00.000Z"
  },
  "profilePicture": "https://lingualink-attachments-xxx.s3.us-east-1.amazonaws.com/profiles/user_abc123/uuid.jpg"
}
```

**Notes:**
- Old profile picture is automatically deleted from S3
- Profile picture URL is permanent (no expiration)

**Errors:**
- `400` - Missing key
- `401` - Unauthorized
- `403` - Invalid profile picture key (must belong to user)

---

### DELETE /api/users/profile-picture

Delete user's profile picture.

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Response (200 OK):**
```json
{
  "user": {
    "id": "user_abc123",
    "email": "user@example.com",
    "username": "johndoe",
    "preferredLanguage": "en",
    "preferredCountry": "US",
    "profilePicture": null,
    "createdAt": "2024-12-08T00:00:00.000Z"
  }
}
```

**Errors:**
- `401` - Unauthorized
- `404` - No profile picture to delete

---

## Devices (Push Notifications)

### POST /api/devices/register

Register a device token for push notifications.

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Request:**
```json
{
  "token": "device_push_token_here",
  "platform": "ios"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| token | string | Yes | Device push notification token |
| platform | string | Yes | Either "ios" or "android" |

**Response (200 OK):**
```json
{
  "success": true
}
```

**Notes:**
- If the token was previously registered to another user, it will be reassigned
- Users can have multiple devices registered

**Errors:**
- `400` - token and platform required
- `401` - Unauthorized

---

### POST /api/devices/unregister

Unregister a device token (on logout or token refresh).

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Request (specific token):**
```json
{
  "token": "device_push_token_here"
}
```

**Request (all tokens - logout):**
```json
{}
```

**Response (200 OK):**
```json
{
  "success": true
}
```

**Notes:**
- If no token is provided, all tokens for the user are removed (useful for logout)
- If a specific token is provided, only that token is removed

**Errors:**
- `401` - Unauthorized

---

## Conversations

### GET /api/conversations

List user's conversations.

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Response (200 OK):**
```json
{
  "conversations": [
    {
      "id": "conv_abc123",
      "type": "direct",
      "name": null,
      "participants": [
        {
          "id": "user_abc123",
          "username": "johndoe",
          "preferredLanguage": "en"
        },
        {
          "id": "user_xyz789",
          "username": "janesmith",
          "preferredLanguage": "es"
        }
      ],
      "lastMessage": {
        "id": "msg_123",
        "conversationId": "conv_abc123",
        "senderId": "user_xyz789",
        "sender": {
          "id": "user_xyz789",
          "username": "janesmith",
          "preferredLanguage": "es"
        },
        "type": "text",
        "originalContent": "Hello!",
        "originalLanguage": "en",
        "status": "sent",
        "createdAt": "2024-12-08T12:00:00.000Z"
      },
      "createdAt": "2024-12-08T00:00:00.000Z",
      "updatedAt": "2024-12-08T12:00:00.000Z"
    }
  ]
}
```

**Notes:**
- Conversations are sorted by `updatedAt` (most recent first)
- `lastMessage` is null for new conversations without messages

**Errors:**
- `401` - Authentication required

---

### POST /api/conversations

Create a new conversation.

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Request (Direct Message):**
```json
{
  "type": "direct",
  "participantIds": ["user_xyz789"]
}
```

**Request (Group Chat):**
```json
{
  "type": "group",
  "name": "Project Team",
  "participantIds": ["user_xyz789", "user_def456"]
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| type | string | Yes | "direct" or "group" |
| participantIds | string[] | Yes | Array of user IDs to add (min 1) |
| name | string | No | Group name (recommended for groups) |

**Response (201 Created):**
```json
{
  "conversation": {
    "id": "conv_abc123",
    "type": "direct",
    "name": null,
    "participants": [
      {
        "id": "user_abc123",
        "username": "johndoe",
        "preferredLanguage": "en"
      },
      {
        "id": "user_xyz789",
        "username": "janesmith",
        "preferredLanguage": "es"
      }
    ],
    "createdAt": "2024-12-08T00:00:00.000Z",
    "updatedAt": "2024-12-08T00:00:00.000Z"
  }
}
```

**Notes:**
- For `direct` type with same participants, returns existing conversation (200 OK)
- Current user is automatically added to participants

**Errors:**
- `400` - Validation error
- `401` - Authentication required

---

### GET /api/conversations/{conversationId}/messages

Get messages for a conversation.

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| conversationId | string | The conversation ID |

**Query Parameters:**
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| limit | number | No | 50 | Max messages to return |
| cursor | string | No | - | Pagination cursor (message timestamp) |

**Example:** `GET /api/conversations/conv_abc123/messages?limit=50`

**Response (200 OK):**
```json
{
  "messages": [
    {
      "id": "msg_abc123",
      "conversationId": "conv_abc123",
      "senderId": "user_xyz789",
      "sender": {
        "id": "user_xyz789",
        "username": "janesmith",
        "preferredLanguage": "es"
      },
      "type": "text",
      "originalContent": "Hello, how are you?",
      "originalLanguage": "en",
      "translatedContent": "¡Hola! ¿Cómo estás?",
      "targetLanguage": "es",
      "status": "sent",
      "reactions": {
        "👍": ["user_abc123"],
        "❤️": ["user_abc123", "user_xyz789"]
      },
      "createdAt": "2024-12-08T12:00:00.000Z"
    }
  ],
  "hasMore": true,
  "nextCursor": "2024-12-08T11:00:00.000Z"
}
```

**Notes:**
- Messages are returned in chronological order (oldest first)
- `translatedContent` is in the requesting user's preferred language
- If no cached translation exists, `translatedContent` equals `originalContent`
- Use `nextCursor` for pagination

**Errors:**
- `400` - Conversation ID required
- `401` - Authentication required
- `404` - Conversation not found

---

### POST /api/conversations/{conversationId}/read

Mark a conversation as read for the current user.

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| conversationId | string | The conversation ID to mark as read |

**Response (200 OK):**
```json
{
  "success": true,
  "conversationId": "conv_abc123",
  "lastReadAt": "2024-12-08T12:00:00.000Z"
}
```

**Notes:**
- Resets unread count to 0 for this conversation
- Updates `lastReadAt` timestamp for the user

**Errors:**
- `400` - Conversation ID required
- `401` - Authentication required
- `404` - Conversation not found

---

### DELETE /api/conversations/{conversationId}

Delete a conversation for the current user (soft delete).

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| conversationId | string | The conversation ID to delete |

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Conversation deleted",
  "conversationId": "conv_abc123"
}
```

**Notes:**
- Soft delete: removes conversation from user's view only
- Other participants can still see the conversation
- Messages are NOT deleted

**Errors:**
- `400` - Conversation ID required
- `401` - Authentication required
- `404` - Conversation not found

---

### DELETE /api/conversations/{conversationId}/messages/{messageId}

Delete a message from a conversation.

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| conversationId | string | The conversation ID |
| messageId | string | The message ID to delete |

**Request Body (optional):**
```json
{
  "forEveryone": true
}
```

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| forEveryone | boolean | No | false | If true, deletes for all users; if false, deletes for current user only |

**Response (200 OK) - Delete for everyone:**
```json
{
  "success": true,
  "messageId": "msg_abc123",
  "conversationId": "conv_abc123",
  "deletedAt": "2024-12-08T12:00:00.000Z",
  "deletedForEveryone": true,
  "participantIds": ["user_abc123", "user_xyz789"]
}
```

**Response (200 OK) - Delete for me:**
```json
{
  "success": true,
  "messageId": "msg_abc123",
  "conversationId": "conv_abc123",
  "deletedForMe": true
}
```

**Notes:**
- `forEveryone: true` - Only the sender can delete for everyone
- `forEveryone: false` - User removes message from their own view only
- When deleted for everyone, message content is hidden but record remains
- Attachments (images, docs) are deleted from S3 when deleted for everyone
- A `message:deleted` WebSocket event is broadcast to all participants

**Errors:**
- `400` - Conversation ID and Message ID required
- `401` - Authentication required
- `403` - Only the sender can delete a message for everyone
- `404` - Conversation not found
- `404` - Message not found

---

## Attachments

### POST /api/attachments/upload-url

Get a presigned URL to upload an attachment to S3.

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Request:**
```json
{
  "fileName": "photo.jpg",
  "contentType": "image/jpeg",
  "fileSize": 1024000,
  "conversationId": "conv_abc123"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| fileName | string | Yes | Original file name with extension |
| contentType | string | Yes | MIME type of the file |
| fileSize | number | Yes | File size in bytes |
| conversationId | string | Yes | Target conversation ID |

**Allowed File Types:**

| Category | MIME Types |
|----------|------------|
| image | `image/jpeg`, `image/png`, `image/gif`, `image/webp` |
| video | `video/mp4`, `video/quicktime`, `video/webm` |
| audio | `audio/mpeg`, `audio/wav`, `audio/ogg`, `audio/webm` |
| document | `application/pdf`, `application/msword`, `application/vnd.openxmlformats-officedocument.wordprocessingml.document`, `application/vnd.ms-excel`, `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`, `text/plain` |

**Max File Size:** 25 MB

**Response (200 OK):**
```json
{
  "attachmentId": "att_abc123",
  "uploadUrl": "https://s3.amazonaws.com/bucket/...",
  "key": "conv_abc123/att_abc123.jpg",
  "category": "image",
  "expiresIn": 300
}
```

| Field | Description |
|-------|-------------|
| attachmentId | Unique ID for the attachment |
| uploadUrl | Presigned S3 PUT URL |
| key | S3 object key (use for download URL) |
| category | File category (image, video, audio, document) |
| expiresIn | URL expiry in seconds (5 minutes) |

**Upload Instructions:**
```javascript
// Use HTTP PUT to upload the file
await fetch(uploadUrl, {
  method: 'PUT',
  body: fileData,
  headers: {
    'Content-Type': contentType
  }
});
```

**Errors:**
- `400` - Missing required fields
- `400` - File type not allowed
- `400` - File too large
- `401` - Authentication required

---

### GET /api/attachments/download-url

Get a presigned URL to download an attachment.

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| key | string | Yes | S3 object key from upload response |

**Example:** `GET /api/attachments/download-url?key=conv_abc123/att_abc123.jpg`

**Response (200 OK):**
```json
{
  "downloadUrl": "https://s3.amazonaws.com/bucket/...",
  "expiresIn": 3600
}
```

| Field | Description |
|-------|-------------|
| downloadUrl | Presigned S3 GET URL |
| expiresIn | URL expiry in seconds (1 hour) |

**Errors:**
- `400` - Missing key parameter
- `400` - Invalid key format
- `401` - Authentication required

---

## WebSocket Events

### Connection

Connect to WebSocket with authentication token:

```javascript
const ws = new WebSocket('wss://ws.intokapp.com?token=<accessToken>');
```

**Connection Events:**
- `$connect` - Validates token and stores connection
- `$disconnect` - Removes connection from database

---

### Client → Server Events

All messages sent to the server follow this format:
```json
{
  "action": "event_name",
  "data": { ... }
}
```

#### message:send

Send a new message to a conversation.

**Text Message:**
```json
{
  "action": "message:send",
  "data": {
    "conversationId": "conv_abc123",
    "content": "Hello!",
    "type": "text"
  }
}
```

**Image/GIF Message (never translated):**
```json
{
  "action": "message:send",
  "data": {
    "conversationId": "conv_abc123",
    "content": "Check out this photo!",
    "type": "image",
    "attachment": {
      "id": "att_abc123",
      "key": "conv_abc123/att_abc123.jpg",
      "fileName": "photo.jpg",
      "contentType": "image/jpeg",
      "fileSize": 1024000,
      "category": "image"
    }
  }
}
```

**Document with Translation:**
```json
{
  "action": "message:send",
  "data": {
    "conversationId": "conv_abc123",
    "content": "Meeting notes from today's standup...",
    "type": "file",
    "translateDocument": true,
    "attachment": {
      "id": "att_abc123",
      "key": "conv_abc123/att_abc123.pdf",
      "fileName": "meeting-notes.pdf",
      "contentType": "application/pdf",
      "fileSize": 102400,
      "category": "document"
    }
  }
}
```

**Reply to Message:**
```json
{
  "action": "message:send",
  "data": {
    "conversationId": "conv_abc123",
    "content": "I agree with you!",
    "type": "text",
    "replyTo": {
      "messageId": "msg_original123",
      "content": "What do you think about the new feature?",
      "senderId": "user_xyz789",
      "senderName": "Jane Smith",
      "type": "text"
    }
  }
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| conversationId | string | Yes | Target conversation |
| content | string | No | Message text (can be empty for attachment-only) |
| type | string | No | "text" (default), "image", "gif", "file", "voice", "video" |
| attachment | object | No | Attachment metadata (from upload-url response) |
| translateDocument | boolean | No | If true, translate document content (only for type: "file") |
| replyTo | object | No | Reference to the message being replied to |

**ReplyTo Object:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| messageId | string | Yes | ID of the original message |
| content | string | Yes | Preview of original message (truncated to 100 chars) |
| senderId | string | Yes | Original message sender's ID |
| senderName | string | Yes | Original message sender's display name |
| type | string | Yes | Original message type (text, image, gif, file) |

**Translation Behavior by Type:**

| Type | Translation |
|------|-------------|
| `text` | Always translated if languages differ |
| `image` | Never translated |
| `gif` | Never translated |
| `file` | Translated only if `translateDocument: true` |
| `voice` | Never translated |
| `video` | Never translated |

**Attachment Flow:**
1. Call `POST /api/attachments/upload-url` to get presigned URL
2. Upload file to the presigned URL using HTTP PUT
3. Send message via WebSocket with attachment metadata

---

#### message:typing

Send typing indicator.

```json
{
  "action": "message:typing",
  "data": {
    "conversationId": "conv_abc123",
    "isTyping": true
  }
}
```

| Field | Type | Description |
|-------|------|-------------|
| conversationId | string | Target conversation |
| isTyping | boolean | true when typing, false when stopped |

---

#### message:reaction

Add or remove a reaction to a message.

```json
{
  "action": "message:reaction",
  "data": {
    "conversationId": "conv_abc123",
    "messageId": "msg_abc123",
    "messageTimestamp": "2024-12-08T12:00:00.000Z",
    "emoji": "👍"
  }
}
```

| Field | Type | Description |
|-------|------|-------------|
| conversationId | string | Conversation containing the message |
| messageId | string | Message ID to react to |
| messageTimestamp | string | Message timestamp (ISO 8601) |
| emoji | string | Emoji character |

**Notes:**
- Toggles reaction: if user already reacted with emoji, removes it; otherwise adds it
- Both `messageId` and `messageTimestamp` are required due to DynamoDB key structure

---

#### message:read

Mark a message as read (send read receipt).

```json
{
  "action": "message:read",
  "data": {
    "conversationId": "conv_abc123",
    "messageId": "msg_abc123",
    "messageTimestamp": "2024-12-08T12:00:00.000Z"
  }
}
```

| Field | Type | Description |
|-------|------|-------------|
| conversationId | string | Conversation containing the message |
| messageId | string | Message ID that was read |
| messageTimestamp | string | Message timestamp (ISO 8601) |

**Notes:**
- Adds user to message's `readBy` array
- Broadcasts read receipt to message sender

---

### Server → Client Events

#### message:receive

Received when a new message is sent to a conversation.

**Text Message:**
```json
{
  "action": "message:receive",
  "message": {
    "id": "msg_abc123",
    "conversationId": "conv_abc123",
    "senderId": "user_xyz789",
    "sender": {
      "id": "user_xyz789",
      "username": "janesmith",
      "preferredLanguage": "es"
    },
    "type": "text",
    "originalContent": "Hello, how are you?",
    "originalLanguage": "en",
    "translatedContent": "¡Hola! ¿Cómo estás?",
    "targetLanguage": "es",
    "status": "sent",
    "attachment": null,
    "replyTo": null,
    "timestamp": "2024-12-08T12:00:00.000Z",
    "createdAt": "2024-12-08T12:00:00.000Z"
  }
}
```

**Message with Attachment:**
```json
{
  "action": "message:receive",
  "message": {
    "id": "msg_abc123",
    "conversationId": "conv_abc123",
    "senderId": "user_xyz789",
    "sender": {
      "id": "user_xyz789",
      "username": "janesmith",
      "preferredLanguage": "es"
    },
    "type": "image",
    "originalContent": "Check out this photo!",
    "originalLanguage": "en",
    "translatedContent": "¡Mira esta foto!",
    "targetLanguage": "es",
    "status": "sent",
    "attachment": {
      "id": "att_abc123",
      "key": "conv_abc123/att_abc123.jpg",
      "fileName": "photo.jpg",
      "contentType": "image/jpeg",
      "fileSize": 1024000,
      "category": "image"
    },
    "replyTo": null,
    "timestamp": "2024-12-08T12:00:00.000Z",
    "createdAt": "2024-12-08T12:00:00.000Z"
  }
}
```

**Reply Message:**
```json
{
  "action": "message:receive",
  "message": {
    "id": "msg_reply456",
    "conversationId": "conv_abc123",
    "senderId": "user_abc123",
    "sender": {
      "id": "user_abc123",
      "username": "johnsmith",
      "preferredLanguage": "en"
    },
    "type": "text",
    "originalContent": "I agree with you!",
    "originalLanguage": "en",
    "translatedContent": "I agree with you!",
    "targetLanguage": "en",
    "status": "sent",
    "attachment": null,
    "replyTo": {
      "messageId": "msg_original123",
      "content": "What do you think about the new feature?",
      "senderId": "user_xyz789",
      "senderName": "Jane Smith",
      "type": "text"
    },
    "timestamp": "2024-12-08T12:05:00.000Z",
    "createdAt": "2024-12-08T12:05:00.000Z"
  }
}
```

**Notes:**
- `translatedContent` is automatically translated to receiver's preferred language
- `targetLanguage` matches receiver's `preferredLanguage`
- Translations are cached for future retrieval
- `attachment` is null for text-only messages
- `replyTo` is null for non-reply messages; contains original message reference for replies
- Use `GET /api/attachments/download-url?key={attachment.key}` to get download URL

---

#### message:typing

Received when another user is typing.

```json
{
  "action": "message:typing",
  "conversationId": "conv_abc123",
  "userId": "user_xyz789",
  "isTyping": true
}
```

---

#### message:reaction

Received when a reaction is added or removed.

```json
{
  "action": "message:reaction",
  "conversationId": "conv_abc123",
  "messageId": "msg_abc123",
  "messageTimestamp": "2024-12-08T12:00:00.000Z",
  "reactions": {
    "👍": ["user_abc123"],
    "❤️": ["user_abc123", "user_xyz789"]
  },
  "userId": "user_xyz789",
  "emoji": "👍"
}
```

| Field | Description |
|-------|-------------|
| reactions | Complete reactions map after update |
| userId | User who added/removed the reaction |
| emoji | The emoji that was added/removed |

---

#### message:read

Received when someone reads your message.

```json
{
  "action": "message:read",
  "conversationId": "conv_abc123",
  "messageId": "msg_abc123",
  "messageTimestamp": "2024-12-08T12:00:00.000Z",
  "readBy": "user_xyz789",
  "readAt": "2024-12-08T12:05:00.000Z"
}
```

| Field | Description |
|-------|-------------|
| conversationId | Conversation containing the message |
| messageId | Message that was read |
| messageTimestamp | Original message timestamp |
| readBy | User ID who read the message |
| readAt | When the message was read |

---

#### message:deleted

Received when a message is deleted for everyone.

```json
{
  "action": "message:deleted",
  "conversationId": "conv_abc123",
  "messageId": "msg_abc123",
  "deletedBy": "user_xyz789",
  "deletedAt": "2024-12-08T12:10:00.000Z"
}
```

| Field | Description |
|-------|-------------|
| conversationId | Conversation containing the message |
| messageId | Message that was deleted |
| deletedBy | User ID who deleted the message |
| deletedAt | When the message was deleted |

**Notes:**
- Only broadcast when `forEveryone: true`
- Client should update message to show "This message was deleted"
- Remove reactions and disable reply options

---

## Data Schemas

### User
```typescript
interface User {
  id: string;
  email: string;
  username: string;
  preferredLanguage: LanguageCode;
  preferredCountry: string; // 2-letter ISO code
  profilePicture: string | null; // S3 URL for profile picture
  createdAt: string; // ISO 8601
}
```

### Conversation
```typescript
interface Conversation {
  id: string;
  type: 'direct' | 'group';
  name: string | null; // null for direct, name for group
  participants: Participant[];
  lastMessage: Message | null;
  createdAt: string;
  updatedAt: string;
}

interface Participant {
  id: string;
  username: string;
  preferredLanguage: LanguageCode;
}
```

### Message
```typescript
interface Message {
  id: string;
  conversationId: string;
  senderId: string;
  sender: Participant;
  type: 'text' | 'image' | 'file' | 'gif' | 'voice' | 'video';
  originalContent: string;
  originalLanguage: LanguageCode;
  translatedContent: string | null;
  targetLanguage: LanguageCode | null;
  status: 'sent' | 'delivered' | 'read';
  reactions: Record<string, string[]>; // emoji -> userIds
  attachment: Attachment | null; // null for text-only messages
  replyTo: ReplyTo | null; // null for non-reply messages
  createdAt: string; // ISO 8601
}
```

### ReplyTo
```typescript
interface ReplyTo {
  messageId: string; // ID of the original message
  content: string; // Preview text (max 100 chars)
  senderId: string; // Original message sender's ID
  senderName: string; // Original message sender's display name
  type: 'text' | 'image' | 'file' | 'gif' | 'voice' | 'video'; // Original message type
}
```

### Attachment
```typescript
interface Attachment {
  id: string;
  key: string; // S3 object key
  fileName: string;
  contentType: string;
  fileSize: number;
  category: 'image' | 'video' | 'audio' | 'document';
  uploadedBy: string;
  createdAt: string;
}
```

### LanguageCode
```typescript
type LanguageCode = 
  | 'en' | 'es' | 'fr' | 'de' | 'it' 
  | 'zh' | 'ja' | 'ko' | 'ar' | 'hi'
  | 'pt' | 'ru' | 'nl' | 'pl' | 'tr' | 'vi';
```

---

## Error Response Format

All errors follow this format:

```json
{
  "message": "Human readable error message",
  "code": "ERROR_CODE",
  "details": {}
}
```

### Common Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `VALIDATION_ERROR` | 400 | Invalid request data |
| `EMAIL_EXISTS` | 400 | Email already registered |
| `INVALID_CREDENTIALS` | 401 | Wrong email or password |
| `UNAUTHORIZED` | 401 | Missing or invalid auth token |
| `FORBIDDEN` | 403 | No permission for resource |
| `NOT_FOUND` | 404 | Resource not found |
| `INTERNAL_ERROR` | 500 | Server error |

---

## Quick Reference

### Authentication Endpoints
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/auth/register` | No | Create account |
| POST | `/api/auth/login` | No | Login with email/password |
| POST | `/api/auth/oauth` | No | Login with Google/Apple |
| POST | `/api/auth/check-email` | No | Check if email exists |
| GET | `/api/auth/me` | Yes | Get current user |

### User Endpoints
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/users/search?q=` | Yes | Search users |
| PATCH | `/api/users/me` | Yes | Update profile |
| PATCH | `/api/users/me/language` | Yes | Update language |
| PATCH | `/api/users/me/country` | Yes | Update country |
| POST | `/api/users/profile-picture/upload-url` | Yes | Get presigned upload URL |
| PUT | `/api/users/profile-picture` | Yes | Update profile picture |
| DELETE | `/api/users/profile-picture` | Yes | Delete profile picture |

### Conversation Endpoints
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/conversations` | Yes | List conversations |
| POST | `/api/conversations` | Yes | Create conversation |
| DELETE | `/api/conversations/{id}` | Yes | Delete conversation |
| GET | `/api/conversations/{id}/messages` | Yes | Get messages |
| DELETE | `/api/conversations/{id}/messages/{msgId}` | Yes | Delete message |
| POST | `/api/conversations/{id}/read` | Yes | Mark as read |

### Attachment Endpoints
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/attachments/upload-url` | Yes | Get upload URL |
| GET | `/api/attachments/download-url?key=` | Yes | Get download URL |

### Device Endpoints
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/devices/register` | Yes | Register push token |
| POST | `/api/devices/unregister` | Yes | Unregister push token |

### WebSocket Actions
| Direction | Action | Description |
|-----------|--------|-------------|
| → Server | `message:send` | Send message |
| → Server | `message:typing` | Typing indicator |
| → Server | `message:reaction` | Add/remove reaction |
| → Server | `message:read` | Mark message as read |
| → Server | `message:deleted` | Broadcast message deletion |
| ← Client | `message:receive` | New message |
| ← Client | `message:typing` | Someone typing |
| ← Client | `message:reaction` | Reaction updated |
| ← Client | `message:read` | Read receipt |
| ← Client | `message:deleted` | Message was deleted |

---

*Last Updated: December 2024*
