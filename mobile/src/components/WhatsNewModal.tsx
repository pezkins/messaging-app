import { useEffect, useState } from 'react';
import {
  Modal,
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
} from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Ionicons } from '@expo/vector-icons';
import { colors, spacing, borderRadius, fontSize } from '../constants/theme';
import { CHANGELOG, CURRENT_VERSION, type ChangelogEntry } from '../constants/changelog';

const LAST_SEEN_VERSION_KEY = 'intok_last_seen_version';

interface WhatsNewModalProps {
  // If provided, shows changelog for specific version. Otherwise auto-detects new version.
  forceShow?: boolean;
}

export function WhatsNewModal({ forceShow = false }: WhatsNewModalProps) {
  const [visible, setVisible] = useState(false);
  const [newChanges, setNewChanges] = useState<ChangelogEntry[]>([]);

  useEffect(() => {
    checkForNewVersion();
  }, []);

  const checkForNewVersion = async () => {
    try {
      const lastSeenVersion = await AsyncStorage.getItem(LAST_SEEN_VERSION_KEY);
      
      if (forceShow || !lastSeenVersion) {
        // First time or force show - show current version
        setNewChanges([CHANGELOG[0]]);
        setVisible(true);
      } else if (lastSeenVersion !== CURRENT_VERSION) {
        // New version - find all changes since last seen
        const lastIndex = CHANGELOG.findIndex(c => c.version === lastSeenVersion);
        if (lastIndex > 0) {
          setNewChanges(CHANGELOG.slice(0, lastIndex));
          setVisible(true);
        } else if (lastIndex === -1) {
          // Unknown version, show latest
          setNewChanges([CHANGELOG[0]]);
          setVisible(true);
        }
      }
    } catch (error) {
      console.error('Error checking version:', error);
    }
  };

  const handleDismiss = async () => {
    try {
      await AsyncStorage.setItem(LAST_SEEN_VERSION_KEY, CURRENT_VERSION);
    } catch (error) {
      console.error('Error saving version:', error);
    }
    setVisible(false);
  };

  if (!visible || newChanges.length === 0) {
    return null;
  }

  return (
    <Modal
      visible={visible}
      transparent
      animationType="fade"
      onRequestClose={handleDismiss}
    >
      <View style={styles.overlay}>
        <View style={styles.container}>
          {/* Header */}
          <View style={styles.header}>
            <View style={styles.headerIcon}>
              <Ionicons name="sparkles" size={28} color={colors.primary[400]} />
            </View>
            <Text style={styles.title}>What's New</Text>
            <Text style={styles.subtitle}>in Intok v{CURRENT_VERSION}</Text>
          </View>

          {/* Changes List */}
          <ScrollView 
            style={styles.scrollView}
            contentContainerStyle={styles.scrollContent}
            showsVerticalScrollIndicator={false}
          >
            {newChanges.map((entry) => (
              <View key={entry.version} style={styles.versionSection}>
                {newChanges.length > 1 && (
                  <Text style={styles.versionHeader}>v{entry.version}</Text>
                )}
                {entry.changes.map((change, index) => (
                  <View key={index} style={styles.changeItem}>
                    <Text style={styles.changeText}>{change}</Text>
                  </View>
                ))}
              </View>
            ))}
          </ScrollView>

          {/* Dismiss Button */}
          <TouchableOpacity style={styles.button} onPress={handleDismiss}>
            <Text style={styles.buttonText}>Got it!</Text>
          </TouchableOpacity>
        </View>
      </View>
    </Modal>
  );
}

// Standalone function to show What's New from Settings
export async function resetWhatsNewSeen() {
  try {
    await AsyncStorage.removeItem(LAST_SEEN_VERSION_KEY);
  } catch (error) {
    console.error('Error resetting version:', error);
  }
}

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.7)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: spacing.lg,
  },
  container: {
    backgroundColor: colors.surface[900],
    borderRadius: borderRadius.xl,
    width: '100%',
    maxWidth: 400,
    maxHeight: '80%',
    borderWidth: 1,
    borderColor: colors.surface[700],
    overflow: 'hidden',
  },
  header: {
    alignItems: 'center',
    paddingTop: spacing.xl,
    paddingBottom: spacing.md,
    paddingHorizontal: spacing.lg,
    borderBottomWidth: 1,
    borderBottomColor: colors.surface[800],
  },
  headerIcon: {
    width: 56,
    height: 56,
    borderRadius: borderRadius.full,
    backgroundColor: colors.primary[900],
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: spacing.md,
  },
  title: {
    fontSize: fontSize.xxl,
    fontWeight: '700',
    color: colors.white,
  },
  subtitle: {
    fontSize: fontSize.md,
    color: colors.surface[400],
    marginTop: spacing.xs,
  },
  scrollView: {
    maxHeight: 300,
  },
  scrollContent: {
    padding: spacing.lg,
  },
  versionSection: {
    marginBottom: spacing.md,
  },
  versionHeader: {
    fontSize: fontSize.lg,
    fontWeight: '600',
    color: colors.primary[400],
    marginBottom: spacing.sm,
  },
  changeItem: {
    flexDirection: 'row',
    paddingVertical: spacing.xs,
  },
  changeText: {
    fontSize: fontSize.md,
    color: colors.surface[200],
    lineHeight: 22,
  },
  button: {
    backgroundColor: colors.primary[600],
    marginHorizontal: spacing.lg,
    marginBottom: spacing.lg,
    marginTop: spacing.sm,
    paddingVertical: spacing.md,
    borderRadius: borderRadius.lg,
    alignItems: 'center',
  },
  buttonText: {
    color: colors.white,
    fontSize: fontSize.lg,
    fontWeight: '600',
  },
});

