import Foundation

// MARK: - Frequent Emoji Manager
class FrequentEmojiManager: ObservableObject {
    static let shared = FrequentEmojiManager()
    
    private let userDefaultsKey = "frequentEmojis"
    private let maxStoredEmojis = 20
    
    @Published var frequentEmojis: [String] = []
    
    private init() {
        loadFrequentEmojis()
    }
    
    /// Returns the top N frequently used emojis
    func topEmojis(_ count: Int = 5) -> [String] {
        if frequentEmojis.isEmpty {
            return EmojiData.defaultFrequent
        }
        return Array(frequentEmojis.prefix(count))
    }
    
    /// Records an emoji usage, moving it to the front of the list
    func recordUsage(_ emoji: String) {
        // Remove if already exists
        frequentEmojis.removeAll { $0 == emoji }
        
        // Add to front
        frequentEmojis.insert(emoji, at: 0)
        
        // Trim to max size
        if frequentEmojis.count > maxStoredEmojis {
            frequentEmojis = Array(frequentEmojis.prefix(maxStoredEmojis))
        }
        
        // Persist
        saveFrequentEmojis()
    }
    
    /// Returns all frequently used emojis (up to maxStoredEmojis)
    func allFrequentEmojis() -> [String] {
        if frequentEmojis.isEmpty {
            return EmojiData.defaultFrequent
        }
        return frequentEmojis
    }
    
    // MARK: - Persistence
    
    private func loadFrequentEmojis() {
        if let saved = UserDefaults.standard.stringArray(forKey: userDefaultsKey) {
            frequentEmojis = saved
        }
    }
    
    private func saveFrequentEmojis() {
        UserDefaults.standard.set(frequentEmojis, forKey: userDefaultsKey)
    }
    
    /// Resets to default emojis
    func reset() {
        frequentEmojis = []
        UserDefaults.standard.removeObject(forKey: userDefaultsKey)
    }
}
