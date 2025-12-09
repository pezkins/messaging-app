import type { APIGatewayProxyHandler, APIGatewayProxyWebsocketHandlerV2 } from 'aws-lambda';
import { ApiGatewayManagementApiClient, PostToConnectionCommand } from '@aws-sdk/client-apigatewaymanagementapi';
import { v4 as uuid } from 'uuid';
import { dynamodb, Tables, GetCommand, PutCommand, QueryCommand, DeleteCommand, UpdateCommand } from '../lib/dynamo';
import { verifyToken } from '../lib/auth';
import { translate, translateDocumentContent, detectLanguage } from '../lib/translation';
import { sendPushNotification, truncateForNotification } from '../lib/notifications';

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
    } else if (action === 'message:reaction') {
      await handleReaction(event, userId, data);
    } else if (action === 'message:read') {
      await handleReadReceipt(event, userId, data);
    }

    return { statusCode: 200, body: 'OK' };
  } catch (error) {
    console.error('Message handler error:', error);
    return { statusCode: 500, body: 'Error' };
  }
};

// Attachment interface for type safety
interface Attachment {
  id: string;
  key: string;
  fileName: string;
  contentType: string;
  fileSize: number;
  category: string;
}

async function handleSendMessage(event: any, senderId: string, data: any) {
  const { 
    conversationId, 
    content, 
    type = 'text', 
    attachment,
    translateDocument = false  // Flag for document translation
  } = data;

  // Get sender info
  const senderResult = await dynamodb.send(new GetCommand({
    TableName: Tables.USERS,
    Key: { id: senderId },
  }));
  const sender = senderResult.Item;

  // Determine if we should translate this message type
  // Skip language detection for images, GIFs, and untranslated documents
  const shouldTranslate = type === 'text' || (type === 'file' && translateDocument);
  const originalLanguage = shouldTranslate && content 
    ? await detectLanguage(content) 
    : 'en';
  
  console.log(`📨 Message type: ${type}, shouldTranslate: ${shouldTranslate}, translateDocument: ${translateDocument}`);

  // Create message
  const messageId = uuid();
  const timestamp = new Date().toISOString();

  // Validate and sanitize attachment data if provided
  let sanitizedAttachment: Attachment | null = null;
  
  if (attachment) {
    const validCategories = ['image', 'video', 'document', 'audio'];
    const maxFileSize = 25 * 1024 * 1024; // 25MB
    
    // Validate category
    if (!validCategories.includes(attachment.category)) {
      console.warn(`Invalid attachment category: ${attachment.category}`);
    }
    
    // Validate file size
    if (attachment.fileSize > maxFileSize) {
      console.warn(`Attachment exceeds max size: ${attachment.fileSize} bytes (max: ${maxFileSize})`);
    }
    
    // Validate required fields
    if (!attachment.id || !attachment.key || !attachment.fileName) {
      console.error('Attachment missing required fields:', { id: attachment.id, key: attachment.key, fileName: attachment.fileName });
    } else {
      sanitizedAttachment = {
        id: attachment.id,
        key: attachment.key,
        fileName: attachment.fileName,
        contentType: attachment.contentType || 'application/octet-stream',
        fileSize: attachment.fileSize || 0,
        category: attachment.category || 'document',
      };
    }
  }

  const message: {
    id: string;
    conversationId: string;
    senderId: string;
    sender: { id?: string; username?: string; preferredLanguage?: string };
    type: string;
    originalContent: string;
    originalLanguage: string;
    status: string;
    timestamp: string;
    createdAt: string;
    translations: Record<string, string>;
    attachment: Attachment | null;
  } = {
    id: messageId,
    conversationId,
    senderId,
    sender: {
      id: sender?.id,
      username: sender?.username,
      preferredLanguage: sender?.preferredLanguage,
    },
    type,
    originalContent: content || '',
    originalLanguage,
    status: 'sent',
    timestamp,
    createdAt: timestamp, // Frontend expects createdAt
    translations: {}, // Cache translations here
    attachment: sanitizedAttachment,
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
    FilterExpression: 'conversationId = :convId',
    ExpressionAttributeValues: {
      ':userId': senderId,
      ':convId': conversationId,
    },
  }));

  const conversation = convResult.Items?.[0];
  if (!conversation) return;

  // Get all participants
  const participantIds = conversation.participantIds as string[];

  // Update lastMessage for all participants' conversation records
  for (const participantId of participantIds) {
    // Find the participant's conversation record
    const participantConvResult = await dynamodb.send(new QueryCommand({
      TableName: Tables.CONVERSATIONS,
      IndexName: 'user-conversations-index',
      KeyConditionExpression: 'visibleTo = :userId',
      FilterExpression: 'conversationId = :convId',
      ExpressionAttributeValues: {
        ':userId': participantId,
        ':convId': conversationId,
      },
    }));

    const participantConv = participantConvResult.Items?.[0];
    if (participantConv) {
      await dynamodb.send(new UpdateCommand({
        TableName: Tables.CONVERSATIONS,
        Key: { id: participantConv.id },
        UpdateExpression: 'SET lastMessage = :lastMessage, updatedAt = :updatedAt',
        ExpressionAttributeValues: {
          ':lastMessage': {
            id: message.id,
            conversationId: message.conversationId,
            senderId: message.senderId,
            sender: message.sender,
            type: message.type,
            originalContent: message.originalContent,
            originalLanguage: message.originalLanguage,
            status: message.status,
            createdAt: message.createdAt,
            attachment: message.attachment,
          },
          ':updatedAt': timestamp,
        },
      }));
    }
  }

  // Get connections for all participants
  const api = getApiClient(event);

  for (const participantId of participantIds) {
    // Get participant's preferred language
    const participantResult = await dynamodb.send(new GetCommand({
      TableName: Tables.USERS,
      Key: { id: participantId },
    }));
    const targetLanguage = participantResult.Item?.preferredLanguage || 'en';
    const targetCountry = participantResult.Item?.preferredCountry || 'US';

    // Translate based on message type
    let translatedContent = content || '';
    
    if (originalLanguage !== targetLanguage && content) {
      switch (type) {
        case 'text':
          // Regular text message - always translate
          console.log(`🌐 Translating text message: ${originalLanguage} -> ${targetLanguage}`);
          translatedContent = await translate(content, originalLanguage, targetLanguage, targetCountry);
          message.translations[targetLanguage] = translatedContent;
          break;
          
        case 'file':
          // Document - translate only if user requested
          if (translateDocument) {
            console.log(`📄 Translating document content: ${originalLanguage} -> ${targetLanguage}`);
            translatedContent = await translateDocumentContent(content, originalLanguage, targetLanguage, targetCountry);
            message.translations[targetLanguage] = translatedContent;
          } else {
            console.log(`📄 Skipping document translation (translateDocument: false)`);
          }
          break;
          
        case 'image':
        case 'gif':
          // Images and GIFs - never translate
          console.log(`🖼️ Skipping translation for ${type} message`);
          // translatedContent stays as original content
          break;
          
        case 'video':
        case 'voice':
        case 'audio':
          // Media types - don't translate
          console.log(`🎬 Skipping translation for ${type} message`);
          break;
          
        default:
          // Unknown type - don't translate
          console.log(`❓ Unknown message type: ${type}, skipping translation`);
          break;
      }
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

  // Send push notifications to offline users
  for (const participantId of participantIds) {
    if (participantId === senderId) continue;
    
    // Check if user has any active WebSocket connections
    const connections = await dynamodb.send(new QueryCommand({
      TableName: Tables.CONNECTIONS,
      IndexName: 'user-connections-index',
      KeyConditionExpression: 'userId = :userId',
      ExpressionAttributeValues: { ':userId': participantId }
    }));

    // If no active connections, send push notification
    if (!connections.Items?.length) {
      const notificationBody = type === 'text' 
        ? truncateForNotification(content || '') 
        : `Sent ${type}`;
      
      await sendPushNotification({
        userId: participantId,
        title: sender?.username || 'New Message',
        body: notificationBody,
        data: {
          conversationId,
          messageId: message.id,
          type: 'new_message'
        }
      });
    }
  }
}

async function handleTyping(event: any, userId: string, data: any) {
  const { conversationId, isTyping } = data;

  // Get conversation participants
  const convResult = await dynamodb.send(new QueryCommand({
    TableName: Tables.CONVERSATIONS,
    IndexName: 'user-conversations-index',
    KeyConditionExpression: 'visibleTo = :userId',
    FilterExpression: 'conversationId = :convId',
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

async function handleReaction(event: any, userId: string, data: any) {
  const { conversationId, messageId, messageTimestamp, emoji } = data;

  // Get the message to update reactions
  const messageResult = await dynamodb.send(new GetCommand({
    TableName: Tables.MESSAGES,
    Key: { conversationId, timestamp: messageTimestamp },
  }));

  if (!messageResult.Item) {
    console.error('Message not found for reaction');
    return;
  }

  // Get current reactions or initialize
  const currentReactions = messageResult.Item.reactions || {};
  
  // Toggle reaction: if user already reacted with this emoji, remove it; otherwise add it
  const userReactions = currentReactions[emoji] || [];
  const userIndex = userReactions.indexOf(userId);
  
  if (userIndex > -1) {
    // User already reacted, remove their reaction
    userReactions.splice(userIndex, 1);
    if (userReactions.length === 0) {
      delete currentReactions[emoji];
    } else {
      currentReactions[emoji] = userReactions;
    }
  } else {
    // Add user's reaction
    currentReactions[emoji] = [...userReactions, userId];
  }

  // Update message in database
  await dynamodb.send(new UpdateCommand({
    TableName: Tables.MESSAGES,
    Key: { conversationId, timestamp: messageTimestamp },
    UpdateExpression: 'SET reactions = :reactions',
    ExpressionAttributeValues: { ':reactions': currentReactions },
  }));

  // Get conversation participants
  const convResult = await dynamodb.send(new QueryCommand({
    TableName: Tables.CONVERSATIONS,
    IndexName: 'user-conversations-index',
    KeyConditionExpression: 'visibleTo = :userId',
    FilterExpression: 'conversationId = :convId',
    ExpressionAttributeValues: {
      ':userId': userId,
      ':convId': conversationId,
    },
  }));

  const conversation = convResult.Items?.[0];
  if (!conversation) return;

  const api = getApiClient(event);
  const participantIds = conversation.participantIds as string[];

  // Broadcast reaction update to all participants
  for (const participantId of participantIds) {
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
            action: 'message:reaction',
            conversationId,
            messageId,
            messageTimestamp,
            reactions: currentReactions,
            userId, // Who reacted
            emoji,
          }),
        }));
      } catch (err: any) {
        if (err.statusCode === 410) {
          await dynamodb.send(new DeleteCommand({
            TableName: Tables.CONNECTIONS,
            Key: { connectionId: conn.connectionId },
          }));
        }
      }
    }
  }
}

/**
 * Handle read receipt - marks messages as read and broadcasts to sender
 */
async function handleReadReceipt(event: any, userId: string, data: any) {
  const { conversationId, messageId, messageTimestamp } = data;

  if (!conversationId || !messageTimestamp) {
    console.error('Read receipt missing required fields');
    return;
  }

  // Update message readBy array
  try {
    await dynamodb.send(new UpdateCommand({
      TableName: Tables.MESSAGES,
      Key: { conversationId, timestamp: messageTimestamp },
      UpdateExpression: 'SET #readBy = list_append(if_not_exists(#readBy, :empty), :userIdList)',
      ExpressionAttributeNames: {
        '#readBy': 'readBy',
      },
      ExpressionAttributeValues: {
        ':empty': [],
        ':userIdList': [userId],
        ':userId': userId,
      },
      ConditionExpression: 'NOT contains(if_not_exists(#readBy, :empty), :userId)',
    }));
  } catch (err: any) {
    // Ignore condition check failures (user already in readBy)
    if (err.name !== 'ConditionalCheckFailedException') {
      console.error('Error updating readBy:', err);
    }
  }

  // Get conversation to find the message sender
  const convResult = await dynamodb.send(new QueryCommand({
    TableName: Tables.CONVERSATIONS,
    IndexName: 'user-conversations-index',
    KeyConditionExpression: 'visibleTo = :userId',
    FilterExpression: 'conversationId = :convId',
    ExpressionAttributeValues: {
      ':userId': userId,
      ':convId': conversationId,
    },
  }));

  const conversation = convResult.Items?.[0];
  if (!conversation) return;

  // Get the message to find the sender
  const messageResult = await dynamodb.send(new GetCommand({
    TableName: Tables.MESSAGES,
    Key: { conversationId, timestamp: messageTimestamp },
  }));

  const originalMessage = messageResult.Item;
  if (!originalMessage || originalMessage.senderId === userId) return;

  const api = getApiClient(event);

  // Broadcast read receipt to message sender
  const senderConnections = await dynamodb.send(new QueryCommand({
    TableName: Tables.CONNECTIONS,
    IndexName: 'user-connections-index',
    KeyConditionExpression: 'userId = :userId',
    ExpressionAttributeValues: { ':userId': originalMessage.senderId },
  }));

  for (const conn of senderConnections.Items || []) {
    try {
      await api.send(new PostToConnectionCommand({
        ConnectionId: conn.connectionId,
        Data: JSON.stringify({
          action: 'message:read',
          conversationId,
          messageId: messageId || originalMessage.id,
          messageTimestamp,
          readBy: userId,
          readAt: new Date().toISOString(),
        }),
      }));
    } catch (err: any) {
      if (err.statusCode === 410) {
        await dynamodb.send(new DeleteCommand({
          TableName: Tables.CONNECTIONS,
          Key: { connectionId: conn.connectionId },
        }));
      }
    }
  }

  console.log(`✅ Read receipt: user ${userId} read message in ${conversationId}`);
}

