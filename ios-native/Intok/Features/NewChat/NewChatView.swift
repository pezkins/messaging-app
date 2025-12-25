import SwiftUI

struct NewChatView: View {
    @EnvironmentObject var authManager: AuthManager
    @StateObject private var chatStore = ChatStore.shared
    @Environment(\.dismiss) var dismiss
    
    @State private var searchText = ""
    @State private var searchResults: [UserPublic] = []
    @State private var isSearching = false
    @State private var isLoading = false
    
    @State private var isGroupMode = false
    @State private var selectedUsers: [UserPublic] = []
    @State private var groupName = ""
    
    var body: some View {
        NavigationView {
            ZStack {
                // Background
                Color(hex: "0F0F0F")
                    .ignoresSafeArea()
                
                VStack(spacing: 0) {
                    // Mode Toggle
                    Picker("", selection: $isGroupMode) {
                        Text("new_chat_direct".localized).tag(false)
                        Text("new_chat_group".localized).tag(true)
                    }
                    .pickerStyle(.segmented)
                    .padding()
                    
                    // Group Name (if group mode)
                    if isGroupMode {
                        TextField("new_chat_group_name_placeholder".localized, text: $groupName)
                            .textFieldStyle(.plain)
                            .padding()
                            .background(Color.white.opacity(0.1))
                            .cornerRadius(12)
                            .foregroundColor(.white)
                            .padding(.horizontal)
                    }
                    
                    // Search Bar
                    HStack {
                        Image(systemName: "magnifyingglass")
                            .foregroundColor(.gray)
                        
                        TextField("new_chat_search_users".localized, text: $searchText)
                            .textFieldStyle(.plain)
                            .foregroundColor(.white)
                            .onChange(of: searchText) { oldValue, newValue in
                                Task {
                                    await performSearch(newValue)
                                }
                            }
                        
                        if isSearching {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .gray))
                                .scaleEffect(0.8)
                        }
                    }
                    .padding()
                    .background(Color.white.opacity(0.05))
                    .cornerRadius(12)
                    .padding()
                    
                    // Selected Users (for group mode)
                    if isGroupMode && !selectedUsers.isEmpty {
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack {
                                ForEach(selectedUsers, id: \.id) { user in
                                    selectedUserChip(user)
                                }
                            }
                            .padding(.horizontal)
                        }
                        .padding(.bottom)
                    }
                    
                    // Search Results
                    if searchResults.isEmpty && !searchText.isEmpty && !isSearching {
                        emptyResultsView
                    } else {
                        searchResultsList
                    }
                    
                    // Create Group Button
                    if isGroupMode && selectedUsers.count >= 2 {
                        createGroupButton
                    }
                }
            }
            .navigationTitle(isGroupMode ? "new_chat_create_group".localized : "new_chat_title".localized)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("common_cancel".localized) {
                        dismiss()
                    }
                }
            }
        }
    }
    
    // MARK: - Selected User Chip
    func selectedUserChip(_ user: UserPublic) -> some View {
        HStack(spacing: 4) {
            Text(user.username)
                .foregroundColor(.white)
                .font(.subheadline)
            
            Button(action: {
                selectedUsers.removeAll { $0.id == user.id }
            }) {
                Image(systemName: "xmark.circle.fill")
                    .foregroundColor(.gray)
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(Color(hex: "8B5CF6").opacity(0.3))
        .cornerRadius(20)
    }
    
    // MARK: - Empty Results View
    var emptyResultsView: some View {
        VStack {
            Spacer()
            
            Image(systemName: "person.slash")
                .font(.system(size: 50))
                .foregroundColor(.gray)
            
            Text("new_chat_no_results".localized)
                .foregroundColor(.gray)
                .padding(.top)
            
            Spacer()
        }
    }
    
    // MARK: - Search Results List
    var searchResultsList: some View {
        ScrollView {
            LazyVStack(spacing: 0) {
                ForEach(searchResults, id: \.id) { user in
                    if user.id != authManager.currentUser?.id {
                        Button(action: {
                            if isGroupMode {
                                toggleUserSelection(user)
                            } else {
                                Task {
                                    await startDirectChat(with: user)
                                }
                            }
                        }) {
                            userRow(user)
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
    
    // MARK: - User Row
    func userRow(_ user: UserPublic) -> some View {
        HStack(spacing: 12) {
            // Avatar
            Circle()
                .fill(Color(hex: "8B5CF6"))
                .frame(width: 48, height: 48)
                .overlay(
                    Text(String(user.username.prefix(1).uppercased()))
                        .fontWeight(.semibold)
                        .foregroundColor(.white)
                )
            
            // Info
            VStack(alignment: .leading, spacing: 4) {
                Text(user.username)
                    .fontWeight(.semibold)
                    .foregroundColor(.white)
                
                Text(getLanguageName(user.preferredLanguage))
                    .font(.caption)
                    .foregroundColor(.gray)
            }
            
            Spacer()
            
            // Selection indicator (for group mode)
            if isGroupMode {
                Image(systemName: selectedUsers.contains(where: { $0.id == user.id }) ? "checkmark.circle.fill" : "circle")
                    .foregroundColor(Color(hex: "8B5CF6"))
                    .font(.title2)
            } else {
                Image(systemName: "chevron.right")
                    .foregroundColor(.gray)
            }
        }
        .padding()
    }
    
    // MARK: - Create Group Button
    var createGroupButton: some View {
        Button(action: {
            Task {
                await createGroup()
            }
        }) {
            HStack {
                if isLoading {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                } else {
                    Text("new_chat_selected_count".localized(with: selectedUsers.count))
                }
            }
            .fontWeight(.semibold)
            .foregroundColor(.white)
            .frame(maxWidth: .infinity)
            .padding()
            .background(Color(hex: "8B5CF6"))
            .cornerRadius(16)
        }
        .disabled(isLoading)
        .padding()
    }
    
    // MARK: - Helper Functions
    func getLanguageName(_ code: String) -> String {
        return getLanguageByCode(code)?.name ?? code
    }
    
    func toggleUserSelection(_ user: UserPublic) {
        if let index = selectedUsers.firstIndex(where: { $0.id == user.id }) {
            selectedUsers.remove(at: index)
        } else {
            selectedUsers.append(user)
        }
    }
    
    // MARK: - Actions
    func performSearch(_ query: String) async {
        guard query.count >= 2 else {
            searchResults = []
            return
        }
        
        isSearching = true
        searchResults = await chatStore.searchUsers(query: query)
        isSearching = false
    }
    
    func startDirectChat(with user: UserPublic) async {
        isLoading = true
        
        if let conversation = await chatStore.startConversation(with: user) {
            dismiss()
        }
        
        isLoading = false
    }
    
    func createGroup() async {
        guard selectedUsers.count >= 2 else { return }
        
        isLoading = true
        
        if let conversation = await chatStore.startGroupConversation(
            with: selectedUsers,
            name: groupName.isEmpty ? nil : groupName
        ) {
            dismiss()
        }
        
        isLoading = false
    }
}

#Preview {
    NewChatView()
        .environmentObject(AuthManager.shared)
}


