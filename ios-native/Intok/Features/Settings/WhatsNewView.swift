import SwiftUI

struct WhatsNewView: View {
    @ObservedObject var manager = WhatsNewManager.shared
    @Environment(\.dismiss) var dismiss
    
    var body: some View {
        NavigationView {
            ZStack {
                Color(hex: "0F0F0F").ignoresSafeArea()
                
                ScrollView {
                    VStack(alignment: .leading, spacing: 24) {
                        // Header
                        VStack(spacing: 12) {
                            Image(systemName: "sparkles")
                                .font(.system(size: 48))
                                .foregroundColor(Color(hex: "8B5CF6"))
                            
                            Text("What's New")
                                .font(.title)
                                .fontWeight(.bold)
                                .foregroundColor(.white)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.top)
                        
                        // Version entries
                        ForEach(manager.newVersionEntries) { entry in
                            VStack(alignment: .leading, spacing: 12) {
                                HStack {
                                    Text("v\(entry.version)")
                                        .font(.headline)
                                        .foregroundColor(Color(hex: "8B5CF6"))
                                    
                                    Text("• \(entry.title)")
                                        .font(.headline)
                                        .foregroundColor(.white)
                                }
                                
                                ForEach(entry.changes, id: \.self) { change in
                                    Text(change)
                                        .foregroundColor(.gray)
                                }
                            }
                            .padding()
                            .background(Color.white.opacity(0.05))
                            .cornerRadius(12)
                        }
                    }
                    .padding()
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") {
                        manager.markAsSeen()
                        dismiss()
                    }
                    .foregroundColor(Color(hex: "8B5CF6"))
                }
            }
        }
    }
}

#Preview {
    WhatsNewView()
}
