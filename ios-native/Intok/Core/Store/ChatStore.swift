import Foundation
import Combine
import os.log

private let logger = Logger(subsystem: "com.pezkins.intok", category: "ChatStore")

// MARK: - Chat Store
@MainActor
class ChatStore: ObservableObject {
    static let shared = ChatStore()
    
    @Published var conversations: [Conversation] = []
    @Published var activeConversation: Conversation?
    @Published var messages: [Message] = []
    @Published var typingUsers: [String: Set<String>] = [:] // conversationId -> Set<userId>
    
    @Published var isLoadingConversations = false
    @Published var isLoadingMessages = false
    @Published var hasMoreMessages = false
    @Published var conversationsError: String?
    @Published var messagesError: String?
    
    private var nextCursor: String?
    
    private init() {
        setupWebSocketHandlers()
    }
    
    // MARK: - WebSocket Event Handlers
    private func setupWebSocketHandlers() {
        WebSocketService.shared.onMessageReceive = { [weak self] data in
            self?.handleMessageReceive(data)
        }
        
        WebSocketService.shared.onTyping = { [weak self] data in
            self?.handleTyping(data)
        }
        
        WebSocketService.shared.onReaction = { [weak self] data in
            self?.handleReaction(data)
        }
        
        WebSocketService.shared.onMessageDeleted = { [weak self] data in
            self?.handleMessageDeleted(data)
        }
    }
    
    private func handleMessageDeleted(_ data: MessageDeletedData) {
        // Find and update the message as deleted
        if let index = messages.firstIndex(where: { $0.id == data.messageId }) {
            messages[index].deletedAt = data.deletedAt
            messages[index].deletedBy = data.deletedBy
            logger.info("🗑️ Message marked as deleted: \(data.messageId, privacy: .public)")
        }
    }
    
    private func handleMessageReceive(_ data: MessageReceiveData) {
        let message = data.message
        let currentUserId = AuthManager.shared.currentUser?.id
        
        // Replace temp message or add new
        if let tempId = data.tempId, let index = messages.firstIndex(where: { $0.id == tempId }) {
            // Server returned tempId, replace the optimistic message
            messages[index] = message
            logger.debug("📨 Replaced temp message \(tempId, privacy: .public) with \(message.id, privacy: .public)")
        } else if activeConversation?.id == message.conversationId {
            // Check for duplicates by ID first
            if messages.contains(where: { $0.id == message.id }) {
                logger.debug("📨 Ignoring duplicate message: \(message.id, privacy: .public)")
                return
            }
            
            // For own messages: check if there's a pending temp message to replace
            // This handles cases where server doesn't echo back the tempId
            if message.senderId == currentUserId {
                if let tempIndex = messages.firstIndex(where: { msg in
                    guard msg.id.hasPrefix("temp-"),
                          msg.originalContent == message.originalContent else {
                        return false
                    }
                    
                    // Only match if the temp message was created recently (within 30 seconds)
                    // This prevents incorrect matching when user sends identical content rapidly
                    if let tempDate = ISO8601DateFormatter().date(from: msg.createdAt) {
                        return abs(tempDate.timeIntervalSinceNow) < 30
                    }
                    return false
                }) {
                    messages[tempIndex] = message
                    logger.debug("📨 Replaced optimistic message with server message: \(message.id, privacy: .public)")
                    return
                }
            }
            
            // New message from another user - add it
            messages.append(message)
            logger.debug("📨 Added new message: \(message.id, privacy: .public)")
        }
        
        // Update conversation's last message
        if let index = conversations.firstIndex(where: { $0.id == message.conversationId }) {
            var updated = conversations[index]
            // Create new conversation with updated lastMessage
            // Note: This is a workaround since Conversation is a struct
            conversations[index] = Conversation(
                id: updated.id,
                type: updated.type,
                name: updated.name,
                participants: updated.participants,
                lastMessage: message,
                createdAt: updated.createdAt,
                updatedAt: message.createdAt
            )
            
            // Sort conversations by last message time
            conversations.sort { ($0.updatedAt) > ($1.updatedAt) }
        }
        
        logger.info("📨 Message received in \(message.conversationId, privacy: .public)")
    }
    
    private func handleTyping(_ data: TypingData) {
        var users = typingUsers[data.conversationId] ?? Set()
        
        if data.isTyping {
            users.insert(data.userId)
        } else {
            users.remove(data.userId)
        }
        
        typingUsers[data.conversationId] = users
    }
    
    private func handleReaction(_ data: ReactionData) {
        if let index = messages.firstIndex(where: { $0.id == data.messageId }) {
            var message = messages[index]
            // Update message with new reactions
            messages[index] = Message(
                id: message.id,
                conversationId: message.conversationId,
                senderId: message.senderId,
                sender: message.sender,
                type: message.type,
                originalContent: message.originalContent,
                originalLanguage: message.originalLanguage,
                translatedContent: message.translatedContent,
                targetLanguage: message.targetLanguage,
                status: message.status,
                createdAt: message.createdAt,
                reactions: data.reactions,
                attachment: message.attachment
            )
        }
    }
    
    // MARK: - Load Conversations
    func loadConversations() async {
        guard !isLoadingConversations else { return }
        
        isLoadingConversations = true
        conversationsError = nil
        logger.info("📥 Loading conversations...")
        
        do {
            let response = try await APIService.shared.getConversations()
            conversations = response.conversations
            conversationsError = nil
            logger.info("✅ Loaded \(response.conversations.count) conversations")
        } catch APIError.unauthorized {
            logger.error("❌ Unauthorized - token may be expired")
            conversationsError = "Session expired. Please sign in again."
        } catch APIError.networkError(let error) {
            logger.error("❌ Network error: \(error.localizedDescription, privacy: .public)")
            conversationsError = "Unable to connect. Check your internet connection."
        } catch APIError.decodingError(let error) {
            logger.error("❌ Decoding error: \(String(describing: error), privacy: .public)")
            conversationsError = "Data format error. Please update the app."
        } catch {
            logger.error("❌ Failed to load conversations: \(String(describing: error), privacy: .public)")
            conversationsError = "Failed to load conversations. Pull to retry."
        }
        
        isLoadingConversations = false
    }
    
    // MARK: - Select Conversation
    func selectConversation(_ conversation: Conversation) async {
        // Leave previous conversation
        if let prev = activeConversation {
            WebSocketService.shared.leaveConversation(prev.id)
        }
        
        // Join new conversation
        WebSocketService.shared.joinConversation(conversation.id)
        
        activeConversation = conversation
        messages = []
        hasMoreMessages = false
        nextCursor = nil
        
        isLoadingMessages = true
        
        do {
            let response = try await APIService.shared.getMessages(conversationId: conversation.id)
            messages = response.messages
            hasMoreMessages = response.hasMore
            nextCursor = response.nextCursor
            logger.info("✅ Loaded \(response.messages.count) messages")
        } catch {
            logger.error("❌ Failed to load messages: \(error.localizedDescription, privacy: .public)")
        }
        
        isLoadingMessages = false
    }
    
    // MARK: - Clear Active Conversation
    func clearActiveConversation() {
        if let prev = activeConversation {
            WebSocketService.shared.leaveConversation(prev.id)
        }
        activeConversation = nil
        messages = []
        hasMoreMessages = false
        nextCursor = nil
    }
    
    // MARK: - Load More Messages
    func loadMoreMessages() async {
        guard let conversation = activeConversation,
              hasMoreMessages,
              !isLoadingMessages,
              let cursor = nextCursor else { return }
        
        isLoadingMessages = true
        
        do {
            let response = try await APIService.shared.getMessages(
                conversationId: conversation.id,
                cursor: cursor
            )
            
            // Prepend older messages
            messages = response.messages + messages
            hasMoreMessages = response.hasMore
            nextCursor = response.nextCursor
        } catch {
            logger.error("❌ Failed to load more messages: \(error.localizedDescription, privacy: .public)")
        }
        
        isLoadingMessages = false
    }
    
    // MARK: - Send Message
    func sendMessage(_ content: String, type: String = "text", attachment: [String: Any]? = nil, translateDocument: Bool? = nil, replyTo: ReplyTo? = nil) {
        guard let conversation = activeConversation,
              !content.trimmingCharacters(in: .whitespaces).isEmpty || attachment != nil else { return }
        
        let currentUser = AuthManager.shared.currentUser
        let tempId = "temp-\(Date().timeIntervalSince1970)-\(UUID().uuidString.prefix(8))"
        
        // Create optimistic message with actual user info for better tracking
        let optimisticMessage = Message(
            id: tempId,
            conversationId: conversation.id,
            senderId: currentUser?.id ?? "pending",
            sender: UserPublic(
                id: currentUser?.id ?? "pending",
                username: currentUser?.username ?? "You",
                preferredLanguage: currentUser?.preferredLanguage ?? "en",
                avatarUrl: nil
            ),
            type: .text,
            originalContent: content,
            originalLanguage: currentUser?.preferredLanguage ?? "en",
            status: .sending,
            createdAt: ISO8601DateFormatter().string(from: Date()),
            replyTo: replyTo
        )
        
        messages.append(optimisticMessage)
        logger.debug("📤 Added optimistic message: \(tempId, privacy: .public)")
        
        // Build replyTo dictionary for WebSocket
        var replyToDict: [String: Any]?
        if let reply = replyTo {
            replyToDict = [
                "messageId": reply.messageId,
                "content": reply.content,
                "senderId": reply.senderId,
                "senderName": reply.senderName,
                "type": reply.type.rawValue
            ]
        }
        
        // Send via WebSocket
        WebSocketService.shared.sendMessage(
            conversationId: conversation.id,
            content: content,
            type: type,
            tempId: tempId,
            attachment: attachment,
            translateDocument: translateDocument,
            replyTo: replyToDict
        )
    }
    
    // MARK: - Start Conversation
    func startConversation(with user: UserPublic) async -> Conversation? {
        // Check for existing direct conversation
        if let existing = conversations.first(where: { conv in
            conv.type == "direct" && conv.participants.contains(where: { $0.id == user.id })
        }) {
            await selectConversation(existing)
            return existing
        }
        
        // Create new conversation
        do {
            let response = try await APIService.shared.createConversation(
                participantIds: [user.id],
                type: "direct"
            )
            
            conversations.insert(response.conversation, at: 0)
            await selectConversation(response.conversation)
            return response.conversation
        } catch {
            logger.error("❌ Failed to start conversation: \(error.localizedDescription, privacy: .public)")
            return nil
        }
    }
    
    // MARK: - Start Group Conversation
    func startGroupConversation(with users: [UserPublic], name: String? = nil) async -> Conversation? {
        do {
            let response = try await APIService.shared.createConversation(
                participantIds: users.map { $0.id },
                type: "group",
                name: name
            )
            
            conversations.insert(response.conversation, at: 0)
            await selectConversation(response.conversation)
            return response.conversation
        } catch {
            logger.error("❌ Failed to start group: \(error.localizedDescription, privacy: .public)")
            return nil
        }
    }
    
    // MARK: - Typing
    func setTyping(_ isTyping: Bool) {
        guard let conversation = activeConversation else { return }
        WebSocketService.shared.sendTyping(conversationId: conversation.id, isTyping: isTyping)
    }
    
    // MARK: - Reaction
    func sendReaction(messageId: String, messageTimestamp: String, emoji: String) {
        guard let conversation = activeConversation else { return }
        WebSocketService.shared.sendReaction(
            conversationId: conversation.id,
            messageId: messageId,
            messageTimestamp: messageTimestamp,
            emoji: emoji
        )
    }
    
    // MARK: - Search Users
    func searchUsers(query: String) async -> [UserPublic] {
        guard query.count >= 2 else { return [] }
        
        do {
            let response = try await APIService.shared.searchUsers(query: query)
            return response.users
        } catch {
            logger.error("❌ User search failed: \(error.localizedDescription, privacy: .public)")
            return []
        }
    }
    
    // MARK: - Delete Message
    func deleteMessage(_ message: Message, forEveryone: Bool) async throws {
        try await APIService.shared.deleteMessage(
            conversationId: message.conversationId,
            messageId: message.id,
            forEveryone: forEveryone
        )
        
        // Update local state
        if forEveryone {
            // Mark as deleted for everyone
            if let index = messages.firstIndex(where: { $0.id == message.id }) {
                messages[index].deletedAt = ISO8601DateFormatter().string(from: Date())
                messages[index].deletedBy = AuthManager.shared.currentUser?.id
            }
        } else {
            // Remove from local view only
            messages.removeAll { $0.id == message.id }
        }
        
        logger.info("🗑️ Message deleted: \(message.id, privacy: .public), forEveryone: \(forEveryone)")
    }
    
    // MARK: - Delete Conversation
    func deleteConversation(_ conversation: Conversation) async throws {
        try await APIService.shared.deleteConversation(conversationId: conversation.id)
        
        // Remove from local state
        conversations.removeAll { $0.id == conversation.id }
        
        // Clear active conversation if it was deleted
        if activeConversation?.id == conversation.id {
            activeConversation = nil
            messages = []
        }
        
        logger.info("🗑️ Conversation deleted: \(conversation.id, privacy: .public)")
    }
}


