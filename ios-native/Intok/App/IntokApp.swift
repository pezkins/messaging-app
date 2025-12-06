import SwiftUI
import GoogleSignIn

@main
struct IntokApp: App {
    @StateObject private var authManager = AuthManager()
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(authManager)
                .preferredColorScheme(.dark)
                .onOpenURL { url in
                    // Handle Google Sign-In callback URL
                    GIDSignIn.sharedInstance.handle(url)
                }
        }
    }
}
