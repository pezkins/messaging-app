import SwiftUI

// MARK: - Reply Preview Bar (Composing)
struct ReplyPreviewBar: View {
    let replyTo: Message
    let onCancel: () -> Void
    
    var body: some View {
        HStack(spacing: 12) {
            // Purple accent bar
            Rectangle()
                .fill(Color(hex: "8B5CF6"))
                .frame(width: 3)
                .cornerRadius(1.5)
            
            VStack(alignment: .leading, spacing: 2) {
                // Sender name
                Text(replyTo.sender?.username ?? "Unknown")
                    .font(.caption)
                    .fontWeight(.semibold)
                    .foregroundColor(Color(hex: "8B5CF6"))
                
                // Message preview
                Text(replyPreviewText)
                    .font(.caption)
                    .foregroundColor(.gray)
                    .lineLimit(1)
            }
            
            Spacer()
            
            // Cancel button
            Button {
                onCancel()
            } label: {
                Image(systemName: "xmark")
                    .font(.caption)
                    .foregroundColor(.gray)
                    .padding(6)
                    .background(Color.white.opacity(0.1))
                    .clipShape(Circle())
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(Color(hex: "1A1A1A"))
    }
    
    private var replyPreviewText: String {
        switch replyTo.type {
        case .image:
            return "📷 Photo"
        case .gif:
            return "GIF"
        case .file:
            return "📄 \(replyTo.attachment?.fileName ?? "Document")"
        case .voice:
            return "🎤 Voice message"
        default:
            return replyTo.originalContent
        }
    }
}

// MARK: - Quoted Message View (In Bubble)
struct QuotedMessageView: View {
    let replyTo: ReplyTo
    let isOwnMessage: Bool
    let onTap: (() -> Void)?
    
    var body: some View {
        Button {
            onTap?()
        } label: {
            HStack(spacing: 8) {
                // Purple accent bar
                Rectangle()
                    .fill(Color(hex: "8B5CF6"))
                    .frame(width: 3)
                    .cornerRadius(1.5)
                
                VStack(alignment: .leading, spacing: 2) {
                    // Sender name
                    Text(replyTo.senderName)
                        .font(.caption2)
                        .fontWeight(.semibold)
                        .foregroundColor(Color(hex: "8B5CF6"))
                    
                    // Content preview
                    Text(contentPreview)
                        .font(.caption2)
                        .foregroundColor(isOwnMessage ? .white.opacity(0.8) : .gray)
                        .lineLimit(2)
                }
                
                Spacer(minLength: 0)
            }
            .padding(8)
            .background(
                isOwnMessage ?
                Color.white.opacity(0.1) :
                Color.black.opacity(0.1)
            )
            .cornerRadius(8)
        }
        .buttonStyle(.plain)
    }
    
    private var contentPreview: String {
        switch replyTo.type {
        case .image:
            return "📷 Photo"
        case .gif:
            return "GIF"
        case .file:
            return "📄 Document"
        case .voice:
            return "🎤 Voice message"
        default:
            // Truncate long content
            let content = replyTo.content
            if content.count > 100 {
                return String(content.prefix(100)) + "..."
            }
            return content
        }
    }
}

#Preview("Reply Preview Bar") {
    VStack {
        ReplyPreviewBar(
            replyTo: Message(
                id: "1",
                conversationId: "conv1",
                senderId: "user1",
                sender: UserPublic(id: "user1", username: "John", preferredLanguage: "en", avatarUrl: nil),
                type: .text,
                originalContent: "Hey, how are you doing today?",
                originalLanguage: "en",
                createdAt: ""
            ),
            onCancel: {}
        )
        
        Spacer()
    }
    .background(Color(hex: "0F0F0F"))
}

#Preview("Quoted Message View") {
    VStack(spacing: 20) {
        // Own message
        QuotedMessageView(
            replyTo: ReplyTo(
                messageId: "1",
                content: "Hey, how are you doing today? This is a longer message to test truncation.",
                senderId: "user1",
                senderName: "John",
                type: .text
            ),
            isOwnMessage: true,
            onTap: nil
        )
        .padding()
        .background(Color(hex: "8B5CF6"))
        .cornerRadius(16)
        
        // Other message
        QuotedMessageView(
            replyTo: ReplyTo(
                messageId: "1",
                content: "Photo message",
                senderId: "user1",
                senderName: "John",
                type: .image
            ),
            isOwnMessage: false,
            onTap: nil
        )
        .padding()
        .background(Color.white.opacity(0.1))
        .cornerRadius(16)
    }
    .padding()
    .background(Color(hex: "0F0F0F"))
}
