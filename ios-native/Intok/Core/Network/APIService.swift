import Foundation

// MARK: - Additional Response Types
struct UserResponse: Codable {
    let user: User
}

// MARK: - API Service
class APIService {
    static let shared = APIService()
    
    private let baseURL: String
    private var accessToken: String?
    
    private init() {
        // Get API URL from Info.plist
        if let url = Bundle.main.object(forInfoDictionaryKey: "API_URL") as? String {
            baseURL = url
        } else {
            baseURL = "https://aji93f9i0k.execute-api.us-east-1.amazonaws.com/prod"
        }
        print("🌐 API Service initialized with: \(baseURL)")
    }
    
    func setAccessToken(_ token: String?) {
        self.accessToken = token
    }
    
    func getAccessToken() -> String? {
        return accessToken
    }
    
    // MARK: - Generic Request Method
    private func request<T: Decodable>(
        endpoint: String,
        method: String = "GET",
        body: Encodable? = nil
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
            request.httpBody = try JSONEncoder().encode(body)
        }
        
        print("📤 API Request: \(method) \(endpoint)")
        
        let (data, response) = try await URLSession.shared.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse else {
            throw APIError.invalidResponse
        }
        
        print("📥 API Response: \(httpResponse.statusCode)")
        
        guard (200...299).contains(httpResponse.statusCode) else {
            if let errorJson = try? JSONDecoder().decode(APIErrorResponse.self, from: data) {
                throw APIError.serverError(errorJson.message)
            }
            throw APIError.httpError(httpResponse.statusCode)
        }
        
        return try JSONDecoder().decode(T.self, from: data)
    }
    
    // MARK: - Auth Endpoints
    func oauthLogin(provider: String, providerId: String, email: String, name: String?, avatarUrl: String?) async throws -> AuthResponse {
        let body = OAuthRequest(
            provider: provider,
            providerId: providerId,
            email: email,
            name: name,
            avatarUrl: avatarUrl
        )
        return try await request(endpoint: "/api/auth/oauth", method: "POST", body: body)
    }
    
    func getMe() async throws -> UserResponse {
        return try await request(endpoint: "/api/auth/me")
    }
    
    func updateProfile(username: String) async throws -> UserResponse {
        struct UpdateRequest: Codable {
            let username: String
        }
        return try await request(endpoint: "/api/users/me", method: "PATCH", body: UpdateRequest(username: username))
    }
    
    func updateLanguage(preferredLanguage: String) async throws -> UserResponse {
        struct UpdateRequest: Codable {
            let preferredLanguage: String
        }
        return try await request(endpoint: "/api/users/me/language", method: "PATCH", body: UpdateRequest(preferredLanguage: preferredLanguage))
    }
    
    func updateCountry(preferredCountry: String) async throws -> UserResponse {
        struct UpdateRequest: Codable {
            let preferredCountry: String
        }
        return try await request(endpoint: "/api/users/me/country", method: "PATCH", body: UpdateRequest(preferredCountry: preferredCountry))
    }
}

// MARK: - API Errors
enum APIError: LocalizedError {
    case invalidURL
    case invalidResponse
    case httpError(Int)
    case serverError(String)
    
    var errorDescription: String? {
        switch self {
        case .invalidURL:
            return "Invalid URL"
        case .invalidResponse:
            return "Invalid response from server"
        case .httpError(let code):
            return "HTTP error: \(code)"
        case .serverError(let message):
            return message
        }
    }
}

struct APIErrorResponse: Codable {
    let message: String
}
