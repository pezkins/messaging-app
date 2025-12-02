import type { APIGatewayProxyHandler, APIGatewayProxyWebsocketHandlerV2 } from 'aws-lambda';
import { ApiGatewayManagementApiClient, PostToConnectionCommand } from '@aws-sdk/client-apigatewaymanagementapi';
import { v4 as uuid } from 'uuid';
import { dynamodb, Tables, GetCommand, PutCommand, QueryCommand, DeleteCommand, UpdateCommand } from '../lib/dynamo';
import { verifyToken } from '../lib/auth';
import { translate, detectLanguage } from '../lib/translation';

function getApiClient(event: any) {
  const domain = event.requestContext.domainName;
  const stage = event.requestContext.stage;
  return new ApiGatewayManagementApiClient({
    endpoint: `https://${domain}/${stage}`,
  });
}

export const connect: APIGatewayProxyHandler = async (event) => {
  try {
    const connectionId = event.requestContext.connectionId!;
    const token = event.queryStringParameters?.token;

    if (!token) {
      return { statusCode: 401, body: 'Token required' };
    }

    let userId: string;
    try {
      const payload = verifyToken(token);
      userId = payload.userId;
    } catch {
      return { statusCode: 401, body: 'Invalid token' };
    }

    // Store connection
    const ttl = Math.floor(Date.now() / 1000) + 86400; // 24 hours
    await dynamodb.send(new PutCommand({
      TableName: Tables.CONNECTIONS,
      Item: {
        connectionId,
        userId,
        connectedAt: new Date().toISOString(),
        ttl,
      },
    }));

    console.log(`Connected: ${connectionId} (user: ${userId})`);
    return { statusCode: 200, body: 'Connected' };
  } catch (error) {
    console.error('Connect error:', error);
    return { statusCode: 500, body: 'Connection failed' };
  }
};

export const disconnect: APIGatewayProxyHandler = async (event) => {
  try {
    const connectionId = event.requestContext.connectionId!;

    await dynamodb.send(new DeleteCommand({
      TableName: Tables.CONNECTIONS,
      Key: { connectionId },
    }));

    console.log(`Disconnected: ${connectionId}`);
    return { statusCode: 200, body: 'Disconnected' };
  } catch (error) {
    console.error('Disconnect error:', error);
    return { statusCode: 500, body: 'Disconnect failed' };
  }
};

export const message: APIGatewayProxyHandler = async (event) => {
  try {
    const connectionId = event.requestContext.connectionId!;
    const body = JSON.parse(event.body || '{}');
    const { action, data } = body;

    // Get user from connection
    const connResult = await dynamodb.send(new GetCommand({
      TableName: Tables.CONNECTIONS,
      Key: { connectionId },
    }));

    if (!connResult.Item) {
      return { statusCode: 401, body: 'Not authenticated' };
    }

    const userId = connResult.Item.userId;

    if (action === 'message:send') {
      await handleSendMessage(event, userId, data);
    } else if (action === 'message:typing') {
      await handleTyping(event, userId, data);
    }

    return { statusCode: 200, body: 'OK' };
  } catch (error) {
    console.error('Message handler error:', error);
    return { statusCode: 500, body: 'Error' };
  }
};

async function handleSendMessage(event: any, senderId: string, data: any) {
  const { conversationId, content, type = 'text' } = data;

  // Get sender info
  const senderResult = await dynamodb.send(new GetCommand({
    TableName: Tables.USERS,
    Key: { id: senderId },
  }));
  const sender = senderResult.Item;

  // Detect language
  const originalLanguage = await detectLanguage(content);

  // Create message
  const messageId = uuid();
  const timestamp = new Date().toISOString();

  const message = {
    id: messageId,
    conversationId,
    senderId,
    sender: {
      id: sender?.id,
      username: sender?.username,
      preferredLanguage: sender?.preferredLanguage,
    },
    type,
    originalContent: content,
    originalLanguage,
    status: 'sent',
    timestamp,
    translations: {}, // Cache translations here
  };

  await dynamodb.send(new PutCommand({
    TableName: Tables.MESSAGES,
    Item: message,
  }));

  // Update conversation lastMessage
  const convResult = await dynamodb.send(new QueryCommand({
    TableName: Tables.CONVERSATIONS,
    IndexName: 'user-conversations-index',
    KeyConditionExpression: 'visibleTo = :userId',
    FilterExpression: 'id = :convId',
    ExpressionAttributeValues: {
      ':userId': senderId,
      ':convId': conversationId,
    },
  }));

  const conversation = convResult.Items?.[0];
  if (!conversation) return;

  // Get all participants
  const participantIds = conversation.participantIds as string[];

  // Get connections for all participants
  const api = getApiClient(event);

  for (const participantId of participantIds) {
    // Get participant's preferred language
    const participantResult = await dynamodb.send(new GetCommand({
      TableName: Tables.USERS,
      Key: { id: participantId },
    }));
    const targetLanguage = participantResult.Item?.preferredLanguage || 'en';

    // Translate if needed
    let translatedContent = content;
    if (originalLanguage !== targetLanguage) {
      translatedContent = await translate(content, originalLanguage, targetLanguage);
      
      // Cache translation
      message.translations[targetLanguage] = translatedContent;
    }

    // Get participant's connections
    const connections = await dynamodb.send(new QueryCommand({
      TableName: Tables.CONNECTIONS,
      IndexName: 'user-connections-index',
      KeyConditionExpression: 'userId = :userId',
      ExpressionAttributeValues: { ':userId': participantId },
    }));

    // Send to all connections
    for (const conn of connections.Items || []) {
      try {
        await api.send(new PostToConnectionCommand({
          ConnectionId: conn.connectionId,
          Data: JSON.stringify({
            action: 'message:receive',
            message: {
              ...message,
              translatedContent,
              targetLanguage,
            },
          }),
        }));
      } catch (err: any) {
        // Connection might be stale, delete it
        if (err.statusCode === 410) {
          await dynamodb.send(new DeleteCommand({
            TableName: Tables.CONNECTIONS,
            Key: { connectionId: conn.connectionId },
          }));
        }
      }
    }
  }

  // Save cached translations
  await dynamodb.send(new UpdateCommand({
    TableName: Tables.MESSAGES,
    Key: { conversationId, timestamp },
    UpdateExpression: 'SET translations = :translations',
    ExpressionAttributeValues: { ':translations': message.translations },
  }));
}

async function handleTyping(event: any, userId: string, data: any) {
  const { conversationId, isTyping } = data;

  // Get conversation participants
  const convResult = await dynamodb.send(new QueryCommand({
    TableName: Tables.CONVERSATIONS,
    IndexName: 'user-conversations-index',
    KeyConditionExpression: 'visibleTo = :userId',
    FilterExpression: 'id = :convId',
    ExpressionAttributeValues: {
      ':userId': userId,
      ':convId': conversationId,
    },
  }));

  const conversation = convResult.Items?.[0];
  if (!conversation) return;

  const api = getApiClient(event);
  const participantIds = conversation.participantIds as string[];

  // Notify other participants
  for (const participantId of participantIds) {
    if (participantId === userId) continue;

    const connections = await dynamodb.send(new QueryCommand({
      TableName: Tables.CONNECTIONS,
      IndexName: 'user-connections-index',
      KeyConditionExpression: 'userId = :userId',
      ExpressionAttributeValues: { ':userId': participantId },
    }));

    for (const conn of connections.Items || []) {
      try {
        await api.send(new PostToConnectionCommand({
          ConnectionId: conn.connectionId,
          Data: JSON.stringify({
            action: 'message:typing',
            conversationId,
            userId,
            isTyping,
          }),
        }));
      } catch {
        // Ignore stale connections
      }
    }
  }
}

