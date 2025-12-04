import { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';
import { router } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { Picker } from '@react-native-picker/picker';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useAuthStore } from '../../src/store/auth';
import { colors, spacing, borderRadius, fontSize } from '../../src/constants/theme';
import { LANGUAGES, COUNTRIES, type LanguageCode, type CountryCode } from '../../src/constants/languages';

export default function SetupScreen() {
  const { user, updateUsername, updateLanguage, updateCountry } = useAuthStore();
  const [displayName, setDisplayName] = useState(user?.username || '');
  const [selectedLanguage, setSelectedLanguage] = useState<LanguageCode>(
    (user?.preferredLanguage as LanguageCode) || 'en'
  );
  const [selectedCountry, setSelectedCountry] = useState<CountryCode>(
    (user?.preferredCountry as CountryCode) || 'US'
  );
  const [isSaving, setIsSaving] = useState(false);
  const [step, setStep] = useState(1);

  const selectedCountryData = COUNTRIES.find(c => c.code === selectedCountry);

  const handleNext = () => {
    if (step < 3) {
      setStep(step + 1);
    }
  };

  const handleBack = () => {
    if (step > 1) {
      setStep(step - 1);
    }
  };

  const handleFinish = async () => {
    if (displayName.trim().length < 3) return;

    setIsSaving(true);
    try {
      // Update all profile settings
      if (displayName.trim() !== user?.username) {
        await updateUsername(displayName.trim());
      }
      if (selectedLanguage !== user?.preferredLanguage) {
        await updateLanguage(selectedLanguage);
      }
      if (selectedCountry !== user?.preferredCountry) {
        await updateCountry(selectedCountry);
      }
      router.replace('/(app)/conversations');
    } catch (error) {
      console.error('Failed to save setup:', error);
    } finally {
      setIsSaving(false);
    }
  };

  const canProceed = step === 1 ? displayName.trim().length >= 3 : true;

  return (
    <SafeAreaView style={styles.container}>
      <KeyboardAvoidingView 
        style={styles.keyboardView}
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
      >
        <ScrollView 
          contentContainerStyle={styles.scrollContent}
          keyboardShouldPersistTaps="handled"
        >
          {/* Header */}
          <View style={styles.header}>
            <View style={styles.logoIcon}>
              <Ionicons name="chatbubbles" size={40} color={colors.white} />
            </View>
            <Text style={styles.title}>Welcome to Intok!</Text>
            <Text style={styles.subtitle}>Let's set up your profile</Text>
          </View>

          {/* Progress indicator */}
          <View style={styles.progressContainer}>
            {[1, 2, 3].map((s) => (
              <View
                key={s}
                style={[
                  styles.progressDot,
                  s === step && styles.progressDotActive,
                  s < step && styles.progressDotCompleted,
                ]}
              />
            ))}
          </View>

          {/* Step 1: Display Name */}
          {step === 1 && (
            <View style={styles.stepContainer}>
              <Text style={styles.stepTitle}>What should we call you?</Text>
              <Text style={styles.stepHint}>
                This is how other users will see you
              </Text>
              <View style={styles.inputContainer}>
                <Ionicons name="person-outline" size={20} color={colors.surface[400]} />
                <TextInput
                  style={styles.input}
                  value={displayName}
                  onChangeText={setDisplayName}
                  placeholder="Enter your display name"
                  placeholderTextColor={colors.surface[500]}
                  maxLength={30}
                  autoFocus
                />
              </View>
              {displayName.length > 0 && displayName.trim().length < 3 && (
                <Text style={styles.errorText}>Name must be at least 3 characters</Text>
              )}
            </View>
          )}

          {/* Step 2: Language */}
          {step === 2 && (
            <View style={styles.stepContainer}>
              <Text style={styles.stepTitle}>What's your preferred language?</Text>
              <Text style={styles.stepHint}>
                Messages will be automatically translated to this language
              </Text>
              <View style={styles.pickerContainer}>
                <Ionicons name="language-outline" size={20} color={colors.surface[400]} />
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
          )}

          {/* Step 3: Country */}
          {step === 3 && (
            <View style={styles.stepContainer}>
              <Text style={styles.stepTitle}>Where are you from?</Text>
              <Text style={styles.stepHint}>
                Translations will use vocabulary from your region
              </Text>
              <View style={styles.pickerContainer}>
                <Text style={styles.flagText}>{selectedCountryData?.flag || '🌍'}</Text>
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
          )}

          {/* Navigation buttons */}
          <View style={styles.buttonContainer}>
            {step > 1 && (
              <TouchableOpacity style={styles.backButton} onPress={handleBack}>
                <Ionicons name="arrow-back" size={20} color={colors.surface[300]} />
                <Text style={styles.backButtonText}>Back</Text>
              </TouchableOpacity>
            )}

            {step < 3 ? (
              <TouchableOpacity
                style={[styles.nextButton, !canProceed && styles.buttonDisabled]}
                onPress={handleNext}
                disabled={!canProceed}
              >
                <Text style={styles.nextButtonText}>Next</Text>
                <Ionicons name="arrow-forward" size={20} color={colors.white} />
              </TouchableOpacity>
            ) : (
              <TouchableOpacity
                style={[styles.finishButton, isSaving && styles.buttonDisabled]}
                onPress={handleFinish}
                disabled={isSaving}
              >
                {isSaving ? (
                  <ActivityIndicator color={colors.white} />
                ) : (
                  <>
                    <Text style={styles.finishButtonText}>Get Started</Text>
                    <Ionicons name="checkmark" size={20} color={colors.white} />
                  </>
                )}
              </TouchableOpacity>
            )}
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.surface[950],
  },
  keyboardView: {
    flex: 1,
  },
  scrollContent: {
    flexGrow: 1,
    padding: spacing.lg,
    justifyContent: 'center',
  },
  header: {
    alignItems: 'center',
    marginBottom: spacing.xl,
  },
  logoIcon: {
    width: 80,
    height: 80,
    borderRadius: borderRadius.xl,
    backgroundColor: colors.primary[500],
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: spacing.md,
  },
  title: {
    fontSize: fontSize.xxl,
    fontWeight: '700',
    color: colors.white,
    marginBottom: spacing.xs,
  },
  subtitle: {
    fontSize: fontSize.md,
    color: colors.surface[400],
  },
  progressContainer: {
    flexDirection: 'row',
    justifyContent: 'center',
    gap: spacing.sm,
    marginBottom: spacing.xl,
  },
  progressDot: {
    width: 10,
    height: 10,
    borderRadius: 5,
    backgroundColor: colors.surface[700],
  },
  progressDotActive: {
    backgroundColor: colors.primary[500],
    width: 24,
  },
  progressDotCompleted: {
    backgroundColor: colors.primary[400],
  },
  stepContainer: {
    backgroundColor: colors.surface[900],
    borderRadius: borderRadius.xl,
    padding: spacing.lg,
    borderWidth: 1,
    borderColor: colors.surface[800],
    marginBottom: spacing.lg,
  },
  stepTitle: {
    fontSize: fontSize.xl,
    fontWeight: '600',
    color: colors.white,
    marginBottom: spacing.sm,
  },
  stepHint: {
    fontSize: fontSize.sm,
    color: colors.surface[400],
    marginBottom: spacing.lg,
  },
  inputContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surface[800],
    borderRadius: borderRadius.lg,
    paddingHorizontal: spacing.md,
    borderWidth: 1,
    borderColor: colors.surface[700],
    gap: spacing.sm,
  },
  input: {
    flex: 1,
    height: 52,
    color: colors.white,
    fontSize: fontSize.md,
  },
  errorText: {
    color: colors.error,
    fontSize: fontSize.sm,
    marginTop: spacing.sm,
  },
  pickerContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surface[800],
    borderRadius: borderRadius.lg,
    paddingLeft: spacing.md,
    borderWidth: 1,
    borderColor: colors.surface[700],
  },
  picker: {
    flex: 1,
    color: colors.white,
    height: 52,
  },
  flagText: {
    fontSize: fontSize.xl,
  },
  buttonContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    gap: spacing.md,
  },
  backButton: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: spacing.md,
    gap: spacing.xs,
  },
  backButtonText: {
    color: colors.surface[300],
    fontSize: fontSize.md,
  },
  nextButton: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.primary[500],
    borderRadius: borderRadius.lg,
    padding: spacing.md,
    gap: spacing.sm,
  },
  nextButtonText: {
    color: colors.white,
    fontSize: fontSize.lg,
    fontWeight: '600',
  },
  finishButton: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.success,
    borderRadius: borderRadius.lg,
    padding: spacing.md,
    gap: spacing.sm,
  },
  finishButtonText: {
    color: colors.white,
    fontSize: fontSize.lg,
    fontWeight: '600',
  },
  buttonDisabled: {
    opacity: 0.6,
  },
});

