import { useState } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
  ActivityIndicator,
  TextInput,
} from 'react-native';
import { router } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { Picker } from '@react-native-picker/picker';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useAuthStore } from '../../src/store/auth';
import { colors, spacing, borderRadius, fontSize } from '../../src/constants/theme';
import { LANGUAGES, COUNTRIES, type LanguageCode, type CountryCode } from '../../src/constants/languages';

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
              <Text style={styles.aboutText}>Intok v1.0.0</Text>
            </View>
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
  aboutText: {
    color: colors.surface[400],
    fontSize: fontSize.sm,
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
});
