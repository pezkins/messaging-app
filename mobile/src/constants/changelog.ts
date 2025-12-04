// App changelog for "What's New" feature
export interface ChangelogEntry {
  version: string;
  date: string;
  changes: string[];
}

export const CHANGELOG: ChangelogEntry[] = [
  {
    version: '0.0.3',
    date: '2024-12-04',
    changes: [
      '🎅 Added Christmas & holiday emojis (Santa, Mrs. Claus, reindeer, elf)',
      '🔄 Fixed translation - Changing language no longer re-translates history',
      '✨ Added "What\'s New" feature to see version updates',
      '📋 Version history now available in Settings',
    ],
  },
  {
    version: '0.0.2',
    date: '2024-12-04',
    changes: [
      '🎉 Emoji reactions - Long-press messages to react with emojis',
      '🔍 Searchable emoji picker - Find any emoji by name',
      '💜 New purple theme matching Intok branding',
      '🍎 iOS support - Now available for iPhone',
      '🔄 Improved translation caching',
      '👥 Group chat support',
      '✏️ Update display name in settings',
      '🌍 Country-specific translations',
    ],
  },
  {
    version: '0.0.1',
    date: '2024-12-01',
    changes: [
      '🚀 Initial release',
      '🔐 Google authentication',
      '💬 Real-time messaging',
      '🌐 Auto-translation to your preferred language',
      '👤 User search and new conversations',
    ],
  },
];

export const CURRENT_VERSION = '0.0.3';

export function getLatestChangelog(): ChangelogEntry | undefined {
  return CHANGELOG[0];
}

export function getChangelogForVersion(version: string): ChangelogEntry | undefined {
  return CHANGELOG.find(entry => entry.version === version);
}

