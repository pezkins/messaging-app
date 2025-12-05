import SwiftUI

struct ContentView: View {
    @EnvironmentObject var authManager: AuthManager
    
    var body: some View {
        Group {
            if authManager.isAuthenticated {
                if authManager.needsSetup {
                    SetupView()
                } else {
                    MainTabView()
                }
            } else {
                LoginView()
            }
        }
        .animation(.easeInOut, value: authManager.isAuthenticated)
    }
}

struct MainTabView: View {
    var body: some View {
        NavigationStack {
            ConversationsView()
        }
    }
}

#Preview {
    ContentView()
        .environmentObject(AuthManager())
}

