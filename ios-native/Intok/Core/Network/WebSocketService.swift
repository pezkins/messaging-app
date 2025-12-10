import Foundation
import os.log

private let logger = Logger(subsystem: "com.pezkins.intok", category: "WebSocket")

// Helper for NSLog that shows in system log
private func wsLog(_ message: String) {
    NSLog("🔌 [WS] %@", message)
}

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

// MARK: - Pending Message for retry
private struct PendingMessage {
    let action: String
    let data: [String: Any]
    let retryCount: Int
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
    
    // Pending messages queue for retry after reconnect
    private var pendingMessages: [PendingMessage] = []
    private let maxRetries = 3
    
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
        wsLog("connect() called")
        
        // Check if already connected and running
        if let existingSocket = webSocket {
            let state = existingSocket.state
            if state == .running {
                wsLog("Already connected and running")
                return
            }
            // Socket exists but not running - clean it up
            wsLog("Existing socket in state: \(String(describing: state)), cleaning up...")
            existingSocket.cancel(with: .goingAway, reason: nil)
            webSocket = nil
        }
        
        self.token = token
        
        guard URL(string: "\(wsURL)?token=\(token.prefix(20))...") != nil else {
            wsLog("Invalid URL")
            return
        }
        
        wsLog("Connecting to: \(wsURL)")
        
        let session = URLSession(configuration: .default)
        let fullURL = URL(string: "\(wsURL)?token=\(token)")!
        webSocket = session.webSocketTask(with: fullURL)
        webSocket?.resume()
        
        wsLog("Task resumed, starting receive loop")
        receiveMessage()
        
        // Check connection after a short delay
        DispatchQueue.main.asyncAfter(deadline: .now() + 1) { [weak self] in
            guard let self = self else { return }
            let state = self.webSocket?.state
            wsLog("State after 1s: \(String(describing: state))")
            
            if state == .running {
                self.isConnected = true
                self.reconnectAttempts = 0
                self.onConnected?()
                wsLog("✅ Connected successfully!")
                
                // Retry any pending messages
                self.retryPendingMessages()
            } else {
                wsLog("❌ Not running after 1s, state: \(String(describing: state))")
            }
        }
    }
    
    private func retryPendingMessages() {
        guard !pendingMessages.isEmpty else { return }
        
        wsLog("Retrying \(pendingMessages.count) pending messages...")
        let messages = pendingMessages
        pendingMessages = []
        
        for pending in messages {
            if pending.retryCount < maxRetries {
                wsLog("Retrying message: \(pending.action) (attempt \(pending.retryCount + 1))")
                sendInternal(action: pending.action, data: pending.data, retryCount: pending.retryCount + 1)
            } else {
                wsLog("Dropping message after \(maxRetries) retries: \(pending.action)")
            }
        }
    }
    
    func disconnect() {
        wsLog("Disconnecting WebSocket...")
        webSocket?.cancel(with: .goingAway, reason: nil)
        webSocket = nil
        token = nil
        reconnectAttempts = 0
        isConnected = false
        pendingMessages = []  // Clear pending messages on intentional disconnect
        onDisconnected?()
    }
    
    private func handleReconnect() {
        guard reconnectAttempts < maxReconnectAttempts else {
            wsLog("⚠️ Max reconnect attempts reached (\(maxReconnectAttempts))")
            return
        }
        
        guard let token = token else {
            wsLog("❌ No token available for reconnect")
            return
        }
        
        reconnectAttempts += 1
        let delay = baseReconnectDelay * pow(2, Double(reconnectAttempts - 1))
        
        wsLog("🔄 Reconnecting in \(delay)s (attempt \(reconnectAttempts)/\(maxReconnectAttempts))")
        
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
                wsLog("📨 Received message")
                switch message {
                case .string(let text):
                    wsLog("Received text: \(text.prefix(300))...")
                    self?.handleMessage(text)
                case .data(let data):
                    wsLog("Received data: \(data.count) bytes")
                    if let text = String(data: data, encoding: .utf8) {
                        self?.handleMessage(text)
                    }
                @unknown default:
                    break
                }
                // Continue receiving
                self?.receiveMessage()
                
            case .failure(let error):
                wsLog("❌ Receive error: \(error.localizedDescription)")
                DispatchQueue.main.async {
                    self?.isConnected = false
                    self?.onDisconnected?()
                }
                self?.handleReconnect()
            }
        }
    }
    
    private func handleMessage(_ text: String) {
        guard let data = text.data(using: .utf8) else {
            wsLog("❌ Failed to convert text to data")
            return
        }
        
        do {
            if let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
               let action = json["action"] as? String {
                
                wsLog("📥 Received action: \(action)")
                
                switch action {
                case WebSocketEvent.messageReceive.rawValue:
                    do {
                        let messageData = try JSONDecoder().decode(MessageReceiveData.self, from: data)
                        wsLog("✅ Decoded message:receive - id: \(messageData.message.id), tempId: \(messageData.tempId ?? "none")")
                        DispatchQueue.main.async {
                            self.onMessageReceive?(messageData)
                        }
                    } catch {
                        wsLog("❌ Failed to decode message:receive - \(error)")
                        // Log the raw JSON for debugging
                        if let messageJson = json["message"] {
                            wsLog("Raw message JSON: \(String(describing: messageJson).prefix(500))")
                        }
                    }
                    
                case WebSocketEvent.messageTyping.rawValue:
                    do {
                        let typingData = try JSONDecoder().decode(TypingData.self, from: data)
                        DispatchQueue.main.async {
                            self.onTyping?(typingData)
                        }
                    } catch {
                        wsLog("❌ Failed to decode message:typing - \(error)")
                    }
                    
                case WebSocketEvent.messageReaction.rawValue:
                    do {
                        let reactionData = try JSONDecoder().decode(ReactionData.self, from: data)
                        DispatchQueue.main.async {
                            self.onReaction?(reactionData)
                        }
                    } catch {
                        wsLog("❌ Failed to decode message:reaction - \(error)")
                    }
                    
                default:
                    wsLog("Unhandled action: \(action)")
                }
            } else {
                wsLog("⚠️ Received non-action message: \(text.prefix(200))")
            }
        } catch {
            wsLog("❌ Failed to parse message JSON: \(error.localizedDescription)")
        }
    }
    
    // MARK: - Send Messages
    private func send(action: String, data: [String: Any]) {
        sendInternal(action: action, data: data, retryCount: 0)
    }
    
    private func sendInternal(action: String, data: [String: Any], retryCount: Int) {
        wsLog("send() called for action: \(action)")
        
        guard let webSocket = webSocket else {
            wsLog("❌ Cannot send '\(action)' - webSocket is nil")
            // Queue for retry and attempt reconnect
            queueMessageAndReconnect(action: action, data: data, retryCount: retryCount)
            return
        }
        
        // Check connection state
        let state = webSocket.state
        wsLog("Current state: \(String(describing: state))")
        
        guard state == .running else {
            wsLog("❌ Cannot send '\(action)' - state is \(String(describing: state))")
            // Queue for retry and attempt reconnect
            queueMessageAndReconnect(action: action, data: data, retryCount: retryCount)
            return
        }
        
        let payload: [String: Any] = [
            "action": action,
            "data": data
        ]
        
        guard let jsonData = try? JSONSerialization.data(withJSONObject: payload),
              let jsonString = String(data: jsonData, encoding: .utf8) else {
            wsLog("❌ Failed to serialize message")
            return
        }
        
        wsLog("Sending payload: \(jsonString.prefix(200))...")
        
        webSocket.send(.string(jsonString)) { [weak self] error in
            if let error = error {
                wsLog("❌ Send error: \(error.localizedDescription)")
                // Queue for retry on send failure
                self?.pendingMessages.append(PendingMessage(action: action, data: data, retryCount: retryCount))
            } else {
                wsLog("✅ Message sent successfully: \(action)")
            }
        }
    }
    
    private func queueMessageAndReconnect(action: String, data: [String: Any], retryCount: Int) {
        // Queue the message for retry
        if retryCount < maxRetries {
            pendingMessages.append(PendingMessage(action: action, data: data, retryCount: retryCount))
            wsLog("Queued message for retry: \(action) (will be attempt \(retryCount + 1))")
        } else {
            wsLog("Message exceeded max retries, dropping: \(action)")
        }
        
        // Attempt reconnect if we have a token
        if let token = self.token {
            wsLog("Attempting to reconnect...")
            connect(token: token)
        }
    }
    
    func sendMessage(conversationId: String, content: String, type: String = "text", tempId: String? = nil, attachment: [String: Any]? = nil, translateDocument: Bool? = nil, replyTo: [String: Any]? = nil) {
        wsLog("sendMessage called - conversationId: \(conversationId), content: \(content.prefix(50)), type: \(type)")
        wsLog("isConnected: \(isConnected), webSocket state: \(String(describing: webSocket?.state))")
        
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
        
        if let translateDocument = translateDocument {
            data["translateDocument"] = translateDocument
        }
        
        if let replyTo = replyTo {
            data["replyTo"] = replyTo
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


