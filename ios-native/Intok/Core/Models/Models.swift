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
    
    // Custom decoding to handle both 'avatarUrl' and 'profilePicture' from backend
    enum CodingKeys: String, CodingKey {
        case id, email, username, preferredLanguage, preferredCountry
        case avatarUrl, profilePicture, createdAt, updatedAt
    }
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(String.self, forKey: .id)
        email = try container.decode(String.self, forKey: .email)
        username = try container.decode(String.self, forKey: .username)
        preferredLanguage = try container.decode(String.self, forKey: .preferredLanguage)
        preferredCountry = try container.decodeIfPresent(String.self, forKey: .preferredCountry)
        createdAt = try container.decode(String.self, forKey: .createdAt)
        updatedAt = try container.decodeIfPresent(String.self, forKey: .updatedAt)
        
        // Try avatarUrl first, then profilePicture
        if let url = try container.decodeIfPresent(String.self, forKey: .avatarUrl) {
            avatarUrl = url
        } else {
            avatarUrl = try container.decodeIfPresent(String.self, forKey: .profilePicture)
        }
    }
    
    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encode(email, forKey: .email)
        try container.encode(username, forKey: .username)
        try container.encode(preferredLanguage, forKey: .preferredLanguage)
        try container.encodeIfPresent(preferredCountry, forKey: .preferredCountry)
        try container.encodeIfPresent(avatarUrl, forKey: .avatarUrl)
        try container.encode(createdAt, forKey: .createdAt)
        try container.encodeIfPresent(updatedAt, forKey: .updatedAt)
    }
}

struct UserPublic: Codable, Identifiable {
    let id: String
    let username: String
    let preferredLanguage: String
    let avatarUrl: String?
    
    enum CodingKeys: String, CodingKey {
        case id, username, preferredLanguage, avatarUrl, profilePicture
    }
    
    init(id: String, username: String, preferredLanguage: String, avatarUrl: String?) {
        self.id = id
        self.username = username
        self.preferredLanguage = preferredLanguage
        self.avatarUrl = avatarUrl
    }
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(String.self, forKey: .id)
        username = try container.decode(String.self, forKey: .username)
        preferredLanguage = try container.decodeIfPresent(String.self, forKey: .preferredLanguage) ?? "en"
        
        // Try avatarUrl first, then fall back to profilePicture
        if let url = try container.decodeIfPresent(String.self, forKey: .avatarUrl) {
            avatarUrl = url
        } else {
            avatarUrl = try container.decodeIfPresent(String.self, forKey: .profilePicture)
        }
    }
    
    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encode(username, forKey: .username)
        try container.encode(preferredLanguage, forKey: .preferredLanguage)
        try container.encodeIfPresent(avatarUrl, forKey: .avatarUrl)
    }
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
    
    // Custom encoding to ensure null values are included in JSON
    // (Backend Zod schema requires fields to be present, even if null)
    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(provider, forKey: .provider)
        try container.encode(providerId, forKey: .providerId)
        try container.encode(email, forKey: .email)
        try container.encode(name, forKey: .name)  // Encodes as null if nil
        try container.encode(avatarUrl, forKey: .avatarUrl)  // Encodes as null if nil
    }
    
    private enum CodingKeys: String, CodingKey {
        case provider, providerId, email, name, avatarUrl
    }
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

// MARK: - Reply To Model
struct ReplyTo: Codable, Equatable {
    let messageId: String
    let content: String
    let senderId: String
    let senderName: String
    let type: MessageType
    
    enum CodingKeys: String, CodingKey {
        case messageId, content, senderId, senderName, type
    }
    
    init(messageId: String, content: String, senderId: String, senderName: String, type: MessageType = .text) {
        self.messageId = messageId
        self.content = content
        self.senderId = senderId
        self.senderName = senderName
        self.type = type
    }
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        messageId = try container.decode(String.self, forKey: .messageId)
        content = try container.decode(String.self, forKey: .content)
        senderId = try container.decode(String.self, forKey: .senderId)
        senderName = try container.decode(String.self, forKey: .senderName)
        
        // Handle type - default to .text if missing or invalid
        if let typeValue = try? container.decode(MessageType.self, forKey: .type) {
            type = typeValue
        } else {
            type = .text
        }
    }
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
    var replyTo: ReplyTo?  // Reply to another message
    var deletedAt: String?  // Timestamp when deleted
    var deletedBy: String?  // User ID who deleted the message
    
    var isDeleted: Bool {
        deletedAt != nil
    }

    // Custom decoding to handle backend variations
    enum CodingKeys: String, CodingKey {
        case id, conversationId, senderId, sender, type
        case originalContent, originalLanguage
        case translatedContent, targetLanguage
        case status, createdAt, reactions, attachment
        case content  // Backend might use 'content' instead of 'originalContent'
        case readBy, readAt, replyTo
        case deletedAt, deletedBy
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
        replyTo = try container.decodeIfPresent(ReplyTo.self, forKey: .replyTo)
        deletedAt = try container.decodeIfPresent(String.self, forKey: .deletedAt)
        deletedBy = try container.decodeIfPresent(String.self, forKey: .deletedBy)
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
        try container.encodeIfPresent(replyTo, forKey: .replyTo)
        try container.encodeIfPresent(deletedAt, forKey: .deletedAt)
        try container.encodeIfPresent(deletedBy, forKey: .deletedBy)
    }
    
    init(id: String, conversationId: String, senderId: String, sender: UserPublic?, type: MessageType = .text, originalContent: String, originalLanguage: String?, translatedContent: String? = nil, targetLanguage: String? = nil, status: MessageStatus? = .sent, createdAt: String, reactions: [String: [String]]? = nil, attachment: Attachment? = nil, readBy: [String]? = nil, readAt: String? = nil, replyTo: ReplyTo? = nil, deletedAt: String? = nil, deletedBy: String? = nil) {
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
        self.replyTo = replyTo
        self.deletedAt = deletedAt
        self.deletedBy = deletedBy
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
