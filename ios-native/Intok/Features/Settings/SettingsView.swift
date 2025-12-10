import SwiftUI
import PhotosUI

struct SettingsView: View {
    @EnvironmentObject var authManager: AuthManager
    @Environment(\.dismiss) var dismiss
    
    @State private var showingLanguagePicker = false
    @State private var showingCountryPicker = false
    @State private var showingEditName = false
    @State private var showingWhatsNew = false
    @State private var showingSignOutConfirm = false
    
    @State private var editingName = ""
    @State private var isLoading = false
    
    // Profile picture states
    @State private var showingImageSourcePicker = false
    @State private var showingPhotoPicker = false
    @State private var showingCamera = false
    @State private var selectedPhotoItem: PhotosPickerItem?
    @State private var isUploadingPhoto = false
    @State private var photoError: String?
    @State private var showPhotoError = false
    
    var body: some View {
        NavigationView {
            ZStack {
                // Background
                Color(hex: "0F0F0F")
                    .ignoresSafeArea()
                
                ScrollView {
                    VStack(spacing: 24) {
                        // Profile Section
                        profileSection
                        
                        // Preferences Section
                        preferencesSection
                        
                        // About Section
                        aboutSection
                        
                        // Sign Out
                        signOutButton
                    }
                    .padding()
                }
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: { dismiss() }) {
                        Image(systemName: "xmark")
                            .foregroundColor(.white)
                    }
                }
            }
            .sheet(isPresented: $showingLanguagePicker) {
                LanguagePickerSheet(onSelect: updateLanguage)
            }
            .sheet(isPresented: $showingCountryPicker) {
                CountryPickerSheet(onSelect: updateCountry)
            }
            .sheet(isPresented: $showingWhatsNew) {
                WhatsNewSheet()
            }
            .alert("Edit Display Name", isPresented: $showingEditName) {
                TextField("Display Name", text: $editingName)
                Button("Cancel", role: .cancel) { }
                Button("Save") {
                    Task { await updateDisplayName() }
                }
            }
            .confirmationDialog("Sign Out", isPresented: $showingSignOutConfirm) {
                Button("Sign Out", role: .destructive) {
                    Task { await authManager.signOut() }
                }
                Button("Cancel", role: .cancel) { }
            } message: {
                Text("Are you sure you want to sign out?")
            }
            // Image source picker action sheet
            .confirmationDialog("Change Profile Photo", isPresented: $showingImageSourcePicker) {
                Button("Take Photo") {
                    showingCamera = true
                }
                Button("Choose from Library") {
                    showingPhotoPicker = true
                }
                if authManager.currentUser?.avatarUrl != nil {
                    Button("Remove Photo", role: .destructive) {
                        Task { await removeProfilePhoto() }
                    }
                }
                Button("Cancel", role: .cancel) { }
            }
            // Photo library picker
            .photosPicker(isPresented: $showingPhotoPicker, selection: $selectedPhotoItem, matching: .images)
            .onChange(of: selectedPhotoItem) { oldValue, newValue in
                Task { await handlePhotoSelection(newValue) }
            }
            // Camera
            .sheet(isPresented: $showingCamera) {
                ProfileCameraCaptureView { image in
                    Task { await uploadProfileImage(image) }
                }
            }
            // Photo error alert
            .alert("Photo Error", isPresented: $showPhotoError) {
                Button("OK") { }
            } message: {
                Text(photoError ?? "Failed to update profile photo")
            }
        }
    }
    
    // MARK: - Profile Section
    var profileSection: some View {
        VStack(spacing: 16) {
            // Avatar - Tappable with camera overlay
            Button(action: { showingImageSourcePicker = true }) {
                ZStack(alignment: .bottomTrailing) {
                    // Avatar circle
                    if let avatarUrl = authManager.currentUser?.avatarUrl,
                       let url = URL(string: avatarUrl) {
                        AsyncImage(url: url) { phase in
                            switch phase {
                            case .success(let image):
                                image
                                    .resizable()
                                    .scaledToFill()
                            case .failure:
                                avatarPlaceholder
                            case .empty:
                                ProgressView()
                                    .frame(width: 100, height: 100)
                            @unknown default:
                                avatarPlaceholder
                            }
                        }
                        .frame(width: 100, height: 100)
                        .clipShape(Circle())
                    } else {
                        avatarPlaceholder
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
            
            // Display Name
            Button(action: {
                editingName = authManager.currentUser?.username ?? ""
                showingEditName = true
            }) {
                HStack {
                    Text(authManager.currentUser?.username ?? "Unknown")
                        .font(.title2)
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                    
                    Image(systemName: "pencil")
                        .foregroundColor(Color(hex: "8B5CF6"))
                        .font(.caption)
                }
            }
            
            // Email
            Text(authManager.currentUser?.email ?? "")
                .font(.subheadline)
                .foregroundColor(.gray)
        }
        .padding()
        .frame(maxWidth: .infinity)
        .background(Color.white.opacity(0.05))
        .cornerRadius(16)
    }
    
    // Avatar placeholder view
    var avatarPlaceholder: some View {
        Circle()
            .fill(Color(hex: "8B5CF6"))
            .frame(width: 100, height: 100)
            .overlay(
                Text(String(authManager.currentUser?.username.prefix(1).uppercased() ?? "?"))
                    .font(.system(size: 40, weight: .semibold))
                    .foregroundColor(.white)
            )
    }
    
    // MARK: - Preferences Section
    var preferencesSection: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("PREFERENCES")
                .font(.caption)
                .fontWeight(.semibold)
                .foregroundColor(.gray)
                .padding(.leading, 4)
            
            VStack(spacing: 0) {
                // Language
                settingsRow(
                    icon: "globe",
                    iconColor: Color(hex: "8B5CF6"),
                    title: "Language",
                    value: getLanguageName(authManager.currentUser?.preferredLanguage ?? "en"),
                    action: { showingLanguagePicker = true }
                )
                
                Divider()
                    .background(Color.white.opacity(0.1))
                
                // Country
                settingsRow(
                    icon: "map",
                    iconColor: Color.green,
                    title: "Country",
                    value: getCountryName(authManager.currentUser?.preferredCountry),
                    action: { showingCountryPicker = true }
                )
            }
            .background(Color.white.opacity(0.05))
            .cornerRadius(12)
        }
    }
    
    // MARK: - About Section
    var aboutSection: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("ABOUT")
                .font(.caption)
                .fontWeight(.semibold)
                .foregroundColor(.gray)
                .padding(.leading, 4)
            
            VStack(spacing: 0) {
                // What's New
                settingsRow(
                    icon: "sparkles",
                    iconColor: Color.yellow,
                    title: "What's New",
                    value: nil,
                    action: { showingWhatsNew = true }
                )
                
                Divider()
                    .background(Color.white.opacity(0.1))
                
                // Version
                HStack {
                    Image(systemName: "info.circle")
                        .foregroundColor(.blue)
                        .frame(width: 24)
                    
                    Text("Version")
                        .foregroundColor(.white)
                    
                    Spacer()
                    
                    Text("0.1.0")
                        .foregroundColor(.gray)
                }
                .padding()
            }
            .background(Color.white.opacity(0.05))
            .cornerRadius(12)
        }
    }
    
    // MARK: - Sign Out Button
    var signOutButton: some View {
        Button(action: {
            showingSignOutConfirm = true
        }) {
            HStack {
                Image(systemName: "rectangle.portrait.and.arrow.right")
                Text("Sign Out")
            }
            .font(.headline)
            .foregroundColor(.red)
            .frame(maxWidth: .infinity)
            .padding()
            .background(Color.red.opacity(0.1))
            .cornerRadius(12)
        }
    }
    
    // MARK: - Helper Views
    func settingsRow(icon: String, iconColor: Color, title: String, value: String?, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack {
                Image(systemName: icon)
                    .foregroundColor(iconColor)
                    .frame(width: 24)
                
                Text(title)
                    .foregroundColor(.white)
                
                Spacer()
                
                if let value = value {
                    Text(value)
                        .foregroundColor(.gray)
                }
                
                Image(systemName: "chevron.right")
                    .foregroundColor(.gray)
                    .font(.caption)
            }
            .padding()
        }
    }
    
    // MARK: - Helper Functions
    func getLanguageName(_ code: String) -> String {
        return getLanguageByCode(code)?.name ?? code
    }
    
    func getCountryName(_ code: String?) -> String {
        guard let code = code else { return "Not set" }
        return getCountryByCode(code)?.name ?? code
    }
    
    // MARK: - Actions
    func updateDisplayName() async {
        guard editingName.count >= 2 else { return }
        
        isLoading = true
        do {
            let response = try await APIService.shared.updateProfile(username: editingName)
            await authManager.updateUser(response.user)
        } catch {
            // Handle error
        }
        isLoading = false
    }
    
    func updateLanguage(_ language: Language) {
        Task {
            do {
                let response = try await APIService.shared.updateLanguage(preferredLanguage: language.code)
                await authManager.updateUser(response.user)
            } catch {
                // Handle error
            }
        }
        showingLanguagePicker = false
    }
    
    func updateCountry(_ country: Country) {
        Task {
            do {
                let response = try await APIService.shared.updateCountry(preferredCountry: country.code)
                await authManager.updateUser(response.user)
            } catch {
                // Handle error
            }
        }
        showingCountryPicker = false
    }
    
    // MARK: - Profile Photo Actions
    func handlePhotoSelection(_ item: PhotosPickerItem?) async {
        guard let item = item,
              let data = try? await item.loadTransferable(type: Data.self),
              let image = UIImage(data: data) else {
            return
        }
        
        selectedPhotoItem = nil
        await uploadProfileImage(image)
    }
    
    func uploadProfileImage(_ image: UIImage) async {
        // Resize image to 512x512
        let resizedImage = resizeImage(image, targetSize: CGSize(width: 512, height: 512))
        
        guard let imageData = resizedImage.jpegData(compressionQuality: 0.8) else {
            photoError = "Failed to process image"
            showPhotoError = true
            return
        }
        
        isUploadingPhoto = true
        
        do {
            // Get presigned upload URL from profile picture endpoint
            let uploadResponse = try await APIService.shared.getProfileUploadUrl(
                fileName: "profile-\(UUID().uuidString).jpg",
                contentType: "image/jpeg",
                fileSize: imageData.count
            )
            
            // Upload to S3
            try await APIService.shared.uploadFile(
                uploadUrl: uploadResponse.uploadUrl,
                data: imageData,
                contentType: "image/jpeg"
            )
            
            // Update user profile with the S3 key (backend constructs the full URL)
            let response = try await APIService.shared.updateProfilePicture(key: uploadResponse.key)
            await authManager.updateUser(response.user)
            
        } catch {
            photoError = "Failed to upload photo. Please try again."
            showPhotoError = true
        }
        
        isUploadingPhoto = false
    }
    
    func removeProfilePhoto() async {
        isUploadingPhoto = true
        
        do {
            let response = try await APIService.shared.deleteProfilePicture()
            await authManager.updateUser(response.user)
        } catch {
            photoError = "Failed to remove photo. Please try again."
            showPhotoError = true
        }
        
        isUploadingPhoto = false
    }
    
    func resizeImage(_ image: UIImage, targetSize: CGSize) -> UIImage {
        // Calculate aspect ratio to crop to square
        let size = image.size
        let minDimension = min(size.width, size.height)
        
        // Crop to center square
        let xOffset = (size.width - minDimension) / 2
        let yOffset = (size.height - minDimension) / 2
        let cropRect = CGRect(x: xOffset, y: yOffset, width: minDimension, height: minDimension)
        
        guard let cgImage = image.cgImage?.cropping(to: cropRect) else {
            return image
        }
        
        let croppedImage = UIImage(cgImage: cgImage, scale: image.scale, orientation: image.imageOrientation)
        
        // Resize to target size
        let renderer = UIGraphicsImageRenderer(size: targetSize)
        return renderer.image { _ in
            croppedImage.draw(in: CGRect(origin: .zero, size: targetSize))
        }
    }
}

// MARK: - Language Picker Sheet
struct LanguagePickerSheet: View {
    @Environment(\.dismiss) var dismiss
    var onSelect: (Language) -> Void
    
    @State private var searchText = ""
    
    var filteredLanguages: [Language] {
        if searchText.isEmpty {
            return LANGUAGES
        }
        return LANGUAGES.filter {
            $0.name.localizedCaseInsensitiveContains(searchText) ||
            $0.native.localizedCaseInsensitiveContains(searchText)
        }
    }
    
    var body: some View {
        NavigationView {
            ZStack {
                Color(hex: "0F0F0F").ignoresSafeArea()
                
                VStack {
                    TextField("Search languages...", text: $searchText)
                        .textFieldStyle(.plain)
                        .padding()
                        .background(Color.white.opacity(0.1))
                        .cornerRadius(12)
                        .foregroundColor(.white)
                        .padding()
                    
                    List(filteredLanguages) { language in
                        Button(action: {
                            onSelect(language)
                            dismiss()
                        }) {
                            HStack {
                                VStack(alignment: .leading) {
                                    Text(language.name)
                                        .foregroundColor(.white)
                                    Text(language.native)
                                        .font(.caption)
                                        .foregroundColor(.gray)
                                }
                                Spacer()
                            }
                        }
                        .listRowBackground(Color.white.opacity(0.05))
                    }
                    .listStyle(.plain)
                    .scrollContentBackground(.hidden)
                }
            }
            .navigationTitle("Select Language")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }
}

// MARK: - Country Picker Sheet
struct CountryPickerSheet: View {
    @Environment(\.dismiss) var dismiss
    var onSelect: (Country) -> Void
    
    @State private var searchText = ""
    
    var filteredCountries: [Country] {
        if searchText.isEmpty {
            return COUNTRIES
        }
        return COUNTRIES.filter {
            $0.name.localizedCaseInsensitiveContains(searchText)
        }
    }
    
    var body: some View {
        NavigationView {
            ZStack {
                Color(hex: "0F0F0F").ignoresSafeArea()
                
                VStack {
                    TextField("Search countries...", text: $searchText)
                        .textFieldStyle(.plain)
                        .padding()
                        .background(Color.white.opacity(0.1))
                        .cornerRadius(12)
                        .foregroundColor(.white)
                        .padding()
                    
                    List(filteredCountries) { country in
                        Button(action: {
                            onSelect(country)
                            dismiss()
                        }) {
                            HStack {
                                Text(country.flag)
                                    .font(.title2)
                                Text(country.name)
                                    .foregroundColor(.white)
                                Spacer()
                            }
                        }
                        .listRowBackground(Color.white.opacity(0.05))
                    }
                    .listStyle(.plain)
                    .scrollContentBackground(.hidden)
                }
            }
            .navigationTitle("Select Country")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }
}

// MARK: - What's New Sheet
struct WhatsNewSheet: View {
    @Environment(\.dismiss) var dismiss
    
    var body: some View {
        NavigationView {
            ZStack {
                Color(hex: "0F0F0F").ignoresSafeArea()
                
                ScrollView {
                    VStack(alignment: .leading, spacing: 24) {
                        // Version 0.1.4
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Version 0.1.4")
                                .font(.title2)
                                .fontWeight(.bold)
                                .foregroundColor(.white)
                            
                            Text("Smart Translation Update")
                                .font(.caption)
                                .foregroundColor(.gray)
                        }
                        
                        VStack(alignment: .leading, spacing: 12) {
                            featureRow(icon: "doc.text.magnifyingglass", title: "Document Translation Control", description: "Choose whether to translate documents when sharing")
                            featureRow(icon: "photo.badge.checkmark", title: "Optimized Media Sharing", description: "Images and GIFs no longer go through translation")
                            featureRow(icon: "bolt.fill", title: "Faster Performance", description: "Improved message handling and delivery")
                        }
                        
                        Divider().background(Color.gray.opacity(0.3))
                        
                        // Version 0.1.3
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Version 0.1.3")
                                .font(.headline)
                                .foregroundColor(.white)
                            
                            Text("Rich Messaging Update")
                                .font(.caption)
                                .foregroundColor(.gray)
                        }
                        
                        VStack(alignment: .leading, spacing: 12) {
                            featureRow(icon: "photo.on.rectangle", title: "Image Sharing", description: "Share photos from your library")
                            featureRow(icon: "camera.fill", title: "Camera Integration", description: "Capture and send photos directly")
                            featureRow(icon: "face.smiling.inverse", title: "GIF Support", description: "Search and send GIFs via GIPHY")
                            featureRow(icon: "doc.fill", title: "Document Sharing", description: "Share PDFs and documents")
                            featureRow(icon: "hand.thumbsup.fill", title: "Message Reactions", description: "React to messages with emojis")
                        }
                    }
                    .padding()
                }
            }
            .navigationTitle("What's New")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
    
    func featureRow(icon: String, title: String, description: String) -> some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: icon)
                .foregroundColor(Color(hex: "8B5CF6"))
                .font(.title3)
                .frame(width: 32)
            
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .fontWeight(.semibold)
                    .foregroundColor(.white)
                
                Text(description)
                    .font(.subheadline)
                    .foregroundColor(.gray)
            }
        }
    }
}

// MARK: - Profile Camera Capture View
struct ProfileCameraCaptureView: UIViewControllerRepresentable {
    let onImageCaptured: (UIImage) -> Void
    @Environment(\.dismiss) var dismiss
    
    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.sourceType = .camera
        picker.cameraDevice = .front
        picker.allowsEditing = true
        picker.delegate = context.coordinator
        return picker
    }
    
    func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {}
    
    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }
    
    class Coordinator: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
        let parent: ProfileCameraCaptureView
        
        init(_ parent: ProfileCameraCaptureView) {
            self.parent = parent
        }
        
        func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey : Any]) {
            if let editedImage = info[.editedImage] as? UIImage {
                parent.onImageCaptured(editedImage)
            } else if let originalImage = info[.originalImage] as? UIImage {
                parent.onImageCaptured(originalImage)
            }
            parent.dismiss()
        }
        
        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            parent.dismiss()
        }
    }
}

#Preview {
    SettingsView()
        .environmentObject(AuthManager.shared)
}
