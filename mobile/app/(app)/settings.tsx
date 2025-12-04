import { useState } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
  ActivityIndicator,
  TextInput,
  Modal,
} from 'react-native';
import { router } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { Picker } from '@react-native-picker/picker';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useAuthStore } from '../../src/store/auth';
import { colors, spacing, borderRadius, fontSize } from '../../src/constants/theme';
import { LANGUAGES, COUNTRIES, type LanguageCode, type CountryCode } from '../../src/constants/languages';
import { CHANGELOG, CURRENT_VERSION, type ChangelogEntry } from '../../src/constants/changelog';

export default function SettingsScreen() {
  const { user, updateLanguage, updateCountry, updateUsername, logout } = useAuthStore();
  const [displayName, setDisplayName] = useState(user?.username || '');
  const [selectedLanguage, setSelectedLanguage] = useState<LanguageCode>(
    (user?.preferredLanguage as LanguageCode) || 'en'
  );
  const [selectedCountry, setSelectedCountry] = useState<CountryCode>(
    (user?.preferredCountry as CountryCode) || 'US'
  );
  const [isSaving, setIsSaving] = useState(false);
  const [showChangelog, setShowChangelog] = useState(false);

  const handleSave = async () => {
    const nameChanged = displayName !== user?.username && displayName.trim().length >= 3;
    const languageChanged = selectedLanguage !== user?.preferredLanguage;
    const countryChanged = selectedCountry !== user?.preferredCountry;

    if (!nameChanged && !languageChanged && !countryChanged) {
      router.back();
      return;
    }

    setIsSaving(true);
    try {
      if (nameChanged) {
        await updateUsername(displayName.trim());
      }
      if (languageChanged) {
        await updateLanguage(selectedLanguage);
      }
      if (countryChanged) {
        await updateCountry(selectedCountry);
      }
      router.back();
    } catch (error) {
      console.error('Failed to update settings:', error);
    } finally {
      setIsSaving(false);
    }
  };

  const handleLogout = async () => {
    await logout();
    router.replace('/(auth)/login');
  };

  const currentCountry = COUNTRIES.find(c => c.code === selectedCountry);

  return (
    <SafeAreaView style={styles.container} edges={['top']}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity style={styles.backButton} onPress={() => router.back()}>
          <Ionicons name="arrow-back" size={24} color={colors.white} />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Settings</Text>
        <TouchableOpacity 
          style={styles.saveButton}
          onPress={handleSave}
          disabled={isSaving}
        >
          {isSaving ? (
            <ActivityIndicator size="small" color={colors.primary[400]} />
          ) : (
            <Text style={styles.saveButtonText}>Save</Text>
          )}
        </TouchableOpacity>
      </View>

      <ScrollView contentContainerStyle={styles.content}>
        {/* Profile section */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Profile</Text>
          <View style={styles.profileCard}>
            <View style={styles.profileAvatar}>
              <Text style={styles.profileAvatarText}>
                {displayName?.charAt(0).toUpperCase() || '?'}
              </Text>
            </View>
            <View style={styles.profileInfo}>
              <TextInput
                style={styles.displayNameInput}
                value={displayName}
                onChangeText={setDisplayName}
                placeholder="Display name"
                placeholderTextColor={colors.surface[500]}
                maxLength={30}
              />
              <Text style={styles.profileEmail}>{user?.email}</Text>
            </View>
          </View>
          {displayName.trim().length < 3 && displayName !== user?.username && (
            <Text style={styles.errorHint}>Display name must be at least 3 characters</Text>
          )}
        </View>

        {/* Language section */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Preferred Language</Text>
          <Text style={styles.sectionHint}>
            All incoming messages will be automatically translated to this language.
          </Text>
          <View style={styles.pickerCard}>
            <Ionicons name="language-outline" size={20} color={colors.surface[400]} style={styles.pickerIcon} />
            <Picker
              selectedValue={selectedLanguage}
              onValueChange={(value) => setSelectedLanguage(value)}
              style={styles.picker}
              dropdownIconColor={colors.surface[400]}
            >
              {LANGUAGES.map((lang) => (
                <Picker.Item 
                  key={lang.code} 
                  label={`${lang.native} (${lang.name})`} 
                  value={lang.code}
                  color="#000000"
                />
              ))}
            </Picker>
          </View>
        </View>

        {/* Country section */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Country / Region</Text>
          <Text style={styles.sectionHint}>
            Translations will use vocabulary and expressions from this country.
          </Text>
          <View style={styles.pickerCard}>
            <Text style={styles.flagText}>{currentCountry?.flag || '🌍'}</Text>
            <Picker
              selectedValue={selectedCountry}
              onValueChange={(value) => setSelectedCountry(value)}
              style={styles.picker}
              dropdownIconColor={colors.surface[400]}
            >
              {COUNTRIES.map((country) => (
                <Picker.Item 
                  key={country.code} 
                  label={`${country.flag} ${country.name}`} 
                  value={country.code}
                  color="#000000"
                />
              ))}
            </Picker>
          </View>
        </View>

        {/* About section */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>About</Text>
          <View style={styles.aboutCard}>
            <View style={styles.aboutItem}>
              <Ionicons name="information-circle-outline" size={20} color={colors.primary[400]} />
              <Text style={styles.aboutText}>Intok v{CURRENT_VERSION}</Text>
            </View>
            <TouchableOpacity 
              style={styles.aboutItemButton}
              onPress={() => setShowChangelog(true)}
            >
              <Ionicons name="sparkles" size={20} color={colors.primary[400]} />
              <Text style={styles.aboutText}>What's New</Text>
              <Ionicons name="chevron-forward" size={16} color={colors.surface[500]} style={styles.chevron} />
            </TouchableOpacity>
            <View style={styles.aboutItem}>
              <Ionicons name="language-outline" size={20} color={colors.primary[400]} />
              <Text style={styles.aboutText}>Powered by OpenAI Translation</Text>
            </View>
          </View>
        </View>

        {/* Logout */}
        <TouchableOpacity style={styles.logoutButton} onPress={handleLogout}>
          <Ionicons name="log-out-outline" size={20} color={colors.error} />
          <Text style={styles.logoutText}>Sign out</Text>
        </TouchableOpacity>
      </ScrollView>

      {/* Changelog Modal */}
      <Modal
        visible={showChangelog}
        transparent
        animationType="fade"
        onRequestClose={() => setShowChangelog(false)}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalContainer}>
            <View style={styles.modalHeader}>
              <View style={styles.modalHeaderIcon}>
                <Ionicons name="sparkles" size={28} color={colors.primary[400]} />
              </View>
              <Text style={styles.modalTitle}>What's New</Text>
            </View>

            <ScrollView 
              style={styles.modalScrollView}
              contentContainerStyle={styles.modalScrollContent}
              showsVerticalScrollIndicator={false}
            >
              {CHANGELOG.map((entry) => (
                <View key={entry.version} style={styles.versionSection}>
                  <View style={styles.versionHeaderRow}>
                    <Text style={styles.versionHeader}>v{entry.version}</Text>
                    <Text style={styles.versionDate}>{entry.date}</Text>
                  </View>
                  {entry.changes.map((change, index) => (
                    <Text key={index} style={styles.changeText}>{change}</Text>
                  ))}
                </View>
              ))}
            </ScrollView>

            <TouchableOpacity 
              style={styles.modalButton} 
              onPress={() => setShowChangelog(false)}
            >
              <Text style={styles.modalButtonText}>Close</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.surface[950],
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: spacing.md,
    borderBottomWidth: 1,
    borderBottomColor: colors.surface[800],
  },
  backButton: {
    padding: spacing.sm,
  },
  headerTitle: {
    color: colors.white,
    fontSize: fontSize.xl,
    fontWeight: '600',
  },
  saveButton: {
    padding: spacing.sm,
  },
  saveButtonText: {
    color: colors.primary[400],
    fontSize: fontSize.md,
    fontWeight: '500',
  },
  content: {
    padding: spacing.md,
  },
  section: {
    marginBottom: spacing.xl,
  },
  sectionTitle: {
    color: colors.surface[400],
    fontSize: fontSize.sm,
    fontWeight: '500',
    marginBottom: spacing.sm,
    textTransform: 'uppercase',
    letterSpacing: 1,
  },
  sectionHint: {
    color: colors.surface[500],
    fontSize: fontSize.sm,
    marginBottom: spacing.md,
  },
  profileCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surface[900],
    borderRadius: borderRadius.xl,
    padding: spacing.md,
    borderWidth: 1,
    borderColor: colors.surface[800],
  },
  profileAvatar: {
    width: 64,
    height: 64,
    borderRadius: borderRadius.full,
    backgroundColor: colors.primary[600],
    justifyContent: 'center',
    alignItems: 'center',
  },
  profileAvatarText: {
    color: colors.white,
    fontSize: fontSize.xxl,
    fontWeight: '700',
  },
  profileInfo: {
    marginLeft: spacing.md,
  },
  profileName: {
    color: colors.white,
    fontSize: fontSize.lg,
    fontWeight: '600',
  },
  displayNameInput: {
    color: colors.white,
    fontSize: fontSize.lg,
    fontWeight: '600',
    padding: 0,
    margin: 0,
    borderBottomWidth: 1,
    borderBottomColor: colors.surface[600],
    paddingBottom: spacing.xs,
  },
  profileEmail: {
    color: colors.surface[400],
    fontSize: fontSize.sm,
    marginTop: spacing.sm,
  },
  errorHint: {
    color: colors.error,
    fontSize: fontSize.xs,
    marginTop: spacing.sm,
    marginLeft: spacing.md,
  },
  pickerCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surface[900],
    borderRadius: borderRadius.xl,
    paddingLeft: spacing.md,
    borderWidth: 1,
    borderColor: colors.surface[800],
  },
  pickerIcon: {
    marginRight: spacing.xs,
  },
  flagText: {
    fontSize: fontSize.xxl,
  },
  picker: {
    flex: 1,
    color: colors.white,
    height: 56,
  },
  aboutCard: {
    backgroundColor: colors.surface[900],
    borderRadius: borderRadius.xl,
    padding: spacing.md,
    borderWidth: 1,
    borderColor: colors.surface[800],
  },
  aboutItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: spacing.sm,
    gap: spacing.sm,
  },
  aboutItemButton: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: spacing.sm,
    gap: spacing.sm,
  },
  aboutText: {
    color: colors.surface[400],
    fontSize: fontSize.sm,
    flex: 1,
  },
  chevron: {
    marginLeft: 'auto',
  },
  logoutButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: `${colors.error}15`,
    borderRadius: borderRadius.xl,
    padding: spacing.md,
    gap: spacing.sm,
    borderWidth: 1,
    borderColor: `${colors.error}30`,
  },
  logoutText: {
    color: colors.error,
    fontSize: fontSize.md,
    fontWeight: '500',
  },
  // Modal styles
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.7)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: spacing.lg,
  },
  modalContainer: {
    backgroundColor: colors.surface[900],
    borderRadius: borderRadius.xl,
    width: '100%',
    maxWidth: 400,
    maxHeight: '80%',
    borderWidth: 1,
    borderColor: colors.surface[700],
    overflow: 'hidden',
  },
  modalHeader: {
    alignItems: 'center',
    paddingTop: spacing.xl,
    paddingBottom: spacing.md,
    paddingHorizontal: spacing.lg,
    borderBottomWidth: 1,
    borderBottomColor: colors.surface[800],
  },
  modalHeaderIcon: {
    width: 56,
    height: 56,
    borderRadius: borderRadius.full,
    backgroundColor: colors.primary[900],
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: spacing.md,
  },
  modalTitle: {
    fontSize: fontSize.xxl,
    fontWeight: '700',
    color: colors.white,
  },
  modalScrollView: {
    maxHeight: 350,
  },
  modalScrollContent: {
    padding: spacing.lg,
  },
  versionSection: {
    marginBottom: spacing.lg,
    paddingBottom: spacing.lg,
    borderBottomWidth: 1,
    borderBottomColor: colors.surface[800],
  },
  versionHeaderRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: spacing.sm,
  },
  versionHeader: {
    fontSize: fontSize.lg,
    fontWeight: '600',
    color: colors.primary[400],
  },
  versionDate: {
    fontSize: fontSize.sm,
    color: colors.surface[500],
  },
  changeText: {
    fontSize: fontSize.md,
    color: colors.surface[200],
    lineHeight: 24,
    paddingVertical: spacing.xs,
  },
  modalButton: {
    backgroundColor: colors.primary[600],
    marginHorizontal: spacing.lg,
    marginBottom: spacing.lg,
    marginTop: spacing.sm,
    paddingVertical: spacing.md,
    borderRadius: borderRadius.lg,
    alignItems: 'center',
  },
  modalButtonText: {
    color: colors.white,
    fontSize: fontSize.lg,
    fontWeight: '600',
  },
});
