import SwiftUI

struct SetupView: View {
    @EnvironmentObject var authManager: AuthManager
    @State private var displayName = ""
    @State private var selectedLanguage = "en"
    @State private var selectedCountry = "US"
    
    var body: some View {
        ZStack {
            Color.surface950.ignoresSafeArea()
            
            VStack(spacing: 24) {
                Spacer()
                    .frame(height: 48)
                
                Text("Welcome to Intok! 👋")
                    .font(.headlineMedium)
                    .foregroundColor(.white)
                
                Text("Let's set up your profile")
                    .font(.bodyLarge)
                    .foregroundColor(.surface400)
                
                Spacer()
                    .frame(height: 24)
                
                // Display Name
                VStack(alignment: .leading, spacing: 8) {
                    Text("Display Name")
                        .font(.labelLarge)
                        .foregroundColor(.surface400)
                    
                    TextField("Enter your name", text: $displayName)
                        .textFieldStyle(IntokTextFieldStyle())
                }
                
                // Language
                VStack(alignment: .leading, spacing: 8) {
                    Text("Preferred Language")
                        .font(.labelLarge)
                        .foregroundColor(.surface400)
                    
                    // TODO: Replace with proper picker
                    HStack {
                        Text("🇺🇸 English")
                            .foregroundColor(.white)
                        Spacer()
                        Image(systemName: "chevron.down")
                            .foregroundColor(.surface400)
                    }
                    .padding()
                    .background(Color.surface800)
                    .cornerRadius(12)
                }
                
                // Country
                VStack(alignment: .leading, spacing: 8) {
                    Text("Country")
                        .font(.labelLarge)
                        .foregroundColor(.surface400)
                    
                    // TODO: Replace with proper picker
                    HStack {
                        Text("🇺🇸 United States")
                            .foregroundColor(.white)
                        Spacer()
                        Image(systemName: "chevron.down")
                            .foregroundColor(.surface400)
                    }
                    .padding()
                    .background(Color.surface800)
                    .cornerRadius(12)
                }
                
                Spacer()
                
                // Continue Button
                Button(action: {
                    Task {
                        await authManager.completeSetup(
                            displayName: displayName,
                            language: selectedLanguage,
                            country: selectedCountry
                        )
                    }
                }) {
                    if authManager.isLoading {
                        ProgressView()
                            .tint(.white)
                    } else {
                        Text("Continue")
                    }
                }
                .buttonStyle(PrimaryButtonStyle())
                .disabled(displayName.isEmpty || authManager.isLoading)
                .opacity(displayName.isEmpty ? 0.5 : 1.0)
                
                Spacer()
                    .frame(height: 24)
            }
            .padding(.horizontal, 24)
        }
    }
}

// Custom TextField Style
struct IntokTextFieldStyle: TextFieldStyle {
    func _body(configuration: TextField<Self._Label>) -> some View {
        configuration
            .padding()
            .background(Color.surface800)
            .foregroundColor(.white)
            .cornerRadius(12)
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(Color.surface600, lineWidth: 1)
            )
    }
}

#Preview {
    SetupView()
        .environmentObject(AuthManager())
}

