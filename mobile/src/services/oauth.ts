import * as AppleAuthentication from 'expo-apple-authentication';
import Constants from 'expo-constants';
import { Platform } from 'react-native';
import {
  GoogleSignin,
  statusCodes,
} from '@react-native-google-signin/google-signin';

// Get config from app.json extra
const config = Constants.expoConfig?.extra || {};

// Google OAuth Client IDs
const GOOGLE_WEB_CLIENT_ID = config.googleClientIdWeb || '';

// Configure Google Sign-In once when the module loads
GoogleSignin.configure({
  webClientId: GOOGLE_WEB_CLIENT_ID, // Required for getting idToken
  offlineAccess: true,
  scopes: ['profile', 'email'],
});

console.log('🔑 Google Sign-In configured with Web Client ID');

export interface OAuthUser {
  provider: 'google' | 'apple';
  providerId: string;
  email: string;
  name: string | null;
  avatarUrl: string | null;
}

/**
 * Google Sign-In using native SDK
 */
export async function signInWithGoogle(): Promise<OAuthUser> {
  try {
    // Check if Google Play Services are available
    await GoogleSignin.hasPlayServices({ showPlayServicesUpdateDialog: true });
    
    // Perform sign in
    const response = await GoogleSignin.signIn();
    
    if (response.type === 'cancelled') {
      throw new Error('Google Sign-In was cancelled');
    }
    
    if (response.type !== 'success' || !response.data.user) {
      throw new Error('Google Sign-In failed');
    }

    const { user } = response.data;
    
    return {
      provider: 'google',
      providerId: user.id,
      email: user.email,
      name: user.name || null,
      avatarUrl: user.photo || null,
    };
  } catch (error: any) {
    if (error.code === statusCodes.SIGN_IN_CANCELLED) {
      throw new Error('Sign in cancelled');
    } else if (error.code === statusCodes.IN_PROGRESS) {
      throw new Error('Sign in already in progress');
    } else if (error.code === statusCodes.PLAY_SERVICES_NOT_AVAILABLE) {
      throw new Error('Google Play Services not available');
    }
    console.error('Google Sign-In error:', error);
    throw error;
  }
}

/**
 * Sign out from Google
 */
export async function signOutFromGoogle(): Promise<void> {
  try {
    await GoogleSignin.signOut();
  } catch (error) {
    console.error('Google Sign-Out error:', error);
  }
}

/**
 * Check if Apple Sign-In is available
 */
export async function isAppleSignInAvailable(): Promise<boolean> {
  if (Platform.OS !== 'ios') {
    return false;
  }
  return await AppleAuthentication.isAvailableAsync();
}

/**
 * Apple Sign-In
 */
export async function signInWithApple(): Promise<OAuthUser> {
  const credential = await AppleAuthentication.signInAsync({
    requestedScopes: [
      AppleAuthentication.AppleAuthenticationScope.FULL_NAME,
      AppleAuthentication.AppleAuthenticationScope.EMAIL,
    ],
  });

  if (!credential.user) {
    throw new Error('Apple Sign-In failed: No user ID');
  }

  return {
    provider: 'apple',
    providerId: credential.user,
    email: credential.email || `${credential.user}@privaterelay.appleid.com`,
    name: credential.fullName 
      ? `${credential.fullName.givenName || ''} ${credential.fullName.familyName || ''}`.trim() || null
      : null,
    avatarUrl: null,
  };
}

export type OAuthProvider = 'google' | 'apple';
