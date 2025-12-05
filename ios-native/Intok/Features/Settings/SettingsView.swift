import SwiftUI

struct SettingsView: View {
    @EnvironmentObject var authManager: AuthManager
    @Environment(\.dismiss) private var dismiss
    @State private var showLogoutAlert = false
    
    var body: some View {
        NavigationStack {
            ZStack {
                Color.surface950.ignoresSafeArea()
                
                ScrollView {
                    VStack(spacing: 24) {
                        // Profile Section
                        VStack(spacing: 16) {
                            Circle()
                                .fill(Color.purple600)
                                .frame(width: 80, height: 80)
                                .overlay(
                                    Text(authManager.currentUser?.username.prefix(1).uppercased() ?? "U")
                                        .font(.title)
                                        .foregroundColor(.white)
                                )
                            
                            Text(authManager.currentUser?.username ?? "User")
                                .font(.titleLarge)
                                .foregroundColor(.white)
                            
                            Text("🇺🇸 English")
                                .font(.bodyMedium)
                                .foregroundColor(.surface400)
                        }
                        .padding(.top, 24)
                        
                        // Preferences Section
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Preferences")
                                .font(.labelLarge)
                                .foregroundColor(.surface400)
                                .padding(.leading, 4)
                            
                            VStack(spacing: 0) {
                                SettingsRow(icon: "person", title: "Edit Profile")
                                Divider().background(Color.surface800)
                                SettingsRow(icon: "globe", title: "Language", subtitle: "English")
                                Divider().background(Color.surface800)
                                SettingsRow(icon: "location", title: "Country", subtitle: "United States")
                            }
                            .background(Color.surface900)
                            .cornerRadius(16)
                        }
                        
                        // About Section
                        VStack(alignment: .leading, spacing: 8) {
                            Text("About")
                                .font(.labelLarge)
                                .foregroundColor(.surface400)
                                .padding(.leading, 4)
                            
                            VStack(spacing: 0) {
                                SettingsRow(icon: "sparkles", title: "What's New")
                                Divider().background(Color.surface800)
                                SettingsRow(icon: "shield", title: "Privacy Policy")
                                Divider().background(Color.surface800)
                                SettingsRow(icon: "doc.text", title: "Terms of Service")
                            }
                            .background(Color.surface900)
                            .cornerRadius(16)
                        }
                        
                        // Version
                        Text("Version 0.1.0")
                            .font(.bodySmall)
                            .foregroundColor(.surface500)
                        
                        // Sign Out Button
                        Button(action: { showLogoutAlert = true }) {
                            HStack {
                                Image(systemName: "rectangle.portrait.and.arrow.right")
                                Text("Sign Out")
                            }
                        }
                        .font(.titleMedium)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .background(Color.error)
                        .cornerRadius(16)
                        .padding(.top, 16)
                    }
                    .padding(.horizontal, 16)
                }
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Done") {
                        dismiss()
                    }
                    .foregroundColor(.purple400)
                }
            }
            .alert("Sign Out", isPresented: $showLogoutAlert) {
                Button("Cancel", role: .cancel) { }
                Button("Sign Out", role: .destructive) {
                    authManager.signOut()
                    dismiss()
                }
            } message: {
                Text("Are you sure you want to sign out?")
            }
        }
    }
}

struct SettingsRow: View {
    let icon: String
    let title: String
    var subtitle: String? = nil
    
    var body: some View {
        HStack {
            Image(systemName: icon)
                .foregroundColor(.purple400)
                .frame(width: 24)
            
            Text(title)
                .foregroundColor(.white)
            
            Spacer()
            
            if let subtitle = subtitle {
                Text(subtitle)
                    .foregroundColor(.surface400)
            }
            
            Image(systemName: "chevron.right")
                .foregroundColor(.surface500)
                .font(.caption)
        }
        .padding()
    }
}

#Preview {
    SettingsView()
        .environmentObject(AuthManager())
}

