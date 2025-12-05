import SwiftUI

// MARK: - Colors
extension Color {
    // Primary Purple Palette
    static let purple50 = Color(hex: "F5F3FF")
    static let purple100 = Color(hex: "EDE9FE")
    static let purple200 = Color(hex: "DDD6FE")
    static let purple300 = Color(hex: "C4B5FD")
    static let purple400 = Color(hex: "A78BFA")
    static let purple500 = Color(hex: "8B5CF6") // Main brand color
    static let purple600 = Color(hex: "7C3AED")
    static let purple700 = Color(hex: "6D28D9")
    static let purple800 = Color(hex: "5B21B6")
    static let purple900 = Color(hex: "4C1D95")
    
    // Accent Colors
    static let accent400 = Color(hex: "FB923C")
    static let accent500 = Color(hex: "F97316")
    
    // Surface Colors (Dark Theme)
    static let surface50 = Color(hex: "F8FAFC")
    static let surface100 = Color(hex: "F1F5F9")
    static let surface200 = Color(hex: "E2E8F0")
    static let surface300 = Color(hex: "CBD5E1")
    static let surface400 = Color(hex: "94A3B8")
    static let surface500 = Color(hex: "64748B")
    static let surface600 = Color(hex: "475569")
    static let surface700 = Color(hex: "334155")
    static let surface800 = Color(hex: "1E293B")
    static let surface900 = Color(hex: "0F172A")
    static let surface950 = Color(hex: "020617")
    
    // Semantic Colors
    static let success = Color(hex: "22C55E")
    static let error = Color(hex: "EF4444")
    static let warning = Color(hex: "F59E0B")
    static let info = Color(hex: "3B82F6")
    
    // Helper initializer for hex colors
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 3: // RGB (12-bit)
            (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6: // RGB (24-bit)
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8: // ARGB (32-bit)
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (1, 1, 1, 0)
        }
        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue: Double(b) / 255,
            opacity: Double(a) / 255
        )
    }
}

// MARK: - Typography
extension Font {
    static let displayLarge = Font.system(size: 57, weight: .bold)
    static let displayMedium = Font.system(size: 45, weight: .bold)
    static let displaySmall = Font.system(size: 36, weight: .bold)
    
    static let headlineLarge = Font.system(size: 32, weight: .semibold)
    static let headlineMedium = Font.system(size: 28, weight: .semibold)
    static let headlineSmall = Font.system(size: 24, weight: .semibold)
    
    static let titleLarge = Font.system(size: 22, weight: .semibold)
    static let titleMedium = Font.system(size: 16, weight: .semibold)
    static let titleSmall = Font.system(size: 14, weight: .medium)
    
    static let bodyLarge = Font.system(size: 16, weight: .regular)
    static let bodyMedium = Font.system(size: 14, weight: .regular)
    static let bodySmall = Font.system(size: 12, weight: .regular)
    
    static let labelLarge = Font.system(size: 14, weight: .medium)
    static let labelMedium = Font.system(size: 12, weight: .medium)
    static let labelSmall = Font.system(size: 11, weight: .medium)
}

// MARK: - View Modifiers
struct PrimaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.titleMedium)
            .foregroundColor(.white)
            .frame(maxWidth: .infinity)
            .frame(height: 56)
            .background(Color.purple500)
            .cornerRadius(16)
            .opacity(configuration.isPressed ? 0.8 : 1.0)
    }
}

struct SecondaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.titleMedium)
            .foregroundColor(.white)
            .frame(maxWidth: .infinity)
            .frame(height: 56)
            .background(Color.surface800)
            .cornerRadius(16)
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(Color.surface600, lineWidth: 1)
            )
            .opacity(configuration.isPressed ? 0.8 : 1.0)
    }
}

