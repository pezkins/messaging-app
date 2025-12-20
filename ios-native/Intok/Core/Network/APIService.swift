import Foundation
import os.log

private let logger = Logger(subsystem: "com.pezkins.intok", category: "APIService")

// MARK: - API Errors
enum APIError: LocalizedError {
    case invalidURL
    case invalidResponse
    case unauthorized
    case serverError(String)
    case decodingError(Error)
    case networkError(Error)
    
    var errorDescription: String? {
        switch self {
        case .invalidURL:
            return "Invalid URL"
        case .invalidResponse:
            return "Invalid response from server"
        case .unauthorized:
            return "Unauthorized - please log in again"
        case .serverError(let message):
            return message
        case .decodingError(let error):
            return "Failed to decode response: \(error.localizedDescription)"
        case .networkError(let error):
            return "Network error: \(error.localizedDescription)"
        }
    }
}

// MARK: - Response Types
struct UserResponse: Codable {
    let user: User
}

struct ConversationsResponse: Codable {
    let conversations: [Conversation]
}

struct MessagesResponse: Codable {
    let messages: [Message]
    let hasMore: Bool
    let nextCursor: String?
}

struct UsersSearchResponse: Codable {
    let users: [UserPublic]
}

struct ConversationResponse: Codable {
    let conversation: Conversation
}

struct UploadUrlResponse: Codable {
    let attachmentId: String
    let uploadUrl: String
    let key: String
    let category: String
    let expiresIn: Int
}

struct ProfilePictureUploadResponse: Codable {
    let uploadUrl: String
    let key: String
    let expiresIn: Int
}

struct DownloadUrlResponse: Codable {
    let downloadUrl: String
    let expiresIn: Int?
}

struct RefreshTokenResponse: Codable {
    let accessToken: String
    let refreshToken: String
}

// MARK: - API Service
class APIService {
    static let shared = APIService()
    
    private let baseURL: String
    private var accessToken: String?
    
    private init() {
        if let url = Bundle.main.object(forInfoDictionaryKey: "API_URL") as? String {
            baseURL = url
        } else {
            baseURL = "https://aji93f9i0k.execute-api.us-east-1.amazonaws.com/prod"
        }
        logger.info("🌐 API Service initialized with: \(self.baseURL, privacy: .public)")
    }
    
    // MARK: - Token Management
    func setAccessToken(_ token: String?) {
        self.accessToken = token
        if token != nil {
            logger.info("🔑 Access token set")
        } else {
            logger.info("🔑 Access token cleared")
        }
    }
    
    func getAccessToken() -> String? {
        return accessToken
    }
    
    // MARK: - Generic Request
    private func request<T: Decodable>(
        endpoint: String,
        method: String = "GET",
        body: Data? = nil
    ) async throws -> T {
        guard let url = URL(string: "\(baseURL)\(endpoint)") else {
            throw APIError.invalidURL
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        if let token = accessToken {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        
        if let body = body {
            request.httpBody = body
        }
        
        logger.debug("📤 \(method, privacy: .public) \(endpoint, privacy: .public)")
        
        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            
            guard let httpResponse = response as? HTTPURLResponse else {
                throw APIError.invalidResponse
            }
            
            logger.debug("📥 Response: \(httpResponse.statusCode, privacy: .public)")
            
            if httpResponse.statusCode == 401 {
                throw APIError.unauthorized
            }
            
            if httpResponse.statusCode >= 400 {
                if let errorJson = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                   let message = errorJson["message"] as? String {
                    throw APIError.serverError(message)
                }
                throw APIError.serverError("Request failed with status \(httpResponse.statusCode)")
            }
            
            do {
                let decoded = try JSONDecoder().decode(T.self, from: data)
                return decoded
            } catch {
                logger.error("❌ Decoding error: \(error.localizedDescription, privacy: .public)")
                // Log raw response for debugging
                if let responseString = String(data: data, encoding: .utf8) {
                    logger.debug("📄 Raw response: \(responseString, privacy: .public)")
                }
                throw APIError.decodingError(error)
            }
        } catch let error as APIError {
            throw error
        } catch {
            logger.error("❌ Network error: \(error.localizedDescription, privacy: .public)")
            throw APIError.networkError(error)
        }
    }
    
    // MARK: - Auth Endpoints
    func oauthLogin(
        provider: String,
        providerId: String,
        email: String,
        name: String?,
        avatarUrl: String?
    ) async throws -> AuthResponse {
        let requestBody = OAuthRequest(
            provider: provider,
            providerId: providerId,
            email: email,
            name: name,
            avatarUrl: avatarUrl
        )
        let body = try JSONEncoder().encode(requestBody)
        
        // Log the request for debugging
        if let jsonString = String(data: body, encoding: .utf8) {
            NSLog("📤 OAuth Request Body: %@", jsonString)
        }
        
        return try await request(endpoint: "/api/auth/oauth", method: "POST", body: body)
    }
    
    func checkEmail(_ email: String) async throws -> Bool {
        struct CheckEmailRequest: Codable {
            let email: String
        }
        struct CheckEmailResponse: Codable {
            let exists: Bool
            let email: String
        }
        let body = try JSONEncoder().encode(CheckEmailRequest(email: email))
        let response: CheckEmailResponse = try await request(endpoint: "/api/auth/check-email", method: "POST", body: body)
        return response.exists
    }
    
    func loginWithEmail(email: String, password: String) async throws -> AuthResponse {
        struct LoginRequest: Codable {
            let email: String
            let password: String
        }
        let body = try JSONEncoder().encode(LoginRequest(email: email, password: password))
        return try await request(endpoint: "/api/auth/login", method: "POST", body: body)
    }
    
    func registerWithEmail(
        email: String,
        password: String,
        username: String,
        preferredLanguage: String,
        preferredCountry: String
    ) async throws -> AuthResponse {
        struct RegisterRequest: Codable {
            let email: String
            let password: String
            let username: String
            let preferredLanguage: String
            let preferredCountry: String
        }
        let body = try JSONEncoder().encode(RegisterRequest(
            email: email,
            password: password,
            username: username,
            preferredLanguage: preferredLanguage,
            preferredCountry: preferredCountry
        ))
        return try await request(endpoint: "/api/auth/register", method: "POST", body: body)
    }
    
    func getMe() async throws -> UserResponse {
        return try await request(endpoint: "/api/auth/me")
    }
    
    func refreshToken(_ refreshToken: String) async throws -> RefreshTokenResponse {
        struct RefreshRequest: Codable {
            let refreshToken: String
        }
        let body = try JSONEncoder().encode(RefreshRequest(refreshToken: refreshToken))
        return try await request(endpoint: "/api/auth/refresh", method: "POST", body: body)
    }
    
    func logout(_ refreshToken: String) async throws {
        struct LogoutRequest: Codable {
            let refreshToken: String
        }
        let body = try JSONEncoder().encode(LogoutRequest(refreshToken: refreshToken))
        let _: EmptyResponse = try await request(endpoint: "/api/auth/logout", method: "POST", body: body)
    }
    
    // MARK: - User Endpoints
    func updateProfile(username: String) async throws -> UserResponse {
        struct UpdateRequest: Codable {
            let username: String
        }
        let body = try JSONEncoder().encode(UpdateRequest(username: username))
        return try await request(endpoint: "/api/users/me", method: "PATCH", body: body)
    }
    
    func updateAvatar(avatarUrl: String?) async throws -> UserResponse {
        struct UpdateRequest: Codable {
            let avatarUrl: String?
        }
        let body = try JSONEncoder().encode(UpdateRequest(avatarUrl: avatarUrl))
        return try await request(endpoint: "/api/users/me", method: "PATCH", body: body)
    }

    func updateLanguage(preferredLanguage: String) async throws -> UserResponse {
        struct UpdateRequest: Codable {
            let preferredLanguage: String
        }
        let body = try JSONEncoder().encode(UpdateRequest(preferredLanguage: preferredLanguage))
        return try await request(endpoint: "/api/users/me/language", method: "PATCH", body: body)
    }
    
    func updateCountry(preferredCountry: String) async throws -> UserResponse {
        struct UpdateRequest: Codable {
            let preferredCountry: String
        }
        let body = try JSONEncoder().encode(UpdateRequest(preferredCountry: preferredCountry))
        return try await request(endpoint: "/api/users/me/country", method: "PATCH", body: body)
    }
    
    func updateRegion(preferredRegion: String?) async throws -> UserResponse {
        struct UpdateRequest: Codable {
            let preferredRegion: String?
        }
        let body = try JSONEncoder().encode(UpdateRequest(preferredRegion: preferredRegion))
        return try await request(endpoint: "/api/users/me/region", method: "PATCH", body: body)
    }
    
    func searchUsers(query: String) async throws -> UsersSearchResponse {
        let encodedQuery = query.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? query
        return try await request(endpoint: "/api/users/search?q=\(encodedQuery)")
    }
    
    // MARK: - Conversation Endpoints
    func getConversations() async throws -> ConversationsResponse {
        return try await request(endpoint: "/api/conversations")
    }
    
    func createConversation(participantIds: [String], type: String, name: String? = nil) async throws -> ConversationResponse {
        struct CreateRequest: Codable {
            let participantIds: [String]
            let type: String
            let name: String?
        }
        let body = try JSONEncoder().encode(CreateRequest(participantIds: participantIds, type: type, name: name))
        return try await request(endpoint: "/api/conversations", method: "POST", body: body)
    }
    
    func updateConversation(conversationId: String, name: String?, pictureUrl: String?) async throws -> ConversationResponse {
        struct UpdateRequest: Codable {
            let name: String?
            let pictureUrl: String?
        }
        let body = try JSONEncoder().encode(UpdateRequest(name: name, pictureUrl: pictureUrl))
        return try await request(endpoint: "/api/conversations/\(conversationId)", method: "PATCH", body: body)
    }
    
    func getGroupPictureUploadUrl(conversationId: String, fileName: String, contentType: String, fileSize: Int) async throws -> ProfilePictureUploadResponse {
        struct UploadRequest: Codable {
            let contentType: String
            let fileSize: Int
            let conversationId: String
        }
        let body = try JSONEncoder().encode(UploadRequest(contentType: contentType, fileSize: fileSize, conversationId: conversationId))
        return try await request(endpoint: "/api/attachments/upload-url", method: "POST", body: body)
    }
    
    func getMessages(conversationId: String, limit: Int? = nil, cursor: String? = nil) async throws -> MessagesResponse {
        var queryItems: [String] = []
        if let limit = limit {
            queryItems.append("limit=\(limit)")
        }
        if let cursor = cursor {
            queryItems.append("cursor=\(cursor)")
        }
        let queryString = queryItems.isEmpty ? "" : "?\(queryItems.joined(separator: "&"))"
        return try await request(endpoint: "/api/conversations/\(conversationId)/messages\(queryString)")
    }
    
    // MARK: - Attachment Endpoints
    func getUploadUrl(fileName: String, contentType: String, fileSize: Int, conversationId: String) async throws -> UploadUrlResponse {
        struct UploadRequest: Codable {
            let fileName: String
            let contentType: String
            let fileSize: Int
            let conversationId: String
        }
        let body = try JSONEncoder().encode(UploadRequest(fileName: fileName, contentType: contentType, fileSize: fileSize, conversationId: conversationId))
        return try await request(endpoint: "/api/attachments/upload-url", method: "POST", body: body)
    }
    
    // MARK: - Profile Picture Endpoints
    func getProfileUploadUrl(fileName: String, contentType: String, fileSize: Int) async throws -> ProfilePictureUploadResponse {
        struct UploadRequest: Codable {
            let contentType: String
            let fileSize: Int
        }
        let body = try JSONEncoder().encode(UploadRequest(contentType: contentType, fileSize: fileSize))
        return try await request(endpoint: "/api/users/profile-picture/upload-url", method: "POST", body: body)
    }
    
    func updateProfilePicture(key: String) async throws -> UserResponse {
        struct UpdateRequest: Codable {
            let key: String
        }
        let body = try JSONEncoder().encode(UpdateRequest(key: key))
        return try await request(endpoint: "/api/users/profile-picture", method: "PUT", body: body)
    }
    
    func deleteProfilePicture() async throws -> UserResponse {
        return try await request(endpoint: "/api/users/profile-picture", method: "DELETE")
    }
    
    func getDownloadUrl(key: String) async throws -> DownloadUrlResponse {
        let encodedKey = key.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? key
        return try await request(endpoint: "/api/attachments/download-url?key=\(encodedKey)")
    }
    
    // MARK: - File Upload
    func uploadFile(uploadUrl: String, data: Data, contentType: String) async throws {
        guard let url = URL(string: uploadUrl) else {
            throw APIError.invalidURL
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "PUT"
        request.setValue(contentType, forHTTPHeaderField: "Content-Type")
        request.httpBody = data
        
        let (_, response) = try await URLSession.shared.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse,
              (200...299).contains(httpResponse.statusCode) else {
            throw APIError.serverError("Upload failed")
        }
    }
    
    // MARK: - Device Registration
    func registerDeviceToken(token: String, platform: String) async throws {
        struct DeviceRequest: Codable {
            let token: String
            let platform: String
        }
        let body = try JSONEncoder().encode(DeviceRequest(token: token, platform: platform))
        let _: EmptyResponse = try await request(endpoint: "/api/devices/register", method: "POST", body: body)
    }
    
    // MARK: - Apple Sign-In
    func appleSignIn(idToken: String, nonce: String, fullName: String?, email: String?) async throws -> AuthResponse {
        struct AppleSignInRequest: Codable {
            let provider: String
            let idToken: String
            let nonce: String
            let fullName: String?
            let email: String?
        }
        let body = try JSONEncoder().encode(AppleSignInRequest(
            provider: "apple",
            idToken: idToken,
            nonce: nonce,
            fullName: fullName,
            email: email
        ))
        return try await request(endpoint: "/api/auth/oauth", method: "POST", body: body)
    }
    
    // MARK: - Mark Conversation as Read
    func markAsRead(conversationId: String) async throws {
        let _: EmptyResponse = try await request(endpoint: "/api/conversations/\(conversationId)/read", method: "POST")
    }
    
    // MARK: - Delete Message
    func deleteMessage(conversationId: String, messageId: String, forEveryone: Bool) async throws {
        var endpoint = "/api/conversations/\(conversationId)/messages/\(messageId)"
        if forEveryone {
            endpoint += "?forEveryone=true"
        }
        NSLog("🗑️ DELETE Message - conversationId: %@, messageId: %@, endpoint: %@", conversationId, messageId, endpoint)
        let _: EmptyResponse = try await request(endpoint: endpoint, method: "DELETE")
        NSLog("🗑️ DELETE Message - Success!")
    }
    
    // MARK: - Delete Conversation
    func deleteConversation(conversationId: String) async throws {
        let _: EmptyResponse = try await request(endpoint: "/api/conversations/\(conversationId)", method: "DELETE")
    }
    
    // MARK: - Add Participants to Group
    func addParticipants(conversationId: String, userIds: [String]) async throws -> ConversationResponse {
        struct AddParticipantsRequest: Codable {
            let userIds: [String]
        }
        let body = try JSONEncoder().encode(AddParticipantsRequest(userIds: userIds))
        return try await request(endpoint: "/api/conversations/\(conversationId)/participants", method: "POST", body: body)
    }
    
    // MARK: - Remove Participant from Group
    func removeParticipant(conversationId: String, userId: String) async throws -> ConversationResponse {
        return try await request(endpoint: "/api/conversations/\(conversationId)/participants/\(userId)", method: "DELETE")
    }
}

// MARK: - Empty Response for void endpoints
private struct EmptyResponse: Codable {}
