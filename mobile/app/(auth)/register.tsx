import { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  ActivityIndicator,
} from 'react-native';
import { Link, router } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { Picker } from '@react-native-picker/picker';
import { useAuthStore } from '../../src/store/auth';
import { colors, spacing, borderRadius, fontSize } from '../../src/constants/theme';
import { SUPPORTED_LANGUAGES, LANGUAGE_FLAGS, type LanguageCode } from '../../src/constants/languages';

export default function RegisterScreen() {
  const { register, isLoading, error, clearError } = useAuthStore();
  const [email, setEmail] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [preferredLanguage, setPreferredLanguage] = useState<LanguageCode>('en');
  const [showPassword, setShowPassword] = useState(false);

  const handleRegister = async () => {
    clearError();
    try {
      await register({ email, username, password, preferredLanguage });
      router.replace('/(app)/conversations');
    } catch {
      // Error handled by store
    }
  };

  return (
    <KeyboardAvoidingView 
      style={styles.container} 
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
    >
      <ScrollView 
        contentContainerStyle={styles.scrollContent}
        keyboardShouldPersistTaps="handled"
      >
        {/* Logo */}
        <View style={styles.logoContainer}>
          <View style={styles.logoIcon}>
            <Ionicons name="chatbubbles" size={40} color={colors.white} />
          </View>
          <Text style={styles.logoText}>LinguaLink</Text>
          <Text style={styles.tagline}>Break language barriers</Text>
        </View>

        {/* Form */}
        <View style={styles.form}>
          <Text style={styles.title}>Create account</Text>

          {error && (
            <View style={styles.errorContainer}>
              <Ionicons name="alert-circle" size={20} color={colors.error} />
              <Text style={styles.errorText}>{error}</Text>
            </View>
          )}

          <View style={styles.inputContainer}>
            <Ionicons name="mail-outline" size={20} color={colors.surface[400]} style={styles.inputIcon} />
            <TextInput
              style={styles.input}
              placeholder="Email"
              placeholderTextColor={colors.surface[500]}
              value={email}
              onChangeText={setEmail}
              autoCapitalize="none"
              keyboardType="email-address"
              autoComplete="email"
            />
          </View>

          <View style={styles.inputContainer}>
            <Ionicons name="person-outline" size={20} color={colors.surface[400]} style={styles.inputIcon} />
            <TextInput
              style={styles.input}
              placeholder="Username"
              placeholderTextColor={colors.surface[500]}
              value={username}
              onChangeText={setUsername}
              autoCapitalize="none"
              autoComplete="username"
            />
          </View>

          <View style={styles.inputContainer}>
            <Ionicons name="lock-closed-outline" size={20} color={colors.surface[400]} style={styles.inputIcon} />
            <TextInput
              style={styles.input}
              placeholder="Password"
              placeholderTextColor={colors.surface[500]}
              value={password}
              onChangeText={setPassword}
              secureTextEntry={!showPassword}
              autoComplete="new-password"
            />
            <TouchableOpacity onPress={() => setShowPassword(!showPassword)}>
              <Ionicons 
                name={showPassword ? 'eye-off-outline' : 'eye-outline'} 
                size={20} 
                color={colors.surface[400]} 
              />
            </TouchableOpacity>
          </View>

          <View style={styles.languageSection}>
            <Text style={styles.languageLabel}>Preferred Language</Text>
            <Text style={styles.languageHint}>Messages will be translated to this language</Text>
            <View style={styles.pickerContainer}>
              <Text style={styles.flagText}>{LANGUAGE_FLAGS[preferredLanguage]}</Text>
              <Picker
                selectedValue={preferredLanguage}
                onValueChange={(value) => setPreferredLanguage(value)}
                style={styles.picker}
                dropdownIconColor={colors.surface[400]}
              >
                {Object.entries(SUPPORTED_LANGUAGES).map(([code, name]) => (
                  <Picker.Item 
                    key={code} 
                    label={`${LANGUAGE_FLAGS[code as LanguageCode]} ${name}`} 
                    value={code}
                    color={Platform.OS === 'web' ? '#000000' : colors.white}
                  />
                ))}
              </Picker>
            </View>
          </View>

          <TouchableOpacity 
            style={[styles.button, isLoading && styles.buttonDisabled]} 
            onPress={handleRegister}
            disabled={isLoading || !email || !username || !password}
          >
            {isLoading ? (
              <ActivityIndicator color={colors.white} />
            ) : (
              <Text style={styles.buttonText}>Create account</Text>
            )}
          </TouchableOpacity>

          <View style={styles.footer}>
            <Text style={styles.footerText}>Already have an account? </Text>
            <Link href="/(auth)/login" asChild>
              <TouchableOpacity>
                <Text style={styles.linkText}>Sign in</Text>
              </TouchableOpacity>
            </Link>
          </View>
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.surface[950],
  },
  scrollContent: {
    flexGrow: 1,
    justifyContent: 'center',
    padding: spacing.lg,
  },
  logoContainer: {
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
  logoText: {
    fontSize: fontSize.xxxl,
    fontFamily: 'outfit-bold',
    color: colors.white,
  },
  tagline: {
    fontSize: fontSize.md,
    color: colors.surface[400],
    marginTop: spacing.xs,
  },
  form: {
    backgroundColor: colors.surface[900],
    borderRadius: borderRadius.xl,
    padding: spacing.lg,
    borderWidth: 1,
    borderColor: colors.surface[800],
  },
  title: {
    fontSize: fontSize.xxl,
    fontFamily: 'outfit-semibold',
    color: colors.white,
    marginBottom: spacing.lg,
  },
  errorContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: `${colors.error}15`,
    padding: spacing.md,
    borderRadius: borderRadius.md,
    marginBottom: spacing.md,
    borderWidth: 1,
    borderColor: `${colors.error}30`,
  },
  errorText: {
    color: colors.error,
    marginLeft: spacing.sm,
    fontSize: fontSize.sm,
    flex: 1,
  },
  inputContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surface[800],
    borderRadius: borderRadius.lg,
    paddingHorizontal: spacing.md,
    marginBottom: spacing.md,
    borderWidth: 1,
    borderColor: colors.surface[700],
  },
  inputIcon: {
    marginRight: spacing.sm,
  },
  input: {
    flex: 1,
    height: 52,
    color: colors.white,
    fontSize: fontSize.md,
  },
  languageSection: {
    marginBottom: spacing.md,
  },
  languageLabel: {
    fontSize: fontSize.sm,
    fontFamily: 'outfit-medium',
    color: colors.surface[300],
    marginBottom: spacing.xs,
  },
  languageHint: {
    fontSize: fontSize.xs,
    color: colors.surface[500],
    marginBottom: spacing.sm,
  },
  pickerContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surface[800],
    borderRadius: borderRadius.lg,
    borderWidth: 1,
    borderColor: colors.surface[700],
    paddingLeft: spacing.md,
  },
  flagText: {
    fontSize: fontSize.xl,
  },
  picker: {
    flex: 1,
    color: colors.white,
    height: 52,
  },
  button: {
    backgroundColor: colors.primary[500],
    borderRadius: borderRadius.lg,
    height: 52,
    justifyContent: 'center',
    alignItems: 'center',
    marginTop: spacing.sm,
  },
  buttonDisabled: {
    opacity: 0.6,
  },
  buttonText: {
    color: colors.white,
    fontSize: fontSize.lg,
    fontFamily: 'outfit-semibold',
  },
  footer: {
    flexDirection: 'row',
    justifyContent: 'center',
    marginTop: spacing.lg,
  },
  footerText: {
    color: colors.surface[400],
    fontSize: fontSize.md,
  },
  linkText: {
    color: colors.primary[400],
    fontSize: fontSize.md,
    fontFamily: 'outfit-medium',
  },
});

