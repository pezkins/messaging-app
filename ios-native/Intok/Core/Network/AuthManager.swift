import Foundation
import Combine
import GoogleSignIn
import os.log

private let logger = Logger(subsystem: "com.pezkins.intok", category: "AuthManager")

@MainActor
class AuthManager: ObservableObject {
    static let shared = AuthManager()
    
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
    
    private init() {
        loadStoredAuth()
    }
    
    // MARK: - Public Methods
    
    func signInWithGoogle() async {
        isLoading = true
        error = nil
        
        // Step 1: Get Google user info
        var googleUserId: String?
        var googleEmail: String?
        var googleName: String?
        var googleAvatarUrl: String?
        
        do {
            guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                  let rootViewController = windowScene.windows.first?.rootViewController else {
                throw AuthError.noRootViewController
            }
            
            let result = try await GIDSignIn.sharedInstance.signIn(withPresenting: rootViewController)
            
            guard let profile = result.user.profile else {
                throw AuthError.noUserProfile
            }
            
            googleUserId = result.user.userID
            googleEmail = profile.email
            googleName = profile.name
            googleAvatarUrl = profile.imageURL(withDimension: 200)?.absoluteString
            
            logger.info("✅ Google Sign-In successful: \(profile.email, privacy: .public)")
            
        } catch let googleError as NSError {
            // Check if GIDSignIn has user data despite the error (keychain errors on simulator)
            if let currentUser = GIDSignIn.sharedInstance.currentUser,
               let profile = currentUser.profile {
                logger.warning("⚠️ GIDSignIn error but user data available: \(googleError.localizedDescription, privacy: .public)")
                googleUserId = currentUser.userID
                googleEmail = profile.email
                googleName = profile.name
                googleAvatarUrl = profile.imageURL(withDimension: 200)?.absoluteString
                logger.info("✅ Retrieved user from GIDSignIn.currentUser: \(profile.email, privacy: .public)")
            } else {
                logger.error("❌ Google Sign-In failed: \(googleError.localizedDescription, privacy: .public)")
                self.error = googleError.localizedDescription
                isLoading = false
                return
            }
        }
        
        // Step 2: Authenticate with backend
        guard let userId = googleUserId, let email = googleEmail else {
            logger.error("❌ No Google user data available")
            self.error = "Failed to get Google user information"
            isLoading = false
            return
        }
        
        logger.info("📤 Calling backend OAuth with email: \(email, privacy: .public)")
        
        do {
            let response = try await APIService.shared.oauthLogin(
                provider: "google",
                providerId: userId,
                email: email,
                name: googleName,
                avatarUrl: googleAvatarUrl
            )
            
            logger.info("✅ Backend auth successful: \(response.user.email, privacy: .public)")
            logger.info("✅ isNewUser: \(String(describing: response.isNewUser)), username: \(response.user.username, privacy: .public)")
            
            // Store tokens
            accessToken = response.accessToken
            refreshToken = response.refreshToken
            APIService.shared.setAccessToken(response.accessToken)
            
            // Connect WebSocket
            WebSocketService.shared.connect(token: response.accessToken)
            
            // Update state
            currentUser = response.user
            isAuthenticated = true
            needsSetup = response.isNewUser ?? (response.user.username.isEmpty || response.user.username == response.user.email)
            
            saveAuth()
            logger.info("✅ Auth state updated - isAuthenticated: \(self.isAuthenticated), needsSetup: \(self.needsSetup)")
            
        } catch {
            logger.error("❌ Backend auth failed: \(error.localizedDescription, privacy: .public)")
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
    
    func signOut() async {
        // Disconnect WebSocket
        WebSocketService.shared.disconnect()
        
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
        
        logger.info("✅ Signed out")
    }
    
    func updateUser(_ user: User) async {
        currentUser = user
        saveAuth()
    }
    
    func completeSetup() {
        needsSetup = false
        saveAuth()
    }
    
    // MARK: - Private Methods
    
    private func loadStoredAuth() {
        accessToken = userDefaults.string(forKey: tokenKey)
        refreshToken = userDefaults.string(forKey: refreshTokenKey)
        
        if let token = accessToken {
            APIService.shared.setAccessToken(token)
            // Connect WebSocket when restoring auth
            WebSocketService.shared.connect(token: token)
            logger.info("🔌 WebSocket connection initiated on auth restore")
        }
        
        if let userData = userDefaults.data(forKey: userKey),
           let user = try? JSONDecoder().decode(User.self, from: userData) {
            currentUser = user
            isAuthenticated = true
            logger.info("✅ Restored auth for: \(user.email, privacy: .public)")
            
            // Validate token by fetching current user - this will catch expired tokens
            Task {
                await validateAndRefreshTokenIfNeeded()
            }
        }
    }
    
    // MARK: - Token Validation & Refresh
    private func validateAndRefreshTokenIfNeeded() async {
        do {
            // Try to fetch current user to validate token
            let response = try await APIService.shared.getMe()
            currentUser = response.user
            saveAuth()
            logger.info("✅ Token validated successfully")
        } catch APIError.unauthorized {
            // Token expired, try to refresh
            logger.warning("⚠️ Access token expired, attempting refresh...")
            await attemptTokenRefresh()
        } catch {
            logger.error("❌ Token validation failed: \(error.localizedDescription, privacy: .public)")
        }
    }
    
    private func attemptTokenRefresh() async {
        guard let refreshTok = refreshToken else {
            logger.error("❌ No refresh token available, signing out")
            await signOut()
            return
        }
        
        do {
            let response = try await APIService.shared.refreshToken(refreshTok)
            
            // Update tokens
            accessToken = response.accessToken
            refreshToken = response.refreshToken
            APIService.shared.setAccessToken(response.accessToken)
            
            // Reconnect WebSocket with new token
            WebSocketService.shared.disconnect()
            WebSocketService.shared.connect(token: response.accessToken)
            
            saveAuth()
            logger.info("✅ Token refreshed successfully")
        } catch {
            logger.error("❌ Token refresh failed: \(error.localizedDescription, privacy: .public)")
            // Refresh failed, sign out user
            await signOut()
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
