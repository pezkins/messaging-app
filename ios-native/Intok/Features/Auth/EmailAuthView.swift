import SwiftUI

enum EmailAuthStep {
    case enterEmail
    case enterPassword  // Existing user
    case createAccount  // New user - password + profile
}

struct EmailAuthView: View {
    @EnvironmentObject var authManager: AuthManager
    @Environment(\.dismiss) var dismiss
    
    @State private var step: EmailAuthStep = .enterEmail
    @State private var email = ""
    @State private var password = ""
    @State private var confirmPassword = ""
    @State private var displayName = ""
    @State private var selectedLanguage = "en"
    @State private var selectedCountry = "US"
    
    @State private var isCheckingEmail = false
    @State private var errorMessage: String?
    @State private var showPassword = false
    
    var body: some View {
        ZStack {
            // Background
            Color(hex: "0F0F0F")
                .ignoresSafeArea()
            
            VStack(spacing: 0) {
                // Header
                header
                
                ScrollView {
                    VStack(spacing: 24) {
                        switch step {
                        case .enterEmail:
                            emailStepView
                        case .enterPassword:
                            passwordStepView
                        case .createAccount:
                            createAccountStepView
                        }
                    }
                    .padding(24)
                }
            }
        }
        .navigationBarHidden(true)
    }
    
    // MARK: - Header
    var header: some View {
        HStack {
            Button(action: handleBack) {
                Image(systemName: "chevron.left")
                    .font(.title3)
                    .foregroundColor(.white)
            }
            
            Spacer()
            
            Text(headerTitle)
                .font(.headline)
                .foregroundColor(.white)
            
            Spacer()
            
            // Placeholder for alignment
            Image(systemName: "chevron.left")
                .font(.title3)
                .foregroundColor(.clear)
        }
        .padding()
        .background(Color(hex: "0F0F0F"))
    }
    
    var headerTitle: String {
        switch step {
        case .enterEmail:
            return "Continue with Email"
        case .enterPassword:
            return "Welcome Back"
        case .createAccount:
            return "Create Account"
        }
    }
    
    // MARK: - Email Step
    var emailStepView: some View {
        VStack(spacing: 24) {
            VStack(alignment: .leading, spacing: 8) {
                Text("What's your email?")
                    .font(.title2)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
                
                Text("We'll check if you have an account")
                    .font(.subheadline)
                    .foregroundColor(.gray)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            
            // Email Input
            VStack(alignment: .leading, spacing: 8) {
                Text("Email")
                    .font(.caption)
                    .foregroundColor(.gray)
                
                TextField("", text: $email)
                    .textFieldStyle(.plain)
                    .keyboardType(.emailAddress)
                    .textContentType(.emailAddress)
                    .autocapitalization(.none)
                    .autocorrectionDisabled()
                    .foregroundColor(.white)
                    .padding()
                    .background(Color.white.opacity(0.1))
                    .cornerRadius(12)
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(Color.white.opacity(0.2), lineWidth: 1)
                    )
            }
            
            if let error = errorMessage {
                Text(error)
                    .font(.caption)
                    .foregroundColor(.red)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            
            // Continue Button
            Button(action: checkEmail) {
                HStack {
                    if isCheckingEmail {
                        ProgressView()
                            .tint(.white)
                    } else {
                        Text("Continue")
                    }
                }
                .fontWeight(.semibold)
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 56)
                .background(isValidEmail ? Color(hex: "8B5CF6") : Color.gray)
                .cornerRadius(12)
            }
            .disabled(!isValidEmail || isCheckingEmail)
            
            Spacer()
        }
    }
    
    // MARK: - Password Step (Existing User)
    var passwordStepView: some View {
        VStack(spacing: 24) {
            VStack(alignment: .leading, spacing: 8) {
                Text("Enter your password")
                    .font(.title2)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
                
                Text(email)
                    .font(.subheadline)
                    .foregroundColor(Color(hex: "8B5CF6"))
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            
            // Password Input
            VStack(alignment: .leading, spacing: 8) {
                Text("Password")
                    .font(.caption)
                    .foregroundColor(.gray)
                
                HStack {
                    if showPassword {
                        TextField("", text: $password)
                            .textFieldStyle(.plain)
                            .textContentType(.password)
                            .autocapitalization(.none)
                            .foregroundColor(.white)
                    } else {
                        SecureField("", text: $password)
                            .textFieldStyle(.plain)
                            .textContentType(.password)
                            .foregroundColor(.white)
                    }
                    
                    Button(action: { showPassword.toggle() }) {
                        Image(systemName: showPassword ? "eye.slash" : "eye")
                            .foregroundColor(.gray)
                    }
                }
                .padding()
                .background(Color.white.opacity(0.1))
                .cornerRadius(12)
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(Color.white.opacity(0.2), lineWidth: 1)
                )
            }
            
            if let error = errorMessage {
                Text(error)
                    .font(.caption)
                    .foregroundColor(.red)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            
            // Sign In Button
            Button(action: signIn) {
                HStack {
                    if authManager.isLoading {
                        ProgressView()
                            .tint(.white)
                    } else {
                        Text("Sign In")
                    }
                }
                .fontWeight(.semibold)
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 56)
                .background(password.count >= 6 ? Color(hex: "8B5CF6") : Color.gray)
                .cornerRadius(12)
            }
            .disabled(password.count < 6 || authManager.isLoading)
            
            // Forgot Password
            Button(action: { /* TODO: Forgot password */ }) {
                Text("Forgot password?")
                    .font(.subheadline)
                    .foregroundColor(Color(hex: "8B5CF6"))
            }
            
            Spacer()
        }
    }
    
    // MARK: - Create Account Step (New User)
    var createAccountStepView: some View {
        VStack(spacing: 24) {
            VStack(alignment: .leading, spacing: 8) {
                Text("Create your account")
                    .font(.title2)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
                
                Text(email)
                    .font(.subheadline)
                    .foregroundColor(Color(hex: "8B5CF6"))
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            
            // Display Name
            VStack(alignment: .leading, spacing: 8) {
                Text("Display Name")
                    .font(.caption)
                    .foregroundColor(.gray)
                
                TextField("", text: $displayName)
                    .textFieldStyle(.plain)
                    .textContentType(.name)
                    .foregroundColor(.white)
                    .padding()
                    .background(Color.white.opacity(0.1))
                    .cornerRadius(12)
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(Color.white.opacity(0.2), lineWidth: 1)
                    )
            }
            
            // Password
            VStack(alignment: .leading, spacing: 8) {
                Text("Password")
                    .font(.caption)
                    .foregroundColor(.gray)
                
                HStack {
                    if showPassword {
                        TextField("", text: $password)
                            .textFieldStyle(.plain)
                            .foregroundColor(.white)
                    } else {
                        SecureField("", text: $password)
                            .textFieldStyle(.plain)
                            .foregroundColor(.white)
                    }
                    
                    Button(action: { showPassword.toggle() }) {
                        Image(systemName: showPassword ? "eye.slash" : "eye")
                            .foregroundColor(.gray)
                    }
                }
                .padding()
                .background(Color.white.opacity(0.1))
                .cornerRadius(12)
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(Color.white.opacity(0.2), lineWidth: 1)
                )
                
                Text("At least 6 characters")
                    .font(.caption2)
                    .foregroundColor(.gray)
            }
            
            // Confirm Password
            VStack(alignment: .leading, spacing: 8) {
                Text("Confirm Password")
                    .font(.caption)
                    .foregroundColor(.gray)
                
                SecureField("", text: $confirmPassword)
                    .textFieldStyle(.plain)
                    .foregroundColor(.white)
                    .padding()
                    .background(Color.white.opacity(0.1))
                    .cornerRadius(12)
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(passwordsMatch ? Color.white.opacity(0.2) : Color.red, lineWidth: 1)
                    )
                
                if !confirmPassword.isEmpty && !passwordsMatch {
                    Text("Passwords don't match")
                        .font(.caption2)
                        .foregroundColor(.red)
                }
            }
            
            // Language Picker
            VStack(alignment: .leading, spacing: 8) {
                Text("Preferred Language")
                    .font(.caption)
                    .foregroundColor(.gray)
                
                Picker("Language", selection: $selectedLanguage) {
                    ForEach(LANGUAGES, id: \.code) { language in
                        Text(language.name).tag(language.code)
                    }
                }
                .pickerStyle(.menu)
                .tint(.white)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding()
                .background(Color.white.opacity(0.1))
                .cornerRadius(12)
            }
            
            // Country Picker
            VStack(alignment: .leading, spacing: 8) {
                Text("Country")
                    .font(.caption)
                    .foregroundColor(.gray)
                
                Picker("Country", selection: $selectedCountry) {
                    ForEach(COUNTRIES, id: \.code) { country in
                        Text("\(country.flag) \(country.name)").tag(country.code)
                    }
                }
                .pickerStyle(.menu)
                .tint(.white)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding()
                .background(Color.white.opacity(0.1))
                .cornerRadius(12)
            }
            
            if let error = errorMessage {
                Text(error)
                    .font(.caption)
                    .foregroundColor(.red)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            
            // Create Account Button
            Button(action: createAccount) {
                HStack {
                    if authManager.isLoading {
                        ProgressView()
                            .tint(.white)
                    } else {
                        Text("Create Account")
                    }
                }
                .fontWeight(.semibold)
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 56)
                .background(canCreateAccount ? Color(hex: "8B5CF6") : Color.gray)
                .cornerRadius(12)
            }
            .disabled(!canCreateAccount || authManager.isLoading)
            
            Spacer()
        }
    }
    
    // MARK: - Validation
    var isValidEmail: Bool {
        let emailRegex = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}"
        let emailPredicate = NSPredicate(format: "SELF MATCHES %@", emailRegex)
        return emailPredicate.evaluate(with: email)
    }
    
    var passwordsMatch: Bool {
        password == confirmPassword
    }
    
    var canCreateAccount: Bool {
        displayName.count >= 2 &&
        password.count >= 6 &&
        passwordsMatch
    }
    
    // MARK: - Actions
    func handleBack() {
        switch step {
        case .enterEmail:
            dismiss()
        case .enterPassword, .createAccount:
            withAnimation {
                step = .enterEmail
                password = ""
                confirmPassword = ""
                errorMessage = nil
            }
        }
    }
    
    func checkEmail() {
        isCheckingEmail = true
        errorMessage = nil
        
        Task {
            do {
                let exists = try await APIService.shared.checkEmail(email)
                await MainActor.run {
                    isCheckingEmail = false
                    withAnimation {
                        if exists {
                            step = .enterPassword
                        } else {
                            // Pre-fill display name from email
                            displayName = email.components(separatedBy: "@").first ?? ""
                            step = .createAccount
                        }
                    }
                }
            } catch {
                await MainActor.run {
                    isCheckingEmail = false
                    errorMessage = "Failed to check email. Please try again."
                }
            }
        }
    }
    
    func signIn() {
        errorMessage = nil
        
        Task {
            await authManager.signInWithEmail(email: email, password: password)
            
            await MainActor.run {
                if let error = authManager.error {
                    errorMessage = error
                }
                // If successful, AuthManager will update isAuthenticated and the view will change
            }
        }
    }
    
    func createAccount() {
        errorMessage = nil
        
        Task {
            await authManager.registerWithEmail(
                email: email,
                password: password,
                username: displayName,
                preferredLanguage: selectedLanguage,
                preferredCountry: selectedCountry
            )
            
            await MainActor.run {
                if let error = authManager.error {
                    errorMessage = error
                }
                // If successful, AuthManager will update isAuthenticated and the view will change
            }
        }
    }
}

#Preview {
    EmailAuthView()
        .environmentObject(AuthManager.shared)
}
