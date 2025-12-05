import SwiftUI

struct ConversationsView: View {
    @State private var conversations: [Conversation] = []
    @State private var showSettings = false
    @State private var showNewChat = false
    
    var body: some View {
        ZStack {
            Color.surface950.ignoresSafeArea()
            
            if conversations.isEmpty {
                // Empty state
                VStack(spacing: 16) {
                    Text("💬")
                        .font(.system(size: 64))
                    
                    Text("No conversations yet")
                        .font(.titleLarge)
                        .foregroundColor(.surface400)
                    
                    Text("Start chatting with someone!")
                        .font(.bodyMedium)
                        .foregroundColor(.surface500)
                }
            } else {
                List(conversations) { conversation in
                    NavigationLink(destination: ChatView(conversationId: conversation.id)) {
                        ConversationRow(conversation: conversation)
                    }
                    .listRowBackground(Color.surface950)
                    .listRowSeparator(.hidden)
                }
                .listStyle(.plain)
                .scrollContentBackground(.hidden)
            }
        }
        .navigationTitle("Messages")
        .navigationBarTitleDisplayMode(.large)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button(action: { showSettings = true }) {
                    Image(systemName: "gearshape")
                        .foregroundColor(.white)
                }
            }
        }
        .overlay(alignment: .bottomTrailing) {
            // FAB
            Button(action: { showNewChat = true }) {
                Image(systemName: "plus")
                    .font(.title2)
                    .foregroundColor(.white)
                    .frame(width: 56, height: 56)
                    .background(Color.purple500)
                    .clipShape(Circle())
                    .shadow(radius: 4)
            }
            .padding()
        }
        .sheet(isPresented: $showSettings) {
            SettingsView()
        }
        .sheet(isPresented: $showNewChat) {
            // TODO: New chat view
            Text("New Chat")
        }
    }
}

struct ConversationRow: View {
    let conversation: Conversation
    
    var displayName: String {
        conversation.name ?? conversation.participants.first?.username ?? "Unknown"
    }
    
    var body: some View {
        HStack(spacing: 12) {
            // Avatar
            Circle()
                .fill(Color.purple600)
                .frame(width: 56, height: 56)
                .overlay(
                    Text(displayName.prefix(1).uppercased())
                        .font(.titleLarge)
                        .foregroundColor(.white)
                )
            
            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(displayName)
                        .font(.titleMedium)
                        .foregroundColor(.white)
                    
                    Spacer()
                    
                    if let lastMessage = conversation.lastMessage {
                        Text(formatTime(lastMessage.createdAt))
                            .font(.bodySmall)
                            .foregroundColor(.surface500)
                    }
                }
                
                if let lastMessage = conversation.lastMessage {
                    Text(lastMessage.originalContent)
                        .font(.bodyMedium)
                        .foregroundColor(.surface400)
                        .lineLimit(1)
                }
            }
        }
        .padding(.vertical, 8)
    }
    
    private func formatTime(_ dateString: String) -> String {
        // TODO: Proper date formatting
        return "12:00"
    }
}

#Preview {
    NavigationStack {
        ConversationsView()
    }
}

