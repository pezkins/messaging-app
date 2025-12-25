import SwiftUI

struct AddParticipantsView: View {
    let conversation: Conversation
    @Environment(\.dismiss) var dismiss
    @StateObject private var chatStore = ChatStore.shared
    
    @State private var searchQuery = ""
    @State private var searchResults: [UserPublic] = []
    @State private var selectedUsers: Set<String> = []
    @State private var isSearching = false
    @State private var isAdding = false
    @State private var errorMessage: String?
    @State private var showError = false
    
    // Filter out current participants from search results
    private var filteredResults: [UserPublic] {
        let participantIds = Set(conversation.participants.map { $0.id })
        return searchResults.filter { !participantIds.contains($0.id) }
    }
    
    var body: some View {
        NavigationStack {
            ZStack {
                Color(hex: "0F0F0F")
                    .ignoresSafeArea()
                
                VStack(spacing: 0) {
                    // Search bar
                    HStack {
                        Image(systemName: "magnifyingglass")
                            .foregroundColor(.gray)
                        
                        TextField("Search users...", text: $searchQuery)
                            .textFieldStyle(.plain)
                            .foregroundColor(.white)
                            .autocapitalization(.none)
                            .autocorrectionDisabled()
                            .onChange(of: searchQuery) { _, newValue in
                                Task { await searchUsers(query: newValue) }
                            }
                        
                        if !searchQuery.isEmpty {
                            Button(action: { searchQuery = "" }) {
                                Image(systemName: "xmark.circle.fill")
                                    .foregroundColor(.gray)
                            }
                        }
                    }
                    .padding()
                    .background(Color.white.opacity(0.1))
                    .cornerRadius(12)
                    .padding()
                    
                    // Selected users chips
                    if !selectedUsers.isEmpty {
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 8) {
                                ForEach(Array(selectedUsers), id: \.self) { userId in
                                    if let user = searchResults.first(where: { $0.id == userId }) {
                                        selectedUserChip(user)
                                    }
                                }
                            }
                            .padding(.horizontal)
                        }
                        .padding(.bottom, 8)
                    }
                    
                    Divider()
                        .background(Color.white.opacity(0.1))
                    
                    // Search results
                    if isSearching {
                        ProgressView()
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                    } else if filteredResults.isEmpty && !searchQuery.isEmpty {
                        VStack(spacing: 12) {
                            Image(systemName: "person.slash")
                                .font(.largeTitle)
                                .foregroundColor(.gray)
                            Text("empty_users".localized)
                                .foregroundColor(.gray)
                        }
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                    } else {
                        List {
                            ForEach(filteredResults) { user in
                                userRow(user)
                                    .listRowBackground(Color.clear)
                                    .listRowSeparatorTint(Color.white.opacity(0.1))
                            }
                        }
                        .listStyle(.plain)
                        .scrollContentBackground(.hidden)
                    }
                }
            }
            .navigationTitle("group_add_participants_title".localized)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("common_cancel".localized) {
                        dismiss()
                    }
                    .foregroundColor(Color(hex: "8B5CF6"))
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Add") {
                        Task { await addSelectedUsers() }
                    }
                    .foregroundColor(selectedUsers.isEmpty ? .gray : Color(hex: "8B5CF6"))
                    .disabled(selectedUsers.isEmpty || isAdding)
                }
            }
            .overlay {
                if isAdding {
                    Color.black.opacity(0.3)
                        .ignoresSafeArea()
                        .overlay {
                            ProgressView("Adding...")
                                .padding()
                                .background(Color(hex: "2A2A2A"))
                                .cornerRadius(12)
                        }
                }
            }
            .alert("Error", isPresented: $showError) {
                Button("OK") { }
            } message: {
                Text(errorMessage ?? "An error occurred")
            }
        }
    }
    
    // MARK: - User Row
    func userRow(_ user: UserPublic) -> some View {
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
            
            // Selection checkmark
            Image(systemName: selectedUsers.contains(user.id) ? "checkmark.circle.fill" : "circle")
                .foregroundColor(selectedUsers.contains(user.id) ? Color(hex: "8B5CF6") : .gray)
                .font(.title2)
        }
        .contentShape(Rectangle())
        .onTapGesture {
            toggleSelection(user.id)
        }
    }
    
    // MARK: - Selected User Chip
    func selectedUserChip(_ user: UserPublic) -> some View {
        HStack(spacing: 6) {
            Text(user.username)
                .font(.subheadline)
                .foregroundColor(.white)
            
            Button(action: { toggleSelection(user.id) }) {
                Image(systemName: "xmark.circle.fill")
                    .foregroundColor(.white.opacity(0.7))
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 6)
        .background(Color(hex: "8B5CF6"))
        .cornerRadius(20)
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
    func toggleSelection(_ userId: String) {
        if selectedUsers.contains(userId) {
            selectedUsers.remove(userId)
        } else {
            selectedUsers.insert(userId)
        }
    }
    
    func searchUsers(query: String) async {
        guard query.count >= 2 else {
            searchResults = []
            return
        }
        
        isSearching = true
        searchResults = await chatStore.searchUsers(query: query)
        isSearching = false
    }
    
    func addSelectedUsers() async {
        guard !selectedUsers.isEmpty else { return }
        
        isAdding = true
        
        do {
            try await chatStore.addParticipants(
                conversationId: conversation.id,
                userIds: Array(selectedUsers)
            )
            await MainActor.run {
                dismiss()
            }
        } catch {
            await MainActor.run {
                errorMessage = "Failed to add participants: \(error.localizedDescription)"
                showError = true
                isAdding = false
            }
        }
    }
}

#Preview {
    AddParticipantsView(conversation: Conversation(
        id: "1",
        type: "group",
        name: "Test Group",
        pictureUrl: nil,
        participants: [],
        lastMessage: nil,
        createdAt: "",
        updatedAt: ""
    ))
}
