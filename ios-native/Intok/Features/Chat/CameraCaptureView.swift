import SwiftUI
import UIKit

// MARK: - Camera Capture View
struct CameraCaptureView: UIViewControllerRepresentable {
    @Environment(\.dismiss) var dismiss
    let onCapture: (UIImage) -> Void
    
    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.sourceType = .camera
        picker.delegate = context.coordinator
        picker.allowsEditing = true
        return picker
    }
    
    func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {}
    
    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }
    
    class Coordinator: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
        let parent: CameraCaptureView
        
        init(_ parent: CameraCaptureView) {
            self.parent = parent
        }
        
        func imagePickerController(
            _ picker: UIImagePickerController,
            didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]
        ) {
            // Use edited image if available, otherwise use original
            if let image = info[.editedImage] as? UIImage ?? info[.originalImage] as? UIImage {
                parent.onCapture(image)
            }
            parent.dismiss()
        }
        
        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            parent.dismiss()
        }
    }
}

// MARK: - Camera Permission View
struct CameraPermissionView: View {
    @Environment(\.dismiss) var dismiss
    
    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: "camera.fill")
                .font(.system(size: 60))
                .foregroundColor(Color(hex: "8B5CF6"))
            
            Text("permission_camera_title".localized)
                .font(.title2)
                .fontWeight(.bold)
            
            Text("permission_camera_message".localized)
                .font(.body)
                .foregroundColor(.gray)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
            
            Button(action: openSettings) {
                Text("permission_go_to_settings".localized)
                    .fontWeight(.semibold)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 50)
                    .background(Color(hex: "8B5CF6"))
                    .cornerRadius(12)
            }
            .padding(.horizontal, 40)
            
            Button(action: { dismiss() }) {
                Text("common_cancel".localized)
                    .foregroundColor(.gray)
            }
        }
        .padding()
    }
    
    func openSettings() {
        if let url = URL(string: UIApplication.openSettingsURLString) {
            UIApplication.shared.open(url)
        }
    }
}

#Preview {
    CameraPermissionView()
}
