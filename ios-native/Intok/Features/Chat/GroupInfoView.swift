import SwiftUI

struct GroupInfoView: View {
    let conversation: Conversation
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject var authManager: AuthManager
    
    var body: some View {
        NavigationStack {
            ZStack {
                Color(hex: "0F0F0F")
                    .ignoresSafeArea()
                
                ScrollView {
                    VStack(spacing: 24) {
                        // Group Icon
                        ZStack {
                            Circle()
                                .fill(Color(hex: "8B5CF6"))
                                .frame(width: 100, height: 100)
                            
                            Image(systemName: "person.3.fill")
                                .font(.system(size: 40))
                                .foregroundColor(.white)
                        }
                        .padding(.top, 24)
                        
                        // Group Name
                        Text(conversation.name ?? "Group Chat")
                            .font(.title2)
                            .fontWeight(.bold)
                            .foregroundColor(.white)
                        
                        // Participant Count
                        Text("\(conversation.participants.count) participants")
                            .font(.subheadline)
                            .foregroundColor(.gray)
                        
                        Divider()
                            .background(Color.white.opacity(0.1))
                            .padding(.horizontal)
                        
                        // Participants Section
                        VStack(alignment: .leading, spacing: 16) {
                            Text("Participants")
                                .font(.headline)
                                .foregroundColor(.white)
                                .padding(.horizontal)
                            
                            ForEach(conversation.participants) { participant in
                                participantRow(participant)
                            }
                        }
                        
                        Spacer(minLength: 50)
                    }
                }
            }
            .navigationTitle("Group Info")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") {
                        dismiss()
                    }
                    .foregroundColor(Color(hex: "8B5CF6"))
                }
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
            
            // Name and info
            VStack(alignment: .leading, spacing: 2) {
                HStack {
                    Text(user.username)
                        .foregroundColor(.white)
                        .fontWeight(.medium)
                    
                    if user.id == authManager.currentUser?.id {
                        Text("(You)")
                            .font(.caption)
                            .foregroundColor(.gray)
                    }
                }
                
                HStack(spacing: 4) {
                    Image(systemName: "globe")
                        .font(.caption2)
                    Text(languageName(for: user.preferredLanguage))
                        .font(.caption)
                }
                .foregroundColor(.gray)
            }
            
            Spacer()
        }
        .padding(.horizontal)
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
    
    // MARK: - Language Name
    func languageName(for code: String) -> String {
        let locale = Locale(identifier: "en")
        return locale.localizedString(forLanguageCode: code) ?? code.uppercased()
    }
}

#Preview {
    GroupInfoView(conversation: Conversation(
        id: "1",
        type: "group",
        name: "Family Chat",
        participants: [
            UserPublic(id: "1", username: "Alice", preferredLanguage: "en", avatarUrl: nil),
            UserPublic(id: "2", username: "Bob", preferredLanguage: "es", avatarUrl: nil),
            UserPublic(id: "3", username: "Charlie", preferredLanguage: "fr", avatarUrl: nil)
        ],
        lastMessage: nil,
        createdAt: "",
        updatedAt: ""
    ))
    .environmentObject(AuthManager.shared)
}
