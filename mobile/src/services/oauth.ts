import * as Google from 'expo-auth-session/providers/google';
import * as AppleAuthentication from 'expo-apple-authentication';
import * as WebBrowser from 'expo-web-browser';
import { makeRedirectUri } from 'expo-auth-session';
import Constants from 'expo-constants';
import { Platform } from 'react-native';

// IMPORTANT: Complete auth session for web
WebBrowser.maybeCompleteAuthSession();

// Get config from app.json extra
const config = Constants.expoConfig?.extra || {};

// Google OAuth Client IDs
const GOOGLE_WEB_CLIENT_ID = config.googleClientIdWeb || '';
const GOOGLE_IOS_CLIENT_ID = config.googleClientIdIos || '';
const GOOGLE_ANDROID_CLIENT_ID = config.googleClientIdAndroid || '';

// Generate redirect URI for the app
// For standalone builds: intok://redirect
// For Expo Go: uses Expo auth proxy automatically
const redirectUri = makeRedirectUri({
  scheme: 'intok',
  path: 'redirect',
});

console.log('🔑 Redirect URI being used:', redirectUri);

export interface OAuthUser {
  provider: 'google' | 'apple';
  providerId: string;
  email: string;
  name: string | null;
  avatarUrl: string | null;
}

/**
 * Google Sign-In Hook
 */
export function useGoogleAuth() {
  const [request, response, promptAsync] = Google.useAuthRequest(
    {
      clientId: GOOGLE_WEB_CLIENT_ID,
      iosClientId: GOOGLE_IOS_CLIENT_ID,
      androidClientId: GOOGLE_ANDROID_CLIENT_ID,
      scopes: ['openid', 'profile', 'email'],
      redirectUri, // Use the proxy redirect URI
    }
  );

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
