import SwiftUI
import os.log

private let logger = Logger(subsystem: "com.pezkins.intok", category: "GifPicker")

// MARK: - GIF Picker View
struct GifPickerView: View {
    @Environment(\.dismiss) var dismiss
    let onSelect: (String) -> Void
    
    @State private var searchText = ""
    @State private var gifs: [GiphyGif] = []
    @State private var isLoading = false
    @State private var searchTask: Task<Void, Never>?
    @State private var errorMessage: String?
    
    // API key from Info.plist - NEVER hardcode API keys
    private var giphyApiKey: String {
        Bundle.main.object(forInfoDictionaryKey: "GIPHY_API_KEY") as? String ?? ""
    }
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Search Bar
                searchBar
                
                // Content
                if isLoading && gifs.isEmpty {
                    loadingView
                } else if gifs.isEmpty {
                    emptyView
                } else {
                    gifGrid
                }
            }
            .background(Color(hex: "0F0F0F"))
            .navigationTitle("chat_attach_gif".localized)
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
        .task {
            await loadTrendingGifs()
        }
    }
    
    // MARK: - Search Bar
    var searchBar: some View {
        HStack {
            Image(systemName: "magnifyingglass")
                .foregroundColor(.gray)
            
            TextField("Search GIPHY", text: $searchText)
                .textFieldStyle(.plain)
                .foregroundColor(.white)
                .autocorrectionDisabled()
                .onChange(of: searchText) { oldValue, newValue in
                    // Debounce search
                    searchTask?.cancel()
                    searchTask = Task {
                        try? await Task.sleep(nanoseconds: 300_000_000) // 300ms
                        guard !Task.isCancelled else { return }
                        if newValue.isEmpty {
                            await loadTrendingGifs()
                        } else {
                            await searchGifs(query: newValue)
                        }
                    }
                }
            
            if !searchText.isEmpty {
                Button(action: { searchText = "" }) {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(.gray)
                }
            }
        }
        .padding(12)
        .background(Color.white.opacity(0.1))
        .cornerRadius(12)
        .padding()
    }
    
    // MARK: - GIF Grid
    var gifGrid: some View {
        ScrollView {
            LazyVGrid(columns: [
                GridItem(.flexible(), spacing: 4),
                GridItem(.flexible(), spacing: 4)
            ], spacing: 4) {
                ForEach(gifs) { gif in
                    GifThumbnail(gif: gif)
                        .onTapGesture {
                            onSelect(gif.images.original.url)
                            dismiss()
                        }
                }
            }
            .padding(4)
        }
    }
    
    // MARK: - Loading View
    var loadingView: some View {
        VStack {
            Spacer()
            ProgressView()
                .tint(Color(hex: "8B5CF6"))
            Text("common_loading".localized)
                .font(.caption)
                .foregroundColor(.gray)
                .padding(.top, 8)
            Spacer()
        }
    }
    
    // MARK: - Empty View
    var emptyView: some View {
        VStack(spacing: 16) {
            Spacer()
            if let error = errorMessage {
                Image(systemName: "exclamationmark.triangle")
                    .font(.system(size: 48))
                    .foregroundColor(.orange)
                Text(error)
                    .font(.headline)
                    .foregroundColor(.gray)
                Button("common_try_again".localized) {
                    Task { await loadTrendingGifs() }
                }
                .foregroundColor(Color(hex: "8B5CF6"))
            } else {
                Image(systemName: "face.smiling")
                    .font(.system(size: 48))
                    .foregroundColor(.gray)
                Text(searchText.isEmpty ? "common_loading".localized : "empty_search_results".localized)
                    .font(.headline)
                    .foregroundColor(.gray)
                Text("gif_try_different_search".localized)
                    .font(.caption)
                    .foregroundColor(.gray.opacity(0.7))
            }
            Spacer()
        }
    }
    
    // MARK: - API Calls
    func loadTrendingGifs() async {
        guard !giphyApiKey.isEmpty else {
            errorMessage = "GIF service not configured"
            logger.error("❌ GIPHY_API_KEY not set in Info.plist")
            return
        }
        
        isLoading = true
        errorMessage = nil
        
        guard let url = URL(string: "https://api.giphy.com/v1/gifs/trending?api_key=\(giphyApiKey)&limit=30&rating=g") else {
            isLoading = false
            errorMessage = "Invalid request"
            return
        }
        
        do {
            let (data, _) = try await URLSession.shared.data(from: url)
            let response = try JSONDecoder().decode(GiphyResponse.self, from: data)
            gifs = response.data
        } catch {
            logger.error("❌ Failed to load trending GIFs: \(error.localizedDescription, privacy: .public)")
            errorMessage = "Failed to load GIFs"
        }
        
        isLoading = false
    }
    
    func searchGifs(query: String) async {
        guard !giphyApiKey.isEmpty else {
            errorMessage = "GIF service not configured"
            return
        }
        
        isLoading = true
        errorMessage = nil
        
        let encodedQuery = query.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? query
        guard let url = URL(string: "https://api.giphy.com/v1/gifs/search?api_key=\(giphyApiKey)&q=\(encodedQuery)&limit=30&rating=g") else {
            isLoading = false
            errorMessage = "Invalid search"
            return
        }
        
        do {
            let (data, _) = try await URLSession.shared.data(from: url)
            let response = try JSONDecoder().decode(GiphyResponse.self, from: data)
            gifs = response.data
        } catch {
            logger.error("❌ Failed to search GIFs: \(error.localizedDescription, privacy: .public)")
            errorMessage = "Search failed"
        }
        
        isLoading = false
    }
}

// MARK: - GIF Thumbnail
struct GifThumbnail: View {
    let gif: GiphyGif
    
    var body: some View {
        AsyncImage(url: URL(string: gif.images.fixedWidthSmall.url)) { phase in
            switch phase {
            case .success(let image):
                image
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(height: 120)
                    .clipped()
            case .failure:
                Color.gray.opacity(0.3)
                    .frame(height: 120)
                    .overlay(
                        Image(systemName: "photo")
                            .foregroundColor(.gray)
                    )
            case .empty:
                Color.gray.opacity(0.2)
                    .frame(height: 120)
                    .overlay(
                        ProgressView()
                            .tint(.gray)
                    )
            @unknown default:
                EmptyView()
            }
        }
        .cornerRadius(8)
    }
}

// MARK: - GIPHY API Models
struct GiphyResponse: Codable {
    let data: [GiphyGif]
}

struct GiphyGif: Codable, Identifiable {
    let id: String
    let title: String
    let images: GiphyImages
}

struct GiphyImages: Codable {
    let original: GiphyImage
    let fixedWidthSmall: GiphyImage
    
    enum CodingKeys: String, CodingKey {
        case original
        case fixedWidthSmall = "fixed_width_small"
    }
}

struct GiphyImage: Codable {
    let url: String
    let width: String
    let height: String
}

#Preview {
    GifPickerView { _ in }
}
