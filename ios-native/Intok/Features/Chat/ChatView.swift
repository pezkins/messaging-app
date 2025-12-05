import SwiftUI

struct ChatView: View {
    let conversationId: String
    @State private var messageText = ""
    @State private var messages: [Message] = []
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        ZStack {
            Color.surface950.ignoresSafeArea()
            
            VStack(spacing: 0) {
                // Messages
                ScrollView {
                    LazyVStack(spacing: 8) {
                        if messages.isEmpty {
                            VStack(spacing: 8) {
                                Spacer()
                                    .frame(height: 100)
                                Text("💬")
                                    .font(.system(size: 48))
                                Text("No messages yet")
                                    .foregroundColor(.surface400)
                                Text("Send a message to start the conversation")
                                    .font(.bodySmall)
                                    .foregroundColor(.surface500)
                            }
                            .frame(maxWidth: .infinity)
                        } else {
                            ForEach(messages) { message in
                                MessageBubble(message: message, isOwn: false) // TODO: Check if own
                            }
                        }
                    }
                    .padding()
                }
                
                // Input Bar
                VStack(spacing: 0) {
                    HStack(spacing: 8) {
                        // Attachment button
                        Button(action: { /* TODO */ }) {
                            Image(systemName: "plus.circle")
                                .font(.title2)
                                .foregroundColor(.surface400)
                        }
                        
                        // Text input
                        TextField("Type a message...", text: $messageText, axis: .vertical)
                            .textFieldStyle(.plain)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 10)
                            .background(Color.surface800)
                            .foregroundColor(.white)
                            .cornerRadius(20)
                            .lineLimit(1...4)
                        
                        // Send button
                        Button(action: sendMessage) {
                            Image(systemName: "arrow.up")
                                .font(.title3)
                                .foregroundColor(.white)
                                .frame(width: 36, height: 36)
                                .background(messageText.isEmpty ? Color.surface700 : Color.purple500)
                                .clipShape(Circle())
                        }
                        .disabled(messageText.isEmpty)
                    }
                    .padding(.horizontal)
                    .padding(.vertical, 8)
                    .background(Color.surface900)
                    
                    // Translation hint
                    Text("Messages translate automatically to each person's language")
                        .font(.caption)
                        .foregroundColor(.surface500)
                        .padding(.bottom, 8)
                        .frame(maxWidth: .infinity)
                        .background(Color.surface900)
                }
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .principal) {
                HStack {
                    Circle()
                        .fill(Color.purple600)
                        .frame(width: 36, height: 36)
                        .overlay(
                            Text("U")
                                .foregroundColor(.white)
                        )
                    
                    VStack(alignment: .leading) {
                        Text("User Name")
                            .font(.titleSmall)
                            .foregroundColor(.white)
                        Text("Online")
                            .font(.caption)
                            .foregroundColor(.surface400)
                    }
                }
            }
        }
    }
    
    private func sendMessage() {
        guard !messageText.isEmpty else { return }
        // TODO: Send message via WebSocket
        messageText = ""
    }
}

struct MessageBubble: View {
    let message: Message
    let isOwn: Bool
    
    var body: some View {
        HStack {
            if isOwn { Spacer() }
            
            VStack(alignment: isOwn ? .trailing : .leading, spacing: 4) {
                if !isOwn {
                    Text(message.sender.username)
                        .font(.caption)
                        .foregroundColor(.purple400)
                }
                
                Text(message.translatedContent ?? message.originalContent)
                    .foregroundColor(.white)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(isOwn ? Color.purple600 : Color.surface800)
                    .cornerRadius(20, corners: isOwn ? [.topLeft, .topRight, .bottomLeft] : [.topLeft, .topRight, .bottomRight])
                
                Text(formatTime(message.createdAt))
                    .font(.caption2)
                    .foregroundColor(isOwn ? .purple200 : .surface500)
            }
            
            if !isOwn { Spacer() }
        }
    }
    
    private func formatTime(_ dateString: String) -> String {
        // TODO: Proper date formatting
        return "12:00"
    }
}

// Helper for selective corner radius
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
    NavigationStack {
        ChatView(conversationId: "test")
    }
}

