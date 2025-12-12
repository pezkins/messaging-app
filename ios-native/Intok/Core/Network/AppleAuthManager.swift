import Foundation
import AuthenticationServices
import CryptoKit
import UIKit

class AppleAuthManager: NSObject, ObservableObject {
    static let shared = AppleAuthManager()
    
    private var currentNonce: String?
    private weak var presentingWindow: UIWindow?
    
    // Published properties for UI feedback
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var showError = false
    
    @MainActor
    func signIn() {
        NSLog("🍎 Apple Sign-In: Starting...")
        
        isLoading = true
        errorMessage = nil
        
        let nonce = randomNonceString()
        currentNonce = nonce
        
        let request = ASAuthorizationAppleIDProvider().createRequest()
        request.requestedScopes = [.fullName, .email]
        request.nonce = sha256(nonce)
        
        // Cache the window reference before performing the request
        if let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
           let window = scene.windows.first(where: { $0.isKeyWindow }) ?? scene.windows.first {
            presentingWindow = window
            NSLog("🍎 Apple Sign-In: Found window")
        } else {
            NSLog("❌ Apple Sign-In: No window found")
            isLoading = false
            errorMessage = "Unable to present Sign in with Apple"
            showError = true
            return
        }
        
        let controller = ASAuthorizationController(authorizationRequests: [request])
        controller.delegate = self
        controller.presentationContextProvider = self
        controller.performRequests()
        
        NSLog("🍎 Apple Sign-In: Request performed")
    }

    private func randomNonceString(length: Int = 32) -> String {
        precondition(length > 0)
        var randomBytes = [UInt8](repeating: 0, count: length)
        let errorCode = SecRandomCopyBytes(kSecRandomDefault, randomBytes.count, &randomBytes)
        if errorCode != errSecSuccess {
            fatalError("Unable to generate nonce")
        }
        let charset: [Character] = Array("0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._")
        return String(randomBytes.map { charset[Int($0) % charset.count] })
    }
    
    private func sha256(_ input: String) -> String {
        let inputData = Data(input.utf8)
        let hashedData = SHA256.hash(data: inputData)
        return hashedData.compactMap { String(format: "%02x", $0) }.joined()
    }
}

// MARK: - ASAuthorizationControllerDelegate
extension AppleAuthManager: ASAuthorizationControllerDelegate {
    func authorizationController(controller: ASAuthorizationController, didCompleteWithAuthorization authorization: ASAuthorization) {
        NSLog("🍎 Apple Sign-In: Authorization completed")
        
        guard let appleIDCredential = authorization.credential as? ASAuthorizationAppleIDCredential else {
            NSLog("❌ Apple Sign-In: Missing credentials")
            DispatchQueue.main.async {
                self.isLoading = false
                self.errorMessage = "Failed to get Apple credentials"
                self.showError = true
            }
            return
        }
        
        // Apple's unique user identifier - this is stable across sign-ins
        let appleUserId = appleIDCredential.user
        
        let fullName = [
            appleIDCredential.fullName?.givenName,
            appleIDCredential.fullName?.familyName
        ].compactMap { $0 }.joined(separator: " ")
        
        // Email is only provided on first sign-in, after that it's nil
        // We need to extract it from the identity token if not provided
        var email = appleIDCredential.email
        
        if email == nil, let identityToken = appleIDCredential.identityToken,
           let tokenString = String(data: identityToken, encoding: .utf8) {
            // Try to decode email from JWT token
            email = decodeEmailFromJWT(tokenString)
        }
        
        // If still no email, use Apple user ID as placeholder (backend will use existing email)
        let finalEmail = email ?? "\(appleUserId)@privaterelay.appleid.com"
        
        NSLog("✅ Apple Sign-In successful - userId: %@, email: %@", appleUserId, finalEmail)
        
        // Send to backend using OAuth endpoint (same as Google)
        Task { @MainActor in
            let errorMessage = await AuthManager.shared.signInWithAppleOAuth(
                providerId: appleUserId,
                email: finalEmail,
                name: fullName.isEmpty ? nil : fullName
            )
            
            self.isLoading = false
            
            if let errorMessage = errorMessage {
                NSLog("❌ Apple Sign-In backend error: %@", errorMessage)
                self.errorMessage = errorMessage
                self.showError = true
            } else {
                NSLog("✅ Apple Sign-In complete - user authenticated")
            }
        }
    }
    
    /// Decode email from Apple's JWT identity token
    private func decodeEmailFromJWT(_ token: String) -> String? {
        let parts = token.split(separator: ".")
        guard parts.count >= 2 else { return nil }
        
        var base64 = String(parts[1])
        // Add padding if needed
        while base64.count % 4 != 0 {
            base64 += "="
        }
        
        guard let data = Data(base64Encoded: base64),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let email = json["email"] as? String else {
            return nil
        }
        
        return email
    }
    
    func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        NSLog("❌ Apple Sign-In: Error received - %@", error.localizedDescription)
        
        DispatchQueue.main.async {
            self.isLoading = false
            
            if let authError = error as? ASAuthorizationError {
                switch authError.code {
                case .canceled:
                    NSLog("🍎 Apple Sign-In: User canceled")
                    // Don't show error for user cancellation
                    return
                case .failed:
                    NSLog("❌ Apple Sign-In failed: %@", error.localizedDescription)
                    self.errorMessage = "Sign in with Apple failed. Please try again."
                case .invalidResponse:
                    NSLog("❌ Apple Sign-In: Invalid response")
                    self.errorMessage = "Invalid response from Apple. Please try again."
                case .notHandled:
                    NSLog("❌ Apple Sign-In: Not handled")
                    self.errorMessage = "Sign in with Apple is not available."
                case .notInteractive:
                    NSLog("❌ Apple Sign-In: Not interactive")
                    self.errorMessage = "Sign in with Apple requires interaction."
                case .unknown:
                    NSLog("❌ Apple Sign-In: Unknown error (code 1000) - Full error: %@", String(describing: error))
                    // Error code 1000 typically means simulator or configuration issue
                    #if targetEnvironment(simulator)
                    self.errorMessage = "Sign in with Apple is not fully supported in the Simulator. Please test on a real device."
                    #else
                    // Log more details for debugging
                    let nsError = error as NSError
                    NSLog("❌ Apple Sign-In: NSError domain=%@, code=%d, userInfo=%@", nsError.domain, nsError.code, String(describing: nsError.userInfo))
                    self.errorMessage = "Sign in with Apple failed. Please check that Sign in with Apple is enabled for this App ID in Apple Developer Portal."
                    #endif
                @unknown default:
                    NSLog("❌ Apple Sign-In: Unexpected error code")
                    self.errorMessage = "An unexpected error occurred."
                }
            } else {
                NSLog("❌ Apple Sign-In error: %@", error.localizedDescription)
                self.errorMessage = "Sign in with Apple failed: \(error.localizedDescription)"
            }
            
            self.showError = true
        }
    }
}

// MARK: - ASAuthorizationControllerPresentationContextProviding
extension AppleAuthManager: ASAuthorizationControllerPresentationContextProviding {
    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        // Use cached window if available
        if let window = presentingWindow {
            return window
        }
        
        // Fallback: try to find any available window
        if let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
           let window = scene.windows.first(where: { $0.isKeyWindow }) ?? scene.windows.first {
            return window
        }
        
        // Last resort: create a new window (shouldn't happen)
        NSLog("⚠️ Apple Sign-In: Creating fallback window")
        let window = UIWindow()
        return window
    }
}
