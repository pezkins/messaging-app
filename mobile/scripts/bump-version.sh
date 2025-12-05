#!/bin/bash
# Version Bump Script for Intok Mobile App
# Usage: ./scripts/bump-version.sh [patch|minor|major]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_JSON="$SCRIPT_DIR/../app.json"
CHANGELOG="$SCRIPT_DIR/../src/constants/changelog.ts"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Get current version info
CURRENT_VERSION=$(grep '"version":' "$APP_JSON" | sed 's/.*"\([0-9.]*\)".*/\1/')
CURRENT_VERSION_CODE=$(grep '"versionCode":' "$APP_JSON" | sed 's/.*: *\([0-9]*\).*/\1/')

echo -e "${YELLOW}📱 Intok Version Bump Script${NC}"
echo "================================"
echo -e "Current version: ${GREEN}$CURRENT_VERSION${NC} (versionCode: $CURRENT_VERSION_CODE)"
echo ""

# Parse version parts
IFS='.' read -r -a VERSION_PARTS <<< "$CURRENT_VERSION"
MAJOR=${VERSION_PARTS[0]:-0}
MINOR=${VERSION_PARTS[1]:-0}
PATCH=${VERSION_PARTS[2]:-0}

# Determine bump type
BUMP_TYPE=${1:-patch}

case $BUMP_TYPE in
  major)
    MAJOR=$((MAJOR + 1))
    MINOR=0
    PATCH=0
    ;;
  minor)
    MINOR=$((MINOR + 1))
    PATCH=0
    ;;
  patch)
    PATCH=$((PATCH + 1))
    ;;
  *)
    echo -e "${RED}Invalid bump type: $BUMP_TYPE${NC}"
    echo "Usage: ./bump-version.sh [patch|minor|major]"
    exit 1
    ;;
esac

NEW_VERSION="$MAJOR.$MINOR.$PATCH"
NEW_VERSION_CODE=$((CURRENT_VERSION_CODE + 1))

echo -e "Bump type: ${YELLOW}$BUMP_TYPE${NC}"
echo -e "New version: ${GREEN}$NEW_VERSION${NC} (versionCode: $NEW_VERSION_CODE)"
echo ""

# Confirm
read -p "Proceed with version bump? (y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
  echo "Aborted."
  exit 0
fi

# Update app.json
echo "Updating app.json..."
sed -i "s/\"version\": \"$CURRENT_VERSION\"/\"version\": \"$NEW_VERSION\"/" "$APP_JSON"
sed -i "s/\"versionCode\": $CURRENT_VERSION_CODE/\"versionCode\": $NEW_VERSION_CODE/" "$APP_JSON"

# Update changelog CURRENT_VERSION
echo "Updating changelog.ts..."
sed -i "s/CURRENT_VERSION = '$CURRENT_VERSION'/CURRENT_VERSION = '$NEW_VERSION'/" "$CHANGELOG"

echo ""
echo -e "${GREEN}✅ Version bumped successfully!${NC}"
echo ""
echo -e "${YELLOW}📋 Don't forget to:${NC}"
echo "  1. Add changelog entry in src/constants/changelog.ts"
echo "  2. Commit changes: git add -A && git commit -m \"Bump version to $NEW_VERSION\""
echo "  3. Push to main to trigger CI/CD"
echo ""

