import SwiftUI
import PhotosUI

struct GroupInfoView: View {
    let conversation: Conversation
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject var authManager: AuthManager
    @EnvironmentObject var chatStore: ChatStore
    
    @State private var groupName: String = ""
    @State private var isEditingName = false
    @State private var isSaving = false
    @State private var showingImagePicker = false
    @State private var selectedPhotoItem: PhotosPickerItem?
    @State private var isUploadingPhoto = false
    @State private var errorMessage: String?
    @State private var showError = false
    
    var body: some View {
        NavigationStack {
            ZStack {
                Color(hex: "0F0F0F")
                    .ignoresSafeArea()
                
                ScrollView {
                    VStack(spacing: 24) {
                        // Group Picture
                        groupPictureSection
                        
                        // Group Name
                        groupNameSection
                        
                        // Participant Count
                        Text("group_info_participants".localized(with: conversation.participants.count))
                            .font(.subheadline)
                            .foregroundColor(.gray)
                        
                        Divider()
                            .background(Color.white.opacity(0.1))
                            .padding(.horizontal)
                        
                        // Participants Section
                        VStack(alignment: .leading, spacing: 16) {
                            Text("group_info_participants_header".localized)
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
            .navigationTitle("group_info_title".localized)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") {
                        dismiss()
                    }
                    .foregroundColor(Color(hex: "8B5CF6"))
                }
            }
            .photosPicker(isPresented: $showingImagePicker, selection: $selectedPhotoItem, matching: .images)
            .onChange(of: selectedPhotoItem) { oldValue, newValue in
                Task { await handlePhotoSelection(newValue) }
            }
            .alert("Edit Group Name", isPresented: $isEditingName) {
                TextField("Group Name", text: $groupName)
                Button("Cancel", role: .cancel) { }
                Button("Save") {
                    Task { await saveGroupName() }
                }
            }
            .alert("Error", isPresented: $showError) {
                Button("OK") { }
            } message: {
                Text(errorMessage ?? "Something went wrong")
            }
            .onAppear {
                groupName = conversation.name ?? ""
            }
        }
    }
    
    // MARK: - Group Picture Section
    var groupPictureSection: some View {
        Button(action: { showingImagePicker = true }) {
            ZStack(alignment: .bottomTrailing) {
                // Group picture or placeholder
                if let pictureUrl = conversation.pictureUrl, let url = URL(string: pictureUrl) {
                    AsyncImage(url: url) { image in
                        image
                            .resizable()
                            .scaledToFill()
                    } placeholder: {
                        groupPlaceholder
                    }
                    .frame(width: 100, height: 100)
                    .clipShape(Circle())
                } else {
                    groupPlaceholder
                }
                
                // Camera icon overlay
                Circle()
                    .fill(Color(hex: "2A2A2A"))
                    .frame(width: 32, height: 32)
                    .overlay(
                        Group {
                            if isUploadingPhoto {
                                ProgressView()
                                    .scaleEffect(0.7)
                                    .tint(.white)
                            } else {
                                Image(systemName: "camera.fill")
                                    .font(.system(size: 14))
                                    .foregroundColor(.white)
                            }
                        }
                    )
                    .overlay(
                        Circle()
                            .stroke(Color(hex: "0F0F0F"), lineWidth: 3)
                    )
            }
        }
        .disabled(isUploadingPhoto)
        .padding(.top, 24)
    }
    
    var groupPlaceholder: some View {
        Circle()
            .fill(Color(hex: "8B5CF6"))
            .frame(width: 100, height: 100)
            .overlay(
                Image(systemName: "person.3.fill")
                    .font(.system(size: 40))
                    .foregroundColor(.white)
            )
    }
    
    // MARK: - Group Name Section
    var groupNameSection: some View {
        Button(action: { isEditingName = true }) {
            HStack {
                Text(conversation.name ?? "Group Chat")
                    .font(.title2)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
                
                Image(systemName: "pencil")
                    .foregroundColor(Color(hex: "8B5CF6"))
                    .font(.caption)
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
                        Text("group_info_you".localized)
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
    
    // MARK: - Actions
    func saveGroupName() async {
        guard !groupName.isEmpty else { return }
        
        isSaving = true
        do {
            let response = try await APIService.shared.updateConversation(
                conversationId: conversation.id,
                name: groupName,
                pictureUrl: nil
            )
            await chatStore.updateConversation(response.conversation)
        } catch {
            errorMessage = "Failed to update group name"
            showError = true
        }
        isSaving = false
    }
    
    func handlePhotoSelection(_ item: PhotosPickerItem?) async {
        guard let item = item else { return }
        
        guard let data = try? await item.loadTransferable(type: Data.self) else { return }
        guard let image = UIImage(data: data) else { return }
        
        selectedPhotoItem = nil
        await uploadGroupPicture(image)
    }
    
    func uploadGroupPicture(_ image: UIImage) async {
        // Resize image to 512x512
        let resizedImage = resizeImage(image, targetSize: CGSize(width: 512, height: 512))
        
        guard let imageData = resizedImage.jpegData(compressionQuality: 0.8) else {
            errorMessage = "Failed to process image"
            showError = true
            return
        }
        
        isUploadingPhoto = true
        
        do {
            // Get presigned upload URL
            let uploadResponse = try await APIService.shared.getGroupPictureUploadUrl(
                conversationId: conversation.id,
                fileName: "group-\(UUID().uuidString).jpg",
                contentType: "image/jpeg",
                fileSize: imageData.count
            )
            
            // Upload to S3
            try await APIService.shared.uploadFile(
                uploadUrl: uploadResponse.uploadUrl,
                data: imageData,
                contentType: "image/jpeg"
            )
            
            // Update conversation with the new picture URL
            let pictureUrl = "https://intok-attachments.s3.amazonaws.com/\(uploadResponse.key)"
            let response = try await APIService.shared.updateConversation(
                conversationId: conversation.id,
                name: nil,
                pictureUrl: pictureUrl
            )
            await chatStore.updateConversation(response.conversation)
            
        } catch {
            errorMessage = "Failed to upload picture"
            showError = true
        }
        
        isUploadingPhoto = false
    }
    
    func resizeImage(_ image: UIImage, targetSize: CGSize) -> UIImage {
        let size = image.size
        let minDimension = min(size.width, size.height)
        
        let xOffset = (size.width - minDimension) / 2
        let yOffset = (size.height - minDimension) / 2
        let cropRect = CGRect(x: xOffset, y: yOffset, width: minDimension, height: minDimension)
        
        guard let cgImage = image.cgImage?.cropping(to: cropRect) else {
            return image
        }
        
        let croppedImage = UIImage(cgImage: cgImage, scale: image.scale, orientation: image.imageOrientation)
        
        let renderer = UIGraphicsImageRenderer(size: targetSize)
        return renderer.image { _ in
            croppedImage.draw(in: CGRect(origin: .zero, size: targetSize))
        }
    }
}

#Preview {
    GroupInfoView(conversation: Conversation(
        id: "1",
        type: "group",
        name: "Family Chat",
        pictureUrl: nil,
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
    .environmentObject(ChatStore.shared)
}
