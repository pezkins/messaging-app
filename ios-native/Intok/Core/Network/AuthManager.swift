import Foundation
import Combine

@MainActor
class AuthManager: ObservableObject {
    @Published var isAuthenticated = false
    @Published var needsSetup = false
    @Published var currentUser: User?
    @Published var isLoading = false
    @Published var error: String?
    
    private var accessToken: String?
    private var refreshToken: String?
    
    private let userDefaults = UserDefaults.standard
    private let tokenKey = "intok_access_token"
    private let refreshTokenKey = "intok_refresh_token"
    private let userKey = "intok_user"
    
    init() {
        loadStoredAuth()
    }
    
    // MARK: - Public Methods
    
    func signInWithGoogle(idToken: String) async {
        isLoading = true
        error = nil
        
        do {
            // TODO: Implement actual API call
            // let response = try await APIService.shared.oauthLogin(provider: "google", token: idToken)
            
            // Simulate successful login for now
            try await Task.sleep(nanoseconds: 1_000_000_000)
            
            // Mock response
            let mockUser = User(
                id: UUID().uuidString,
                email: "user@example.com",
                username: "NewUser",
                preferredLanguage: "en",
                preferredCountry: "US",
                avatarUrl: nil,
                createdAt: ISO8601DateFormatter().string(from: Date()),
                updatedAt: ISO8601DateFormatter().string(from: Date())
            )
            
            currentUser = mockUser
            isAuthenticated = true
            needsSetup = true // New user needs setup
            
        } catch {
            self.error = error.localizedDescription
        }
        
        isLoading = false
    }
    
    func signInWithEmail(email: String, password: String) async {
        isLoading = true
        error = nil
        
        do {
            // TODO: Implement actual API call
            try await Task.sleep(nanoseconds: 1_000_000_000)
            
            // Mock response
            let mockUser = User(
                id: UUID().uuidString,
                email: email,
                username: email.components(separatedBy: "@").first ?? "User",
                preferredLanguage: "en",
                preferredCountry: "US",
                avatarUrl: nil,
                createdAt: ISO8601DateFormatter().string(from: Date()),
                updatedAt: ISO8601DateFormatter().string(from: Date())
            )
            
            currentUser = mockUser
            isAuthenticated = true
            needsSetup = false
            
        } catch {
            self.error = error.localizedDescription
        }
        
        isLoading = false
    }
    
    func completeSetup(displayName: String, language: String, country: String) async {
        isLoading = true
        error = nil
        
        do {
            // TODO: Implement actual API call to update profile
            try await Task.sleep(nanoseconds: 500_000_000)
            
            // Update local user
            if var user = currentUser {
                currentUser = User(
                    id: user.id,
                    email: user.email,
                    username: displayName,
                    preferredLanguage: language,
                    preferredCountry: country,
                    avatarUrl: user.avatarUrl,
                    createdAt: user.createdAt,
                    updatedAt: ISO8601DateFormatter().string(from: Date())
                )
            }
            
            needsSetup = false
            saveAuth()
            
        } catch {
            self.error = error.localizedDescription
        }
        
        isLoading = false
    }
    
    func signOut() {
        accessToken = nil
        refreshToken = nil
        currentUser = nil
        isAuthenticated = false
        needsSetup = false
        clearStoredAuth()
    }
    
    // MARK: - Private Methods
    
    private func loadStoredAuth() {
        accessToken = userDefaults.string(forKey: tokenKey)
        refreshToken = userDefaults.string(forKey: refreshTokenKey)
        
        if let userData = userDefaults.data(forKey: userKey),
           let user = try? JSONDecoder().decode(User.self, from: userData) {
            currentUser = user
            isAuthenticated = true
        }
    }
    
    private func saveAuth() {
        userDefaults.set(accessToken, forKey: tokenKey)
        userDefaults.set(refreshToken, forKey: refreshTokenKey)
        
        if let user = currentUser,
           let userData = try? JSONEncoder().encode(user) {
            userDefaults.set(userData, forKey: userKey)
        }
    }
    
    private func clearStoredAuth() {
        userDefaults.removeObject(forKey: tokenKey)
        userDefaults.removeObject(forKey: refreshTokenKey)
        userDefaults.removeObject(forKey: userKey)
    }
}

