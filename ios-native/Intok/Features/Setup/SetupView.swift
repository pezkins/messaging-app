import SwiftUI

struct SetupView: View {
    @EnvironmentObject var authManager: AuthManager
    
    @State private var currentStep = 0
    @State private var displayName = ""
    @State private var selectedLanguage: Language?
    @State private var selectedCountry: Country?
    @State private var selectedRegion: Region?
    @State private var isLoading = false
    @State private var error: String?
    
    @State private var languageSearchText = ""
    @State private var countrySearchText = ""
    
    var countryHasRegions: Bool {
        guard let countryCode = selectedCountry?.code else { return false }
        return hasRegions(countryCode)
    }
    
    var availableRegions: [Region] {
        guard let countryCode = selectedCountry?.code else { return [] }
        return getRegionsForCountry(countryCode)
    }
    
    var totalSteps: Int {
        // 3 base steps + 1 if country has regions
        return countryHasRegions ? 4 : 3
    }
    
    var filteredLanguages: [Language] {
        if languageSearchText.isEmpty {
            return LANGUAGES
        }
        return LANGUAGES.filter {
            $0.name.localizedCaseInsensitiveContains(languageSearchText) ||
            $0.native.localizedCaseInsensitiveContains(languageSearchText)
        }
    }
    
    var filteredCountries: [Country] {
        if countrySearchText.isEmpty {
            return COUNTRIES
        }
        return COUNTRIES.filter {
            $0.name.localizedCaseInsensitiveContains(countrySearchText)
        }
    }
    
    var body: some View {
        ZStack {
            // Background gradient
            LinearGradient(
                gradient: Gradient(colors: [Color(hex: "0F0F0F"), Color(hex: "1A1A2E")]),
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()
            
            VStack(spacing: 0) {
                // Progress Indicator (dynamic based on whether country has regions)
                HStack(spacing: 8) {
                    ForEach(0..<totalSteps, id: \.self) { index in
                        Circle()
                            .fill(index <= currentStep ? Color(hex: "8B5CF6") : Color.gray.opacity(0.3))
                            .frame(width: 8, height: 8)
                    }
                }
                .padding(.top, 20)
                
                // Content
                TabView(selection: $currentStep) {
                    // Step 1: Display Name
                    displayNameStep
                        .tag(0)
                    
                    // Step 2: Language
                    languageStep
                        .tag(1)
                    
                    // Step 3: Country
                    countryStep
                        .tag(2)
                    
                    // Step 4: Region (only if country has regions)
                    if countryHasRegions {
                        regionStep
                            .tag(3)
                    }
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
                .animation(.easeInOut, value: currentStep)
            }
        }
    }
    
    // MARK: - Step 1: Display Name
    var displayNameStep: some View {
        VStack(spacing: 24) {
            Spacer()
            
            Image(systemName: "person.circle.fill")
                .font(.system(size: 80))
                .foregroundColor(Color(hex: "8B5CF6"))
            
            Text("setup_choose_display_name".localized)
                .font(.title)
                .fontWeight(.bold)
                .foregroundColor(.white)
            
            Text("setup_how_others_see_you".localized)
                .font(.subheadline)
                .foregroundColor(.gray)
            
            TextField("setup_enter_name_placeholder".localized, text: $displayName)
                .textFieldStyle(.plain)
                .padding()
                .background(Color.white.opacity(0.1))
                .cornerRadius(12)
                .foregroundColor(.white)
                .padding(.horizontal, 24)
            
            if let error = error {
                Text(error)
                    .font(.caption)
                    .foregroundColor(.red)
            }
            
            Spacer()
            
            Button(action: {
                Task {
                    await saveDisplayName()
                }
            }) {
                HStack {
                    if isLoading {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle(tint: .white))
                    } else {
                        Text("common_continue".localized)
                        Image(systemName: "arrow.right")
                    }
                }
                .font(.headline)
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding()
                .background(
                    displayName.count >= 2 ? Color(hex: "8B5CF6") : Color.gray
                )
                .cornerRadius(16)
            }
            .disabled(displayName.count < 2 || isLoading)
            .padding(.horizontal, 24)
            .padding(.bottom, 40)
        }
    }
    
    // MARK: - Step 2: Language
    var languageStep: some View {
        VStack(spacing: 24) {
            VStack(spacing: 8) {
                Image(systemName: "globe")
                    .font(.system(size: 60))
                    .foregroundColor(Color(hex: "8B5CF6"))
                
                Text("setup_select_language".localized)
                    .font(.title)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
                
                Text("setup_messages_translated".localized)
                    .font(.subheadline)
                    .foregroundColor(.gray)
            }
            .padding(.top, 40)
            
            // Search
            TextField("setup_search_languages".localized, text: $languageSearchText)
                .textFieldStyle(.plain)
                .padding()
                .background(Color.white.opacity(0.1))
                .cornerRadius(12)
                .foregroundColor(.white)
                .padding(.horizontal, 24)
            
            // Language List
            ScrollView {
                LazyVStack(spacing: 8) {
                    ForEach(filteredLanguages) { language in
                        Button(action: {
                            selectedLanguage = language
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
                                
                                if selectedLanguage?.code == language.code {
                                    Image(systemName: "checkmark.circle.fill")
                                        .foregroundColor(Color(hex: "8B5CF6"))
                                }
                            }
                            .padding()
                            .background(
                                selectedLanguage?.code == language.code ?
                                Color(hex: "8B5CF6").opacity(0.2) :
                                Color.white.opacity(0.05)
                            )
                            .cornerRadius(12)
                        }
                    }
                }
                .padding(.horizontal, 24)
            }
            
            Button(action: {
                Task {
                    await saveLanguage()
                }
            }) {
                HStack {
                    if isLoading {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle(tint: .white))
                    } else {
                        Text("common_continue".localized)
                        Image(systemName: "arrow.right")
                    }
                }
                .font(.headline)
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding()
                .background(
                    selectedLanguage != nil ? Color(hex: "8B5CF6") : Color.gray
                )
                .cornerRadius(16)
            }
            .disabled(selectedLanguage == nil || isLoading)
            .padding(.horizontal, 24)
            .padding(.bottom, 40)
        }
    }
    
    // MARK: - Step 3: Country
    var countryStep: some View {
        VStack(spacing: 24) {
            VStack(spacing: 8) {
                Image(systemName: "map")
                    .font(.system(size: 60))
                    .foregroundColor(Color(hex: "8B5CF6"))
                
                Text("setup_select_country".localized)
                    .font(.title)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
                
                Text("setup_country_optional".localized)
                    .font(.subheadline)
                    .foregroundColor(.gray)
            }
            .padding(.top, 40)
            
            // Search
            TextField("setup_search_countries".localized, text: $countrySearchText)
                .textFieldStyle(.plain)
                .padding()
                .background(Color.white.opacity(0.1))
                .cornerRadius(12)
                .foregroundColor(.white)
                .padding(.horizontal, 24)
            
            // Country List
            ScrollView {
                LazyVStack(spacing: 8) {
                    ForEach(filteredCountries) { country in
                        Button(action: {
                            selectedCountry = country
                        }) {
                            HStack {
                                Text(country.flag)
                                    .font(.title2)
                                
                                Text(country.name)
                                    .foregroundColor(.white)
                                
                                Spacer()
                                
                                if selectedCountry?.code == country.code {
                                    Image(systemName: "checkmark.circle.fill")
                                        .foregroundColor(Color(hex: "8B5CF6"))
                                }
                            }
                            .padding()
                            .background(
                                selectedCountry?.code == country.code ?
                                Color(hex: "8B5CF6").opacity(0.2) :
                                Color.white.opacity(0.05)
                            )
                            .cornerRadius(12)
                        }
                    }
                }
                .padding(.horizontal, 24)
            }
            
            HStack(spacing: 12) {
                Button(action: {
                    completeSetup()
                }) {
                    Text("common_skip".localized)
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.white.opacity(0.1))
                        .cornerRadius(16)
                }
                
                Button(action: {
                    Task {
                        await saveCountry()
                    }
                }) {
                    HStack {
                        if isLoading {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                        } else {
                            // Show "Continue" if country has regions, otherwise "Finish"
                            if countryHasRegions {
                                Text("common_continue".localized)
                                Image(systemName: "arrow.right")
                            } else {
                                Text("common_finish".localized)
                                Image(systemName: "checkmark")
                            }
                        }
                    }
                    .font(.headline)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color(hex: "8B5CF6"))
                    .cornerRadius(16)
                }
                .disabled(isLoading)
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 40)
        }
    }
    
    // MARK: - Step 4: Region (only for countries with regions)
    var regionStep: some View {
        VStack(spacing: 24) {
            VStack(spacing: 8) {
                Image(systemName: "mappin.and.ellipse")
                    .font(.system(size: 60))
                    .foregroundColor(Color(hex: "8B5CF6"))
                
                Text("setup_select_region".localized)
                    .font(.title)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
                
                Text("setup_region_accuracy".localized)
                    .font(.subheadline)
                    .foregroundColor(.gray)
            }
            .padding(.top, 40)
            
            // Region List
            ScrollView {
                LazyVStack(spacing: 8) {
                    ForEach(availableRegions, id: \.code) { region in
                        Button(action: {
                            selectedRegion = region
                        }) {
                            HStack {
                                Text(region.name)
                                    .foregroundColor(.white)
                                
                                Spacer()
                                
                                if selectedRegion?.code == region.code {
                                    Image(systemName: "checkmark.circle.fill")
                                        .foregroundColor(Color(hex: "8B5CF6"))
                                }
                            }
                            .padding()
                            .background(
                                selectedRegion?.code == region.code ?
                                Color(hex: "8B5CF6").opacity(0.2) :
                                Color.white.opacity(0.05)
                            )
                            .cornerRadius(12)
                        }
                    }
                }
                .padding(.horizontal, 24)
            }
            
            HStack(spacing: 12) {
                Button(action: {
                    completeSetup()
                }) {
                    Text("common_skip".localized)
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.white.opacity(0.1))
                        .cornerRadius(16)
                }
                
                Button(action: {
                    Task {
                        await saveRegion()
                    }
                }) {
                    HStack {
                        if isLoading {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                        } else {
                            Text("common_finish".localized)
                            Image(systemName: "checkmark")
                        }
                    }
                    .font(.headline)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color(hex: "8B5CF6"))
                    .cornerRadius(16)
                }
                .disabled(isLoading)
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 40)
        }
    }
    
    // MARK: - Actions
    private func saveDisplayName() async {
        guard displayName.count >= 2 else { return }
        
        isLoading = true
        error = nil
        
        do {
            let response = try await APIService.shared.updateProfile(username: displayName)
            await authManager.updateUser(response.user)
            withAnimation {
                currentStep = 1
            }
        } catch {
            self.error = error.localizedDescription
        }
        
        isLoading = false
    }
    
    private func saveLanguage() async {
        guard let language = selectedLanguage else { return }
        
        isLoading = true
        error = nil
        
        do {
            let response = try await APIService.shared.updateLanguage(preferredLanguage: language.code)
            await authManager.updateUser(response.user)
            withAnimation {
                currentStep = 2
            }
        } catch {
            self.error = error.localizedDescription
        }
        
        isLoading = false
    }
    
    private func saveCountry() async {
        guard let country = selectedCountry else {
            completeSetup()
            return
        }
        
        isLoading = true
        error = nil
        
        do {
            let response = try await APIService.shared.updateCountry(preferredCountry: country.code)
            await authManager.updateUser(response.user)
            
            // Check if country has regions - if so, go to region step
            if hasRegions(country.code) {
                withAnimation {
                    currentStep = 3
                }
            } else {
                completeSetup()
            }
        } catch {
            self.error = error.localizedDescription
        }
        
        isLoading = false
    }
    
    private func saveRegion() async {
        guard let region = selectedRegion else {
            completeSetup()
            return
        }
        
        isLoading = true
        error = nil
        
        do {
            let response = try await APIService.shared.updateRegion(preferredRegion: region.code)
            await authManager.updateUser(response.user)
            completeSetup()
        } catch {
            self.error = error.localizedDescription
        }
        
        isLoading = false
    }
    
    private func completeSetup() {
        authManager.completeSetup()
    }
}

#Preview {
    SetupView()
        .environmentObject(AuthManager.shared)
}


