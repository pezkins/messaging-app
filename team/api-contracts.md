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

### POST /api/auth/register

Create a new user account.

**Request:**
```json
{
  "email": "user@example.com",
  "password": "securePassword123",
  "name": "John Doe"
}
```

**Response (201 Created):**
```json
{
  "user": {
    "id": "user_abc123",
    "email": "user@example.com",
    "name": "John Doe",
    "preferredLanguage": "en",
    "avatarUrl": null,
    "createdAt": "2024-12-08T00:00:00.000Z"
  },
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

**Errors:**
- `400` - Invalid email format or weak password
- `409` - Email already registered

---

### POST /api/auth/login

Authenticate user and get tokens.

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
    "name": "John Doe",
    "preferredLanguage": "en",
    "avatarUrl": "https://..."
  },
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

**Errors:**
- `401` - Invalid credentials

---

### POST /api/auth/refresh

Refresh access token using refresh token.

**Request:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

**Errors:**
- `401` - Invalid or expired refresh token

---

### POST /api/auth/google

Authenticate with Google OAuth.

**Request:**
```json
{
  "idToken": "google_id_token_here"
}
```

**Response (200 OK):**
```json
{
  "user": { ... },
  "accessToken": "...",
  "refreshToken": "...",
  "isNewUser": false
}
```

---

## Users

### GET /api/users/me

Get current user profile.

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Response (200 OK):**
```json
{
  "id": "user_abc123",
  "email": "user@example.com",
  "name": "John Doe",
  "preferredLanguage": "en",
  "avatarUrl": "https://...",
  "createdAt": "2024-12-08T00:00:00.000Z"
}
```

---

### PATCH /api/users/me/language

Update user's preferred language.

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Request:**
```json
{
  "language": "es"
}
```

**Response (200 OK):**
```json
{
  "id": "user_abc123",
  "preferredLanguage": "es"
}
```

**Valid Languages:** `en`, `es`, `fr`, `de`, `it`, `zh`, `ja`, `ko`, `ar`, `hi`, `pt`, `ru`, `nl`, `pl`, `tr`, `vi`

---

### GET /api/users/search

Search for users by name or email.

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Query Parameters:**
- `q` (required) - Search query (min 2 characters)
- `limit` (optional) - Max results (default: 20)

**Example:** `GET /api/users/search?q=john&limit=10`

**Response (200 OK):**
```json
{
  "users": [
    {
      "id": "user_xyz789",
      "name": "John Smith",
      "email": "john.smith@example.com",
      "avatarUrl": "https://..."
    }
  ]
}
```

---

## Conversations

### GET /api/conversations

List user's conversations.

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Query Parameters:**
- `limit` (optional) - Max results (default: 50)
- `cursor` (optional) - Pagination cursor

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
          "userId": "user_abc123",
          "name": "John Doe",
          "avatarUrl": "https://..."
        },
        {
          "userId": "user_xyz789",
          "name": "Jane Smith",
          "avatarUrl": "https://..."
        }
      ],
      "lastMessage": {
        "id": "msg_123",
        "content": "Hello!",
        "translatedContent": "¡Hola!",
        "senderId": "user_xyz789",
        "createdAt": "2024-12-08T12:00:00.000Z"
      },
      "unreadCount": 3,
      "updatedAt": "2024-12-08T12:00:00.000Z"
    }
  ],
  "nextCursor": "cursor_value"
}
```

---

### POST /api/conversations

Create a new conversation.

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Request (Direct):**
```json
{
  "type": "direct",
  "participantIds": ["user_xyz789"]
}
```

**Request (Group):**
```json
{
  "type": "group",
  "name": "Project Team",
  "participantIds": ["user_xyz789", "user_def456"]
}
```

**Response (201 Created):**
```json
{
  "id": "conv_abc123",
  "type": "direct",
  "name": null,
  "participants": [...],
  "createdAt": "2024-12-08T00:00:00.000Z"
}
```

---

### GET /api/conversations/:id/messages

Get messages for a conversation.

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Query Parameters:**
- `limit` (optional) - Max results (default: 50)
- `before` (optional) - Get messages before this message ID
- `after` (optional) - Get messages after this message ID

**Response (200 OK):**
```json
{
  "messages": [
    {
      "id": "msg_abc123",
      "conversationId": "conv_abc123",
      "senderId": "user_xyz789",
      "content": "Hello, how are you?",
      "translatedContent": "¡Hola! ¿Cómo estás?",
      "originalLanguage": "en",
      "targetLanguage": "es",
      "type": "text",
      "attachments": [],
      "reactions": [],
      "readBy": ["user_abc123"],
      "createdAt": "2024-12-08T12:00:00.000Z"
    }
  ],
  "hasMore": true
}
```

---

## Messages

### POST /api/messages/preview-translation

Preview translation before sending.

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Request:**
```json
{
  "content": "Hello, how are you?",
  "targetLanguage": "es"
}
```

**Response (200 OK):**
```json
{
  "originalContent": "Hello, how are you?",
  "translatedContent": "¡Hola! ¿Cómo estás?",
  "detectedLanguage": "en",
  "targetLanguage": "es"
}
```

---

## WebSocket Events

### Connection

Connect with authentication:
```javascript
const ws = new WebSocket('wss://api.intokapp.com?token=<accessToken>');
```

### Client → Server Events

#### message:send
```json
{
  "type": "message:send",
  "payload": {
    "conversationId": "conv_abc123",
    "content": "Hello!",
    "type": "text",
    "attachments": []
  }
}
```

#### message:typing
```json
{
  "type": "message:typing",
  "payload": {
    "conversationId": "conv_abc123",
    "isTyping": true
  }
}
```

#### message:read
```json
{
  "type": "message:read",
  "payload": {
    "conversationId": "conv_abc123",
    "messageId": "msg_abc123"
  }
}
```

### Server → Client Events

#### message:receive
```json
{
  "type": "message:receive",
  "payload": {
    "id": "msg_abc123",
    "conversationId": "conv_abc123",
    "senderId": "user_xyz789",
    "senderName": "Jane Smith",
    "content": "Hello, how are you?",
    "translatedContent": "¡Hola! ¿Cómo estás?",
    "originalLanguage": "en",
    "targetLanguage": "es",
    "type": "text",
    "createdAt": "2024-12-08T12:00:00.000Z"
  }
}
```

#### message:typing
```json
{
  "type": "message:typing",
  "payload": {
    "conversationId": "conv_abc123",
    "userId": "user_xyz789",
    "userName": "Jane Smith",
    "isTyping": true
  }
}
```

#### message:read
```json
{
  "type": "message:read",
  "payload": {
    "conversationId": "conv_abc123",
    "messageId": "msg_abc123",
    "userId": "user_xyz789"
  }
}
```

---

## Data Schemas

### User
```typescript
interface User {
  id: string;
  email: string;
  name: string;
  preferredLanguage: LanguageCode;
  avatarUrl: string | null;
  createdAt: string; // ISO 8601
}
```

### Conversation
```typescript
interface Conversation {
  id: string;
  type: 'direct' | 'group';
  name: string | null; // null for direct, required for group
  participants: Participant[];
  lastMessage: Message | null;
  unreadCount: number;
  createdAt: string;
  updatedAt: string;
}
```

### Message
```typescript
interface Message {
  id: string;
  conversationId: string;
  senderId: string;
  content: string;
  translatedContent: string | null;
  originalLanguage: LanguageCode;
  targetLanguage: LanguageCode | null;
  type: 'text' | 'image' | 'file' | 'gif' | 'voice';
  attachments: Attachment[];
  reactions: Reaction[];
  readBy: string[];
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
  "error": {
    "code": "INVALID_TOKEN",
    "message": "The provided token is invalid or expired",
    "details": {}
  }
}
```

### Common Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `VALIDATION_ERROR` | 400 | Invalid request data |
| `UNAUTHORIZED` | 401 | Missing or invalid auth |
| `FORBIDDEN` | 403 | No permission |
| `NOT_FOUND` | 404 | Resource not found |
| `CONFLICT` | 409 | Resource already exists |
| `RATE_LIMITED` | 429 | Too many requests |
| `INTERNAL_ERROR` | 500 | Server error |

---

*Last Updated: December 2024*

