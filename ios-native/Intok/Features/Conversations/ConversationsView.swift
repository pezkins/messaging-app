import SwiftUI

struct ConversationsView: View {
    @EnvironmentObject var authManager: AuthManager
    @StateObject private var chatStore = ChatStore.shared
    
    @State private var searchText = ""
    @State private var showingSettings = false
    @State private var showingNewChat = false
    
    var filteredConversations: [Conversation] {
        if searchText.isEmpty {
            return chatStore.conversations
        }
        return chatStore.conversations.filter { conversation in
            conversation.name?.localizedCaseInsensitiveContains(searchText) == true ||
            conversation.participants.contains { $0.username.localizedCaseInsensitiveContains(searchText) }
        }
    }
    
    var body: some View {
        NavigationView {
            ZStack {
                // Background
                Color(hex: "0F0F0F")
                    .ignoresSafeArea()
                
                VStack(spacing: 0) {
                    // Search Bar
                    searchBar
                    
                    // Content
                    if chatStore.isLoadingConversations && chatStore.conversations.isEmpty {
                        loadingView
                    } else if let error = chatStore.conversationsError {
                        errorView(error)
                    } else if chatStore.conversations.isEmpty {
                        emptyView
                    } else {
                        conversationsList
                    }
                }
            }
            .navigationTitle("Messages")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: { showingSettings = true }) {
                        if let avatarUrl = authManager.currentUser?.avatarUrl,
                           let url = URL(string: avatarUrl) {
                            AsyncImage(url: url) { image in
                                image
                                    .resizable()
                                    .scaledToFill()
                            } placeholder: {
                                Circle()
                                    .fill(Color(hex: "8B5CF6"))
                            }
                            .frame(width: 32, height: 32)
                            .clipShape(Circle())
                        } else {
                            Image(systemName: "person.circle.fill")
                                .font(.title2)
                                .foregroundColor(Color(hex: "8B5CF6"))
                        }
                    }
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { showingNewChat = true }) {
                        Image(systemName: "square.and.pencil")
                            .font(.title3)
                            .foregroundColor(Color(hex: "8B5CF6"))
                    }
                }
            }
            .sheet(isPresented: $showingSettings) {
                SettingsView()
            }
            .sheet(isPresented: $showingNewChat) {
                NewChatView()
            }
            .task {
                await chatStore.loadConversations()
            }
            .refreshable {
                await chatStore.loadConversations()
            }
        }
    }
    
    // MARK: - Search Bar
    var searchBar: some View {
        HStack {
            Image(systemName: "magnifyingglass")
                .foregroundColor(.gray)
            
            TextField("Search conversations...", text: $searchText)
                .textFieldStyle(.plain)
                .foregroundColor(.white)
        }
        .padding()
        .background(Color.white.opacity(0.05))
        .cornerRadius(12)
        .padding()
    }
    
    // MARK: - Loading View
    var loadingView: some View {
        VStack {
            Spacer()
            ProgressView()
                .progressViewStyle(CircularProgressViewStyle(tint: Color(hex: "8B5CF6")))
                .scaleEffect(1.5)
            Text("Loading conversations...")
                .foregroundColor(.gray)
                .padding(.top)
            Spacer()
        }
    }
    
    // MARK: - Error View
    func errorView(_ message: String) -> some View {
        VStack(spacing: 16) {
            Spacer()
            
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 60))
                .foregroundColor(.orange.opacity(0.7))
            
            Text("Something went wrong")
                .font(.title2)
                .fontWeight(.semibold)
                .foregroundColor(.white)
            
            Text(message)
                .font(.subheadline)
                .foregroundColor(.gray)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
            
            Button(action: {
                Task {
                    await chatStore.loadConversations()
                }
            }) {
                Text("Try Again")
                    .fontWeight(.semibold)
                    .foregroundColor(.white)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
                    .background(Color(hex: "8B5CF6"))
                    .cornerRadius(25)
            }
            .padding(.top)
            
            Spacer()
        }
    }
    
    // MARK: - Empty View
    var emptyView: some View {
        VStack(spacing: 16) {
            Spacer()
            
            Image(systemName: "message")
                .font(.system(size: 60))
                .foregroundColor(Color(hex: "8B5CF6").opacity(0.5))
            
            Text("No conversations yet")
                .font(.title2)
                .fontWeight(.semibold)
                .foregroundColor(.white)
            
            Text("Start a conversation by tapping the compose button")
                .font(.subheadline)
                .foregroundColor(.gray)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
            
            Button(action: { showingNewChat = true }) {
                Text("Start New Chat")
                    .fontWeight(.semibold)
                    .foregroundColor(.white)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
                    .background(Color(hex: "8B5CF6"))
                    .cornerRadius(25)
            }
            .padding(.top)
            
            Spacer()
        }
    }
    
    // MARK: - Conversations List
    var conversationsList: some View {
        ScrollView {
            LazyVStack(spacing: 0) {
                ForEach(filteredConversations) { conversation in
                    NavigationLink(destination: ChatView(conversation: conversation)) {
                        ConversationRow(conversation: conversation)
                    }
                    .buttonStyle(.plain)
                    
                    Divider()
                        .background(Color.white.opacity(0.1))
                        .padding(.leading, 76)
                }
            }
        }
    }
}

// MARK: - Conversation Row
struct ConversationRow: View {
    let conversation: Conversation
    @EnvironmentObject var authManager: AuthManager
    
    var displayName: String {
        if conversation.type == "group" {
            return conversation.name ?? "Group Chat"
        }
        
        // For direct messages, show the other participant
        if let otherUser = conversation.participants.first(where: { $0.id != authManager.currentUser?.id }) {
            return otherUser.username
        }
        
        return "Unknown"
    }
    
    var avatarUrl: String? {
        if conversation.type == "direct",
           let otherUser = conversation.participants.first(where: { $0.id != authManager.currentUser?.id }) {
            return otherUser.avatarUrl
        }
        return nil
    }
    
    var lastMessagePreview: String {
        guard let message = conversation.lastMessage else {
            return "No messages yet"
        }
        
        switch message.type {
        case .image:
            return "📷 Photo"
        case .voice:
            return "🎤 Voice message"
        case .file:
            return "📎 File"
        case .gif:
            return "GIF"
        default:
            return message.translatedContent ?? message.originalContent
        }
    }
    
    var timeString: String {
        guard let message = conversation.lastMessage else { return "" }
        
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        
        guard let date = formatter.date(from: message.createdAt) else {
            // Try without fractional seconds
            formatter.formatOptions = [.withInternetDateTime]
            guard let date = formatter.date(from: message.createdAt) else {
                return ""
            }
            return formatTime(date)
        }
        
        return formatTime(date)
    }
    
    private func formatTime(_ date: Date) -> String {
        let calendar = Calendar.current
        
        if calendar.isDateInToday(date) {
            let formatter = DateFormatter()
            formatter.dateFormat = "h:mm a"
            return formatter.string(from: date)
        } else if calendar.isDateInYesterday(date) {
            return "Yesterday"
        } else {
            let formatter = DateFormatter()
            formatter.dateFormat = "MMM d"
            return formatter.string(from: date)
        }
    }
    
    var body: some View {
        HStack(spacing: 12) {
            // Avatar
            ZStack {
                if conversation.type == "group" {
                    Circle()
                        .fill(Color(hex: "8B5CF6"))
                        .overlay(
                            Image(systemName: "person.2.fill")
                                .foregroundColor(.white)
                                .font(.headline)
                        )
                } else if let urlString = avatarUrl, let url = URL(string: urlString) {
                    AsyncImage(url: url) { image in
                        image
                            .resizable()
                            .scaledToFill()
                    } placeholder: {
                        Circle()
                            .fill(Color(hex: "8B5CF6"))
                            .overlay(
                                Text(String(displayName.prefix(1).uppercased()))
                                    .fontWeight(.semibold)
                                    .foregroundColor(.white)
                            )
                    }
                    .clipShape(Circle())
                } else {
                    Circle()
                        .fill(Color(hex: "8B5CF6"))
                        .overlay(
                            Text(String(displayName.prefix(1).uppercased()))
                                .fontWeight(.semibold)
                                .foregroundColor(.white)
                        )
                }
            }
            .frame(width: 52, height: 52)
            
            // Content
            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(displayName)
                        .fontWeight(.semibold)
                        .foregroundColor(.white)
                        .lineLimit(1)
                    
                    Spacer()
                    
                    Text(timeString)
                        .font(.caption)
                        .foregroundColor(.gray)
                }
                
                Text(lastMessagePreview)
                    .font(.subheadline)
                    .foregroundColor(.gray)
                    .lineLimit(2)
            }
        }
        .padding()
    }
}

#Preview {
    ConversationsView()
        .environmentObject(AuthManager.shared)
}
