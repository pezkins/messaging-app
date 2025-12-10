/**
 * Push Notification Service
 * Supports iOS (APNs) and Android (FCM V1 API)
 */

import { dynamodb, Tables, QueryCommand } from './dynamo';
import * as jwt from 'jsonwebtoken';
import { GoogleAuth } from 'google-auth-library';

// APNs Configuration
const APNS_KEY_ID = process.env.APNS_KEY_ID;
const APNS_TEAM_ID = process.env.APNS_TEAM_ID;
const APNS_AUTH_KEY = process.env.APNS_AUTH_KEY;

// FCM V1 Configuration (Service Account)
const FCM_SERVICE_ACCOUNT = process.env.FCM_SERVICE_ACCOUNT; // JSON string
const FCM_PROJECT_ID = process.env.FCM_PROJECT_ID || 'lingualink-479903';

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
function getAPNsJWT(): string {
  const now = Math.floor(Date.now() / 1000);
  
  // Return cached token if still valid (refresh 10 mins before expiry)
  if (apnsJwtToken && apnsJwtExpiry > now + 600) {
    return apnsJwtToken;
  }

  if (!APNS_AUTH_KEY || !APNS_KEY_ID || !APNS_TEAM_ID) {
    throw new Error('APNs credentials not configured');
  }

  // The .p8 key content - handle both with and without header/footer
  let key = APNS_AUTH_KEY;
  if (!key.includes('-----BEGIN PRIVATE KEY-----')) {
    key = `-----BEGIN PRIVATE KEY-----\n${key}\n-----END PRIVATE KEY-----`;
  }

  const token = jwt.sign({}, key, {
    algorithm: 'ES256',
    issuer: APNS_TEAM_ID,
    header: {
      alg: 'ES256',
      kid: APNS_KEY_ID,
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
  // Skip if APNs is not configured
  if (!APNS_KEY_ID || !APNS_TEAM_ID || !APNS_AUTH_KEY) {
    console.log(`📱 APNs not configured, skipping iOS push to ${deviceToken.substring(0, 10)}...`);
    return;
  }

  console.log(`📱 Sending APNs to ${deviceToken.substring(0, 10)}...`);
  
  const jwtToken = getAPNsJWT();
  
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
async function getFCMAccessToken(): Promise<string> {
  if (!FCM_SERVICE_ACCOUNT) {
    throw new Error('FCM Service Account not configured');
  }

  // Initialize auth client if needed
  if (!fcmAuthClient) {
    const serviceAccount = JSON.parse(FCM_SERVICE_ACCOUNT);
    fcmAuthClient = new GoogleAuth({
      credentials: serviceAccount,
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
  // Skip if FCM is not configured
  if (!FCM_SERVICE_ACCOUNT) {
    console.log(`📱 FCM not configured, skipping Android push to ${deviceToken.substring(0, 10)}...`);
    return;
  }

  console.log(`📱 Sending FCM V1 to ${deviceToken.substring(0, 10)}...`);
  
  // Get project ID from service account if not explicitly set
  let projectId = FCM_PROJECT_ID;
  if (!projectId || projectId === 'lingualink-479903') {
    try {
      const serviceAccount = JSON.parse(FCM_SERVICE_ACCOUNT);
      projectId = serviceAccount.project_id || 'lingualink-479903';
    } catch {
      projectId = 'lingualink-479903';
    }
  }

  const accessToken = await getFCMAccessToken();
  
  // FCM V1 API endpoint
  const url = `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`;
  
  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${accessToken}`
    },
    body: JSON.stringify({
      message: {
        token: deviceToken,
        notification: {
          title,
          body,
        },
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

  const result = await response.json();
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
