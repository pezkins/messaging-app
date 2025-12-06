import Foundation
import Combine
import GoogleSignIn

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
    
    func signInWithGoogle() async {
        isLoading = true
        error = nil
        
        do {
            // Get the root view controller
            guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                  let rootViewController = windowScene.windows.first?.rootViewController else {
                throw AuthError.noRootViewController
            }
            
            // Perform Google Sign-In
            let result = try await GIDSignIn.sharedInstance.signIn(withPresenting: rootViewController)
            
            guard let user = result.user.profile else {
                throw AuthError.noUserProfile
            }
            
            print("✅ Google Sign-In successful: \(user.email)")
            
            // Call backend OAuth endpoint
            let response = try await APIService.shared.oauthLogin(
                provider: "google",
                providerId: result.user.userID ?? "",
                email: user.email,
                name: user.name,
                avatarUrl: user.imageURL(withDimension: 200)?.absoluteString
            )
            
            print("✅ Backend auth successful: \(response.user.email)")
            
            // Store tokens
            accessToken = response.accessToken
            refreshToken = response.refreshToken
            APIService.shared.setAccessToken(response.accessToken)
            
            // Update state
            currentUser = response.user
            isAuthenticated = true
            needsSetup = response.isNewUser ?? (response.user.username.isEmpty || response.user.username == response.user.email)
            
            saveAuth()
            
        } catch {
            print("❌ Sign-in error: \(error.localizedDescription)")
            self.error = error.localizedDescription
        }
        
        isLoading = false
    }
    
    func completeSetup(displayName: String, language: String, country: String) async {
        isLoading = true
        error = nil
        
        do {
            // Update profile on backend
            let _ = try await APIService.shared.updateProfile(username: displayName)
            let _ = try await APIService.shared.updateLanguage(preferredLanguage: language)
            let userResponse = try await APIService.shared.updateCountry(preferredCountry: country)
            
            // Update local user
            currentUser = userResponse.user
            needsSetup = false
            saveAuth()
            
            print("✅ Setup complete for: \(displayName)")
            
        } catch {
            print("❌ Setup error: \(error.localizedDescription)")
            self.error = error.localizedDescription
        }
        
        isLoading = false
    }
    
    func signOut() {
        // Sign out from Google
        GIDSignIn.sharedInstance.signOut()
        
        // Clear local state
        accessToken = nil
        refreshToken = nil
        currentUser = nil
        isAuthenticated = false
        needsSetup = false
        APIService.shared.setAccessToken(nil)
        clearStoredAuth()
        
        print("✅ Signed out")
    }
    
    // MARK: - Private Methods
    
    private func loadStoredAuth() {
        accessToken = userDefaults.string(forKey: tokenKey)
        refreshToken = userDefaults.string(forKey: refreshTokenKey)
        
        if let token = accessToken {
            APIService.shared.setAccessToken(token)
        }
        
        if let userData = userDefaults.data(forKey: userKey),
           let user = try? JSONDecoder().decode(User.self, from: userData) {
            currentUser = user
            isAuthenticated = true
            print("✅ Restored auth for: \(user.email)")
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

// MARK: - Auth Errors
enum AuthError: LocalizedError {
    case noRootViewController
    case noUserProfile
    case cancelled
    
    var errorDescription: String? {
        switch self {
        case .noRootViewController:
            return "Could not find root view controller"
        case .noUserProfile:
            return "Could not get user profile from Google"
        case .cancelled:
            return "Sign-in was cancelled"
        }
    }
}
