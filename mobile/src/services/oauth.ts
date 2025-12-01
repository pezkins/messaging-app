import * as Google from 'expo-auth-session/providers/google';
import * as AppleAuthentication from 'expo-apple-authentication';
import * as WebBrowser from 'expo-web-browser';
import Constants from 'expo-constants';
import { Platform } from 'react-native';

// Complete auth session for web
WebBrowser.maybeCompleteAuthSession();

// Get config from app.json extra
const config = Constants.expoConfig?.extra || {};

// Google OAuth configuration
const GOOGLE_CLIENT_ID_WEB = config.googleClientIdWeb || '';
const GOOGLE_CLIENT_ID_IOS = config.googleClientIdIos || '';
const GOOGLE_CLIENT_ID_ANDROID = config.googleClientIdAndroid || '';

export interface OAuthUser {
  provider: 'google' | 'apple';
  providerId: string;
  email: string;
  name: string | null;
  avatarUrl: string | null;
}

/**
 * Google Sign-In Hook
 * Returns the request, response, and promptAsync function
 */
export function useGoogleAuth() {
  const [request, response, promptAsync] = Google.useAuthRequest({
    webClientId: GOOGLE_CLIENT_ID_WEB,
    iosClientId: GOOGLE_CLIENT_ID_IOS,
    androidClientId: GOOGLE_CLIENT_ID_ANDROID,
  });

  return { request, response, promptAsync };
}

/**
 * Get Google user info from access token
 */
export async function getGoogleUserInfo(accessToken: string): Promise<OAuthUser> {
  const response = await fetch('https://www.googleapis.com/oauth2/v3/userinfo', {
    headers: { Authorization: `Bearer ${accessToken}` },
  });

  if (!response.ok) {
    throw new Error('Failed to get Google user info');
  }

  const data = await response.json();

  return {
    provider: 'google',
    providerId: data.sub,
    email: data.email,
    name: data.name || null,
    avatarUrl: data.picture || null,
  };
}

/**
 * Check if Apple Sign-In is available
 */
export async function isAppleSignInAvailable(): Promise<boolean> {
  if (Platform.OS !== 'ios') {
    // Apple Sign-In on Android/Web requires additional setup
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

  // Note: Apple only provides email and name on first sign-in
  // After that, you need to store and retrieve from your backend
  return {
    provider: 'apple',
    providerId: credential.user,
    email: credential.email || `${credential.user}@privaterelay.appleid.com`,
    name: credential.fullName 
      ? `${credential.fullName.givenName || ''} ${credential.fullName.familyName || ''}`.trim() || null
      : null,
    avatarUrl: null, // Apple doesn't provide avatar
  };
}

/**
 * OAuth provider types
 */
export type OAuthProvider = 'google' | 'apple';

