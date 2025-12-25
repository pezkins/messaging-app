import SwiftUI
import PhotosUI

struct SettingsView: View {
    @EnvironmentObject var authManager: AuthManager
    @Environment(\.dismiss) var dismiss
    
    @State private var showingLanguagePicker = false
    @State private var showingCountryPicker = false
    @State private var showingRegionPicker = false
    @State private var showingAppLanguagePicker = false
    @State private var showingEditName = false
    @State private var showingWhatsNew = false
    @State private var showingSignOutConfirm = false
    
    @StateObject private var localizationManager = LocalizationManager.shared
    
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
            .localizedLayoutDirection()
            .navigationTitle("settings_title".localized)
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
            .sheet(isPresented: $showingRegionPicker) {
                if let countryCode = authManager.currentUser?.preferredCountry {
                    RegionPickerSheet(countryCode: countryCode, onSelect: updateRegion)
                }
            }
            .sheet(isPresented: $showingAppLanguagePicker) {
                AppLanguagePickerSheet(localizationManager: localizationManager)
            }
            .sheet(isPresented: $showingWhatsNew) {
                WhatsNewSheet()
            }
            .alert("settings_edit_name".localized, isPresented: $showingEditName) {
                TextField("settings_display_name".localized, text: $editingName)
                Button("common_cancel".localized, role: .cancel) { }
                Button("common_save".localized) {
                    Task { await updateDisplayName() }
                }
            }
            .confirmationDialog("settings_sign_out".localized, isPresented: $showingSignOutConfirm) {
                Button("settings_sign_out".localized, role: .destructive) {
                    Task { await authManager.signOut() }
                }
                Button("common_cancel".localized, role: .cancel) { }
            } message: {
                Text("settings_sign_out_confirm".localized)
            }
            // Image source picker action sheet
            .confirmationDialog("settings_change_photo".localized, isPresented: $showingImageSourcePicker) {
                Button("settings_take_photo".localized) {
                    showingCamera = true
                }
                Button("settings_choose_library".localized) {
                    showingPhotoPicker = true
                }
                if authManager.currentUser?.avatarUrl != nil {
                    Button("settings_remove_photo".localized, role: .destructive) {
                        Task { await removeProfilePhoto() }
                    }
                }
                Button("common_cancel".localized, role: .cancel) { }
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
            .alert("settings_photo_error".localized, isPresented: $showPhotoError) {
                Button("common_ok".localized) { }
            } message: {
                Text(photoError ?? "settings_photo_error_message".localized)
            }
        }
    }
    
    // MARK: - Profile Section
    var profileSection: some View {
        VStack(spacing: 16) {
            // Avatar - Tappable with camera overlay
            Button(action: { showingImageSourcePicker = true }) {
                ZStack(alignment: .bottomTrailing) {
                    // Avatar circle - use id to force refresh when URL changes
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
                        .id(avatarUrl) // Force refresh when URL changes
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
            Text("settings_preferences".localized)
                .font(.caption)
                .fontWeight(.semibold)
                .foregroundColor(.gray)
                .padding(.leading, 4)
            
            VStack(spacing: 0) {
                // App UI Language (NEW)
                settingsRow(
                    icon: "textformat",
                    iconColor: Color.cyan,
                    title: "settings_app_language".localized,
                    value: getAppLanguageDisplayName(),
                    action: { showingAppLanguagePicker = true }
                )
                
                Divider()
                    .background(Color.white.opacity(0.1))
                
                // Message Translation Language
                settingsRow(
                    icon: "globe",
                    iconColor: Color(hex: "8B5CF6"),
                    title: "settings_language".localized,
                    value: getLanguageName(authManager.currentUser?.preferredLanguage ?? "en"),
                    action: { showingLanguagePicker = true }
                )
                
                Divider()
                    .background(Color.white.opacity(0.1))
                
                // Country
                settingsRow(
                    icon: "map",
                    iconColor: Color.green,
                    title: "settings_country".localized,
                    value: getCountryName(authManager.currentUser?.preferredCountry),
                    action: { showingCountryPicker = true }
                )
                
                // Region (only show if country has regions)
                if let countryCode = authManager.currentUser?.preferredCountry,
                   hasRegions(countryCode) {
                    Divider()
                        .background(Color.white.opacity(0.1))
                    
                    settingsRow(
                        icon: "mappin.and.ellipse",
                        iconColor: Color.orange,
                        title: "settings_region".localized,
                        value: getRegionName(authManager.currentUser?.preferredCountry, authManager.currentUser?.preferredRegion),
                        action: { showingRegionPicker = true }
                    )
                }
            }
            .background(Color.white.opacity(0.05))
            .cornerRadius(12)
        }
    }
    
    // Get display name for current app language
    func getAppLanguageDisplayName() -> String {
        if localizationManager.appLanguage == "auto" {
            let deviceLang = localizationManager.currentLanguageCode
            if let language = getLanguageByCode(deviceLang) {
                return "Auto (\(language.name))"
            }
            return "Auto"
        }
        return getLanguageName(localizationManager.appLanguage)
    }
    
    // MARK: - About Section
    var aboutSection: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("settings_about".localized)
                .font(.caption)
                .fontWeight(.semibold)
                .foregroundColor(.gray)
                .padding(.leading, 4)
            
            VStack(spacing: 0) {
                // What's New
                settingsRow(
                    icon: "sparkles",
                    iconColor: Color.yellow,
                    title: "settings_whats_new".localized,
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
                    
                    Text("settings_version".localized)
                        .foregroundColor(.white)
                    
                    Spacer()
                    
                    Text(Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "0.0.0")
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
                Text("settings_sign_out".localized)
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
        guard let code = code else { return "common_not_set".localized }
        return getCountryByCode(code)?.name ?? code
    }
    
    func getRegionName(_ countryCode: String?, _ regionCode: String?) -> String {
        guard let countryCode = countryCode, let regionCode = regionCode else { return "common_not_set".localized }
        return getRegionByCode(countryCode, regionCode: regionCode)?.name ?? regionCode
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
                
                // Clear region when country changes (regions are country-specific)
                if hasRegions(country.code) {
                    // Don't clear, will prompt to select new region
                } else {
                    // Clear region for countries without regions
                    let regionResponse = try await APIService.shared.updateRegion(preferredRegion: nil)
                    await authManager.updateUser(regionResponse.user)
                }
            } catch {
                // Handle error
            }
        }
        showingCountryPicker = false
    }
    
    func updateRegion(_ region: Region) {
        Task {
            do {
                let response = try await APIService.shared.updateRegion(preferredRegion: region.code)
                await authManager.updateUser(response.user)
            } catch {
                // Handle error
            }
        }
        showingRegionPicker = false
    }
    
    // MARK: - Profile Photo Actions
    func handlePhotoSelection(_ item: PhotosPickerItem?) async {
        NSLog("📸 handlePhotoSelection: Called with item: \(item != nil)")
        
        guard let item = item else {
            NSLog("📸 handlePhotoSelection: No item provided")
            return
        }
        
        guard let data = try? await item.loadTransferable(type: Data.self) else {
            NSLog("📸 handlePhotoSelection: Failed to load data from item")
            return
        }
        
        guard let image = UIImage(data: data) else {
            NSLog("📸 handlePhotoSelection: Failed to create UIImage from data")
            return
        }
        
        NSLog("📸 handlePhotoSelection: Image loaded successfully, size: \(image.size)")
        selectedPhotoItem = nil
        await uploadProfileImage(image)
    }
    
    func uploadProfileImage(_ image: UIImage) async {
        NSLog("📸 uploadProfileImage: Starting...")
        
        // Resize image to 512x512
        let resizedImage = resizeImage(image, targetSize: CGSize(width: 512, height: 512))
        
        guard let imageData = resizedImage.jpegData(compressionQuality: 0.8) else {
            NSLog("📸 uploadProfileImage: Failed to create JPEG data")
            photoError = "Failed to process image"
            showPhotoError = true
            return
        }
        
        NSLog("📸 uploadProfileImage: Image data size: \(imageData.count) bytes")
        isUploadingPhoto = true
        
        do {
            // Get presigned upload URL from profile picture endpoint
            NSLog("📸 uploadProfileImage: Getting upload URL...")
            let uploadResponse = try await APIService.shared.getProfileUploadUrl(
                fileName: "profile-\(UUID().uuidString).jpg",
                contentType: "image/jpeg",
                fileSize: imageData.count
            )
            NSLog("📸 uploadProfileImage: Got upload URL, key: \(uploadResponse.key)")
            
            // Upload to S3
            NSLog("📸 uploadProfileImage: Uploading to S3...")
            try await APIService.shared.uploadFile(
                uploadUrl: uploadResponse.uploadUrl,
                data: imageData,
                contentType: "image/jpeg"
            )
            NSLog("📸 uploadProfileImage: S3 upload complete")
            
            // Update user profile with the S3 key (backend constructs the full URL)
            NSLog("📸 uploadProfileImage: Updating profile picture...")
            let response = try await APIService.shared.updateProfilePicture(key: uploadResponse.key)
            NSLog("📸 uploadProfileImage: Profile updated, avatarUrl: \(response.user.avatarUrl ?? "nil")")
            await authManager.updateUser(response.user)
            NSLog("📸 uploadProfileImage: Success!")
            
        } catch {
            NSLog("📸 uploadProfileImage: Error - \(error)")
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
                    TextField("setup_search_languages".localized, text: $searchText)
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
            .navigationTitle("setup_select_language".localized)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("common_cancel".localized) { dismiss() }
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
                    TextField("setup_search_countries".localized, text: $searchText)
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
            .navigationTitle("setup_select_country".localized)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("common_cancel".localized) { dismiss() }
                }
            }
        }
    }
}

// MARK: - Region Picker Sheet
struct RegionPickerSheet: View {
    @Environment(\.dismiss) var dismiss
    let countryCode: String
    var onSelect: (Region) -> Void
    
    var regions: [Region] {
        return getRegionsForCountry(countryCode)
    }
    
    var body: some View {
        NavigationView {
            ZStack {
                Color(hex: "0F0F0F").ignoresSafeArea()
                
                VStack {
                    if regions.isEmpty {
                        Text("empty_search_results".localized)
                            .foregroundColor(.gray)
                            .padding()
                    } else {
                        List(regions, id: \.code) { region in
                            Button(action: {
                                onSelect(region)
                                dismiss()
                            }) {
                                HStack {
                                    Text(region.name)
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
            }
            .navigationTitle("setup_select_region".localized)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("common_cancel".localized) { dismiss() }
                }
            }
        }
    }
}

// MARK: - App Language Picker Sheet
struct AppLanguagePickerSheet: View {
    @Environment(\.dismiss) var dismiss
    @ObservedObject var localizationManager: LocalizationManager
    
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
                
                VStack(spacing: 0) {
                    // Search bar
                    TextField("setup_search_languages".localized, text: $searchText)
                        .textFieldStyle(.plain)
                        .padding()
                        .background(Color.white.opacity(0.1))
                        .cornerRadius(12)
                        .foregroundColor(.white)
                        .padding()
                    
                    List {
                        // Auto option (use device language)
                        Button(action: {
                            localizationManager.appLanguage = "auto"
                            dismiss()
                        }) {
                            HStack {
                                VStack(alignment: .leading) {
                                    Text("language_auto".localized)
                                        .foregroundColor(.white)
                                    Text("settings_app_language_auto".localized)
                                        .font(.caption)
                                        .foregroundColor(.gray)
                                }
                                Spacer()
                                if localizationManager.appLanguage == "auto" {
                                    Image(systemName: "checkmark.circle.fill")
                                        .foregroundColor(Color(hex: "8B5CF6"))
                                }
                            }
                        }
                        .listRowBackground(
                            localizationManager.appLanguage == "auto" ?
                            Color(hex: "8B5CF6").opacity(0.2) :
                            Color.white.opacity(0.05)
                        )
                        
                        // All languages
                        ForEach(filteredLanguages) { language in
                            Button(action: {
                                localizationManager.appLanguage = language.code
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
                                    if localizationManager.appLanguage == language.code {
                                        Image(systemName: "checkmark.circle.fill")
                                            .foregroundColor(Color(hex: "8B5CF6"))
                                    }
                                }
                            }
                            .listRowBackground(
                                localizationManager.appLanguage == language.code ?
                                Color(hex: "8B5CF6").opacity(0.2) :
                                Color.white.opacity(0.05)
                            )
                        }
                    }
                    .listStyle(.plain)
                    .scrollContentBackground(.hidden)
                }
            }
            .navigationTitle("settings_app_language".localized)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("common_cancel".localized) { dismiss() }
                }
            }
        }
    }
}

// MARK: - What's New Sheet
struct WhatsNewSheet: View {
    @Environment(\.dismiss) var dismiss
    
    // Show first 5 changelog entries
    private var entries: [ChangelogEntry] {
        Array(Changelog.entries.prefix(5))
    }
    
    var body: some View {
        NavigationView {
            ZStack {
                Color(hex: "0F0F0F").ignoresSafeArea()
                
                ScrollView {
                    VStack(alignment: .leading, spacing: 24) {
                        ForEach(Array(entries.enumerated()), id: \.element.id) { index, entry in
                            VStack(alignment: .leading, spacing: 8) {
                                // Version header
                                VStack(alignment: .leading, spacing: 4) {
                                    Text("\("settings_version".localized) \(entry.version)")
                                        .font(index == 0 ? .title2 : .headline)
                                        .fontWeight(.bold)
                                        .foregroundColor(.white)
                                    
                                    Text(entry.title)
                                        .font(.caption)
                                        .foregroundColor(.gray)
                                }
                                
                                // Changes list
                                VStack(alignment: .leading, spacing: 8) {
                                    ForEach(entry.changes, id: \.self) { change in
                                        Text(change)
                                            .font(.subheadline)
                                            .foregroundColor(.gray)
                                    }
                                }
                                .padding(.top, 4)
                            }
                            
                            if index < entries.count - 1 {
                                Divider().background(Color.gray.opacity(0.3))
                            }
                        }
                    }
                    .padding()
                }
            }
            .navigationTitle("settings_whats_new".localized)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("common_done".localized) { dismiss() }
                }
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
