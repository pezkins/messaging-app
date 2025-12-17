import Foundation

struct ChangelogEntry: Identifiable {
    let id = UUID()
    let version: String
    let date: String
    let title: String
    let changes: [String]
}

struct Changelog {
    static let entries: [ChangelogEntry] = [
        ChangelogEntry(
            version: "0.1.22",
            date: "December 2024",
            title: "Performance & Group Chat",
            changes: [
                "⚡ Instant chat history loading with local caching",
                "👥 Add and remove participants from group chats",
                "🔔 Enhanced push notification logging",
                "💾 Offline message support",
                "🚀 Faster app performance"
            ]
        ),
        ChangelogEntry(
            version: "0.1.21",
            date: "December 2024",
            title: "Authentication & Stability",
            changes: [
                "🍎 Fixed Apple Sign-In authentication flow",
                "🚀 Improved CI/CD deployment pipeline",
                "🔧 Bug fixes and performance improvements"
            ]
        ),
        ChangelogEntry(
            version: "0.1.19",
            date: "December 2024",
            title: "Languages & Regional Translation",
            changes: [
                "🌍 120+ languages now supported",
                "🗺️ Regional language variants (Catalan, Welsh, Basque, etc.)",
                "🎯 Regional translation targeting for maximum accuracy",
                "📜 Classical languages (Latin, Sanskrit, Ancient Greek)",
                "⚡ Real-time message sync improvements"
            ]
        ),
        ChangelogEntry(
            version: "0.1.18",
            date: "December 2024",
            title: "Message Management Update",
            changes: [
                "🗑️ Delete individual messages",
                "🗑️ Delete entire conversations",
                "💾 Save images to photos",
                "📥 Download documents to device"
            ]
        ),
        ChangelogEntry(
            version: "0.1.5",
            date: "December 2024",
            title: "Engagement Update",
            changes: [
                "🔔 Push notifications for new messages",
                "🍎 Sign in with Apple",
                "🔢 Unread message count badges",
                "✓✓ Read receipts on messages",
                "✨ What's New popup on app updates"
            ]
        ),
        ChangelogEntry(
            version: "0.1.4",
            date: "December 2024",
            title: "Smart Translation Update",
            changes: [
                "📄 Choose whether to translate documents",
                "🖼️ Images and GIFs skip translation for faster delivery",
                "⚡ Improved message performance"
            ]
        ),
        ChangelogEntry(
            version: "0.1.3",
            date: "December 2024",
            title: "Rich Messaging Update",
            changes: [
                "📸 Share images from your photo library",
                "📷 Take photos directly in chat",
                "🎬 Send GIFs with GIPHY integration",
                "📄 Share documents and PDFs",
                "👍 React to messages with emojis"
            ]
        ),
        ChangelogEntry(
            version: "0.1.2",
            date: "December 2024",
            title: "Authentication Update",
            changes: [
                "📧 Sign in with email and password",
                "🍎 Sign in with Apple",
                "🔔 Push notifications support"
            ]
        ),
        ChangelogEntry(
            version: "0.1.1",
            date: "December 2024",
            title: "Initial Release",
            changes: [
                "💬 Real-time messaging",
                "🌐 Automatic translation",
                "🔐 Google Sign-In"
            ]
        )
    ]
}
