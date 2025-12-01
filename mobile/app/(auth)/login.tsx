import { useState, useEffect } from 'react';
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
import { useAuthStore } from '../../src/store/auth';
import { colors, spacing, borderRadius, fontSize } from '../../src/constants/theme';
import { 
  useGoogleAuth, 
  getGoogleUserInfo, 
  isAppleSignInAvailable, 
  signInWithApple 
} from '../../src/services/oauth';

export default function LoginScreen() {
  const { login, oauthLogin, isLoading, error, clearError } = useAuthStore();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [appleAvailable, setAppleAvailable] = useState(false);
  const [oauthLoading, setOauthLoading] = useState<'google' | 'apple' | null>(null);

  // Google Auth Hook - may be null if not configured
  const googleAuth = useGoogleAuth();
  const googleRequest = googleAuth?.request;
  const googleResponse = googleAuth?.response;
  const googlePromptAsync = googleAuth?.promptAsync;

  // Check Apple Sign-In availability
  useEffect(() => {
    isAppleSignInAvailable().then(setAppleAvailable);
  }, []);

  // Check if OAuth is configured
  const isGoogleConfigured = !!googleRequest;

  // Handle Google Sign-In response
  useEffect(() => {
    if (googleResponse?.type === 'success') {
      const { authentication } = googleResponse;
      if (authentication?.accessToken) {
        handleGoogleSuccess(authentication.accessToken);
      }
    } else if (googleResponse?.type === 'error') {
      setOauthLoading(null);
    }
  }, [googleResponse]);

  const handleGoogleSuccess = async (accessToken: string) => {
    try {
      const userInfo = await getGoogleUserInfo(accessToken);
      await oauthLogin(userInfo);
      router.replace('/(app)/conversations');
    } catch {
      setOauthLoading(null);
    }
  };

  const handleGoogleSignIn = async () => {
    if (!googlePromptAsync) {
      console.log('Google Sign-In not configured');
      return;
    }
    clearError();
    setOauthLoading('google');
    try {
      await googlePromptAsync();
    } catch {
      setOauthLoading(null);
    }
  };

  const handleAppleSignIn = async () => {
    clearError();
    setOauthLoading('apple');
    try {
      const userInfo = await signInWithApple();
      await oauthLogin(userInfo);
      router.replace('/(app)/conversations');
    } catch {
      setOauthLoading(null);
    }
  };

  const handleLogin = async () => {
    clearError();
    try {
      await login(email, password);
      router.replace('/(app)/conversations');
    } catch {
      // Error handled by store
    }
  };

  const isAnyLoading = isLoading || oauthLoading !== null;

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
          <Text style={styles.tagline}>Seamless multilingual messaging</Text>
        </View>

        {/* Form */}
        <View style={styles.form}>
          <Text style={styles.title}>Welcome back</Text>

          {error && (
            <View style={styles.errorContainer}>
              <Ionicons name="alert-circle" size={20} color={colors.error} />
              <Text style={styles.errorText}>{error}</Text>
            </View>
          )}

          {/* Social Login Buttons */}
          <View style={styles.socialButtons}>
            {/* Google Sign-In - Always visible */}
            <TouchableOpacity 
              style={[
                styles.socialButton, 
                styles.googleButton,
                !isGoogleConfigured && styles.socialButtonDisabled
              ]}
              onPress={handleGoogleSignIn}
              disabled={!isGoogleConfigured || isAnyLoading}
            >
              {oauthLoading === 'google' ? (
                <ActivityIndicator color={colors.surface[900]} size="small" />
              ) : (
                <>
                  <Ionicons name="logo-google" size={20} color={colors.surface[900]} />
                  <Text style={styles.socialButtonText}>Continue with Google</Text>
                </>
              )}
            </TouchableOpacity>

            {/* Apple Sign-In - Only on iOS */}
            {Platform.OS === 'ios' && (
              <TouchableOpacity 
                style={[
                  styles.socialButton, 
                  styles.appleButton,
                  !appleAvailable && styles.socialButtonDisabled
                ]}
                onPress={handleAppleSignIn}
                disabled={!appleAvailable || isAnyLoading}
              >
                {oauthLoading === 'apple' ? (
                  <ActivityIndicator color={colors.white} size="small" />
                ) : (
                  <>
                    <Ionicons name="logo-apple" size={20} color={colors.white} />
                    <Text style={[styles.socialButtonText, styles.appleButtonText]}>
                      Continue with Apple
                    </Text>
                  </>
                )}
              </TouchableOpacity>
            )}

            {/* Note when OAuth not configured */}
            {!isGoogleConfigured && (
              <Text style={styles.oauthNote}>
                ⚙️ OAuth not configured yet
              </Text>
            )}
          </View>

          {/* Divider */}
          <View style={styles.divider}>
            <View style={styles.dividerLine} />
            <Text style={styles.dividerText}>or</Text>
            <View style={styles.dividerLine} />
          </View>

          {/* Email/Password Form */}
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
            <Ionicons name="lock-closed-outline" size={20} color={colors.surface[400]} style={styles.inputIcon} />
            <TextInput
              style={styles.input}
              placeholder="Password"
              placeholderTextColor={colors.surface[500]}
              value={password}
              onChangeText={setPassword}
              secureTextEntry={!showPassword}
              autoComplete="password"
            />
            <TouchableOpacity onPress={() => setShowPassword(!showPassword)}>
              <Ionicons 
                name={showPassword ? 'eye-off-outline' : 'eye-outline'} 
                size={20} 
                color={colors.surface[400]} 
              />
            </TouchableOpacity>
          </View>

          <TouchableOpacity 
            style={[styles.button, isAnyLoading && styles.buttonDisabled]} 
            onPress={handleLogin}
            disabled={isAnyLoading || !email || !password}
          >
            {isLoading ? (
              <ActivityIndicator color={colors.white} />
            ) : (
              <Text style={styles.buttonText}>Sign in</Text>
            )}
          </TouchableOpacity>

          <View style={styles.footer}>
            <Text style={styles.footerText}>Don't have an account? </Text>
            <Link href="/(auth)/register" asChild>
              <TouchableOpacity>
                <Text style={styles.linkText}>Sign up</Text>
              </TouchableOpacity>
            </Link>
          </View>
        </View>

        {/* Demo hint */}
        <View style={styles.demoHint}>
          <Text style={styles.demoText}>Demo: alice@example.com / password123</Text>
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
    marginBottom: spacing.xxl,
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
  socialButtons: {
    gap: spacing.sm,
    marginBottom: spacing.md,
  },
  socialButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    height: 52,
    borderRadius: borderRadius.lg,
    gap: spacing.sm,
  },
  googleButton: {
    backgroundColor: colors.white,
  },
  appleButton: {
    backgroundColor: colors.surface[800],
    borderWidth: 1,
    borderColor: colors.surface[700],
  },
  socialButtonText: {
    fontSize: fontSize.md,
    fontFamily: 'outfit-medium',
    color: colors.surface[900],
  },
  appleButtonText: {
    color: colors.white,
  },
  socialButtonDisabled: {
    opacity: 0.5,
  },
  oauthNote: {
    textAlign: 'center',
    color: colors.surface[500],
    fontSize: fontSize.xs,
    marginTop: spacing.xs,
  },
  divider: {
    flexDirection: 'row',
    alignItems: 'center',
    marginVertical: spacing.md,
  },
  dividerLine: {
    flex: 1,
    height: 1,
    backgroundColor: colors.surface[700],
  },
  dividerText: {
    color: colors.surface[500],
    paddingHorizontal: spacing.md,
    fontSize: fontSize.sm,
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
  demoHint: {
    marginTop: spacing.lg,
    alignItems: 'center',
  },
  demoText: {
    color: colors.surface[500],
    fontSize: fontSize.sm,
  },
});
