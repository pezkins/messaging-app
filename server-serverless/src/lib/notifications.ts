/**
 * Push Notification Service
 * Supports iOS (APNs) and Android (FCM V1 API)
 * 
 * Credentials are fetched from AWS Secrets Manager:
 * - intok/push/apns: APNs credentials (keyId, teamId, privateKey)
 * - intok/push/fcm: FCM credentials (projectId, serviceAccount)
 */

import { dynamodb, Tables, QueryCommand, DeleteCommand } from './dynamo';
import { SecretsManagerClient, GetSecretValueCommand } from '@aws-sdk/client-secrets-manager';
import * as jwt from 'jsonwebtoken';
import { GoogleAuth } from 'google-auth-library';

// Secrets Manager client
const secretsManager = new SecretsManagerClient({ 
  region: process.env.AWS_REGION_NAME || 'us-east-1' 
});

// Cache for credentials (fetched once per Lambda cold start)
let apnsCredentials: {
  keyId: string;
  teamId: string;
  privateKey: string;
} | null = null;

let fcmCredentials: {
  projectId: string;
  serviceAccount: Record<string, unknown>;
} | null = null;

// APNs JWT token cache (valid for 1 hour max, we refresh every 50 mins)
let apnsJwtToken: string | null = null;
let apnsJwtExpiry: number = 0;

// FCM GoogleAuth client (handles OAuth2 token caching automatically)
let fcmAuthClient: GoogleAuth | null = null;

interface NotificationPayload {
  userId: string;
  title: string;
  body: string;
  data?: Record<string, string>;
}

interface DeviceToken {
  userId: string;
  token: string;
  platform: 'ios' | 'android';
}

/**
 * Fetch APNs credentials from AWS Secrets Manager
 */
async function getAPNsCredentials(): Promise<typeof apnsCredentials> {
  if (apnsCredentials) {
    console.log('📱 [APNs] Using cached credentials');
    return apnsCredentials;
  }
  
  const secretName = process.env.APNS_SECRET_NAME;
  if (!secretName) {
    console.log('📱 [APNs] APNS_SECRET_NAME not configured - iOS push disabled');
    return null;
  }
  
  console.log(`📱 [APNs] Fetching credentials from Secrets Manager: ${secretName}`);
  
  try {
    const response = await secretsManager.send(new GetSecretValueCommand({
      SecretId: secretName
    }));
    
    if (response.SecretString) {
      apnsCredentials = JSON.parse(response.SecretString);
      console.log(`✅ [APNs] Credentials loaded - keyId: ${apnsCredentials?.keyId}, teamId: ${apnsCredentials?.teamId}`);
      return apnsCredentials;
    } else {
      console.error('❌ [APNs] Secret exists but has no string value');
    }
  } catch (error: any) {
    console.error(`❌ [APNs] Failed to fetch credentials: ${error.message}`);
    console.error(`❌ [APNs] Error code: ${error.code || error.name}`);
  }
  
  return null;
}

/**
 * Fetch FCM credentials from AWS Secrets Manager
 * Supports two formats:
 * 1. Wrapped: { projectId: "...", serviceAccount: {...} }
 * 2. Raw Firebase service account JSON: { project_id: "...", private_key: "...", ... }
 */
async function getFCMCredentials(): Promise<typeof fcmCredentials> {
  if (fcmCredentials) {
    console.log('📱 [FCM] Using cached credentials');
    return fcmCredentials;
  }
  
  const secretName = process.env.FCM_SECRET_NAME;
  if (!secretName) {
    console.log('📱 [FCM] FCM_SECRET_NAME not configured - Android push disabled');
    return null;
  }
  
  console.log(`📱 [FCM] Fetching credentials from Secrets Manager: ${secretName}`);
  
  try {
    const response = await secretsManager.send(new GetSecretValueCommand({
      SecretId: secretName
    }));
    
    if (response.SecretString) {
      const parsed = JSON.parse(response.SecretString);
      
      // Check if this is a raw Firebase service account JSON (has 'project_id' and 'type')
      if (parsed.project_id && parsed.type === 'service_account') {
        // Raw Firebase service account format
        fcmCredentials = {
          projectId: parsed.project_id,
          serviceAccount: parsed
        };
        console.log(`✅ [FCM] Credentials loaded (raw format) - projectId: ${fcmCredentials.projectId}`);
      } else if (parsed.projectId) {
        // Wrapped format: { projectId, serviceAccount }
        fcmCredentials = {
          projectId: parsed.projectId,
          serviceAccount: typeof parsed.serviceAccount === 'string' 
            ? JSON.parse(parsed.serviceAccount)
            : parsed.serviceAccount
        };
        console.log(`✅ [FCM] Credentials loaded (wrapped format) - projectId: ${fcmCredentials.projectId}`);
      } else {
        console.error('❌ [FCM] Invalid secret format - missing project_id or projectId');
        return null;
      }
      
      return fcmCredentials;
    } else {
      console.error('❌ [FCM] Secret exists but has no string value');
    }
  } catch (error: any) {
    console.error(`❌ [FCM] Failed to fetch credentials: ${error.message}`);
    console.error(`❌ [FCM] Error code: ${error.code || error.name}`);
  }
  
  return null;
}

/**
 * Remove an invalid device token from the database
 */
async function removeInvalidToken(userId: string, token: string, reason: string): Promise<void> {
  try {
    await dynamodb.send(new DeleteCommand({
      TableName: Tables.DEVICE_TOKENS,
      Key: { userId, token }
    }));
    console.log(`🧹 Removed invalid token for user ${userId}: ${reason}`);
  } catch (error) {
    console.error(`Failed to remove invalid token:`, error);
  }
}

/**
 * Send push notification to a user's devices
 */
export async function sendPushNotification(payload: NotificationPayload): Promise<void> {
  const { userId, title, body, data } = payload;

  console.log(`📱 [PUSH] Starting notification for user ${userId}`);
  console.log(`📱 [PUSH] Title: "${title}", Body: "${body?.substring(0, 50)}..."`);
  console.log(`📱 [PUSH] Data:`, JSON.stringify(data));

  // Get user's device tokens
  const tokens = await dynamodb.send(new QueryCommand({
    TableName: Tables.DEVICE_TOKENS,
    KeyConditionExpression: 'userId = :userId',
    ExpressionAttributeValues: { ':userId': userId }
  }));

  if (!tokens.Items?.length) {
    console.log(`📱 [PUSH] No devices registered for user ${userId} - skipping`);
    return;
  }

  console.log(`📱 [PUSH] Found ${tokens.Items.length} device(s) for user ${userId}:`);
  tokens.Items.forEach((device: any, i: number) => {
    console.log(`📱 [PUSH]   ${i + 1}. ${device.platform}: ${device.token.substring(0, 20)}...`);
  });

  let successCount = 0;
  let failCount = 0;

  for (const device of tokens.Items as DeviceToken[]) {
    try {
      if (device.platform === 'ios') {
        await sendAPNS(device.token, title, body, data, userId);
        successCount++;
      } else if (device.platform === 'android') {
        await sendFCMv1(device.token, title, body, data, userId);
        successCount++;
      } else {
        console.warn(`📱 [PUSH] Unknown platform: ${device.platform}`);
      }
    } catch (error: any) {
      failCount++;
      console.error(`📱 [PUSH] Failed to send to ${device.platform} device:`, error.message);
      
      // Check if we should remove the token
      if (error.shouldRemoveToken) {
        await removeInvalidToken(userId, device.token, error.message);
      }
    }
  }

  console.log(`📱 [PUSH] Completed for user ${userId}: ${successCount} success, ${failCount} failed`);
}

/**
 * Generate JWT token for APNs authentication
 */
async function getAPNsJWT(): Promise<string | null> {
  const now = Math.floor(Date.now() / 1000);
  
  // Return cached token if still valid (refresh 10 mins before expiry)
  if (apnsJwtToken && apnsJwtExpiry > now + 600) {
    return apnsJwtToken;
  }

  const creds = await getAPNsCredentials();
  if (!creds) return null;

  // The .p8 key content - handle both with and without header/footer
  let key = creds.privateKey;
  if (!key.includes('-----BEGIN PRIVATE KEY-----')) {
    key = `-----BEGIN PRIVATE KEY-----\n${key}\n-----END PRIVATE KEY-----`;
  }

  const token = jwt.sign({}, key, {
    algorithm: 'ES256',
    issuer: creds.teamId,
    header: {
      alg: 'ES256',
      kid: creds.keyId,
    },
    expiresIn: '1h',
  });

  apnsJwtToken = token;
  apnsJwtExpiry = now + 3600; // 1 hour from now
  
  return token;
}

/**
 * Custom error class for push notification errors
 */
class PushNotificationError extends Error {
  shouldRemoveToken: boolean;
  
  constructor(message: string, shouldRemoveToken: boolean = false) {
    super(message);
    this.shouldRemoveToken = shouldRemoveToken;
  }
}

/**
 * Send notification via Apple Push Notification Service
 * Supports both production and sandbox environments via APNS_ENVIRONMENT env var
 */
async function sendAPNS(
  deviceToken: string, 
  title: string, 
  body: string, 
  data?: Record<string, string>,
  userId?: string
): Promise<void> {
  const jwtToken = await getAPNsJWT();
  
  if (!jwtToken) {
    console.log(`📱 [APNs] Not configured, skipping iOS push to ${deviceToken.substring(0, 20)}...`);
    return;
  }

  // Support sandbox for development builds (set APNS_ENVIRONMENT=sandbox)
  const environment = process.env.APNS_ENVIRONMENT || 'production';
  const host = environment === 'sandbox' 
    ? 'api.sandbox.push.apple.com' 
    : 'api.push.apple.com';
  
  console.log(`📱 [APNs] Sending to ${deviceToken.substring(0, 20)}... (${environment})`);
  
  const url = `https://${host}/3/device/${deviceToken}`;
  
  const payload = {
    aps: {
      alert: { title, body },
      badge: 1,
      sound: 'default',
    },
    ...data
  };
  
  console.log(`📱 [APNs] Payload:`, JSON.stringify(payload));
  
  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'authorization': `bearer ${jwtToken}`,
      'apns-topic': 'com.pezkins.intok',
      'apns-push-type': 'alert',
      'apns-priority': '10',
    },
    body: JSON.stringify(payload)
  });
  
  if (!response.ok) {
    const errorBody = await response.text();
    console.error(`❌ [APNs] Error ${response.status}: ${errorBody}`);
    
    // APNs error reasons that indicate invalid token
    // See: https://developer.apple.com/documentation/usernotifications/handling-notification-responses-from-apns
    const invalidTokenStatuses = [400, 410]; // BadDeviceToken, Unregistered
    const shouldRemove = invalidTokenStatuses.includes(response.status) || 
                         errorBody.includes('BadDeviceToken') ||
                         errorBody.includes('Unregistered') ||
                         errorBody.includes('DeviceTokenNotForTopic');
    
    throw new PushNotificationError(
      `APNs error: ${response.status} - ${errorBody}`,
      shouldRemove
    );
  }
  
  console.log(`✅ [APNs] Notification sent to ${deviceToken.substring(0, 20)}...`);
}

/**
 * Get FCM OAuth2 access token using Service Account
 */
async function getFCMAccessToken(): Promise<string | null> {
  const creds = await getFCMCredentials();
  if (!creds) return null;

  // Initialize auth client if needed
  if (!fcmAuthClient) {
    fcmAuthClient = new GoogleAuth({
      credentials: creds.serviceAccount,
      scopes: ['https://www.googleapis.com/auth/firebase.messaging'],
    });
  }

  const client = await fcmAuthClient.getClient();
  const accessToken = await client.getAccessToken();
  
  if (!accessToken.token) {
    throw new Error('Failed to get FCM access token');
  }
  
  return accessToken.token;
}

/**
 * Send notification via Firebase Cloud Messaging V1 API
 */
async function sendFCMv1(
  deviceToken: string, 
  title: string, 
  body: string, 
  data?: Record<string, string>,
  userId?: string
): Promise<void> {
  const creds = await getFCMCredentials();
  const accessToken = await getFCMAccessToken();
  
  if (!creds || !accessToken) {
    console.log(`📱 [FCM] Not configured, skipping Android push to ${deviceToken.substring(0, 20)}...`);
    return;
  }

  console.log(`📱 [FCM] Sending to ${deviceToken.substring(0, 20)}...`);
  console.log(`📱 [FCM] Project: ${creds.projectId}`);
  
  // FCM V1 API endpoint
  const url = `https://fcm.googleapis.com/v1/projects/${creds.projectId}/messages:send`;
  
  const payload = {
    message: {
      token: deviceToken,
      notification: { title, body },
      android: {
        priority: 'high',
        notification: {
          sound: 'default',
          channel_id: 'intok_messages', // Match Android app's notification channel
        },
      },
      data: {
        ...data,
        click_action: 'OPEN_ACTIVITY', // Use data payload for navigation intent
      },
    }
  };
  
  console.log(`📱 [FCM] Payload:`, JSON.stringify(payload));
  
  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${accessToken}`
    },
    body: JSON.stringify(payload)
  });

  if (!response.ok) {
    const errorBody = await response.text();
    console.error(`❌ [FCM] Error ${response.status}: ${errorBody}`);
    
    // FCM V1 API error codes that indicate invalid/unregistered token
    // See: https://firebase.google.com/docs/cloud-messaging/send-message#rest
    const shouldRemove = errorBody.includes('UNREGISTERED') ||
                         errorBody.includes('INVALID_ARGUMENT') ||
                         errorBody.includes('NOT_FOUND') ||
                         errorBody.includes('Requested entity was not found');
    
    throw new PushNotificationError(
      `FCM V1 error: ${response.status} - ${errorBody}`,
      shouldRemove
    );
  }

  const result = await response.json() as { name: string };
  console.log(`✅ [FCM] Notification sent: ${result.name}`);
}

/**
 * Send push notification to multiple users
 */
export async function sendPushNotificationToUsers(
  userIds: string[],
  title: string,
  body: string,
  data?: Record<string, string>
): Promise<void> {
  const promises = userIds.map(userId => 
    sendPushNotification({ userId, title, body, data })
  );
  await Promise.allSettled(promises);
}

/**
 * Truncate message content for notification body
 */
export function truncateForNotification(content: string, maxLength = 100): string {
  if (content.length <= maxLength) return content;
  return content.substring(0, maxLength - 3) + '...';
}
