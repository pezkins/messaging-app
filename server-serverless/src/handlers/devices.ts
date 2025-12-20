import type { APIGatewayProxyHandler } from 'aws-lambda';
import { dynamodb, Tables, PutCommand, DeleteCommand, QueryCommand } from '../lib/dynamo';
import { verifyToken, response } from '../lib/auth';

interface RegisterDeviceRequest {
  token: string;
  platform: 'ios' | 'android';
}

/**
 * Register a device token for push notifications
 */
export const register: APIGatewayProxyHandler = async (event) => {
  try {
    const authHeader = event.headers.Authorization || event.headers.authorization;
    if (!authHeader?.startsWith('Bearer ')) {
      return response(401, { error: 'Unauthorized' });
    }

    const jwtToken = authHeader.slice(7);
    let userId: string;
    try {
      const payload = verifyToken(jwtToken);
      userId = payload.userId;
    } catch {
      return response(401, { error: 'Invalid token' });
    }

    const body: RegisterDeviceRequest = JSON.parse(event.body || '{}');
    
    if (!body.token || !body.platform) {
      return response(400, { error: 'token and platform required' });
    }

    if (!['ios', 'android'].includes(body.platform)) {
      return response(400, { error: 'platform must be ios or android' });
    }

    // Remove any existing entries with this device token (could be from another user)
    // Note: 'token' is a DynamoDB reserved keyword, so we must use ExpressionAttributeNames
    const existingTokens = await dynamodb.send(new QueryCommand({
      TableName: Tables.DEVICE_TOKENS,
      IndexName: 'token-index',
      KeyConditionExpression: '#tokenAttr = :tokenVal',
      ExpressionAttributeNames: { '#tokenAttr': 'token' },
      ExpressionAttributeValues: { ':tokenVal': body.token }
    }));

    for (const item of existingTokens.Items || []) {
      await dynamodb.send(new DeleteCommand({
        TableName: Tables.DEVICE_TOKENS,
        Key: { userId: item.userId, token: item.token }
      }));
    }

    // Register new token
    await dynamodb.send(new PutCommand({
      TableName: Tables.DEVICE_TOKENS,
      Item: {
        userId,
        token: body.token,
        platform: body.platform,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      }
    }));

    console.log(`📱 Registered ${body.platform} device for user ${userId}`);

    return response(200, { success: true });
  } catch (error) {
    console.error('Device registration error:', error);
    return response(500, { error: 'Failed to register device' });
  }
};

/**
 * Unregister a device token (on logout or token refresh)
 */
export const unregister: APIGatewayProxyHandler = async (event) => {
  try {
    const authHeader = event.headers.Authorization || event.headers.authorization;
    if (!authHeader?.startsWith('Bearer ')) {
      return response(401, { error: 'Unauthorized' });
    }

    const jwtToken = authHeader.slice(7);
    let userId: string;
    try {
      const payload = verifyToken(jwtToken);
      userId = payload.userId;
    } catch {
      return response(401, { error: 'Invalid token' });
    }

    const body = JSON.parse(event.body || '{}');
    const deviceToken = body.token;

    if (deviceToken) {
      // Remove specific token
      await dynamodb.send(new DeleteCommand({
        TableName: Tables.DEVICE_TOKENS,
        Key: { userId, token: deviceToken }
      }));
      console.log(`📱 Unregistered device token for user ${userId}`);
    } else {
      // Remove all tokens for user (on logout)
      const tokens = await dynamodb.send(new QueryCommand({
        TableName: Tables.DEVICE_TOKENS,
        KeyConditionExpression: 'userId = :userId',
        ExpressionAttributeValues: { ':userId': userId }
      }));

      for (const item of tokens.Items || []) {
        await dynamodb.send(new DeleteCommand({
          TableName: Tables.DEVICE_TOKENS,
          Key: { userId, token: item.token }
        }));
      }
      console.log(`📱 Unregistered all device tokens for user ${userId}`);
    }

    return response(200, { success: true });
  } catch (error) {
    console.error('Device unregister error:', error);
    return response(500, { error: 'Failed to unregister device' });
  }
};
