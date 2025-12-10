import SwiftUI
import AuthenticationServices

struct LoginView: View {
    @EnvironmentObject var authManager: AuthManager
    @StateObject private var appleAuthManager = AppleAuthManager.shared
    @State private var showEmailAuth = false
    
    var body: some View {
        NavigationStack {
            ZStack {
                // Background gradient
                LinearGradient(
                    colors: [Color.surface950, Color.surface900],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .ignoresSafeArea()
                
                VStack(spacing: 24) {
                    Spacer()
                    
                    // Logo
                    Image("AppLogo")
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 120, height: 120)
                        .clipShape(RoundedRectangle(cornerRadius: 24))
                    
                    // App Name
                    Text("Intok")
                        .font(.displaySmall)
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                    
                    Text("Connect globally, communicate naturally")
                        .font(.bodyLarge)
                        .foregroundColor(.surface400)
                        .multilineTextAlignment(.center)
                    
                    Spacer()
                    
                    // Apple Sign In Button
                    Button(action: {
                        appleAuthManager.signIn()
                    }) {
                        HStack {
                            if appleAuthManager.isLoading {
                                ProgressView()
                                    .tint(.white)
                            } else {
                                Image(systemName: "apple.logo")
                                Text("Continue with Apple")
                            }
                        }
                    }
                    .font(.titleMedium)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .background(Color.black)
                    .cornerRadius(16)
                    .overlay(
                        RoundedRectangle(cornerRadius: 16)
                            .stroke(Color.white.opacity(0.2), lineWidth: 1)
                    )
                    .disabled(authManager.isLoading || appleAuthManager.isLoading)
                    
                    // Google Sign In Button
                    Button(action: {
                        Task {
                            await authManager.signInWithGoogle()
                        }
                    }) {
                        HStack {
                            if authManager.isLoading {
                                ProgressView()
                                    .tint(.purple500)
                            } else {
                                Text("Continue with Google")
                            }
                        }
                    }
                    .font(.titleMedium)
                    .foregroundColor(.surface900)
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .background(Color.white)
                    .cornerRadius(16)
                    .disabled(authManager.isLoading)
                    
                    // Divider
                    HStack {
                        Rectangle()
                            .fill(Color.surface700)
                            .frame(height: 1)
                        Text("or")
                            .foregroundColor(.surface500)
                            .padding(.horizontal, 16)
                        Rectangle()
                            .fill(Color.surface700)
                            .frame(height: 1)
                    }
                    
                    // Email Sign In Button
                    Button(action: {
                        showEmailAuth = true
                    }) {
                        Text("Continue with Email")
                    }
                    .buttonStyle(SecondaryButtonStyle())
                    
                    Spacer()
                        .frame(height: 16)
                    
                    // Terms
                    Text("By continuing, you agree to our Terms of Service and Privacy Policy")
                        .font(.bodySmall)
                        .foregroundColor(.surface500)
                        .multilineTextAlignment(.center)
                    
                    Spacer()
                        .frame(height: 24)
                }
                .padding(.horizontal, 24)
            }
            .navigationDestination(isPresented: $showEmailAuth) {
                EmailAuthView()
            }
            .alert("Sign in with Apple", isPresented: $appleAuthManager.showError) {
                Button("OK") { }
            } message: {
                Text(appleAuthManager.errorMessage ?? "An error occurred")
            }
        }
    }
}

#Preview {
    LoginView()
        .environmentObject(AuthManager.shared)
}

