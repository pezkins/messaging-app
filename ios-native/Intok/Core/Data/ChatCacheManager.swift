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
        container.loadPersistentStores { _, error in
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

    private var viewContext: NSManagedObjectContext { persistentContainer.viewContext }

    private init() {}

    // MARK: - Core Data Model Helpers

    private static func stringAttr(_ name: String, optional: Bool = false) -> NSAttributeDescription {
        let attr = NSAttributeDescription()
        attr.name = name
        attr.attributeType = .stringAttributeType
        attr.isOptional = optional
        return attr
    }

    private static func dataAttr(_ name: String, optional: Bool = true) -> NSAttributeDescription {
        let attr = NSAttributeDescription()
        attr.name = name
        attr.attributeType = .binaryDataAttributeType
        attr.isOptional = optional
        return attr
    }

    private static func dateAttr(_ name: String) -> NSAttributeDescription {
        let attr = NSAttributeDescription()
        attr.name = name
        attr.attributeType = .dateAttributeType
        return attr
    }

    private static func createConversationEntity() -> NSEntityDescription {
        let entity = NSEntityDescription()
        entity.name = "CachedConversation"
        entity.managedObjectClassName = NSStringFromClass(CachedConversation.self)

        let unreadAttr = NSAttributeDescription()
        unreadAttr.name = "unreadCount"
        unreadAttr.attributeType = .integer32AttributeType
        unreadAttr.defaultValue = 0
        unreadAttr.isOptional = true

        entity.properties = [
            stringAttr("id"), stringAttr("type"), stringAttr("name", optional: true),
            dataAttr("participantsJSON", optional: false), dataAttr("lastMessageJSON"),
            stringAttr("createdAt"), stringAttr("updatedAt"), unreadAttr, dateAttr("cachedAt")
        ]
        return entity
    }

    private static func createMessageEntity() -> NSEntityDescription {
        let entity = NSEntityDescription()
        entity.name = "CachedMessage"
        entity.managedObjectClassName = NSStringFromClass(CachedMessage.self)

        entity.properties = [
            stringAttr("id"), stringAttr("conversationId"), stringAttr("senderId"),
            dataAttr("senderJSON"), stringAttr("type"), stringAttr("originalContent"),
            stringAttr("originalLanguage", optional: true), stringAttr("translatedContent", optional: true),
            stringAttr("targetLanguage", optional: true), stringAttr("status", optional: true),
            stringAttr("createdAt"), dataAttr("reactionsJSON"), dataAttr("attachmentJSON"),
            dataAttr("readByJSON"), stringAttr("readAt", optional: true), dataAttr("replyToJSON"),
            stringAttr("deletedAt", optional: true), stringAttr("deletedBy", optional: true),
            dateAttr("cachedAt")
        ]
        return entity
    }

    private static func createManagedObjectModel() -> NSManagedObjectModel {
        let model = NSManagedObjectModel()
        model.entities = [createConversationEntity(), createMessageEntity()]
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

    func saveConversations(_ conversations: [Conversation]) {
        for conversation in conversations {
            saveConversation(conversation)
        }
        saveContext()
        logger.info("💾 Cached \(conversations.count) conversations")
    }

    private func saveConversation(_ conversation: Conversation) {
        let fetchRequest: NSFetchRequest<CachedConversation> = CachedConversation.fetchRequest()
        fetchRequest.predicate = NSPredicate(format: "id == %@", conversation.id)

        do {
            let existing = try viewContext.fetch(fetchRequest).first
            let cached = existing ?? CachedConversation(context: viewContext)
            cached.id = conversation.id
            cached.type = conversation.type
            cached.name = conversation.name
            cached.createdAt = conversation.createdAt
            cached.updatedAt = conversation.updatedAt
            cached.unreadCount = Int32(conversation.unreadCount ?? 0)
            cached.cachedAt = Date()
            cached.participantsJSON = try? JSONEncoder().encode(conversation.participants)
            if let lastMessage = conversation.lastMessage {
                cached.lastMessageJSON = try? JSONEncoder().encode(lastMessage)
            }
        } catch {
            logger.error("❌ Failed to save conversation: \(error.localizedDescription, privacy: .public)")
        }
    }

    func loadConversations() -> [Conversation] {
        let fetchRequest: NSFetchRequest<CachedConversation> = CachedConversation.fetchRequest()
        fetchRequest.sortDescriptors = [NSSortDescriptor(key: "updatedAt", ascending: false)]

        do {
            let cached = try viewContext.fetch(fetchRequest)
            let conversations = cached.compactMap { mapCachedConversation($0) }
            logger.info("📂 Loaded \(conversations.count) conversations from cache")
            return conversations
        } catch {
            logger.error("❌ Failed to load conversations: \(error.localizedDescription, privacy: .public)")
            return []
        }
    }

    private func mapCachedConversation(_ cached: CachedConversation) -> Conversation? {
        guard let id = cached.id, let type = cached.type,
              let createdAt = cached.createdAt, let updatedAt = cached.updatedAt else { return nil }

        let participants: [UserPublic] = cached.participantsJSON.flatMap {
            try? JSONDecoder().decode([UserPublic].self, from: $0)
        } ?? []

        let lastMessage: Message? = cached.lastMessageJSON.flatMap {
            try? JSONDecoder().decode(Message.self, from: $0)
        }

        return Conversation(
            id: id, type: type, name: cached.name, pictureUrl: nil,
            participants: participants, lastMessage: lastMessage,
            createdAt: createdAt, updatedAt: updatedAt, unreadCount: Int(cached.unreadCount)
        )
    }

    // MARK: - Message Caching

    func saveMessages(_ messages: [Message], conversationId: String) {
        for message in messages where !message.id.hasPrefix("temp-") {
            saveMessageToCache(message)
        }
        saveContext()
        enforceMessageLimit(conversationId: conversationId)
        logger.info("💾 Cached \(messages.count) messages for \(conversationId, privacy: .public)")
    }

    func saveMessage(_ message: Message) {
        guard !message.id.hasPrefix("temp-") else { return }
        saveMessageToCache(message)
        saveContext()
    }

    private func saveMessageToCache(_ message: Message) {
        let fetchRequest: NSFetchRequest<CachedMessage> = CachedMessage.fetchRequest()
        fetchRequest.predicate = NSPredicate(format: "id == %@", message.id)

        do {
            let existing = try viewContext.fetch(fetchRequest).first
            let cached = existing ?? CachedMessage(context: viewContext)
            populateCachedMessage(cached, from: message)
        } catch {
            logger.error("❌ Failed to save message: \(error.localizedDescription, privacy: .public)")
        }
    }

    private func populateCachedMessage(_ cached: CachedMessage, from message: Message) {
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
        cached.senderJSON = message.sender.flatMap { try? JSONEncoder().encode($0) }
        cached.reactionsJSON = message.reactions.flatMap { try? JSONEncoder().encode($0) }
        cached.attachmentJSON = message.attachment.flatMap { try? JSONEncoder().encode($0) }
        cached.readByJSON = message.readBy.flatMap { try? JSONEncoder().encode($0) }
        cached.replyToJSON = message.replyTo.flatMap { try? JSONEncoder().encode($0) }
    }

    func loadMessages(conversationId: String) -> [Message] {
        let fetchRequest: NSFetchRequest<CachedMessage> = CachedMessage.fetchRequest()
        fetchRequest.predicate = NSPredicate(format: "conversationId == %@", conversationId)
        fetchRequest.sortDescriptors = [NSSortDescriptor(key: "createdAt", ascending: true)]
        fetchRequest.fetchLimit = Self.maxMessagesPerConversation

        do {
            let cached = try viewContext.fetch(fetchRequest)
            let messages = cached.compactMap { mapCachedMessage($0) }
            logger.info("📂 Loaded \(messages.count) messages from cache")
            return messages
        } catch {
            logger.error("❌ Failed to load messages: \(error.localizedDescription, privacy: .public)")
            return []
        }
    }

    private func mapCachedMessage(_ cached: CachedMessage) -> Message? {
        guard let id = cached.id, let conversationId = cached.conversationId,
              let senderId = cached.senderId, let typeString = cached.type,
              let originalContent = cached.originalContent, let createdAt = cached.createdAt else { return nil }

        return Message(
            id: id, conversationId: conversationId, senderId: senderId,
            sender: cached.senderJSON.flatMap { try? JSONDecoder().decode(UserPublic.self, from: $0) },
            type: MessageType(rawValue: typeString) ?? .text,
            originalContent: originalContent, originalLanguage: cached.originalLanguage,
            translatedContent: cached.translatedContent, targetLanguage: cached.targetLanguage,
            status: cached.status.flatMap { MessageStatus(rawValue: $0) },
            createdAt: createdAt,
            reactions: cached.reactionsJSON.flatMap { try? JSONDecoder().decode([String: [String]].self, from: $0) },
            attachment: cached.attachmentJSON.flatMap { try? JSONDecoder().decode(Attachment.self, from: $0) },
            readBy: cached.readByJSON.flatMap { try? JSONDecoder().decode([String].self, from: $0) },
            readAt: cached.readAt,
            replyTo: cached.replyToJSON.flatMap { try? JSONDecoder().decode(ReplyTo.self, from: $0) },
            deletedAt: cached.deletedAt, deletedBy: cached.deletedBy
        )
    }

    // MARK: - Cache Management

    private func enforceMessageLimit(conversationId: String) {
        let fetchRequest: NSFetchRequest<CachedMessage> = CachedMessage.fetchRequest()
        fetchRequest.predicate = NSPredicate(format: "conversationId == %@", conversationId)
        fetchRequest.sortDescriptors = [NSSortDescriptor(key: "createdAt", ascending: false)]

        do {
            let allMessages = try viewContext.fetch(fetchRequest)
            if allMessages.count > Self.maxMessagesPerConversation {
                let toDelete = Array(allMessages.dropFirst(Self.maxMessagesPerConversation))
                toDelete.forEach { viewContext.delete($0) }
                saveContext()
                logger.info("🗑️ Deleted \(toDelete.count) old messages")
            }
        } catch {
            logger.error("❌ Failed to enforce limit: \(error.localizedDescription, privacy: .public)")
        }
    }

    func updateMessage(_ message: Message) { saveMessage(message) }

    func deleteMessage(messageId: String) {
        let fetchRequest: NSFetchRequest<CachedMessage> = CachedMessage.fetchRequest()
        fetchRequest.predicate = NSPredicate(format: "id == %@", messageId)

        do {
            if let cached = try viewContext.fetch(fetchRequest).first {
                viewContext.delete(cached)
                saveContext()
                logger.info("🗑️ Deleted message \(messageId, privacy: .public)")
            }
        } catch {
            logger.error("❌ Failed to delete message: \(error.localizedDescription, privacy: .public)")
        }
    }

    func deleteConversation(conversationId: String) {
        let convRequest: NSFetchRequest<CachedConversation> = CachedConversation.fetchRequest()
        convRequest.predicate = NSPredicate(format: "id == %@", conversationId)

        let msgRequest: NSFetchRequest<CachedMessage> = CachedMessage.fetchRequest()
        msgRequest.predicate = NSPredicate(format: "conversationId == %@", conversationId)

        do {
            try viewContext.fetch(convRequest).forEach { viewContext.delete($0) }
            try viewContext.fetch(msgRequest).forEach { viewContext.delete($0) }
            saveContext()
            logger.info("🗑️ Deleted conversation \(conversationId, privacy: .public)")
        } catch {
            logger.error("❌ Failed to delete conversation: \(error.localizedDescription, privacy: .public)")
        }
    }

    func clearAllCache() {
        let convFetch: NSFetchRequest<NSFetchRequestResult> = CachedConversation.fetchRequest()
        let msgFetch: NSFetchRequest<NSFetchRequestResult> = CachedMessage.fetchRequest()

        do {
            try viewContext.execute(NSBatchDeleteRequest(fetchRequest: convFetch))
            try viewContext.execute(NSBatchDeleteRequest(fetchRequest: msgFetch))
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
