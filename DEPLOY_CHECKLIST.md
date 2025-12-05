# 🚀 Deploy Checklist

Use this checklist before merging to `main` to ensure a successful deployment.

## Pre-Deploy Checklist

### Version Bump (REQUIRED for Play Store)
- [ ] **Increment `versionCode`** in `mobile/app.json`
  ```bash
  # Easy way - use the bump script:
  cd mobile && ./scripts/bump-version.sh patch
  ```
- [ ] **Update `version`** string (e.g., `0.0.5` → `0.0.6`)
- [ ] **Update `CURRENT_VERSION`** in `mobile/src/constants/changelog.ts`
- [ ] **Add changelog entry** for the new version

### Code Quality
- [ ] All features tested locally
- [ ] No TypeScript/lint errors
- [ ] Build completes successfully (`eas build --local`)

### Backend Changes (if applicable)
- [ ] Backend changes deployed to AWS
- [ ] API endpoints tested

## Quick Version Bump

```bash
# From the mobile directory:
cd mobile

# Patch bump (0.0.5 → 0.0.6)
./scripts/bump-version.sh patch

# Minor bump (0.0.5 → 0.1.0)
./scripts/bump-version.sh minor

# Major bump (0.0.5 → 1.0.0)
./scripts/bump-version.sh major
```

## Manual Version Update

If you prefer to update manually:

1. **`mobile/app.json`**:
   ```json
   {
     "version": "0.0.6",      // Increment this
     "android": {
       "versionCode": 8       // MUST increment for Play Store
     }
   }
   ```

2. **`mobile/src/constants/changelog.ts`**:
   ```typescript
   export const CURRENT_VERSION = '0.0.6';  // Match app.json version
   
   export const CHANGELOG: ChangelogEntry[] = [
     {
       version: '0.0.6',
       date: '2024-12-06',
       changes: [
         '✨ New feature description',
         '🔧 Bug fix description',
       ],
     },
     // ... previous versions
   ];
   ```

## Common Errors

### "Version code X has already been used"
**Cause:** `versionCode` in `app.json` wasn't incremented.
**Fix:** Increment `versionCode` and push again.

### Build fails on EAS
**Cause:** Usually dependency or configuration issues.
**Fix:** Check the build logs, ensure `package-lock.json` is committed.

## CI/CD Pipeline

The pipeline automatically:
1. ✅ Checks version info
2. ✅ Builds Android AAB
3. ✅ Uploads to Google Play Internal Testing

Monitor at: https://github.com/pezkins/messaging-app/actions

