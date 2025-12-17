import SwiftUI

struct RemoveParticipantsView: View {
    let conversation: Conversation
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject var authManager: AuthManager
    @StateObject private var chatStore = ChatStore.shared
    
    @State private var selectedUserId: String?
    @State private var isRemoving = false
    @State private var showConfirmation = false
    @State private var errorMessage: String?
    @State private var showError = false
    
    // Filter out current user from the list
    private var removableParticipants: [UserPublic] {
        conversation.participants.filter { $0.id != authManager.currentUser?.id }
    }
    
    var body: some View {
        NavigationStack {
            ZStack {
                Color(hex: "0F0F0F")
                    .ignoresSafeArea()
                
                if removableParticipants.isEmpty {
                    VStack(spacing: 12) {
                        Image(systemName: "person.2.slash")
                            .font(.largeTitle)
                            .foregroundColor(.gray)
                        Text("No participants to remove")
                            .foregroundColor(.gray)
                    }
                } else {
                    List {
                        ForEach(removableParticipants) { user in
                            participantRow(user)
                                .listRowBackground(Color.clear)
                                .listRowSeparatorTint(Color.white.opacity(0.1))
                        }
                    }
                    .listStyle(.plain)
                    .scrollContentBackground(.hidden)
                }
            }
            .navigationTitle("Remove People")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        dismiss()
                    }
                    .foregroundColor(Color(hex: "8B5CF6"))
                }
            }
            .overlay {
                if isRemoving {
                    Color.black.opacity(0.3)
                        .ignoresSafeArea()
                        .overlay {
                            ProgressView("Removing...")
                                .padding()
                                .background(Color(hex: "2A2A2A"))
                                .cornerRadius(12)
                        }
                }
            }
            .confirmationDialog(
                "Remove from Group",
                isPresented: $showConfirmation,
                titleVisibility: .visible
            ) {
                Button("Remove", role: .destructive) {
                    if let userId = selectedUserId {
                        Task { await removeUser(userId) }
                    }
                }
                Button("Cancel", role: .cancel) {
                    selectedUserId = nil
                }
            } message: {
                if let userId = selectedUserId,
                   let user = removableParticipants.first(where: { $0.id == userId }) {
                    Text("Are you sure you want to remove \(user.username) from this group?")
                }
            }
            .alert("Error", isPresented: $showError) {
                Button("OK") { }
            } message: {
                Text(errorMessage ?? "An error occurred")
            }
        }
    }
    
    // MARK: - Participant Row
    func participantRow(_ user: UserPublic) -> some View {
        HStack(spacing: 12) {
            // Avatar
            if let avatarUrl = user.avatarUrl, let url = URL(string: avatarUrl) {
                AsyncImage(url: url) { image in
                    image
                        .resizable()
                        .scaledToFill()
                } placeholder: {
                    avatarPlaceholder(for: user)
                }
                .frame(width: 44, height: 44)
                .clipShape(Circle())
            } else {
                avatarPlaceholder(for: user)
            }
            
            // Name
            VStack(alignment: .leading, spacing: 2) {
                Text(user.username)
                    .foregroundColor(.white)
                    .fontWeight(.medium)
                
                Text(user.preferredLanguage.uppercased())
                    .font(.caption)
                    .foregroundColor(.gray)
            }
            
            Spacer()
            
            // Remove button
            Button(action: {
                selectedUserId = user.id
                showConfirmation = true
            }) {
                Image(systemName: "minus.circle.fill")
                    .foregroundColor(.red)
                    .font(.title2)
            }
        }
    }
    
    // MARK: - Avatar Placeholder
    func avatarPlaceholder(for user: UserPublic) -> some View {
        Circle()
            .fill(Color(hex: "8B5CF6"))
            .frame(width: 44, height: 44)
            .overlay(
                Text(String(user.username.prefix(1).uppercased()))
                    .foregroundColor(.white)
                    .fontWeight(.semibold)
            )
    }
    
    // MARK: - Actions
    func removeUser(_ userId: String) async {
        isRemoving = true
        
        do {
            try await chatStore.removeParticipant(
                conversationId: conversation.id,
                userId: userId
            )
            selectedUserId = nil
            
            // If no more participants (other than current user), dismiss
            if removableParticipants.count <= 1 {
                await MainActor.run {
                    dismiss()
                }
            }
        } catch {
            await MainActor.run {
                errorMessage = "Failed to remove participant: \(error.localizedDescription)"
                showError = true
            }
        }
        
        await MainActor.run {
            isRemoving = false
        }
    }
}

#Preview {
    RemoveParticipantsView(conversation: Conversation(
        id: "1",
        type: "group",
        name: "Test Group",
        participants: [
            UserPublic(id: "1", username: "Alice", preferredLanguage: "en", avatarUrl: nil),
            UserPublic(id: "2", username: "Bob", preferredLanguage: "es", avatarUrl: nil)
        ],
        lastMessage: nil,
        createdAt: "",
        updatedAt: ""
    ))
    .environmentObject(AuthManager.shared)
}
