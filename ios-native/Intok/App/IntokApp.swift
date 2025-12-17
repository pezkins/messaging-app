import SwiftUI
import GoogleSignIn
import UserNotifications

@main
struct IntokApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(AuthManager.shared)
                .environmentObject(ChatStore.shared)
                .preferredColorScheme(.dark)
                .onOpenURL { url in
                    // Handle Google Sign-In callback URL
                    GIDSignIn.sharedInstance.handle(url)
                }
        }
    }
}

// MARK: - App Delegate for Push Notifications
class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    
    /// Stored APNs token for registration after login
    static var apnsToken: String?
    
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        requestNotificationPermission()
        return true
    }
    
    func requestNotificationPermission() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { granted, error in
            if granted {
                DispatchQueue.main.async {
                    UIApplication.shared.registerForRemoteNotifications()
                }
                print("📱 Push notification permission granted")
            } else if let error = error {
                print("❌ Push notification permission error: \(error)")
            }
        }
    }
    
    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        let token = deviceToken.map { String(format: "%02.2hhx", $0) }.joined()
        print("📱 APNs Token received: \(token.prefix(20))...")
        
        // Store token for later registration (after login)
        AppDelegate.apnsToken = token
        
        // Try to register with backend if user is already logged in
        Task {
            await registerDeviceTokenIfAuthenticated(token)
        }
    }
    
    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        print("❌ APNs registration failed: \(error)")
    }
    
    // Handle notification when app is in foreground
    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification) async -> UNNotificationPresentationOptions {
        print("📱 Notification received in foreground")
        return [.banner, .sound, .badge]
    }
    
    // Handle notification tap
    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse) async {
        let userInfo = response.notification.request.content.userInfo
        print("📱 Notification tapped: \(userInfo)")
        
        if let conversationId = userInfo["conversationId"] as? String {
            // Navigate to conversation
            await MainActor.run {
                NotificationCenter.default.post(
                    name: .openConversation,
                    object: nil,
                    userInfo: ["conversationId": conversationId]
                )
            }
        }
    }
    
    /// Register device token only if user is authenticated
    private func registerDeviceTokenIfAuthenticated(_ token: String) async {
        // Check if user is authenticated (has access token)
        guard APIService.shared.getAccessToken() != nil else {
            print("📱 APNs token stored, will register after login")
            return
        }
        
        await Self.registerStoredDeviceToken()
    }
    
    /// Static method to register stored token (called after login)
    static func registerStoredDeviceToken() async {
        guard let token = apnsToken else {
            print("📱 No APNs token to register")
            return
        }
        
        do {
            try await APIService.shared.registerDeviceToken(token: token, platform: "ios")
            print("✅ Device token registered with backend")
        } catch {
            print("❌ Failed to register device token: \(error)")
        }
    }
}

// MARK: - Notification Names
extension Notification.Name {
    static let openConversation = Notification.Name("openConversation")
}
