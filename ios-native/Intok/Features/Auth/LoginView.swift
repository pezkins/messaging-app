import SwiftUI

struct LoginView: View {
    @EnvironmentObject var authManager: AuthManager
    
    var body: some View {
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
                RoundedRectangle(cornerRadius: 24)
                    .fill(Color.purple500)
                    .frame(width: 120, height: 120)
                    .overlay(
                        Text("🌐")
                            .font(.system(size: 48))
                    )
                
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
                
                // Google Sign In Button
                Button(action: {
                    Task {
                        // TODO: Implement Google Sign-In
                        await authManager.signInWithGoogle(idToken: "mock_token")
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
                    // TODO: Navigate to email sign in
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
    }
}

#Preview {
    LoginView()
        .environmentObject(AuthManager())
}

