import Foundation

class WhatsNewManager: ObservableObject {
    static let shared = WhatsNewManager()
    
    private let lastSeenVersionKey = "intok_last_seen_version"
    
    @Published var shouldShowWhatsNew = false
    @Published var newVersionEntries: [ChangelogEntry] = []
    
    var currentVersion: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "0.0.0"
    }
    
    func checkForNewVersion() {
        let lastSeenVersion = UserDefaults.standard.string(forKey: lastSeenVersionKey)
        
        if lastSeenVersion == nil {
            // First launch - show current version
            newVersionEntries = [Changelog.entries.first].compactMap { $0 }
            shouldShowWhatsNew = true
        } else if lastSeenVersion != currentVersion {
            // New version - show all changes since last seen
            if let lastIndex = Changelog.entries.firstIndex(where: { $0.version == lastSeenVersion }) {
                newVersionEntries = Array(Changelog.entries.prefix(lastIndex))
                if !newVersionEntries.isEmpty {
                    shouldShowWhatsNew = true
                }
            } else {
                // Unknown version - show latest
                newVersionEntries = [Changelog.entries.first].compactMap { $0 }
                shouldShowWhatsNew = true
            }
        }
    }
    
    func markAsSeen() {
        UserDefaults.standard.set(currentVersion, forKey: lastSeenVersionKey)
        shouldShowWhatsNew = false
    }
}
