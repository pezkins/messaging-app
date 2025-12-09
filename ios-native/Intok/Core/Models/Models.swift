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
    let updatedAt: String?
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
    let providerId: String
    let email: String
    let name: String?
    let avatarUrl: String?
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
    var unreadCount: Int?
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
    
    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        let rawValue = try container.decode(String.self).lowercased()
        guard let status = MessageStatus(rawValue: rawValue) else {
            throw DecodingError.dataCorruptedError(in: container, debugDescription: "Unknown message status: \(rawValue)")
        }
        self = status
    }
}

enum MessageType: String, Codable {
    case text
    case voice
    case image
    case file
    case gif
    case attachment
    
    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        let rawValue = try container.decode(String.self).lowercased()
        guard let type = MessageType(rawValue: rawValue) else {
            throw DecodingError.dataCorruptedError(in: container, debugDescription: "Unknown message type: \(rawValue)")
        }
        self = type
    }
}

struct Message: Codable, Identifiable {
    let id: String
    let conversationId: String
    let senderId: String
    let sender: UserPublic?  // Made optional - may not always be present
    let type: MessageType
    let originalContent: String
    let originalLanguage: String?  // Made optional - backend may not always return
    let translatedContent: String?
    let targetLanguage: String?
    let status: MessageStatus?  // Made optional - backend may return different status
    let createdAt: String
    let reactions: [String: [String]]?
    let attachment: Attachment?
    var readBy: [String]?  // Array of user IDs who have read
    var readAt: String?    // Timestamp when read
    
    // Custom decoding to handle backend variations
    enum CodingKeys: String, CodingKey {
        case id, conversationId, senderId, sender, type
        case originalContent, originalLanguage
        case translatedContent, targetLanguage
        case status, createdAt, reactions, attachment
        case content  // Backend might use 'content' instead of 'originalContent'
        case readBy, readAt
    }
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        
        id = try container.decode(String.self, forKey: .id)
        conversationId = try container.decode(String.self, forKey: .conversationId)
        senderId = try container.decode(String.self, forKey: .senderId)
        sender = try container.decodeIfPresent(UserPublic.self, forKey: .sender)
        
        // Handle type - default to .text if missing or invalid
        if let typeValue = try? container.decode(MessageType.self, forKey: .type) {
            type = typeValue
        } else {
            type = .text
        }
        
        // Try originalContent first, then fall back to content
        if let original = try? container.decode(String.self, forKey: .originalContent) {
            originalContent = original
        } else if let content = try? container.decode(String.self, forKey: .content) {
            originalContent = content
        } else {
            originalContent = ""
        }
        
        originalLanguage = try container.decodeIfPresent(String.self, forKey: .originalLanguage)
        translatedContent = try container.decodeIfPresent(String.self, forKey: .translatedContent)
        targetLanguage = try container.decodeIfPresent(String.self, forKey: .targetLanguage)
        status = try container.decodeIfPresent(MessageStatus.self, forKey: .status)
        createdAt = try container.decode(String.self, forKey: .createdAt)
        reactions = try container.decodeIfPresent([String: [String]].self, forKey: .reactions)
        attachment = try container.decodeIfPresent(Attachment.self, forKey: .attachment)
        readBy = try container.decodeIfPresent([String].self, forKey: .readBy)
        readAt = try container.decodeIfPresent(String.self, forKey: .readAt)
    }
    
    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encode(conversationId, forKey: .conversationId)
        try container.encode(senderId, forKey: .senderId)
        try container.encodeIfPresent(sender, forKey: .sender)
        try container.encode(type, forKey: .type)
        try container.encode(originalContent, forKey: .originalContent)
        try container.encodeIfPresent(originalLanguage, forKey: .originalLanguage)
        try container.encodeIfPresent(translatedContent, forKey: .translatedContent)
        try container.encodeIfPresent(targetLanguage, forKey: .targetLanguage)
        try container.encodeIfPresent(status, forKey: .status)
        try container.encode(createdAt, forKey: .createdAt)
        try container.encodeIfPresent(reactions, forKey: .reactions)
        try container.encodeIfPresent(attachment, forKey: .attachment)
        try container.encodeIfPresent(readBy, forKey: .readBy)
        try container.encodeIfPresent(readAt, forKey: .readAt)
    }
    
    init(id: String, conversationId: String, senderId: String, sender: UserPublic?, type: MessageType = .text, originalContent: String, originalLanguage: String?, translatedContent: String? = nil, targetLanguage: String? = nil, status: MessageStatus? = .sent, createdAt: String, reactions: [String: [String]]? = nil, attachment: Attachment? = nil, readBy: [String]? = nil, readAt: String? = nil) {
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
        self.readBy = readBy
        self.readAt = readAt
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


