# 🚀 Release Process

This document outlines the release and deployment procedures for Intok.

---

## Release Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           RELEASE PIPELINE                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   Developer  ──►  dev branch  ──►  stage branch  ──►  main branch           │
│      │               │                 │                   │                 │
│      │               ▼                 ▼                   ▼                 │
│      │          Debug Builds      Internal Test       Production            │
│      │          (Artifacts)       (TestFlight/        (App Store/           │
│      │                            Play Internal)       Play Store)          │
│      │                                                                       │
│      └───────────────────────────────────────────────────────────────────────┤
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Branch Strategy

| Branch | Purpose | Trigger | Deployment Target |
|--------|---------|---------|-------------------|
| `dev` | Development | Automatic on push | Debug artifacts |
| `stage` | Internal testing | PR merge from dev | TestFlight + Play Internal |
| `main` | Production | PR merge from stage | App Store + Play Store |

---

## Development Workflow (dev branch)

### Daily Development

1. **Create feature branch:**
   ```bash
   git checkout dev
   git pull origin dev
   git checkout -b feature/my-feature
   ```

2. **Make changes and commit:**
   ```bash
   git add .
   git commit -m "feat(ios): add new feature"
   ```

3. **Push and create PR to dev:**
   ```bash
   git push origin feature/my-feature
   # Create PR: feature/my-feature → dev
   ```

4. **After PR approval and merge:**
   - CI/CD builds debug artifacts automatically
   - **Architect/Reviewer initiates local simulator testing**
   - Download artifacts from GitHub Actions

### 📱 Local Simulator Testing (Architect/Reviewer Responsibility)

After changes are merged to `dev`, the Architect/Reviewer initiates local testing for quick verification:

**iOS (run from project root):**
```bash
cd ios-native && xcodebuild -project Intok.xcodeproj -scheme Intok \
  -destination 'platform=iOS Simulator,name=iPhone 15' build && \
  xcrun simctl install booted ./build/Debug-iphonesimulator/Intok.app && \
  xcrun simctl launch booted com.intokapp.app
```

**Android (run from project root):**
```bash
cd android-native && ./gradlew assembleDebug && \
  adb install -r app/build/outputs/apk/debug/app-debug.apk && \
  adb shell am start -n com.intokapp.app/.MainActivity
```

**Quick Verification Checklist:**
- [ ] App launches without crashes
- [ ] New feature works as expected
- [ ] No obvious UI regressions
- [ ] Bug fix resolves the issue

### Testing Debug Builds

**Download artifacts:**
```bash
# List recent runs
gh run list --branch dev --limit 5

# Download latest artifacts
gh run download <RUN_ID> -D ./builds
```

**Install on simulators:**
```bash
# Android (emulator must be running)
adb install ./builds/intok-android-debug-*/app-debug.apk

# iOS (simulator must be running)
xcrun simctl install booted ./builds/intok-ios-debug-*/Intok.app
```

---

## Staging Release (stage branch)

### When to Release to Stage

- Feature is complete and tested on dev
- Ready for internal team testing
- QA cycle needed before production

### Release Process

1. **Create PR from dev to stage:**
   ```bash
   # On GitHub: New Pull Request → dev → stage
   # Or via CLI:
   gh pr create --base stage --head dev --title "Release v1.2.0 to staging"
   ```

2. **PR Checklist:**
   - [ ] All features complete
   - [ ] Unit tests pass
   - [ ] **Local simulator testing completed** (by Architect/Reviewer)
   - [ ] Manual testing on dev completed
   - [ ] No blocking bugs
   - [ ] Version numbers updated (if needed)

3. **After merge:**
   - CI/CD automatically builds signed releases
   - Deploys to TestFlight (iOS)
   - Deploys to Play Store Internal Testing (Android)

### Version Bumping

**iOS (ios-native/Intok.xcodeproj):**
- Update `MARKETING_VERSION` (e.g., 1.2.0)
- Update `CURRENT_PROJECT_VERSION` (increment build number)

**Android (android-native/app/build.gradle.kts):**
```kotlin
defaultConfig {
    versionCode = 12        // Increment for each release
    versionName = "1.2.0"   // Semantic version
}
```

---

## Production Release (main branch)

### Pre-Release Checklist

- [ ] Stage testing completed successfully
- [ ] All critical bugs fixed
- [ ] Release notes prepared
- [ ] App Store metadata updated (if needed)
- [ ] Screenshots updated (if UI changed)

### Release Process

1. **Create PR from stage to main:**
   ```bash
   gh pr create --base main --head stage --title "Release v1.2.0 to production"
   ```

2. **Release Notes PR Description:**
   ```markdown
   ## Release v1.2.0
   
   ### New Features
   - Feature A description
   - Feature B description
   
   ### Bug Fixes
   - Fix for issue #123
   - Fix for issue #456
   
   ### Breaking Changes
   - None
   
   ### Internal Changes
   - Updated dependencies
   ```

3. **After approval and merge:**
   - CI/CD builds production releases
   - Submits to App Store (iOS)
   - Submits to Play Store Production (Android)

4. **Post-Release:**
   - Create GitHub release with tag
   - Update changelog
   - Monitor crash reports

### Creating GitHub Release

```bash
# Create and push tag
git checkout main
git pull origin main
git tag -a v1.2.0 -m "Release v1.2.0"
git push origin v1.2.0

# Create release on GitHub
gh release create v1.2.0 --title "v1.2.0" --notes "Release notes here"
```

---

## Backend Releases

The backend (`server-serverless/`) deploys automatically when changes are merged to `dev`.

### CI/CD Behavior by Branch

| Branch | Code Quality | Build | Deploy to AWS |
|--------|--------------|-------|---------------|
| `dev` | ✅ | ✅ | ✅ Auto-deploy |
| `stage` | ✅ | ✅ | ❌ Build only |
| `main` | ✅ | ✅ | ❌ Build only |

**Path Filter:** Only triggers when `server-serverless/**` changes.

### Deploy Backend to Production

**Option 1: Merge to dev (Recommended)**
```bash
# Backend deploys automatically when merged to dev
git checkout dev
git merge feature/backend-fix
git push origin dev
# → Triggers build + deploy to AWS
```

**Option 2: Manual workflow dispatch**
1. Go to **GitHub Actions** → **Backend CI/CD**
2. Click **"Run workflow"**
3. Select branch, check **"Deploy to AWS"**
4. Click **"Run workflow"**

### Manual Deployment (Local)

```bash
cd server-serverless
npm run build
sam build
sam deploy --guided  # First time
sam deploy           # Subsequent deploys
```

### Rollback

```bash
# List recent deployments
aws cloudformation list-stacks --stack-status-filter UPDATE_COMPLETE

# Rollback to previous version
aws cloudformation rollback-stack --stack-name intok-api
```

---

## Emergency Hotfix Process

For critical production bugs:

1. **Create hotfix branch from main:**
   ```bash
   git checkout main
   git pull origin main
   git checkout -b hotfix/critical-fix
   ```

2. **Make minimal fix:**
   ```bash
   git add .
   git commit -m "hotfix: fix critical auth issue"
   ```

3. **PR directly to main:**
   ```bash
   gh pr create --base main --head hotfix/critical-fix --title "HOTFIX: Critical auth fix"
   ```

4. **After merge, backport to dev and stage:**
   ```bash
   git checkout dev
   git cherry-pick <commit-hash>
   git push origin dev
   
   git checkout stage
   git cherry-pick <commit-hash>
   git push origin stage
   ```

---

## CI/CD Pipeline Details

### GitHub Actions Workflows

| Workflow | File | Purpose |
|----------|------|---------|
| iOS CI | `ios-native-ci.yml` | Build and deploy iOS app |
| Android CI | `android-native-ci.yml` | Build and deploy Android app |
| Backend CI | `backend-ci.yml` | Deploy serverless backend |

### Required Secrets

See `NATIVE_MIGRATION.md` for complete list of required GitHub secrets.

### Workflow Triggers

```yaml
# Automatic on push
on:
  push:
    branches: [dev, stage, main]
    paths:
      - 'ios-native/**'     # For iOS workflow
      - 'android-native/**' # For Android workflow
      - 'server-serverless/**' # For backend workflow

# Manual trigger
on:
  workflow_dispatch:
    inputs:
      environment:
        description: 'Target environment'
        required: true
        default: 'dev'
```

---

## App Store Guidelines

### iOS (App Store Connect)

1. **Build Processing:** ~30 minutes after upload
2. **Review Time:** 24-48 hours (usually)
3. **Rejection Reasons:**
   - Privacy policy issues
   - Missing functionality
   - Crashes during review
   - Guideline violations

### Android (Play Console)

1. **Internal Testing:** Immediate after upload
2. **Production Review:** 1-3 days
3. **Rejection Reasons:**
   - Policy violations
   - Sensitive permissions without justification
   - Deceptive behavior

---

## Monitoring & Rollback

### Crash Monitoring

- iOS: Firebase Crashlytics / Xcode Organizer
- Android: Firebase Crashlytics / Play Console

### Key Metrics to Watch

- Crash-free rate (target: >99%)
- ANR rate on Android (target: <0.5%)
- User reviews and ratings
- API error rates

### Rollback Procedures

**iOS:** Not possible to rollback after release. Must submit new version with fix.

**Android:**
1. Go to Play Console → Release management
2. Create new release with previous version
3. Or use staged rollout to pause

**Backend:**
```bash
# Use CloudFormation rollback
aws cloudformation cancel-update-stack --stack-name intok-api
aws cloudformation rollback-stack --stack-name intok-api
```

---

*Last Updated: December 2024*

