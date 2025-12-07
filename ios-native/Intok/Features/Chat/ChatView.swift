import SwiftUI

struct ChatView: View {
    let conversation: Conversation
    @EnvironmentObject var authManager: AuthManager
    @StateObject private var chatStore = ChatStore.shared
    @Environment(\.dismiss) var dismiss
    
    @State private var messageText = ""
    @State private var showingAttachmentOptions = false
    @FocusState private var isInputFocused: Bool
    
    var displayName: String {
        if conversation.type == "group" {
            return conversation.name ?? "Group Chat"
        }
        if let otherUser = conversation.participants.first(where: { $0.id != authManager.currentUser?.id }) {
            return otherUser.username
        }
        return "Chat"
    }
    
    var body: some View {
        ZStack {
            // Background
            Color(hex: "0F0F0F")
                .ignoresSafeArea()
            
            VStack(spacing: 0) {
                // Messages
                messagesView
                
                // Typing Indicator
                if let typingUsers = chatStore.typingUsers[conversation.id], !typingUsers.isEmpty {
                    typingIndicator
                }
                
                // Input
                inputBar
            }
        }
        .navigationTitle(displayName)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Menu {
                    Button(action: {}) {
                        Label("View Profile", systemImage: "person")
                    }
                    Button(action: {}) {
                        Label("Search in Chat", systemImage: "magnifyingglass")
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                        .foregroundColor(Color(hex: "8B5CF6"))
                }
            }
        }
        .task {
            await chatStore.selectConversation(conversation)
        }
        .onDisappear {
            chatStore.clearActiveConversation()
        }
    }
    
    // MARK: - Messages View
    var messagesView: some View {
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(spacing: 4) {
                    if chatStore.hasMoreMessages {
                        Button("Load more...") {
                            Task { await chatStore.loadMoreMessages() }
                        }
                        .foregroundColor(Color(hex: "8B5CF6"))
                        .padding()
                    }
                    
                    ForEach(chatStore.messages) { message in
                        MessageBubble(
                            message: message,
                            isOwnMessage: message.senderId == authManager.currentUser?.id || message.id.hasPrefix("temp-")
                        )
                        .id(message.id)
                    }
                }
                .padding()
            }
            .onChange(of: chatStore.messages.count) { _ in
                if let lastId = chatStore.messages.last?.id {
                    withAnimation {
                        proxy.scrollTo(lastId, anchor: .bottom)
                    }
                }
            }
        }
    }
    
    // MARK: - Typing Indicator
    var typingIndicator: some View {
        HStack {
            Text("Someone is typing...")
                .font(.caption)
                .foregroundColor(.gray)
                .italic()
            
            HStack(spacing: 4) {
                ForEach(0..<3) { i in
                    Circle()
                        .fill(Color.gray)
                        .frame(width: 6, height: 6)
                        .animation(
                            Animation.easeInOut(duration: 0.6)
                                .repeatForever()
                                .delay(Double(i) * 0.2),
                            value: true
                        )
                }
            }
            
            Spacer()
        }
        .padding(.horizontal)
        .padding(.vertical, 8)
    }
    
    // MARK: - Input Bar
    var inputBar: some View {
        VStack(spacing: 0) {
            Divider()
                .background(Color.white.opacity(0.1))
            
            HStack(spacing: 12) {
                // Attachment Button
                Button(action: { showingAttachmentOptions = true }) {
                    Image(systemName: "plus.circle.fill")
                        .font(.title2)
                        .foregroundColor(Color(hex: "8B5CF6"))
                }
                
                // Text Field
                TextField("Message...", text: $messageText, axis: .vertical)
                    .textFieldStyle(.plain)
                    .padding(12)
                    .background(Color.white.opacity(0.1))
                    .cornerRadius(20)
                    .foregroundColor(.white)
                    .lineLimit(5)
                    .focused($isInputFocused)
                    .onChange(of: messageText) { text in
                        chatStore.setTyping(!text.isEmpty)
                    }
                
                // Send Button
                Button(action: sendMessage) {
                    Image(systemName: "arrow.up.circle.fill")
                        .font(.title)
                        .foregroundColor(
                            messageText.trimmingCharacters(in: .whitespaces).isEmpty ?
                            Color.gray : Color(hex: "8B5CF6")
                        )
                }
                .disabled(messageText.trimmingCharacters(in: .whitespaces).isEmpty)
            }
            .padding()
            .background(Color(hex: "1A1A1A"))
        }
    }
    
    // MARK: - Actions
    func sendMessage() {
        let text = messageText.trimmingCharacters(in: .whitespaces)
        guard !text.isEmpty else { return }
        
        chatStore.sendMessage(text)
        messageText = ""
        chatStore.setTyping(false)
    }
}

// MARK: - Message Bubble
struct MessageBubble: View {
    let message: Message
    let isOwnMessage: Bool
    
    @State private var showTranslation = false
    
    var body: some View {
        HStack(alignment: .bottom, spacing: 8) {
            if isOwnMessage {
                Spacer(minLength: 60)
            } else {
                // Avatar
                Circle()
                    .fill(Color(hex: "8B5CF6"))
                    .frame(width: 28, height: 28)
                    .overlay(
                        Text(String(message.sender.username.prefix(1).uppercased()))
                            .font(.caption2)
                            .foregroundColor(.white)
                    )
            }
            
            VStack(alignment: isOwnMessage ? .trailing : .leading, spacing: 4) {
                // Sender name for group chats
                if !isOwnMessage {
                    Text(message.sender.username)
                        .font(.caption)
                        .foregroundColor(.gray)
                }
                
                // Message Content
                VStack(alignment: .leading, spacing: 4) {
                    // Original or Translated Content
                    Text(showTranslation ? (message.translatedContent ?? message.originalContent) : message.originalContent)
                        .foregroundColor(isOwnMessage ? .white : .white)
                    
                    // Translation toggle if available
                    if message.translatedContent != nil {
                        Button(action: { showTranslation.toggle() }) {
                            HStack(spacing: 4) {
                                Image(systemName: "globe")
                                Text(showTranslation ? "Original" : "Translate")
                            }
                            .font(.caption2)
                            .foregroundColor(isOwnMessage ? .white.opacity(0.7) : Color(hex: "8B5CF6"))
                        }
                    }
                }
                .padding(12)
                .background(
                    isOwnMessage ?
                    Color(hex: "8B5CF6") :
                    Color.white.opacity(0.1)
                )
                .cornerRadius(16, corners: isOwnMessage ? [.topLeft, .topRight, .bottomLeft] : [.topLeft, .topRight, .bottomRight])
                
                // Status and Time
                HStack(spacing: 4) {
                    Text(formatTime(message.createdAt))
                        .font(.caption2)
                        .foregroundColor(.gray)
                    
                    if isOwnMessage {
                        Image(systemName: statusIcon)
                            .font(.caption2)
                            .foregroundColor(statusColor)
                    }
                }
                
                // Reactions
                if let reactions = message.reactions, !reactions.isEmpty {
                    HStack(spacing: 4) {
                        ForEach(Array(reactions.keys), id: \.self) { emoji in
                            if let users = reactions[emoji], !users.isEmpty {
                                HStack(spacing: 2) {
                                    Text(emoji)
                                    Text("\(users.count)")
                                        .font(.caption2)
                                        .foregroundColor(.gray)
                                }
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(Color.white.opacity(0.1))
                                .cornerRadius(12)
                            }
                        }
                    }
                }
            }
            
            if !isOwnMessage {
                Spacer(minLength: 60)
            }
        }
        .padding(.vertical, 2)
    }
    
    var statusIcon: String {
        switch message.status {
        case .sending:
            return "clock"
        case .sent:
            return "checkmark"
        case .delivered:
            return "checkmark.circle"
        case .seen:
            return "checkmark.circle.fill"
        case .failed:
            return "exclamationmark.circle"
        }
    }
    
    var statusColor: Color {
        switch message.status {
        case .failed:
            return .red
        case .seen:
            return Color(hex: "8B5CF6")
        default:
            return .gray
        }
    }
    
    func formatTime(_ isoString: String) -> String {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        
        guard let date = formatter.date(from: isoString) else {
            formatter.formatOptions = [.withInternetDateTime]
            guard let date = formatter.date(from: isoString) else {
                return ""
            }
            return formatTimeFromDate(date)
        }
        
        return formatTimeFromDate(date)
    }
    
    func formatTimeFromDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "h:mm a"
        return formatter.string(from: date)
    }
}

// MARK: - Corner Radius Extension
extension View {
    func cornerRadius(_ radius: CGFloat, corners: UIRectCorner) -> some View {
        clipShape(RoundedCorner(radius: radius, corners: corners))
    }
}

struct RoundedCorner: Shape {
    var radius: CGFloat = .infinity
    var corners: UIRectCorner = .allCorners
    
    func path(in rect: CGRect) -> Path {
        let path = UIBezierPath(
            roundedRect: rect,
            byRoundingCorners: corners,
            cornerRadii: CGSize(width: radius, height: radius)
        )
        return Path(path.cgPath)
    }
}

#Preview {
    ChatView(conversation: Conversation(
        id: "1",
        type: "direct",
        name: nil,
        participants: [UserPublic(id: "1", username: "Test User", preferredLanguage: "en", avatarUrl: nil)],
        lastMessage: nil,
        createdAt: "",
        updatedAt: ""
    ))
    .environmentObject(AuthManager.shared)
}
