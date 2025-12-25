import SwiftUI

struct ContentView: View {
    @EnvironmentObject var authManager: AuthManager
    @StateObject private var whatsNewManager = WhatsNewManager.shared
    
    var body: some View {
        Group {
            if authManager.isLoading && authManager.currentUser == nil {
                // Loading state
                loadingView
            } else if !authManager.isAuthenticated {
                // Not authenticated - show login
                LoginView()
            } else if authManager.needsSetup {
                // Authenticated but needs setup
                SetupView()
            } else {
                // Fully authenticated - show main app
                MainTabView()
            }
        }
        .animation(.easeInOut, value: authManager.isAuthenticated)
        .animation(.easeInOut, value: authManager.needsSetup)
        .onAppear {
            if authManager.isAuthenticated && !authManager.needsSetup {
                whatsNewManager.checkForNewVersion()
            }
        }
        .sheet(isPresented: $whatsNewManager.shouldShowWhatsNew) {
            WhatsNewView()
        }
        .onReceive(NotificationCenter.default.publisher(for: .openConversation)) { notification in
            if let conversationId = notification.userInfo?["conversationId"] as? String {
                // Handle navigation to conversation via ChatStore
                print("📱 Opening conversation: \(conversationId)")
                ChatStore.shared.pendingConversationId = conversationId
            }
        }
    }
    
    var loadingView: some View {
        ZStack {
            Color(hex: "0F0F0F")
                .ignoresSafeArea()
            
            VStack(spacing: 20) {
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: Color(hex: "8B5CF6")))
                    .scaleEffect(1.5)
                
                Text("common_loading".localized)
                    .foregroundColor(.gray)
            }
        }
    }
}

// MARK: - Main Tab View
struct MainTabView: View {
    @State private var selectedTab = 0
    
    var body: some View {
        TabView(selection: $selectedTab) {
            ConversationsView()
                .tabItem {
                    Image(systemName: "message.fill")
                    Text("conversations_title".localized)
                }
                .tag(0)
            
            // Future: Additional tabs can be added here
            // e.g., Contacts, Profile, etc.
        }
        .accentColor(Color(hex: "8B5CF6"))
    }
}

#Preview {
    ContentView()
        .environmentObject(AuthManager.shared)
}
