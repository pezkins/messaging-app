import Foundation

// MARK: - User Models
struct User: Codable, Identifiable {
    let id: String
    let email: String
    let username: String
    let preferredLanguage: String
    let preferredCountry: String?
    let avatarUrl: String?
    let createdAt: String
    let updatedAt: String
}

struct UserPublic: Codable, Identifiable {
    let id: String
    let username: String
    let preferredLanguage: String
    let avatarUrl: String?
}

// MARK: - Auth Models
struct AuthResponse: Codable {
    let user: User
    let accessToken: String
    let refreshToken: String
    let isNewUser: Bool?
}

struct LoginRequest: Codable {
    let email: String
    let password: String
}

struct RegisterRequest: Codable {
    let email: String
    let password: String
    let username: String
    let preferredLanguage: String
    let preferredCountry: String?
}

struct OAuthRequest: Codable {
    let provider: String
    let token: String
    let email: String?
    let name: String?
}

// MARK: - Conversation Models
struct Conversation: Codable, Identifiable {
    let id: String
    let type: String // "direct" or "group"
    let name: String?
    let participants: [UserPublic]
    let lastMessage: Message?
    let createdAt: String
    let updatedAt: String
}

struct CreateConversationRequest: Codable {
    let participantIds: [String]
    let type: String
    let name: String?
}

// MARK: - Message Models
enum MessageStatus: String, Codable {
    case sending
    case sent
    case delivered
    case seen
    case failed
}

enum MessageType: String, Codable {
    case text
    case voice
    case image
    case file
    case gif
    case attachment
}

struct Message: Codable, Identifiable {
    let id: String
    let conversationId: String
    let senderId: String
    let sender: UserPublic
    let type: MessageType
    let originalContent: String
    let originalLanguage: String
    let translatedContent: String?
    let targetLanguage: String?
    let status: MessageStatus
    let createdAt: String
    let reactions: [String: [String]]?
    let attachment: Attachment?
    
    init(id: String, conversationId: String, senderId: String, sender: UserPublic, type: MessageType = .text, originalContent: String, originalLanguage: String, translatedContent: String? = nil, targetLanguage: String? = nil, status: MessageStatus = .sent, createdAt: String, reactions: [String: [String]]? = nil, attachment: Attachment? = nil) {
        self.id = id
        self.conversationId = conversationId
        self.senderId = senderId
        self.sender = sender
        self.type = type
        self.originalContent = originalContent
        self.originalLanguage = originalLanguage
        self.translatedContent = translatedContent
        self.targetLanguage = targetLanguage
        self.status = status
        self.createdAt = createdAt
        self.reactions = reactions
        self.attachment = attachment
    }
}

struct Attachment: Codable {
    let id: String
    let key: String
    let fileName: String
    let contentType: String
    let fileSize: Int64
    let category: String // "image", "video", "document", "audio"
    let url: String?
}

// MARK: - API Response Models
struct ConversationsResponse: Codable {
    let conversations: [Conversation]
}

struct MessagesResponse: Codable {
    let messages: [Message]
    let hasMore: Bool
    let nextCursor: String?
}

struct UsersSearchResponse: Codable {
    let users: [UserPublic]
}

