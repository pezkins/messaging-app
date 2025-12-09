import SwiftUI

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
        }
    }
    
    // MARK: - Profile Section
    var profileSection: some View {
        VStack(spacing: 16) {
            // Avatar
            if let avatarUrl = authManager.currentUser?.avatarUrl,
               let url = URL(string: avatarUrl) {
                AsyncImage(url: url) { image in
                    image
                        .resizable()
                        .scaledToFill()
                } placeholder: {
                    Circle()
                        .fill(Color(hex: "8B5CF6"))
                        .overlay(
                            Text(String(authManager.currentUser?.username.prefix(1).uppercased() ?? "?"))
                                .font(.title)
                                .foregroundColor(.white)
                        )
                }
                .frame(width: 100, height: 100)
                .clipShape(Circle())
            } else {
                Circle()
                    .fill(Color(hex: "8B5CF6"))
                    .frame(width: 100, height: 100)
                    .overlay(
                        Text(String(authManager.currentUser?.username.prefix(1).uppercased() ?? "?"))
                            .font(.title)
                            .foregroundColor(.white)
                    )
            }
            
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
                        // Version 0.1.3
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Version 0.1.3")
                                .font(.title2)
                                .fontWeight(.bold)
                                .foregroundColor(.white)
                            
                            Text("Rich Messaging Update")
                                .font(.caption)
                                .foregroundColor(.gray)
                        }
                        
                        VStack(alignment: .leading, spacing: 12) {
                            featureRow(icon: "photo.on.rectangle", title: "Image Sharing", description: "Share photos from your library in conversations")
                            featureRow(icon: "camera.fill", title: "Camera Integration", description: "Capture and send photos directly from the camera")
                            featureRow(icon: "face.smiling.inverse", title: "GIF Support", description: "Search and send GIFs powered by GIPHY")
                            featureRow(icon: "doc.fill", title: "Document Sharing", description: "Share PDFs and documents in chats")
                            featureRow(icon: "hand.thumbsup.fill", title: "Message Reactions", description: "Long-press messages to add emoji reactions")
                        }
                        
                        Divider().background(Color.gray.opacity(0.3))
                        
                        // Version 0.1.1
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Version 0.1.1")
                                .font(.headline)
                                .foregroundColor(.white)
                            
                            Text("Initial Release")
                                .font(.caption)
                                .foregroundColor(.gray)
                        }
                        
                        VStack(alignment: .leading, spacing: 12) {
                            featureRow(icon: "message.fill", title: "Real-time Messaging", description: "Send and receive messages instantly")
                            featureRow(icon: "globe", title: "Auto Translation", description: "Messages are automatically translated to your preferred language")
                            featureRow(icon: "person.2.fill", title: "Group Chats", description: "Create group conversations with multiple participants")
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

#Preview {
    SettingsView()
        .environmentObject(AuthManager.shared)
}
