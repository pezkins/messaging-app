import Foundation
import os.log

private let logger = Logger(subsystem: "com.pezkins.intok", category: "WebSocket")

// MARK: - WebSocket Events
enum WebSocketEvent: String {
    case messageReceive = "message:receive"
    case messageTyping = "message:typing"
    case messageReaction = "message:reaction"
    case messageSend = "message:send"
    case messageRead = "message:read"
    case conversationJoin = "conversation:join"
    case conversationLeave = "conversation:leave"
}

// MARK: - Event Data Types
struct MessageReceiveData: Codable {
    let message: Message
    let tempId: String?
}

struct TypingData: Codable {
    let conversationId: String
    let userId: String
    let isTyping: Bool
}

struct ReactionData: Codable {
    let conversationId: String
    let messageId: String
    let messageTimestamp: String
    let reactions: [String: [String]]
    let userId: String
    let emoji: String
}

// MARK: - WebSocket Service
class WebSocketService: ObservableObject {
    static let shared = WebSocketService()
    
    private var webSocket: URLSessionWebSocketTask?
    private var token: String?
    private var reconnectAttempts = 0
    private let maxReconnectAttempts = 5
    private let baseReconnectDelay: TimeInterval = 1.0
    
    private let wsURL: String
    
    // Event handlers
    var onMessageReceive: ((MessageReceiveData) -> Void)?
    var onTyping: ((TypingData) -> Void)?
    var onReaction: ((ReactionData) -> Void)?
    var onConnected: (() -> Void)?
    var onDisconnected: (() -> Void)?
    
    @Published var isConnected = false
    
    private init() {
        if let url = Bundle.main.object(forInfoDictionaryKey: "WS_URL") as? String {
            wsURL = url
        } else {
            wsURL = "wss://ksupcb7ucf.execute-api.us-east-1.amazonaws.com/prod"
        }
        logger.info("🔌 WebSocket Service initialized with: \(self.wsURL, privacy: .public)")
    }
    
    // MARK: - Connection Management
    func connect(token: String) {
        guard webSocket == nil else {
            logger.info("🔌 WebSocket already connected or connecting")
            return
        }
        
        self.token = token
        
        guard let url = URL(string: "\(wsURL)?token=\(token)") else {
            logger.error("🔌 Invalid WebSocket URL")
            return
        }
        
        logger.info("🔌 Connecting to WebSocket...")
        
        let session = URLSession(configuration: .default)
        webSocket = session.webSocketTask(with: url)
        webSocket?.resume()
        
        receiveMessage()
        
        // Check connection after a short delay
        DispatchQueue.main.asyncAfter(deadline: .now() + 1) { [weak self] in
            if self?.webSocket?.state == .running {
                self?.isConnected = true
                self?.reconnectAttempts = 0
                self?.onConnected?()
                logger.info("🔌 WebSocket connected")
            }
        }
    }
    
    func disconnect() {
        logger.info("🔌 Disconnecting WebSocket...")
        webSocket?.cancel(with: .goingAway, reason: nil)
        webSocket = nil
        token = nil
        reconnectAttempts = 0
        isConnected = false
        onDisconnected?()
    }
    
    private func handleReconnect() {
        guard reconnectAttempts < maxReconnectAttempts else {
            logger.warning("🔌 Max reconnect attempts reached")
            return
        }
        
        guard let token = token else { return }
        
        reconnectAttempts += 1
        let delay = baseReconnectDelay * pow(2, Double(reconnectAttempts - 1))
        
        logger.info("🔌 Reconnecting in \(delay)s (attempt \(self.reconnectAttempts))")
        
        DispatchQueue.main.asyncAfter(deadline: .now() + delay) { [weak self] in
            self?.webSocket = nil
            self?.connect(token: token)
        }
    }
    
    // MARK: - Receive Messages
    private func receiveMessage() {
        webSocket?.receive { [weak self] result in
            switch result {
            case .success(let message):
                switch message {
                case .string(let text):
                    self?.handleMessage(text)
                case .data(let data):
                    if let text = String(data: data, encoding: .utf8) {
                        self?.handleMessage(text)
                    }
                @unknown default:
                    break
                }
                // Continue receiving
                self?.receiveMessage()
                
            case .failure(let error):
                logger.error("🔌 WebSocket receive error: \(error.localizedDescription, privacy: .public)")
                DispatchQueue.main.async {
                    self?.isConnected = false
                    self?.onDisconnected?()
                }
                self?.handleReconnect()
            }
        }
    }
    
    private func handleMessage(_ text: String) {
        guard let data = text.data(using: .utf8) else { return }
        
        do {
            if let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
               let action = json["action"] as? String {
                
                logger.debug("🔌 Received: \(action, privacy: .public)")
                
                switch action {
                case WebSocketEvent.messageReceive.rawValue:
                    if let messageData = try? JSONDecoder().decode(MessageReceiveData.self, from: data) {
                        DispatchQueue.main.async {
                            self.onMessageReceive?(messageData)
                        }
                    }
                    
                case WebSocketEvent.messageTyping.rawValue:
                    if let typingData = try? JSONDecoder().decode(TypingData.self, from: data) {
                        DispatchQueue.main.async {
                            self.onTyping?(typingData)
                        }
                    }
                    
                case WebSocketEvent.messageReaction.rawValue:
                    if let reactionData = try? JSONDecoder().decode(ReactionData.self, from: data) {
                        DispatchQueue.main.async {
                            self.onReaction?(reactionData)
                        }
                    }
                    
                default:
                    logger.debug("🔌 Unhandled action: \(action, privacy: .public)")
                }
            }
        } catch {
            logger.error("🔌 Failed to parse message: \(error.localizedDescription, privacy: .public)")
        }
    }
    
    // MARK: - Send Messages
    private func send(action: String, data: [String: Any]) {
        guard let webSocket = webSocket else {
            logger.warning("🔌 Cannot send - not connected")
            return
        }
        
        let payload: [String: Any] = [
            "action": action,
            "data": data
        ]
        
        guard let jsonData = try? JSONSerialization.data(withJSONObject: payload),
              let jsonString = String(data: jsonData, encoding: .utf8) else {
            return
        }
        
        webSocket.send(.string(jsonString)) { [weak self] error in
            if let error = error {
                logger.error("🔌 Send error: \(error.localizedDescription, privacy: .public)")
            } else {
                logger.debug("🔌 Sent: \(action, privacy: .public)")
            }
        }
    }
    
    func sendMessage(conversationId: String, content: String, type: String = "TEXT", tempId: String? = nil, attachment: [String: Any]? = nil) {
        var data: [String: Any] = [
            "conversationId": conversationId,
            "content": content,
            "type": type
        ]
        
        if let tempId = tempId {
            data["tempId"] = tempId
        }
        
        if let attachment = attachment {
            data["attachment"] = attachment
        }
        
        send(action: WebSocketEvent.messageSend.rawValue, data: data)
    }
    
    func sendTyping(conversationId: String, isTyping: Bool) {
        send(action: WebSocketEvent.messageTyping.rawValue, data: [
            "conversationId": conversationId,
            "isTyping": isTyping
        ])
    }
    
    func markAsRead(conversationId: String, messageId: String) {
        send(action: WebSocketEvent.messageRead.rawValue, data: [
            "conversationId": conversationId,
            "messageId": messageId
        ])
    }
    
    func joinConversation(_ conversationId: String) {
        send(action: WebSocketEvent.conversationJoin.rawValue, data: ["conversationId": conversationId])
    }
    
    func leaveConversation(_ conversationId: String) {
        send(action: WebSocketEvent.conversationLeave.rawValue, data: ["conversationId": conversationId])
    }
    
    func sendReaction(conversationId: String, messageId: String, messageTimestamp: String, emoji: String) {
        send(action: WebSocketEvent.messageReaction.rawValue, data: [
            "conversationId": conversationId,
            "messageId": messageId,
            "messageTimestamp": messageTimestamp,
            "emoji": emoji
        ])
    }
}
