import { useState } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
  ActivityIndicator,
} from 'react-native';
import { router } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { Picker } from '@react-native-picker/picker';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useAuthStore } from '../../src/store/auth';
import { colors, spacing, borderRadius, fontSize } from '../../src/constants/theme';
import { SUPPORTED_LANGUAGES, LANGUAGE_FLAGS, type LanguageCode } from '../../src/constants/languages';

export default function SettingsScreen() {
  const { user, updateLanguage, logout } = useAuthStore();
  const [selectedLanguage, setSelectedLanguage] = useState<LanguageCode>(
    (user?.preferredLanguage as LanguageCode) || 'en'
  );
  const [isSaving, setIsSaving] = useState(false);

  const handleSave = async () => {
    if (selectedLanguage === user?.preferredLanguage) {
      router.back();
      return;
    }

    setIsSaving(true);
    try {
      await updateLanguage(selectedLanguage);
      router.back();
    } catch (error) {
      console.error('Failed to update language:', error);
    } finally {
      setIsSaving(false);
    }
  };

  const handleLogout = async () => {
    await logout();
    router.replace('/(auth)/login');
  };

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
                {user?.username?.charAt(0).toUpperCase()}
              </Text>
            </View>
            <View style={styles.profileInfo}>
              <Text style={styles.profileName}>{user?.username}</Text>
              <Text style={styles.profileEmail}>{user?.email}</Text>
            </View>
          </View>
        </View>

        {/* Language section */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Preferred Language</Text>
          <Text style={styles.sectionHint}>
            All incoming messages will be automatically translated to this language.
          </Text>
          <View style={styles.pickerCard}>
            <Text style={styles.flagText}>{LANGUAGE_FLAGS[selectedLanguage]}</Text>
            <Picker
              selectedValue={selectedLanguage}
              onValueChange={(value) => setSelectedLanguage(value)}
              style={styles.picker}
              dropdownIconColor={colors.surface[400]}
            >
              {Object.entries(SUPPORTED_LANGUAGES).map(([code, name]) => (
                <Picker.Item 
                  key={code} 
                  label={`${LANGUAGE_FLAGS[code as LanguageCode]} ${name}`} 
                  value={code}
                  color={colors.white}
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
              <Text style={styles.aboutText}>LinguaLink v1.0.0</Text>
            </View>
            <View style={styles.aboutItem}>
              <Ionicons name="language-outline" size={20} color={colors.primary[400]} />
              <Text style={styles.aboutText}>Powered by DeepSeek AI Translation</Text>
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
    fontFamily: 'outfit-semibold',
  },
  saveButton: {
    padding: spacing.sm,
  },
  saveButtonText: {
    color: colors.primary[400],
    fontSize: fontSize.md,
    fontFamily: 'outfit-medium',
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
    fontFamily: 'outfit-medium',
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
    fontFamily: 'outfit-bold',
  },
  profileInfo: {
    marginLeft: spacing.md,
  },
  profileName: {
    color: colors.white,
    fontSize: fontSize.lg,
    fontFamily: 'outfit-semibold',
  },
  profileEmail: {
    color: colors.surface[400],
    fontSize: fontSize.sm,
    marginTop: spacing.xs,
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
    fontFamily: 'outfit-medium',
  },
});

