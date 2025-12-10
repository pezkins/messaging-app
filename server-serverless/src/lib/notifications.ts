/**
 * Push Notification Service
 * Supports iOS (APNs) and Android (FCM V1 API)
 * 
 * Credentials are fetched from AWS Secrets Manager:
 * - intok/push/apns: APNs credentials (keyId, teamId, privateKey)
 * - intok/push/fcm: FCM credentials (projectId, serviceAccount)
 */

import { dynamodb, Tables, QueryCommand } from './dynamo';
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
  if (apnsCredentials) return apnsCredentials;
  
  const secretName = process.env.APNS_SECRET_NAME;
  if (!secretName) {
    console.log('📱 APNS_SECRET_NAME not configured');
    return null;
  }
  
  try {
    const response = await secretsManager.send(new GetSecretValueCommand({
      SecretId: secretName
    }));
    
    if (response.SecretString) {
      apnsCredentials = JSON.parse(response.SecretString);
      console.log('✅ APNs credentials loaded from Secrets Manager');
      return apnsCredentials;
    }
  } catch (error) {
    console.error('❌ Failed to fetch APNs credentials:', error);
  }
  
  return null;
}

/**
 * Fetch FCM credentials from AWS Secrets Manager
 */
async function getFCMCredentials(): Promise<typeof fcmCredentials> {
  if (fcmCredentials) return fcmCredentials;
  
  const secretName = process.env.FCM_SECRET_NAME;
  if (!secretName) {
    console.log('📱 FCM_SECRET_NAME not configured');
    return null;
  }
  
  try {
    const response = await secretsManager.send(new GetSecretValueCommand({
      SecretId: secretName
    }));
    
    if (response.SecretString) {
      const parsed = JSON.parse(response.SecretString);
      fcmCredentials = {
        projectId: parsed.projectId,
        serviceAccount: typeof parsed.serviceAccount === 'string' 
          ? JSON.parse(parsed.serviceAccount)
          : parsed.serviceAccount
      };
      console.log('✅ FCM credentials loaded from Secrets Manager');
      return fcmCredentials;
    }
  } catch (error) {
    console.error('❌ Failed to fetch FCM credentials:', error);
  }
  
  return null;
}

/**
 * Send push notification to a user's devices
 */
export async function sendPushNotification(payload: NotificationPayload): Promise<void> {
  const { userId, title, body, data } = payload;

  // Get user's device tokens
  const tokens = await dynamodb.send(new QueryCommand({
    TableName: Tables.DEVICE_TOKENS,
    KeyConditionExpression: 'userId = :userId',
    ExpressionAttributeValues: { ':userId': userId }
  }));

  if (!tokens.Items?.length) {
    console.log(`📱 No devices registered for user ${userId}`);
    return;
  }

  console.log(`📱 Sending push notification to ${tokens.Items.length} device(s) for user ${userId}`);

  for (const device of tokens.Items as DeviceToken[]) {
    try {
      if (device.platform === 'ios') {
        await sendAPNS(device.token, title, body, data);
      } else if (device.platform === 'android') {
        await sendFCMv1(device.token, title, body, data);
      }
    } catch (error) {
      console.error(`Failed to send to ${device.platform} device:`, error);
      // TODO: Consider removing invalid tokens (e.g., on 404/410 errors)
    }
  }
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
 * Send notification via Apple Push Notification Service
 */
async function sendAPNS(
  deviceToken: string, 
  title: string, 
  body: string, 
  data?: Record<string, string>
): Promise<void> {
  const jwtToken = await getAPNsJWT();
  
  if (!jwtToken) {
    console.log(`📱 APNs not configured, skipping iOS push to ${deviceToken.substring(0, 10)}...`);
    return;
  }

  console.log(`📱 Sending APNs to ${deviceToken.substring(0, 10)}...`);
  
  // Use production APNs endpoint
  const url = `https://api.push.apple.com/3/device/${deviceToken}`;
  
  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'authorization': `bearer ${jwtToken}`,
      'apns-topic': 'com.pezkins.intok',
      'apns-push-type': 'alert',
      'apns-priority': '10',
    },
    body: JSON.stringify({
      aps: {
        alert: { title, body },
        badge: 1,
        sound: 'default',
      },
      ...data
    })
  });
  
  if (!response.ok) {
    const errorBody = await response.text();
    console.error(`❌ APNs error: ${response.status} - ${errorBody}`);
    throw new Error(`APNs error: ${response.status} - ${errorBody}`);
  }
  
  console.log(`✅ APNs notification sent to ${deviceToken.substring(0, 10)}...`);
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
  data?: Record<string, string>
): Promise<void> {
  const creds = await getFCMCredentials();
  const accessToken = await getFCMAccessToken();
  
  if (!creds || !accessToken) {
    console.log(`📱 FCM not configured, skipping Android push to ${deviceToken.substring(0, 10)}...`);
    return;
  }

  console.log(`📱 Sending FCM V1 to ${deviceToken.substring(0, 10)}...`);
  
  // FCM V1 API endpoint
  const url = `https://fcm.googleapis.com/v1/projects/${creds.projectId}/messages:send`;
  
  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${accessToken}`
    },
    body: JSON.stringify({
      message: {
        token: deviceToken,
        notification: { title, body },
        android: {
          priority: 'high',
          notification: {
            sound: 'default',
            click_action: 'OPEN_ACTIVITY',
          },
        },
        data: data || {},
      }
    })
  });

  if (!response.ok) {
    const errorBody = await response.text();
    console.error(`❌ FCM V1 error: ${response.status} - ${errorBody}`);
    throw new Error(`FCM V1 error: ${response.status} - ${errorBody}`);
  }

  const result = await response.json() as { name: string };
  console.log(`✅ FCM V1 notification sent:`, result.name);
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
