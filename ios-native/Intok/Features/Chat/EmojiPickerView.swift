import SwiftUI

// MARK: - Emoji Picker View
struct EmojiPickerView: View {
    let onEmojiSelected: (String) -> Void
    @Environment(\.dismiss) var dismiss
    
    @StateObject private var frequentManager = FrequentEmojiManager.shared
    @State private var searchText = ""
    @State private var selectedCategory: EmojiCategory = .frequent
    
    private let columns = Array(repeating: GridItem(.flexible(), spacing: 4), count: 8)
    
    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                // Search bar
                searchBar
                
                // Category tabs
                categoryTabs
                
                // Emoji grid
                ScrollViewReader { proxy in
                    ScrollView {
                        LazyVGrid(columns: columns, spacing: 8) {
                            ForEach(currentEmojis, id: \.self) { emoji in
                                Button {
                                    selectEmoji(emoji)
                                } label: {
                                    Text(emoji)
                                        .font(.system(size: 28))
                                }
                                .buttonStyle(.plain)
                            }
                        }
                        .padding()
                    }
                    .onChange(of: selectedCategory) { _, newValue in
                        // Scroll to top when category changes
                        proxy.scrollTo("top", anchor: .top)
                    }
                }
            }
            .background(Color(hex: "0F0F0F"))
            .navigationTitle("accessibility_select_emoji".localized)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("common_cancel".localized) {
                        dismiss()
                    }
                    .foregroundColor(Color(hex: "8B5CF6"))
                }
            }
        }
        .preferredColorScheme(.dark)
    }
    
    // MARK: - Search Bar
    var searchBar: some View {
        HStack {
            Image(systemName: "magnifyingglass")
                .foregroundColor(.gray)
            
            TextField("Search emoji", text: $searchText)
                .textFieldStyle(.plain)
                .foregroundColor(.white)
            
            if !searchText.isEmpty {
                Button {
                    searchText = ""
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(.gray)
                }
            }
        }
        .padding(10)
        .background(Color.white.opacity(0.1))
        .cornerRadius(10)
        .padding()
    }
    
    // MARK: - Category Tabs
    var categoryTabs: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 0) {
                ForEach(EmojiCategory.allCases) { category in
                    Button {
                        withAnimation(.easeInOut(duration: 0.2)) {
                            selectedCategory = category
                            searchText = ""
                        }
                    } label: {
                        VStack(spacing: 4) {
                            Image(systemName: category.icon)
                                .font(.system(size: 20))
                                .foregroundColor(selectedCategory == category ? Color(hex: "8B5CF6") : .gray)
                            
                            Rectangle()
                                .fill(selectedCategory == category ? Color(hex: "8B5CF6") : Color.clear)
                                .frame(height: 2)
                        }
                        .frame(width: 44, height: 44)
                    }
                }
            }
            .padding(.horizontal)
        }
        .background(Color(hex: "1A1A1A"))
    }
    
    // MARK: - Current Emojis
    var currentEmojis: [String] {
        // If searching, return search results
        if !searchText.isEmpty {
            return EmojiData.search(searchText)
        }
        
        // Return emojis for selected category
        switch selectedCategory {
        case .frequent:
            return frequentManager.allFrequentEmojis()
        default:
            return EmojiData.categories[selectedCategory] ?? []
        }
    }
    
    // MARK: - Select Emoji
    func selectEmoji(_ emoji: String) {
        frequentManager.recordUsage(emoji)
        onEmojiSelected(emoji)
        dismiss()
    }
}

// MARK: - Quick Reaction Bar
struct QuickReactionBar: View {
    let onEmojiSelected: (String) -> Void
    let onShowFullPicker: () -> Void
    let onReply: () -> Void
    let onCopy: () -> Void
    let onDelete: () -> Void
    let showDeleteForEveryone: Bool

    @StateObject private var frequentManager = FrequentEmojiManager.shared

    var body: some View {
        VStack(spacing: 0) {
            // Quick reactions row
            HStack(spacing: 12) {
                ForEach(frequentManager.topEmojis(5), id: \.self) { emoji in
                    Button {
                        frequentManager.recordUsage(emoji)
                        onEmojiSelected(emoji)
                    } label: {
                        Text(emoji)
                            .font(.title2)
                    }
                    .buttonStyle(.plain)
                }

                // Plus button for full picker
                Button {
                    onShowFullPicker()
                } label: {
                    Image(systemName: "plus.circle")
                        .font(.title2)
                        .foregroundColor(.gray)
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)

            Divider()
                .background(Color.white.opacity(0.1))

            // Action buttons row
            HStack(spacing: 0) {
                // Reply button
                Button {
                    onReply()
                } label: {
                    HStack(spacing: 6) {
                        Image(systemName: "arrowshape.turn.up.left")
                        Text("chat_reply".localized)
                    }
                    .font(.subheadline)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                }

                Divider()
                    .frame(height: 20)
                    .background(Color.white.opacity(0.1))
                
                // Copy button
                Button {
                    onCopy()
                } label: {
                    HStack(spacing: 6) {
                        Image(systemName: "doc.on.doc")
                        Text("chat_copy".localized)
                    }
                    .font(.subheadline)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                }
                
                Divider()
                    .frame(height: 20)
                    .background(Color.white.opacity(0.1))
                
                // Delete button
                Button {
                    onDelete()
                } label: {
                    HStack(spacing: 6) {
                        Image(systemName: "trash")
                        Text("chat_delete".localized)
                    }
                    .font(.subheadline)
                    .foregroundColor(.red)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                }
            }
        }
        .background(Color(hex: "2A2A2A"))
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.4), radius: 12, y: 4)
    }
}

#Preview {
    EmojiPickerView { emoji in
        print("Selected: \(emoji)")
    }
}
