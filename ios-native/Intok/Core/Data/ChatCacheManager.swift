import Foundation
import CoreData
import os.log

private let logger = Logger(subsystem: "com.pezkins.intok", category: "ChatCacheManager")

/// Manages Core Data caching for chat messages and conversations
class ChatCacheManager {
    static let shared = ChatCacheManager()
    
    /// Maximum messages to keep per conversation
    static let maxMessagesPerConversation = 100
    
    // MARK: - Core Data Stack
    
    private lazy var persistentContainer: NSPersistentContainer = {
        let container = NSPersistentContainer(name: "ChatCache", managedObjectModel: Self.createManagedObjectModel())
        
        container.loadPersistentStores { description, error in
            if let error = error {
                logger.error("❌ Core Data failed to load: \(error.localizedDescription, privacy: .public)")
            } else {
                logger.info("✅ Core Data loaded successfully")
            }
        }
        
        container.viewContext.automaticallyMergesChangesFromParent = true
        container.viewContext.mergePolicy = NSMergeByPropertyObjectTrumpMergePolicy
        return container
    }()
    
    private var viewContext: NSManagedObjectContext {
        persistentContainer.viewContext
    }
    
    private init() {}
    
    // MARK: - Programmatic Core Data Model
    
    private static func createManagedObjectModel() -> NSManagedObjectModel {
        let model = NSManagedObjectModel()
        
        // CachedConversation Entity
        let conversationEntity = NSEntityDescription()
        conversationEntity.name = "CachedConversation"
        conversationEntity.managedObjectClassName = NSStringFromClass(CachedConversation.self)
        
        let convIdAttr = NSAttributeDescription()
        convIdAttr.name = "id"
        convIdAttr.attributeType = .stringAttributeType
        
        let convTypeAttr = NSAttributeDescription()
        convTypeAttr.name = "type"
        convTypeAttr.attributeType = .stringAttributeType
        
        let convNameAttr = NSAttributeDescription()
        convNameAttr.name = "name"
        convNameAttr.attributeType = .stringAttributeType
        convNameAttr.isOptional = true
        
        let convParticipantsAttr = NSAttributeDescription()
        convParticipantsAttr.name = "participantsJSON"
        convParticipantsAttr.attributeType = .binaryDataAttributeType
        
        let convLastMessageAttr = NSAttributeDescription()
        convLastMessageAttr.name = "lastMessageJSON"
        convLastMessageAttr.attributeType = .binaryDataAttributeType
        convLastMessageAttr.isOptional = true
        
        let convCreatedAtAttr = NSAttributeDescription()
        convCreatedAtAttr.name = "createdAt"
        convCreatedAtAttr.attributeType = .stringAttributeType
        
        let convUpdatedAtAttr = NSAttributeDescription()
        convUpdatedAtAttr.name = "updatedAt"
        convUpdatedAtAttr.attributeType = .stringAttributeType
        
        let convUnreadCountAttr = NSAttributeDescription()
        convUnreadCountAttr.name = "unreadCount"
        convUnreadCountAttr.attributeType = .integer32AttributeType
        convUnreadCountAttr.defaultValue = 0
        convUnreadCountAttr.isOptional = true
        
        let convCachedAtAttr = NSAttributeDescription()
        convCachedAtAttr.name = "cachedAt"
        convCachedAtAttr.attributeType = .dateAttributeType
        
        conversationEntity.properties = [
            convIdAttr, convTypeAttr, convNameAttr, convParticipantsAttr,
            convLastMessageAttr, convCreatedAtAttr, convUpdatedAtAttr,
            convUnreadCountAttr, convCachedAtAttr
        ]
        
        // CachedMessage Entity
        let messageEntity = NSEntityDescription()
        messageEntity.name = "CachedMessage"
        messageEntity.managedObjectClassName = NSStringFromClass(CachedMessage.self)
        
        let msgIdAttr = NSAttributeDescription()
        msgIdAttr.name = "id"
        msgIdAttr.attributeType = .stringAttributeType
        
        let msgConvIdAttr = NSAttributeDescription()
        msgConvIdAttr.name = "conversationId"
        msgConvIdAttr.attributeType = .stringAttributeType
        
        let msgSenderIdAttr = NSAttributeDescription()
        msgSenderIdAttr.name = "senderId"
        msgSenderIdAttr.attributeType = .stringAttributeType
        
        let msgSenderJSONAttr = NSAttributeDescription()
        msgSenderJSONAttr.name = "senderJSON"
        msgSenderJSONAttr.attributeType = .binaryDataAttributeType
        msgSenderJSONAttr.isOptional = true
        
        let msgTypeAttr = NSAttributeDescription()
        msgTypeAttr.name = "type"
        msgTypeAttr.attributeType = .stringAttributeType
        
        let msgOriginalContentAttr = NSAttributeDescription()
        msgOriginalContentAttr.name = "originalContent"
        msgOriginalContentAttr.attributeType = .stringAttributeType
        
        let msgOriginalLanguageAttr = NSAttributeDescription()
        msgOriginalLanguageAttr.name = "originalLanguage"
        msgOriginalLanguageAttr.attributeType = .stringAttributeType
        msgOriginalLanguageAttr.isOptional = true
        
        let msgTranslatedContentAttr = NSAttributeDescription()
        msgTranslatedContentAttr.name = "translatedContent"
        msgTranslatedContentAttr.attributeType = .stringAttributeType
        msgTranslatedContentAttr.isOptional = true
        
        let msgTargetLanguageAttr = NSAttributeDescription()
        msgTargetLanguageAttr.name = "targetLanguage"
        msgTargetLanguageAttr.attributeType = .stringAttributeType
        msgTargetLanguageAttr.isOptional = true
        
        let msgStatusAttr = NSAttributeDescription()
        msgStatusAttr.name = "status"
        msgStatusAttr.attributeType = .stringAttributeType
        msgStatusAttr.isOptional = true
        
        let msgCreatedAtAttr = NSAttributeDescription()
        msgCreatedAtAttr.name = "createdAt"
        msgCreatedAtAttr.attributeType = .stringAttributeType
        
        let msgReactionsJSONAttr = NSAttributeDescription()
        msgReactionsJSONAttr.name = "reactionsJSON"
        msgReactionsJSONAttr.attributeType = .binaryDataAttributeType
        msgReactionsJSONAttr.isOptional = true
        
        let msgAttachmentJSONAttr = NSAttributeDescription()
        msgAttachmentJSONAttr.name = "attachmentJSON"
        msgAttachmentJSONAttr.attributeType = .binaryDataAttributeType
        msgAttachmentJSONAttr.isOptional = true
        
        let msgReadByJSONAttr = NSAttributeDescription()
        msgReadByJSONAttr.name = "readByJSON"
        msgReadByJSONAttr.attributeType = .binaryDataAttributeType
        msgReadByJSONAttr.isOptional = true
        
        let msgReadAtAttr = NSAttributeDescription()
        msgReadAtAttr.name = "readAt"
        msgReadAtAttr.attributeType = .stringAttributeType
        msgReadAtAttr.isOptional = true
        
        let msgReplyToJSONAttr = NSAttributeDescription()
        msgReplyToJSONAttr.name = "replyToJSON"
        msgReplyToJSONAttr.attributeType = .binaryDataAttributeType
        msgReplyToJSONAttr.isOptional = true
        
        let msgDeletedAtAttr = NSAttributeDescription()
        msgDeletedAtAttr.name = "deletedAt"
        msgDeletedAtAttr.attributeType = .stringAttributeType
        msgDeletedAtAttr.isOptional = true
        
        let msgDeletedByAttr = NSAttributeDescription()
        msgDeletedByAttr.name = "deletedBy"
        msgDeletedByAttr.attributeType = .stringAttributeType
        msgDeletedByAttr.isOptional = true
        
        let msgCachedAtAttr = NSAttributeDescription()
        msgCachedAtAttr.name = "cachedAt"
        msgCachedAtAttr.attributeType = .dateAttributeType
        
        messageEntity.properties = [
            msgIdAttr, msgConvIdAttr, msgSenderIdAttr, msgSenderJSONAttr,
            msgTypeAttr, msgOriginalContentAttr, msgOriginalLanguageAttr,
            msgTranslatedContentAttr, msgTargetLanguageAttr, msgStatusAttr,
            msgCreatedAtAttr, msgReactionsJSONAttr, msgAttachmentJSONAttr,
            msgReadByJSONAttr, msgReadAtAttr, msgReplyToJSONAttr,
            msgDeletedAtAttr, msgDeletedByAttr, msgCachedAtAttr
        ]
        
        model.entities = [conversationEntity, messageEntity]
        return model
    }
    
    // MARK: - Save Context
    
    private func saveContext() {
        guard viewContext.hasChanges else { return }
        
        do {
            try viewContext.save()
        } catch {
            logger.error("❌ Failed to save Core Data context: \(error.localizedDescription, privacy: .public)")
        }
    }
    
    // MARK: - Conversation Caching
    
    /// Save conversations to cache
    func saveConversations(_ conversations: [Conversation]) {
        let context = viewContext
        
        for conversation in conversations {
            let fetchRequest: NSFetchRequest<CachedConversation> = CachedConversation.fetchRequest()
            fetchRequest.predicate = NSPredicate(format: "id == %@", conversation.id)
            
            do {
                let existing = try context.fetch(fetchRequest).first
                let cached = existing ?? CachedConversation(context: context)
                
                cached.id = conversation.id
                cached.type = conversation.type
                cached.name = conversation.name
                cached.createdAt = conversation.createdAt
                cached.updatedAt = conversation.updatedAt
                cached.unreadCount = Int32(conversation.unreadCount ?? 0)
                cached.cachedAt = Date()
                
                // Encode participants as JSON
                if let participantsData = try? JSONEncoder().encode(conversation.participants) {
                    cached.participantsJSON = participantsData
                }
                
                // Encode last message as JSON
                if let lastMessage = conversation.lastMessage,
                   let messageData = try? JSONEncoder().encode(lastMessage) {
                    cached.lastMessageJSON = messageData
                }
            } catch {
                logger.error("❌ Failed to save conversation \(conversation.id, privacy: .public): \(error.localizedDescription, privacy: .public)")
            }
        }
        
        saveContext()
        logger.info("💾 Cached \(conversations.count) conversations")
    }
    
    /// Load conversations from cache
    func loadConversations() -> [Conversation] {
        let fetchRequest: NSFetchRequest<CachedConversation> = CachedConversation.fetchRequest()
        fetchRequest.sortDescriptors = [NSSortDescriptor(key: "updatedAt", ascending: false)]
        
        do {
            let cachedConversations = try viewContext.fetch(fetchRequest)
            let conversations = cachedConversations.compactMap { cached -> Conversation? in
                guard let id = cached.id,
                      let type = cached.type,
                      let createdAt = cached.createdAt,
                      let updatedAt = cached.updatedAt else { return nil }
                
                // Decode participants
                var participants: [UserPublic] = []
                if let participantsData = cached.participantsJSON {
                    participants = (try? JSONDecoder().decode([UserPublic].self, from: participantsData)) ?? []
                }
                
                // Decode last message
                var lastMessage: Message?
                if let messageData = cached.lastMessageJSON {
                    lastMessage = try? JSONDecoder().decode(Message.self, from: messageData)
                }
                
                return Conversation(
                    id: id,
                    type: type,
                    name: cached.name,
                    participants: participants,
                    lastMessage: lastMessage,
                    createdAt: createdAt,
                    updatedAt: updatedAt,
                    unreadCount: Int(cached.unreadCount)
                )
            }
            
            logger.info("📂 Loaded \(conversations.count) conversations from cache")
            return conversations
        } catch {
            logger.error("❌ Failed to load conversations from cache: \(error.localizedDescription, privacy: .public)")
            return []
        }
    }
    
    // MARK: - Message Caching
    
    /// Save messages to cache for a conversation
    func saveMessages(_ messages: [Message], conversationId: String) {
        let context = viewContext
        
        for message in messages {
            // Skip temp messages
            guard !message.id.hasPrefix("temp-") else { continue }
            
            let fetchRequest: NSFetchRequest<CachedMessage> = CachedMessage.fetchRequest()
            fetchRequest.predicate = NSPredicate(format: "id == %@", message.id)
            
            do {
                let existing = try context.fetch(fetchRequest).first
                let cached = existing ?? CachedMessage(context: context)
                
                cached.id = message.id
                cached.conversationId = message.conversationId
                cached.senderId = message.senderId
                cached.type = message.type.rawValue
                cached.originalContent = message.originalContent
                cached.originalLanguage = message.originalLanguage
                cached.translatedContent = message.translatedContent
                cached.targetLanguage = message.targetLanguage
                cached.status = message.status?.rawValue
                cached.createdAt = message.createdAt
                cached.readAt = message.readAt
                cached.deletedAt = message.deletedAt
                cached.deletedBy = message.deletedBy
                cached.cachedAt = Date()
                
                // Encode sender as JSON
                if let sender = message.sender,
                   let senderData = try? JSONEncoder().encode(sender) {
                    cached.senderJSON = senderData
                }
                
                // Encode reactions as JSON
                if let reactions = message.reactions,
                   let reactionsData = try? JSONEncoder().encode(reactions) {
                    cached.reactionsJSON = reactionsData
                }
                
                // Encode attachment as JSON
                if let attachment = message.attachment,
                   let attachmentData = try? JSONEncoder().encode(attachment) {
                    cached.attachmentJSON = attachmentData
                }
                
                // Encode readBy as JSON
                if let readBy = message.readBy,
                   let readByData = try? JSONEncoder().encode(readBy) {
                    cached.readByJSON = readByData
                }
                
                // Encode replyTo as JSON
                if let replyTo = message.replyTo,
                   let replyToData = try? JSONEncoder().encode(replyTo) {
                    cached.replyToJSON = replyToData
                }
            } catch {
                logger.error("❌ Failed to save message \(message.id, privacy: .public): \(error.localizedDescription, privacy: .public)")
            }
        }
        
        saveContext()
        
        // Enforce message limit per conversation
        enforceMessageLimit(conversationId: conversationId)
        
        logger.info("💾 Cached \(messages.count) messages for conversation \(conversationId, privacy: .public)")
    }
    
    /// Save a single message to cache (for WebSocket received messages)
    func saveMessage(_ message: Message) {
        // Skip temp messages
        guard !message.id.hasPrefix("temp-") else { return }
        
        saveMessages([message], conversationId: message.conversationId)
    }
    
    /// Load messages from cache for a conversation
    func loadMessages(conversationId: String) -> [Message] {
        let fetchRequest: NSFetchRequest<CachedMessage> = CachedMessage.fetchRequest()
        fetchRequest.predicate = NSPredicate(format: "conversationId == %@", conversationId)
        fetchRequest.sortDescriptors = [NSSortDescriptor(key: "createdAt", ascending: true)]
        fetchRequest.fetchLimit = Self.maxMessagesPerConversation
        
        do {
            let cachedMessages = try viewContext.fetch(fetchRequest)
            let messages = cachedMessages.compactMap { cached -> Message? in
                guard let id = cached.id,
                      let conversationId = cached.conversationId,
                      let senderId = cached.senderId,
                      let typeString = cached.type,
                      let originalContent = cached.originalContent,
                      let createdAt = cached.createdAt else { return nil }
                
                let type = MessageType(rawValue: typeString) ?? .text
                let status: MessageStatus? = cached.status.flatMap { MessageStatus(rawValue: $0) }
                
                // Decode sender
                var sender: UserPublic?
                if let senderData = cached.senderJSON {
                    sender = try? JSONDecoder().decode(UserPublic.self, from: senderData)
                }
                
                // Decode reactions
                var reactions: [String: [String]]?
                if let reactionsData = cached.reactionsJSON {
                    reactions = try? JSONDecoder().decode([String: [String]].self, from: reactionsData)
                }
                
                // Decode attachment
                var attachment: Attachment?
                if let attachmentData = cached.attachmentJSON {
                    attachment = try? JSONDecoder().decode(Attachment.self, from: attachmentData)
                }
                
                // Decode readBy
                var readBy: [String]?
                if let readByData = cached.readByJSON {
                    readBy = try? JSONDecoder().decode([String].self, from: readByData)
                }
                
                // Decode replyTo
                var replyTo: ReplyTo?
                if let replyToData = cached.replyToJSON {
                    replyTo = try? JSONDecoder().decode(ReplyTo.self, from: replyToData)
                }
                
                return Message(
                    id: id,
                    conversationId: conversationId,
                    senderId: senderId,
                    sender: sender,
                    type: type,
                    originalContent: originalContent,
                    originalLanguage: cached.originalLanguage,
                    translatedContent: cached.translatedContent,
                    targetLanguage: cached.targetLanguage,
                    status: status,
                    createdAt: createdAt,
                    reactions: reactions,
                    attachment: attachment,
                    readBy: readBy,
                    readAt: cached.readAt,
                    replyTo: replyTo,
                    deletedAt: cached.deletedAt,
                    deletedBy: cached.deletedBy
                )
            }
            
            logger.info("📂 Loaded \(messages.count) messages from cache for conversation \(conversationId, privacy: .public)")
            return messages
        } catch {
            logger.error("❌ Failed to load messages from cache: \(error.localizedDescription, privacy: .public)")
            return []
        }
    }
    
    // MARK: - Cache Management
    
    /// Enforce message limit per conversation (delete oldest beyond limit)
    private func enforceMessageLimit(conversationId: String) {
        let fetchRequest: NSFetchRequest<CachedMessage> = CachedMessage.fetchRequest()
        fetchRequest.predicate = NSPredicate(format: "conversationId == %@", conversationId)
        fetchRequest.sortDescriptors = [NSSortDescriptor(key: "createdAt", ascending: false)]
        
        do {
            let allMessages = try viewContext.fetch(fetchRequest)
            
            // If over limit, delete the oldest messages
            if allMessages.count > Self.maxMessagesPerConversation {
                let messagesToDelete = Array(allMessages.dropFirst(Self.maxMessagesPerConversation))
                for message in messagesToDelete {
                    viewContext.delete(message)
                }
                saveContext()
                logger.info("🗑️ Deleted \(messagesToDelete.count) old messages to enforce limit")
            }
        } catch {
            logger.error("❌ Failed to enforce message limit: \(error.localizedDescription, privacy: .public)")
        }
    }
    
    /// Update a single message in cache (for reactions, deletions, etc.)
    func updateMessage(_ message: Message) {
        saveMessage(message)
    }
    
    /// Delete a message from cache
    func deleteMessage(messageId: String) {
        let fetchRequest: NSFetchRequest<CachedMessage> = CachedMessage.fetchRequest()
        fetchRequest.predicate = NSPredicate(format: "id == %@", messageId)
        
        do {
            if let cached = try viewContext.fetch(fetchRequest).first {
                viewContext.delete(cached)
                saveContext()
                logger.info("🗑️ Deleted message \(messageId, privacy: .public) from cache")
            }
        } catch {
            logger.error("❌ Failed to delete message from cache: \(error.localizedDescription, privacy: .public)")
        }
    }
    
    /// Delete a conversation and all its messages from cache
    func deleteConversation(conversationId: String) {
        // Delete conversation
        let convFetchRequest: NSFetchRequest<CachedConversation> = CachedConversation.fetchRequest()
        convFetchRequest.predicate = NSPredicate(format: "id == %@", conversationId)
        
        // Delete messages
        let msgFetchRequest: NSFetchRequest<CachedMessage> = CachedMessage.fetchRequest()
        msgFetchRequest.predicate = NSPredicate(format: "conversationId == %@", conversationId)
        
        do {
            let conversations = try viewContext.fetch(convFetchRequest)
            let messages = try viewContext.fetch(msgFetchRequest)
            
            for conv in conversations {
                viewContext.delete(conv)
            }
            for msg in messages {
                viewContext.delete(msg)
            }
            
            saveContext()
            logger.info("🗑️ Deleted conversation \(conversationId, privacy: .public) and \(messages.count) messages from cache")
        } catch {
            logger.error("❌ Failed to delete conversation from cache: \(error.localizedDescription, privacy: .public)")
        }
    }
    
    /// Clear all cached data
    func clearAllCache() {
        let conversationFetch: NSFetchRequest<NSFetchRequestResult> = CachedConversation.fetchRequest()
        let messageFetch: NSFetchRequest<NSFetchRequestResult> = CachedMessage.fetchRequest()
        
        let deleteConversations = NSBatchDeleteRequest(fetchRequest: conversationFetch)
        let deleteMessages = NSBatchDeleteRequest(fetchRequest: messageFetch)
        
        do {
            try viewContext.execute(deleteConversations)
            try viewContext.execute(deleteMessages)
            saveContext()
            logger.info("🗑️ Cleared all cache")
        } catch {
            logger.error("❌ Failed to clear cache: \(error.localizedDescription, privacy: .public)")
        }
    }
}

// MARK: - Core Data Managed Object Subclasses

@objc(CachedConversation)
class CachedConversation: NSManagedObject {
    @NSManaged var id: String?
    @NSManaged var type: String?
    @NSManaged var name: String?
    @NSManaged var participantsJSON: Data?
    @NSManaged var lastMessageJSON: Data?
    @NSManaged var createdAt: String?
    @NSManaged var updatedAt: String?
    @NSManaged var unreadCount: Int32
    @NSManaged var cachedAt: Date?
    
    @nonobjc class func fetchRequest() -> NSFetchRequest<CachedConversation> {
        return NSFetchRequest<CachedConversation>(entityName: "CachedConversation")
    }
}

@objc(CachedMessage)
class CachedMessage: NSManagedObject {
    @NSManaged var id: String?
    @NSManaged var conversationId: String?
    @NSManaged var senderId: String?
    @NSManaged var senderJSON: Data?
    @NSManaged var type: String?
    @NSManaged var originalContent: String?
    @NSManaged var originalLanguage: String?
    @NSManaged var translatedContent: String?
    @NSManaged var targetLanguage: String?
    @NSManaged var status: String?
    @NSManaged var createdAt: String?
    @NSManaged var reactionsJSON: Data?
    @NSManaged var attachmentJSON: Data?
    @NSManaged var readByJSON: Data?
    @NSManaged var readAt: String?
    @NSManaged var replyToJSON: Data?
    @NSManaged var deletedAt: String?
    @NSManaged var deletedBy: String?
    @NSManaged var cachedAt: Date?
    
    @nonobjc class func fetchRequest() -> NSFetchRequest<CachedMessage> {
        return NSFetchRequest<CachedMessage>(entityName: "CachedMessage")
    }
}
