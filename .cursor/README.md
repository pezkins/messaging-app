# Cursor AI Agent Configuration

This directory contains configuration for 5 specialized AI agents for the Intok messaging app project.

## 🤖 Model Configuration

**All agents are pinned to: `claude-opus-4-20250514` (Claude Opus 4.5)**

This ensures consistent, high-quality responses across all agents.

## Agent Overview

| Agent | Specialization | Working Directory | Model |
|-------|---------------|-------------------|-------|
| 🍎 **iOS Developer** | Staff-level iOS/Swift/SwiftUI | `ios-native/` | Opus 4.5 |
| 🤖 **Android Developer** | Staff-level Android/Kotlin/Compose | `android-native/` | Opus 4.5 |
| ☁️ **Backend Developer** | Staff-level AWS/Serverless | `server-serverless/` | Opus 4.5 |
| 👁️ **Architect/Reviewer** | Code review & architecture | All folders | Opus 4.5 |
| 🔧 **CI/CD Engineer** | DevOps/Pipelines | `.github/` | Opus 4.5 |

## How to Use Agents

### In Cursor Chat
Reference an agent by starting your prompt with the agent name:

```
@ios-developer Implement push notification handling
@android-developer Add biometric authentication
@backend-developer Create new WebSocket event for typing indicators
@architect-reviewer Review the latest PR changes
@cicd-engineer Add automated testing to the pipeline
```

### Agent Rule Files
Each agent has a dedicated rule file in `.cursor/rules/`:
- `ios-developer.mdc` - iOS development guidelines
- `android-developer.mdc` - Android development guidelines
- `backend-developer.mdc` - Backend/AWS guidelines
- `architect-reviewer.mdc` - Code review guidelines
- `cicd-engineer.mdc` - CI/CD pipeline guidelines

## Deprecated Folders

The following folders are deprecated and should NOT be modified:
- `server/` - Legacy Express server (replaced by `server-serverless/`)
- `shared/` - Legacy shared types
- `mobile/` - Legacy React Native/Expo app (replaced by native apps)
- `infrastructure/` - Legacy infrastructure code

## Shared Team Documentation

All agents should reference the shared documentation in `/team/` folder:
- `team/project-overview.md` - Project architecture and goals
- `team/api-contracts.md` - API specifications
- `team/coding-standards.md` - Shared coding standards
- `team/release-process.md` - Release and deployment process
- `team/feature-roadmap.md` - Planned features and priorities

---

## 📁 Nested Rules Structure

Each agent's working directory has nested `.cursorrules` files for context-specific guidance:

### iOS Native (`ios-native/`)
```
ios-native/
├── .cursorrules                           # Root iOS rules
└── Intok/
    ├── Features/.cursorrules              # SwiftUI view guidelines
    └── Core/Network/.cursorrules          # Networking layer rules
```

### Android Native (`android-native/`)
```
android-native/
├── .cursorrules                           # Root Android rules
└── app/src/main/java/com/intokapp/app/
    ├── ui/.cursorrules                    # Compose UI guidelines
    └── data/.cursorrules                  # Data layer rules
```

### Backend Serverless (`server-serverless/`)
```
server-serverless/
├── .cursorrules                           # Root backend rules
└── src/
    ├── handlers/.cursorrules              # Lambda handler guidelines
    └── lib/.cursorrules                   # Shared library rules
```

### CI/CD (`.github/`)
```
.github/
└── .cursorrules                           # Workflow guidelines
```

## Rule Hierarchy

Rules are applied in order of specificity:
1. **Global** - `.cursorrules` (project root)
2. **Agent** - `.cursor/rules/*.mdc` (agent-specific)
3. **Directory** - `*/**.cursorrules` (nested context-specific)

More specific rules take precedence and add context to broader rules.

