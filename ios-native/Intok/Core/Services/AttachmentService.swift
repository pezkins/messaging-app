import Foundation
import UIKit
import os.log

private let logger = Logger(subsystem: "com.pezkins.intok", category: "AttachmentService")

// MARK: - Attachment Service
class AttachmentService {
    static let shared = AttachmentService()
    
    private init() {}
    
    // MARK: - Upload Image
    func uploadImage(
        _ image: UIImage,
        conversationId: String,
        progressHandler: ((Double) -> Void)? = nil
    ) async throws -> UploadedAttachment {
        // Compress image to JPEG
        guard let imageData = image.jpegData(compressionQuality: 0.8) else {
            throw AttachmentError.compressionFailed
        }
        
        let fileName = "image_\(Date().timeIntervalSince1970).jpg"
        let contentType = "image/jpeg"
        let fileSize = imageData.count
        
        logger.info("📤 Uploading image: \(fileName, privacy: .public), size: \(fileSize)")
        progressHandler?(0.1)
        
        // Get upload URL from backend
        let uploadResponse = try await APIService.shared.getUploadUrl(
            fileName: fileName,
            contentType: contentType,
            fileSize: fileSize,
            conversationId: conversationId
        )
        
        progressHandler?(0.3)
        
        // Upload to S3
        try await APIService.shared.uploadFile(
            uploadUrl: uploadResponse.uploadUrl,
            data: imageData,
            contentType: contentType
        )
        
        progressHandler?(0.9)
        
        logger.info("✅ Image uploaded successfully: \(uploadResponse.key, privacy: .public)")
        progressHandler?(1.0)
        
        return UploadedAttachment(
            id: uploadResponse.attachmentId,
            key: uploadResponse.key,
            fileName: fileName,
            contentType: contentType,
            fileSize: Int64(fileSize),
            category: uploadResponse.category
        )
    }
    
    // MARK: - Upload Document
    func uploadDocument(
        _ url: URL,
        conversationId: String,
        progressHandler: ((Double) -> Void)? = nil
    ) async throws -> UploadedAttachment {
        // Read file data
        let fileData = try Data(contentsOf: url)
        let fileName = url.lastPathComponent
        let contentType = mimeType(for: url)
        let fileSize = fileData.count
        
        logger.info("📤 Uploading document: \(fileName, privacy: .public), size: \(fileSize)")
        progressHandler?(0.1)
        
        // Get upload URL from backend
        let uploadResponse = try await APIService.shared.getUploadUrl(
            fileName: fileName,
            contentType: contentType,
            fileSize: fileSize,
            conversationId: conversationId
        )
        
        progressHandler?(0.3)
        
        // Upload to S3
        try await APIService.shared.uploadFile(
            uploadUrl: uploadResponse.uploadUrl,
            data: fileData,
            contentType: contentType
        )
        
        progressHandler?(0.9)
        
        logger.info("✅ Document uploaded successfully: \(uploadResponse.key, privacy: .public)")
        progressHandler?(1.0)
        
        return UploadedAttachment(
            id: uploadResponse.attachmentId,
            key: uploadResponse.key,
            fileName: fileName,
            contentType: contentType,
            fileSize: Int64(fileSize),
            category: uploadResponse.category
        )
    }
    
    // MARK: - Get Download URL
    func getDownloadUrl(key: String) async throws -> URL {
        let response = try await APIService.shared.getDownloadUrl(key: key)
        guard let url = URL(string: response.downloadUrl) else {
            throw AttachmentError.invalidURL
        }
        return url
    }
    
    // MARK: - MIME Type Detection
    private func mimeType(for url: URL) -> String {
        let pathExtension = url.pathExtension.lowercased()
        
        switch pathExtension {
        // Images
        case "jpg", "jpeg":
            return "image/jpeg"
        case "png":
            return "image/png"
        case "gif":
            return "image/gif"
        case "webp":
            return "image/webp"
            
        // Videos
        case "mp4":
            return "video/mp4"
        case "mov":
            return "video/quicktime"
        case "webm":
            return "video/webm"
            
        // Audio
        case "mp3":
            return "audio/mpeg"
        case "wav":
            return "audio/wav"
        case "ogg":
            return "audio/ogg"
            
        // Documents
        case "pdf":
            return "application/pdf"
        case "doc":
            return "application/msword"
        case "docx":
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        case "xls":
            return "application/vnd.ms-excel"
        case "xlsx":
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        case "txt":
            return "text/plain"
            
        default:
            return "application/octet-stream"
        }
    }
}

// MARK: - Uploaded Attachment
struct UploadedAttachment {
    let id: String
    let key: String
    let fileName: String
    let contentType: String
    let fileSize: Int64
    let category: String
}

// MARK: - Attachment Errors
enum AttachmentError: LocalizedError {
    case compressionFailed
    case invalidURL
    case uploadFailed(String)
    
    var errorDescription: String? {
        switch self {
        case .compressionFailed:
            return "Failed to compress image"
        case .invalidURL:
            return "Invalid download URL"
        case .uploadFailed(let message):
            return "Upload failed: \(message)"
        }
    }
}
