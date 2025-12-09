# 📝 Coding Standards

This document defines the shared coding standards for all Intok projects.

---

## General Principles

### 1. Clean Code
- Write self-documenting code with clear names
- Keep functions/methods small and focused
- Follow the Single Responsibility Principle
- Avoid deep nesting (max 3 levels)

### 2. DRY (Don't Repeat Yourself)
- Extract common logic into shared utilities
- Use inheritance/protocols/interfaces appropriately
- Create reusable components

### 3. KISS (Keep It Simple, Stupid)
- Choose the simplest solution that works
- Avoid premature optimization
- Don't over-engineer

### 4. YAGNI (You Aren't Gonna Need It)
- Only implement what's currently needed
- Don't build for hypothetical futures
- Remove dead code promptly

---

## iOS (Swift/SwiftUI)

### Naming Conventions

```swift
// Types: UpperCamelCase
struct MessageBubble { }
class ChatViewModel { }
enum MessageType { }
protocol MessageDelegate { }

// Variables/Functions: lowerCamelCase
let messageCount = 5
func sendMessage(_ message: Message) { }

// Constants: lowerCamelCase
let maxMessageLength = 1000

// Boolean prefixes
var isLoading = false
var hasUnreadMessages = true
var canSendMessage = true
```

### SwiftUI Conventions

```swift
// View structure
struct ChatView: View {
    // 1. Environment and state
    @Environment(\.dismiss) private var dismiss
    @State private var messageText = ""
    
    // 2. Dependencies
    let viewModel: ChatViewModel
    
    // 3. Body
    var body: some View {
        content
            .navigationTitle("Chat")
            .onAppear { viewModel.loadMessages() }
    }
    
    // 4. View builders (private)
    @ViewBuilder
    private var content: some View {
        VStack {
            messageList
            inputBar
        }
    }
    
    private var messageList: some View {
        ScrollView { /* ... */ }
    }
}
```

### File Organization

```swift
// MARK: - Properties
// MARK: - Initialization
// MARK: - View Body
// MARK: - Private Views
// MARK: - Actions
// MARK: - Helpers
```

### Error Handling

```swift
// Prefer Result types or async throws
func fetchMessages() async throws -> [Message] {
    let response = try await apiClient.get("/messages")
    return try decoder.decode([Message].self, from: response.data)
}

// Handle errors gracefully
do {
    messages = try await fetchMessages()
} catch {
    errorMessage = error.localizedDescription
    showError = true
}
```

---

## Android (Kotlin/Jetpack Compose)

### Naming Conventions

```kotlin
// Classes: UpperCamelCase
class ChatViewModel : ViewModel()
data class Message(...)
interface MessageRepository
object NetworkModule

// Variables/Functions: lowerCamelCase
val messageCount = 5
fun sendMessage(message: Message) { }

// Constants: UPPER_SNAKE_CASE (in companion object)
companion object {
    const val MAX_MESSAGE_LENGTH = 1000
}

// Boolean prefixes
var isLoading = false
var hasUnreadMessages = true
```

### Compose Conventions

```kotlin
// Composable naming: UpperCamelCase
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    ChatScreenContent(
        uiState = uiState,
        onSendMessage = viewModel::sendMessage,
        onNavigateBack = onNavigateBack
    )
}

// Stateless composable (for preview/testing)
@Composable
private fun ChatScreenContent(
    uiState: ChatUiState,
    onSendMessage: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    // UI implementation
}
```

### ViewModel Pattern

```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    fun sendMessage(content: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true) }
            try {
                repository.sendMessage(content)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isSending = false) }
            }
        }
    }
}

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null
)
```

### Null Safety

```kotlin
// Prefer non-null types
val message: Message  // Not Message?

// Use safe calls and elvis operator
val userName = user?.name ?: "Unknown"

// NEVER use !! in production code
// Bad: user!!.name
// Good: user?.name ?: throw IllegalStateException("User required")
```

---

## Backend (TypeScript/Node.js)

### Naming Conventions

```typescript
// Types/Interfaces: UpperCamelCase
interface User { }
type MessageType = 'text' | 'image';

// Variables/Functions: camelCase
const messageCount = 5;
function sendMessage(message: Message): void { }

// Constants: UPPER_SNAKE_CASE
const MAX_MESSAGE_LENGTH = 1000;

// Files: kebab-case
// user-service.ts, message-handler.ts
```

### TypeScript Best Practices

```typescript
// Use strict types - avoid any
interface CreateMessageInput {
  content: string;
  conversationId: string;
  type: MessageType;
}

// Use discriminated unions for state
type ApiResult<T> = 
  | { status: 'success'; data: T }
  | { status: 'error'; error: string };

// Prefer async/await over callbacks
async function getMessages(conversationId: string): Promise<Message[]> {
  const result = await dynamoClient.query({
    TableName: TABLE_NAME,
    KeyConditionExpression: 'conversationId = :convId',
    ExpressionAttributeValues: { ':convId': conversationId }
  });
  return result.Items as Message[];
}
```

### Lambda Handler Structure

```typescript
import { APIGatewayProxyHandler, APIGatewayProxyResult } from 'aws-lambda';

export const handler: APIGatewayProxyHandler = async (event) => {
  try {
    // 1. Parse and validate input
    const body = JSON.parse(event.body || '{}');
    
    // 2. Business logic
    const result = await processRequest(body);
    
    // 3. Return success response
    return success(result);
    
  } catch (error) {
    // 4. Handle errors consistently
    return handleError(error);
  }
};

// Helper functions
function success(data: unknown): APIGatewayProxyResult {
  return {
    statusCode: 200,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  };
}

function handleError(error: unknown): APIGatewayProxyResult {
  console.error('Error:', error);
  return {
    statusCode: error instanceof ValidationError ? 400 : 500,
    body: JSON.stringify({ error: { message: error.message } })
  };
}
```

---

## Git Conventions

### Branch Naming

```
feature/add-push-notifications
bugfix/fix-message-ordering
hotfix/critical-auth-issue
chore/update-dependencies
refactor/cleanup-api-service
```

### Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

**Types:**
- `feat` - New feature
- `fix` - Bug fix
- `docs` - Documentation only
- `style` - Code style (formatting, etc.)
- `refactor` - Code change that neither fixes a bug nor adds a feature
- `perf` - Performance improvement
- `test` - Adding tests
- `chore` - Build process or auxiliary tool changes

**Examples:**
```
feat(ios): add push notification handling

fix(android): resolve message ordering issue in chat view

docs: update API contracts for new endpoints

refactor(backend): extract translation logic into service
```

### Pull Request Guidelines

1. **Title:** Use conventional commit format
2. **Description:** 
   - What changed and why
   - Screenshots for UI changes
   - Testing steps
3. **Checklist:**
   - [ ] Code compiles without warnings
   - [ ] Tests pass
   - [ ] Documentation updated
   - [ ] Reviewed own code

---

## Code Review Standards

### What to Look For

1. **Correctness:** Does it work as intended?
2. **Readability:** Is it easy to understand?
3. **Maintainability:** Will it be easy to modify?
4. **Performance:** Are there obvious inefficiencies?
5. **Security:** Are there vulnerabilities?

### Review Etiquette

- **Be constructive:** Suggest improvements, don't criticize
- **Be specific:** Point to exact lines/issues
- **Explain why:** Help the author learn
- **Acknowledge good work:** Positive feedback matters

---

## Documentation Standards

### Code Comments

```swift
// Good: Explains WHY
// Using exponential backoff because the server rate-limits aggressive retries
let delay = pow(2.0, Double(retryCount))

// Bad: Explains WHAT (code already shows this)
// Increment counter by 1
counter += 1
```

### API Documentation

- Document all public interfaces
- Include parameter descriptions
- Provide usage examples
- Note any side effects

---

*Last Updated: December 2024*

