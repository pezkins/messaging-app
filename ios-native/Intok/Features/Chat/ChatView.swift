import SwiftUI
import PhotosUI
import Photos
import QuickLook
import UniformTypeIdentifiers
import os.log

private let logger = Logger(subsystem: "com.pezkins.intok", category: "ChatView")

// MARK: - Download URL Cache
actor DownloadURLCache {
    static let shared = DownloadURLCache()
    
    private var cache: [String: (url: String, timestamp: Date)] = [:]
    private let cacheDuration: TimeInterval = 30 * 60 // 30 minutes
    
    func getURL(for key: String) async throws -> URL {
        // Check cache first
        if let cached = cache[key],
           Date().timeIntervalSince(cached.timestamp) < cacheDuration,
           let url = URL(string: cached.url) {
            logger.debug("📦 Cache hit for key: \(key, privacy: .public)")
            return url
        }
        
        // Fetch new URL
        let response = try await APIService.shared.getDownloadUrl(key: key)
        cache[key] = (response.downloadUrl, Date())
        
        guard let url = URL(string: response.downloadUrl) else {
            throw AttachmentError.invalidURL
        }
        
        logger.debug("📦 Cached new URL for key: \(key, privacy: .public)")
        return url
    }
    
    func clearExpired() {
        let now = Date()
        cache = cache.filter { now.timeIntervalSince($0.value.timestamp) < cacheDuration }
    }
}

struct ChatView: View {
    let conversation: Conversation
    @EnvironmentObject var authManager: AuthManager
    @StateObject private var chatStore = ChatStore.shared
    @Environment(\.dismiss) var dismiss
    
    @State private var messageText = ""
    @State private var showingAttachmentOptions = false
    @State private var selectedPhotoItem: PhotosPickerItem?
    @State private var showingCamera = false
    @State private var showingGifPicker = false
    @State private var showingDocumentPicker = false
    @State private var isUploading = false
    @State private var uploadProgress: Double = 0
    @State private var previewURL: URL?
    @State private var errorMessage: String?
    @State private var showError = false
    @State private var showDocumentTranslationAlert = false
    @State private var pendingDocumentAttachment: UploadedAttachment?
    @State private var replyingTo: Message?
    @State private var showEmojiPicker = false
    @State private var selectedMessageForReaction: Message?
    @State private var showDeleteConfirmation = false
    @State private var messageToDelete: Message?
    @State private var isDeletingMessage = false
    @State private var showSaveSuccess = false
    @State private var showSaveError = false
    @State private var saveErrorMessage: String?
    @State private var isDownloading = false
    @State private var showShareSheet = false
    @State private var downloadedFileURL: URL?
    @FocusState private var isInputFocused: Bool
    
    var displayName: String {
        if conversation.type == "group" {
            return conversation.name ?? "Group Chat"
        }
        if let otherUser = conversation.participants.first(where: { $0.id != authManager.currentUser?.id }) {
            return otherUser.username
        }
        return "Chat"
    }
    
    var body: some View {
        ZStack {
            // Background
            Color(hex: "0F0F0F")
                .ignoresSafeArea()
            
            VStack(spacing: 0) {
                // Messages
                messagesView
                
                // Typing Indicator
                if let typingUsers = chatStore.typingUsers[conversation.id], !typingUsers.isEmpty {
                    typingIndicator
                }
                
                // Upload Progress
                if isUploading {
                    uploadProgressView
                }
                
                // Reply Preview Bar
                if let replyTo = replyingTo {
                    ReplyPreviewBar(replyTo: replyTo) {
                        withAnimation {
                            replyingTo = nil
                        }
                    }
                }
                
                // Input
                inputBar
            }
        }
        .navigationTitle(displayName)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Menu {
                    Button(action: {}) {
                        Label("View Profile", systemImage: "person")
                    }
                    Button(action: {}) {
                        Label("Search in Chat", systemImage: "magnifyingglass")
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                        .foregroundColor(Color(hex: "8B5CF6"))
                }
            }
        }
        .task {
            await chatStore.selectConversation(conversation)
        }
        .onDisappear {
            chatStore.clearActiveConversation()
        }
        .sheet(isPresented: $showingCamera) {
            CameraCaptureView { image in
                Task { await uploadImage(image) }
            }
        }
        .sheet(isPresented: $showingGifPicker) {
            GifPickerView { gifUrl in
                sendGif(gifUrl)
            }
        }
        .sheet(isPresented: $showEmojiPicker) {
            EmojiPickerView { emoji in
                if let message = selectedMessageForReaction {
                    chatStore.sendReaction(
                        messageId: message.id,
                        messageTimestamp: message.createdAt,
                        emoji: emoji
                    )
                }
                selectedMessageForReaction = nil
            }
            .presentationDetents([.medium, .large])
        }
        .fileImporter(
            isPresented: $showingDocumentPicker,
            allowedContentTypes: [.pdf, .plainText, .data],
            allowsMultipleSelection: false
        ) { result in
            handleDocumentSelection(result)
        }
        .quickLookPreview($previewURL)
        .onChange(of: selectedPhotoItem) { oldValue, newValue in
            Task { await handlePhotoSelection(newValue) }
        }
        .alert("Error", isPresented: $showError) {
            Button("OK") { }
        } message: {
            Text(errorMessage ?? "An unknown error occurred")
        }
        .alert("Translate Document?", isPresented: $showDocumentTranslationAlert) {
            Button("Translate") {
                sendDocument(translate: true)
            }
            Button("Send Without Translation") {
                sendDocument(translate: false)
            }
            Button("Cancel", role: .cancel) {
                pendingDocumentAttachment = nil
            }
        } message: {
            Text("Would you like to translate \"\(pendingDocumentAttachment?.fileName ?? "this document")\" for recipients who speak other languages?")
        }
        .confirmationDialog(
            "Delete Message",
            isPresented: $showDeleteConfirmation,
            titleVisibility: .visible
        ) {
            if let message = messageToDelete {
                let isOwnMessage = message.senderId == authManager.currentUser?.id
                
                Button("Delete for Me", role: .destructive) {
                    Task { await deleteMessage(message, forEveryone: false) }
                }
                
                if isOwnMessage {
                    Button("Delete for Everyone", role: .destructive) {
                        Task { await deleteMessage(message, forEveryone: true) }
                    }
                }
                
                Button("Cancel", role: .cancel) {
                    messageToDelete = nil
                }
            }
        } message: {
            Text("This action cannot be undone.")
        }
        .alert("Saved!", isPresented: $showSaveSuccess) {
            Button("OK") { }
        } message: {
            Text("Image saved to Photos")
        }
        .alert("Error", isPresented: $showSaveError) {
            Button("OK") { }
        } message: {
            Text(saveErrorMessage ?? "Failed to save image")
        }
        .overlay {
            if isDeletingMessage {
                Color.black.opacity(0.3)
                    .ignoresSafeArea()
                    .overlay {
                        ProgressView("Deleting...")
                            .padding()
                            .background(Color(hex: "2A2A2A"))
                            .cornerRadius(12)
                    }
            }
            if isDownloading {
                Color.black.opacity(0.3)
                    .ignoresSafeArea()
                    .overlay {
                        ProgressView("Downloading...")
                            .padding()
                            .background(Color(hex: "2A2A2A"))
                            .cornerRadius(12)
                    }
            }
        }
        .sheet(isPresented: $showShareSheet) {
            if let fileURL = downloadedFileURL {
                ShareSheet(activityItems: [fileURL])
            }
        }
    }
    
    // MARK: - Messages View
    var messagesView: some View {
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(spacing: 4) {
                    if chatStore.hasMoreMessages {
                        Button("Load more...") {
                            Task { await chatStore.loadMoreMessages() }
                        }
                        .foregroundColor(Color(hex: "8B5CF6"))
                        .padding()
                    }
                    
                    ForEach(chatStore.messages) { message in
                        MessageBubble(
                            message: message,
                            isOwnMessage: message.senderId == authManager.currentUser?.id || message.id.hasPrefix("temp-"),
                            onReaction: { emoji in
                                chatStore.sendReaction(
                                    messageId: message.id,
                                    messageTimestamp: message.createdAt,
                                    emoji: emoji
                                )
                            },
                            onShowFullEmojiPicker: {
                                selectedMessageForReaction = message
                                showEmojiPicker = true
                            },
                            onReply: {
                                withAnimation {
                                    replyingTo = message
                                    isInputFocused = true
                                }
                            },
                            onCopy: {
                                UIPasteboard.general.string = message.translatedContent ?? message.originalContent
                            },
                            onDelete: {
                                messageToDelete = message
                                showDeleteConfirmation = true
                            },
                            onSaveImage: { urlString in
                                Task {
                                    await saveImageToPhotos(from: urlString)
                                }
                            },
                            onDownloadDocument: { attachment in
                                Task {
                                    await downloadDocument(attachment)
                                }
                            },
                            onImageTap: { url in
                                previewURL = url
                            },
                            onScrollToMessage: { messageId in
                                // Scroll to the replied message
                                withAnimation {
                                    // Proxy scrollTo happens here
                                }
                            }
                        )
                        .id(message.id)
                    }
                }
                .padding()
            }
            .onChange(of: chatStore.messages.count) { oldCount, newCount in
                if let lastId = chatStore.messages.last?.id {
                    withAnimation {
                        proxy.scrollTo(lastId, anchor: .bottom)
                    }
                }
            }
        }
    }
    
    // MARK: - Typing Indicator
    var typingIndicator: some View {
        HStack {
            Text("Someone is typing...")
                .font(.caption)
                .foregroundColor(.gray)
                .italic()
            
            HStack(spacing: 4) {
                ForEach(0..<3) { i in
                    Circle()
                        .fill(Color.gray)
                        .frame(width: 6, height: 6)
                        .animation(
                            Animation.easeInOut(duration: 0.6)
                                .repeatForever()
                                .delay(Double(i) * 0.2),
                            value: true
                        )
                }
            }
            
            Spacer()
        }
        .padding(.horizontal)
        .padding(.vertical, 8)
    }
    
    // MARK: - Upload Progress
    var uploadProgressView: some View {
        HStack {
            ProgressView(value: uploadProgress)
                .tint(Color(hex: "8B5CF6"))
            Text("Uploading...")
                .font(.caption)
                .foregroundColor(.gray)
        }
        .padding(.horizontal)
        .padding(.vertical, 8)
    }
    
    // MARK: - Input Bar
    var inputBar: some View {
        VStack(spacing: 0) {
            Divider()
                .background(Color.white.opacity(0.1))
            
            HStack(spacing: 12) {
                // Attachment Menu Button
                Menu {
                    // Photo Library
                    Button(action: {}) {
                        Label("Photo Library", systemImage: "photo.on.rectangle")
                    }
                    .background(
                        PhotosPicker(selection: $selectedPhotoItem, matching: .images) {
                            Color.clear
                        }
                    )
                    
                    // Camera
                    Button(action: { showingCamera = true }) {
                        Label("Camera", systemImage: "camera")
                    }
                    
                    // GIF
                    Button(action: { showingGifPicker = true }) {
                        Label("GIF", systemImage: "face.smiling")
                    }
                    
                    // Document
                    Button(action: { showingDocumentPicker = true }) {
                        Label("Document", systemImage: "doc")
                    }
                } label: {
                    Image(systemName: "plus.circle.fill")
                        .font(.title2)
                        .foregroundColor(Color(hex: "8B5CF6"))
                }
                
                // Text Field
                TextField("Message...", text: $messageText, axis: .vertical)
                    .textFieldStyle(.plain)
                    .padding(12)
                    .background(Color.white.opacity(0.1))
                    .cornerRadius(20)
                    .foregroundColor(.white)
                    .lineLimit(5)
                    .focused($isInputFocused)
                    .onChange(of: messageText) { oldValue, newValue in
                        chatStore.setTyping(!newValue.isEmpty)
                    }
                
                // Send Button
                Button(action: sendMessage) {
                    Image(systemName: "arrow.up.circle.fill")
                        .font(.title)
                        .foregroundColor(
                            messageText.trimmingCharacters(in: .whitespaces).isEmpty ?
                            Color.gray : Color(hex: "8B5CF6")
                        )
                }
                .disabled(messageText.trimmingCharacters(in: .whitespaces).isEmpty)
            }
            .padding()
            .background(Color(hex: "1A1A1A"))
        }
    }
    
    // MARK: - Actions
    func sendMessage() {
        let text = messageText.trimmingCharacters(in: .whitespaces)
        guard !text.isEmpty else { return }
        
        // Build replyTo if replying
        var replyToData: ReplyTo?
        if let replyMessage = replyingTo {
            replyToData = ReplyTo(
                messageId: replyMessage.id,
                content: replyMessage.originalContent,
                senderId: replyMessage.senderId,
                senderName: replyMessage.sender?.username ?? "Unknown",
                type: replyMessage.type
            )
        }
        
        chatStore.sendMessage(text, replyTo: replyToData)
        messageText = ""
        replyingTo = nil
        chatStore.setTyping(false)
    }
    
    func handlePhotoSelection(_ item: PhotosPickerItem?) async {
        guard let item = item,
              let data = try? await item.loadTransferable(type: Data.self),
              let image = UIImage(data: data) else { return }
        
        await uploadImage(image)
        selectedPhotoItem = nil
    }
    
    func uploadImage(_ image: UIImage) async {
        guard let conversation = chatStore.activeConversation else { return }
        
        isUploading = true
        uploadProgress = 0
        
        do {
            let attachment = try await AttachmentService.shared.uploadImage(
                image,
                conversationId: conversation.id
            ) { progress in
                Task { @MainActor in
                    uploadProgress = progress
                }
            }
            
            chatStore.sendMessage("", type: "image", attachment: [
                "id": attachment.id,
                "key": attachment.key,
                "fileName": attachment.fileName,
                "contentType": attachment.contentType,
                "fileSize": attachment.fileSize,
                "category": attachment.category
            ])
        } catch {
            logger.error("❌ Image upload failed: \(error.localizedDescription, privacy: .public)")
            errorMessage = "Failed to upload image. Please try again."
            showError = true
        }
        
        isUploading = false
    }
    
    func sendGif(_ url: String) {
        chatStore.sendMessage(url, type: "gif")
    }
    
    func handleDocumentSelection(_ result: Result<[URL], Error>) {
        switch result {
        case .success(let urls):
            guard let url = urls.first,
                  let conversation = chatStore.activeConversation else { return }

            // Start accessing security-scoped resource
            guard url.startAccessingSecurityScopedResource() else {
                errorMessage = "Unable to access the selected file."
                showError = true
                return
            }
            defer { url.stopAccessingSecurityScopedResource() }

            Task {
                isUploading = true
                uploadProgress = 0

                do {
                    let attachment = try await AttachmentService.shared.uploadDocument(
                        url,
                        conversationId: conversation.id
                    ) { progress in
                        Task { @MainActor in
                            uploadProgress = progress
                        }
                    }

                    // Store attachment and show translation dialog
                    pendingDocumentAttachment = attachment
                    showDocumentTranslationAlert = true
                } catch {
                    logger.error("❌ Document upload failed: \(error.localizedDescription, privacy: .public)")
                    errorMessage = "Failed to upload document. Please try again."
                    showError = true
                }

                isUploading = false
            }

        case .failure(let error):
            logger.error("❌ Document selection failed: \(error.localizedDescription, privacy: .public)")
            errorMessage = "Could not select document."
            showError = true
        }
    }
    
    func sendDocument(translate: Bool) {
        guard let attachment = pendingDocumentAttachment else { return }
        
        chatStore.sendMessage(
            attachment.fileName,
            type: "file",
            attachment: [
                "id": attachment.id,
                "key": attachment.key,
                "fileName": attachment.fileName,
                "contentType": attachment.contentType,
                "fileSize": attachment.fileSize,
                "category": attachment.category
            ],
            translateDocument: translate
        )
        
        pendingDocumentAttachment = nil
    }
    
    // MARK: - Delete Message
    func deleteMessage(_ message: Message, forEveryone: Bool) async {
        isDeletingMessage = true
        
        do {
            try await chatStore.deleteMessage(message, forEveryone: forEveryone)
            messageToDelete = nil
        } catch {
            errorMessage = "Failed to delete message: \(error.localizedDescription)"
            showError = true
        }
        
        isDeletingMessage = false
    }
    
    // MARK: - Save Image to Photos
    func saveImageToPhotos(from urlString: String) async {
        guard let url = URL(string: urlString) else {
            saveErrorMessage = "Invalid image URL"
            showSaveError = true
            return
        }
        
        do {
            // Download image data
            let (data, _) = try await URLSession.shared.data(from: url)
            guard let image = UIImage(data: data) else {
                saveErrorMessage = "Could not load image"
                showSaveError = true
                return
            }
            
            // Request photo library permission
            let status = await PHPhotoLibrary.requestAuthorization(for: .addOnly)
            guard status == .authorized || status == .limited else {
                saveErrorMessage = "Photo library access denied. Please enable in Settings."
                showSaveError = true
                return
            }
            
            // Save to photo library
            try await PHPhotoLibrary.shared().performChanges {
                PHAssetChangeRequest.creationRequestForAsset(from: image)
            }
            
            await MainActor.run {
                showSaveSuccess = true
            }
        } catch {
            await MainActor.run {
                saveErrorMessage = "Failed to save image: \(error.localizedDescription)"
                showSaveError = true
            }
        }
    }
    
    // MARK: - Download Document
    func downloadDocument(_ attachment: Attachment) async {
        guard let urlString = attachment.url, let downloadUrl = URL(string: urlString) else {
            errorMessage = "No download URL available"
            showError = true
            return
        }
        
        await MainActor.run {
            isDownloading = true
        }
        
        do {
            // Download file to temporary location
            let (tempURL, _) = try await URLSession.shared.download(from: downloadUrl)
            
            // Move to documents directory with proper filename
            let documentsPath = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
            let destinationURL = documentsPath.appendingPathComponent(attachment.fileName)
            
            // Remove existing file if present
            try? FileManager.default.removeItem(at: destinationURL)
            try FileManager.default.moveItem(at: tempURL, to: destinationURL)
            
            // Present share sheet
            await MainActor.run {
                isDownloading = false
                downloadedFileURL = destinationURL
                showShareSheet = true
            }
        } catch {
            await MainActor.run {
                isDownloading = false
                errorMessage = "Failed to download: \(error.localizedDescription)"
                showError = true
            }
        }
    }
}

// MARK: - Message Bubble
struct MessageBubble: View {
    let message: Message
    let isOwnMessage: Bool
    let onReaction: (String) -> Void
    let onShowFullEmojiPicker: () -> Void
    let onReply: () -> Void
    let onCopy: () -> Void
    let onDelete: () -> Void
    let onSaveImage: ((String) -> Void)?
    let onDownloadDocument: ((Attachment) -> Void)?
    let onImageTap: (URL) -> Void
    let onScrollToMessage: ((String) -> Void)?
    
    @State private var showTranslation = true
    @State private var showReactionBar = false
    @State private var downloadedImageURL: URL?
    @StateObject private var frequentManager = FrequentEmojiManager.shared
    
    private var hasReactions: Bool {
        if let reactions = message.reactions, !reactions.isEmpty {
            return true
        }
        return false
    }
    
    private var avatarPlaceholder: some View {
        Circle()
            .fill(Color(hex: "8B5CF6"))
            .frame(width: 28, height: 28)
            .overlay(
                Text(String((message.sender?.username ?? "?").prefix(1).uppercased()))
                    .font(.caption2)
                    .foregroundColor(.white)
            )
    }
    
    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            if isOwnMessage {
                Spacer(minLength: 60)
            } else {
                // Avatar - aligned to top
                if let avatarUrl = message.sender?.avatarUrl,
                   let url = URL(string: avatarUrl) {
                    AsyncImage(url: url) { phase in
                        switch phase {
                        case .success(let image):
                            image
                                .resizable()
                                .scaledToFill()
                        case .failure, .empty:
                            avatarPlaceholder
                        @unknown default:
                            avatarPlaceholder
                        }
                    }
                    .frame(width: 28, height: 28)
                    .clipShape(Circle())
                } else {
                    avatarPlaceholder
                }
            }
            
            VStack(alignment: isOwnMessage ? .trailing : .leading, spacing: 4) {
                // Sender name for group chats
                if !isOwnMessage, let senderName = message.sender?.username {
                    Text(senderName)
                        .font(.caption)
                        .foregroundColor(.gray)
                }
                
                // Message Content with reactions overlapping (bottom-left)
                ZStack(alignment: .bottomLeading) {
                    // Inner ZStack for reaction bar popup
                    ZStack(alignment: isOwnMessage ? .topLeading : .topTrailing) {
                        messageContent
                            .onLongPressGesture {
                                withAnimation(.spring(response: 0.3)) {
                                    showReactionBar = true
                                }
                            }
                        
                        // Quick Reaction Bar Popup (only for non-deleted messages)
                        if showReactionBar && !message.isDeleted {
                            QuickReactionBar(
                                onEmojiSelected: { emoji in
                                    onReaction(emoji)
                                    withAnimation {
                                        showReactionBar = false
                                    }
                                },
                                onShowFullPicker: {
                                    showReactionBar = false
                                    onShowFullEmojiPicker()
                                },
                                onReply: {
                                    showReactionBar = false
                                    onReply()
                                },
                                onCopy: {
                                    showReactionBar = false
                                    onCopy()
                                },
                                onDelete: {
                                    showReactionBar = false
                                    onDelete()
                                },
                                showDeleteForEveryone: isOwnMessage
                            )
                            .frame(width: 320)
                            .offset(y: -120)
                            .transition(.scale.combined(with: .opacity))
                        }
                    }
                    
                    // Reactions overlapping bottom edge of bubble (top ~10% overlaps)
                    if let reactions = message.reactions, !reactions.isEmpty {
                        reactionsView(reactions)
                            .offset(x: 4, y: 20)
                    }
                }
                .padding(.bottom, hasReactions ? 22 : 0)
            }
            
            if !isOwnMessage {
                Spacer(minLength: 60)
            }
        }
        .padding(.vertical, 2)
        .animation(.spring(response: 0.3), value: showReactionBar)
        .onTapGesture {
            if showReactionBar {
                withAnimation {
                    showReactionBar = false
                }
            }
        }
    }
    
    // MARK: - Message Content
    @ViewBuilder
    var messageContent: some View {
        VStack(alignment: .leading, spacing: 4) {
            // Check if message is deleted
            if message.isDeleted {
                HStack(spacing: 4) {
                    Image(systemName: "nosign")
                        .font(.caption)
                    Text("This message was deleted")
                        .italic()
                }
                .foregroundColor(.gray)
                .font(.subheadline)
            } else {
                // Quoted message (reply)
                if let replyTo = message.replyTo {
                    QuotedMessageView(
                        replyTo: replyTo,
                        isOwnMessage: isOwnMessage,
                        onTap: {
                            onScrollToMessage?(replyTo.messageId)
                        }
                    )
                }

                // Image/GIF Attachment
                if message.type == .image || message.type == .gif {
                    imageContent
                }
                // Document Attachment
                else if message.type == .file, let attachment = message.attachment {
                    documentContent(attachment)
                }
                // Text Content
                else {
                    textContent
                }
            }

            // Timestamp inside bubble (bottom-right)
            HStack {
                Spacer()
                HStack(spacing: 4) {
                    Text(formatTime(message.createdAt))
                        .font(.system(size: 10))
                        .foregroundColor(isOwnMessage ? .white.opacity(0.7) : .gray)
                    
                    if isOwnMessage {
                        Image(systemName: statusIcon)
                            .font(.system(size: 10))
                            .foregroundColor(isOwnMessage ? statusColor.opacity(0.9) : statusColor)
                    }
                }
            }
        }
        .padding(12)
        .background(
            isOwnMessage ?
            Color(hex: "8B5CF6") :
            Color.white.opacity(0.1)
        )
        .cornerRadius(16, corners: isOwnMessage ? [.topLeft, .topRight, .bottomLeft] : [.topLeft, .topRight, .bottomRight])
    }
    
    // MARK: - Text Content
    @ViewBuilder
    var textContent: some View {
        Text(showTranslation ? (message.translatedContent ?? message.originalContent) : message.originalContent)
            .foregroundColor(.white)
        
        // Translation toggle if available
        if message.translatedContent != nil {
            Button(action: { showTranslation.toggle() }) {
                HStack(spacing: 4) {
                    Image(systemName: "globe")
                    Text(showTranslation ? "Show Original" : "Translate")
                }
                .font(.caption2)
                .foregroundColor(isOwnMessage ? .white.opacity(0.7) : Color(hex: "8B5CF6"))
            }
        }
    }
    
    // MARK: - Image Content
    @ViewBuilder
    var imageContent: some View {
        Group {
            if message.type == .gif {
                // GIF - use URL directly from originalContent
                AsyncImage(url: URL(string: message.originalContent)) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                            .frame(maxWidth: 200, maxHeight: 200)
                    case .failure:
                        imageErrorView
                    case .empty:
                        ProgressView()
                            .frame(width: 100, height: 100)
                    @unknown default:
                        EmptyView()
                    }
                }
                .contextMenu {
                    Button {
                        onSaveImage?(message.originalContent)
                    } label: {
                        Label("Save to Photos", systemImage: "square.and.arrow.down")
                    }
                }
            } else if let attachment = message.attachment {
                // Image from attachment
                AttachmentImageView(attachment: attachment, onTap: onImageTap)
                    .contextMenu {
                        if let url = attachment.url {
                            Button {
                                onSaveImage?(url)
                            } label: {
                                Label("Save to Photos", systemImage: "square.and.arrow.down")
                            }
                        }
                    }
            }
        }
        .cornerRadius(8)
    }
    
    var imageErrorView: some View {
        VStack {
            Image(systemName: "photo")
                .font(.title)
                .foregroundColor(.gray)
            Text("Failed to load")
                .font(.caption)
                .foregroundColor(.gray)
        }
        .frame(width: 100, height: 100)
        .background(Color.white.opacity(0.1))
    }
    
    // MARK: - Document Content
    func documentContent(_ attachment: Attachment) -> some View {
        HStack {
            Image(systemName: documentIcon(for: attachment.contentType))
                .font(.title2)
                .foregroundColor(isOwnMessage ? .white : Color(hex: "8B5CF6"))
            
            VStack(alignment: .leading, spacing: 2) {
                Text(attachment.fileName)
                    .font(.subheadline)
                    .fontWeight(.medium)
                    .foregroundColor(.white)
                    .lineLimit(1)
                
                Text(formatFileSize(attachment.fileSize))
                    .font(.caption)
                    .foregroundColor(.white.opacity(0.7))
            }
            
            Spacer()
            
            // Download button
            Button {
                onDownloadDocument?(attachment)
            } label: {
                Image(systemName: "arrow.down.circle")
                    .font(.title2)
                    .foregroundColor(isOwnMessage ? .white.opacity(0.7) : Color(hex: "8B5CF6"))
            }
        }
        .padding(4)
        .contextMenu {
            Button {
                onDownloadDocument?(attachment)
            } label: {
                Label("Download", systemImage: "arrow.down.circle")
            }
        }
    }
    
    // MARK: - Reactions Display
    func reactionsView(_ reactions: [String: [String]]) -> some View {
        HStack(spacing: 6) {
            ForEach(Array(reactions.keys.sorted()), id: \.self) { emoji in
                if let users = reactions[emoji], !users.isEmpty {
                    Button(action: { onReaction(emoji) }) {
                        HStack(spacing: 2) {
                            Text(emoji)
                                .font(.system(size: 14))
                            if users.count > 1 {
                                Text("\(users.count)")
                                    .font(.caption2)
                                    .foregroundColor(.white.opacity(0.7))
                            }
                        }
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 6)
        .background(Color(hex: "1F1F1F"))  // Solid dark background (Surface800)
        .cornerRadius(14)
        .shadow(color: .black.opacity(0.2), radius: 4, y: 2)
    }
    
    // MARK: - Helper Functions
    var statusIcon: String {
        // Check readBy first for read receipts
        if let readBy = message.readBy, !readBy.isEmpty {
            return "checkmark.circle.fill"
        }
        
        switch message.status {
        case .sending:
            return "clock"
        case .sent, .none:
            return "checkmark"
        case .delivered:
            return "checkmark.circle"
        case .seen:
            return "checkmark.circle.fill"
        case .failed:
            return "exclamationmark.circle"
        }
    }
    
    var statusColor: Color {
        // Check readBy first for read receipts
        if let readBy = message.readBy, !readBy.isEmpty {
            return Color(hex: "8B5CF6")
        }
        
        switch message.status {
        case .failed:
            return .red
        case .seen:
            return Color(hex: "8B5CF6")
        default:
            return .gray
        }
    }
    
    func formatTime(_ isoString: String) -> String {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        
        guard let date = formatter.date(from: isoString) else {
            formatter.formatOptions = [.withInternetDateTime]
            guard let date = formatter.date(from: isoString) else {
                return ""
            }
            return formatTimeFromDate(date)
        }
        
        return formatTimeFromDate(date)
    }
    
    func formatTimeFromDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "h:mm a"
        return formatter.string(from: date)
    }
    
    func documentIcon(for contentType: String) -> String {
        switch contentType {
        case let type where type.contains("pdf"):
            return "doc.fill"
        case let type where type.contains("word"):
            return "doc.text.fill"
        case let type where type.contains("excel"), let type where type.contains("spreadsheet"):
            return "tablecells.fill"
        case let type where type.contains("text"):
            return "doc.plaintext.fill"
        default:
            return "doc.fill"
        }
    }
    
    func formatFileSize(_ bytes: Int64) -> String {
        let formatter = ByteCountFormatter()
        formatter.countStyle = .file
        return formatter.string(fromByteCount: bytes)
    }
}

// MARK: - Attachment Image View
struct AttachmentImageView: View {
    let attachment: Attachment
    let onTap: (URL) -> Void
    
    @State private var imageURL: URL?
    @State private var isLoading = true
    @State private var loadError = false
    
    var body: some View {
        Group {
            if let url = imageURL {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                            .frame(maxWidth: 200, maxHeight: 200)
                            .onTapGesture {
                                onTap(url)
                            }
                    case .failure:
                        errorView
                    case .empty:
                        loadingView
                    @unknown default:
                        EmptyView()
                    }
                }
            } else if isLoading {
                loadingView
            } else {
                errorView
            }
        }
        .task {
            await loadImageURL()
        }
    }
    
    var loadingView: some View {
        ProgressView()
            .frame(width: 100, height: 100)
    }
    
    var errorView: some View {
        VStack {
            Image(systemName: "photo")
                .font(.title)
                .foregroundColor(.gray)
            Text("Failed to load")
                .font(.caption)
                .foregroundColor(.gray)
            
            if loadError {
                Button("Retry") {
                    Task { await loadImageURL() }
                }
                .font(.caption)
                .foregroundColor(Color(hex: "8B5CF6"))
            }
        }
        .frame(width: 100, height: 100)
        .background(Color.white.opacity(0.1))
    }
    
    func loadImageURL() async {
        isLoading = true
        loadError = false
        
        do {
            // Use cached URL when available (30-minute cache)
            imageURL = try await DownloadURLCache.shared.getURL(for: attachment.key)
        } catch {
            logger.error("❌ Failed to get download URL: \(error.localizedDescription, privacy: .public)")
            loadError = true
        }
        
        isLoading = false
    }
}

// MARK: - Corner Radius Extension
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

// MARK: - Share Sheet
struct ShareSheet: UIViewControllerRepresentable {
    let activityItems: [Any]
    
    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: activityItems, applicationActivities: nil)
    }
    
    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}

#Preview {
    ChatView(conversation: Conversation(
        id: "1",
        type: "direct",
        name: nil,
        participants: [UserPublic(id: "1", username: "Test User", preferredLanguage: "en", avatarUrl: nil)],
        lastMessage: nil,
        createdAt: "",
        updatedAt: ""
    ))
    .environmentObject(AuthManager.shared)
}
