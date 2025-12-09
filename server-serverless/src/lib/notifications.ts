/**
 * Push Notification Service
 * Supports iOS (APNs) and Android (FCM)
 */

import { dynamodb, Tables, QueryCommand } from './dynamo';

const FCM_SERVER_KEY = process.env.FCM_SERVER_KEY;
const APNS_KEY_ID = process.env.APNS_KEY_ID;
const APNS_TEAM_ID = process.env.APNS_TEAM_ID;

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
        await sendFCM(device.token, title, body, data);
      }
    } catch (error) {
      console.error(`Failed to send to ${device.platform} device:`, error);
      // TODO: Consider removing invalid tokens (e.g., on 404/410 errors)
    }
  }
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
  if (!APNS_KEY_ID || !APNS_TEAM_ID) {
    console.log(`📱 APNs not configured, skipping iOS push to ${deviceToken.substring(0, 10)}...`);
    return;
  }

  console.log(`📱 Sending APNs to ${deviceToken.substring(0, 10)}...`);
  
  // Note: Full APNs implementation requires:
  // 1. APNs Auth Key (.p8 file)
  // 2. JWT signing for APNs token
  // 3. HTTP/2 client for APNs API
  // 
  // For production, consider using:
  // - AWS SNS (Simple Notification Service)
  // - Firebase Admin SDK
  // - A dedicated push service like OneSignal
  //
  // Example implementation with APNs HTTP/2:
  /*
  const jwt = generateAPNsJWT();
  const response = await fetch(
    `https://api.push.apple.com/3/device/${deviceToken}`,
    {
      method: 'POST',
      headers: {
        'authorization': `bearer ${jwt}`,
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
    }
  );
  
  if (!response.ok) {
    const errorBody = await response.text();
    throw new Error(`APNs error: ${response.status} - ${errorBody}`);
  }
  */
}

/**
 * Send notification via Firebase Cloud Messaging
 */
async function sendFCM(
  deviceToken: string, 
  title: string, 
  body: string, 
  data?: Record<string, string>
): Promise<void> {
  // Skip if FCM is not configured
  if (!FCM_SERVER_KEY) {
    console.log(`📱 FCM not configured, skipping Android push to ${deviceToken.substring(0, 10)}...`);
    return;
  }

  console.log(`📱 Sending FCM to ${deviceToken.substring(0, 10)}...`);
  
  const response = await fetch('https://fcm.googleapis.com/fcm/send', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `key=${FCM_SERVER_KEY}`
    },
    body: JSON.stringify({
      to: deviceToken,
      notification: {
        title,
        body,
        sound: 'default'
      },
      data: {
        ...data,
        click_action: 'FLUTTER_NOTIFICATION_CLICK'
      },
      priority: 'high'
    })
  });

  if (!response.ok) {
    const errorBody = await response.text();
    throw new Error(`FCM error: ${response.status} - ${errorBody}`);
  }

  const result = await response.json();
  console.log(`📱 FCM response:`, result);
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
