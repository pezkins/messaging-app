import type { APIGatewayProxyHandler } from 'aws-lambda';
import { S3Client, DeleteObjectCommand } from '@aws-sdk/client-s3';
import { dynamodb, Tables, QueryCommand, GetCommand, UpdateCommand } from '../lib/dynamo';
import { getUserIdFromEvent, response } from '../lib/auth';

const s3 = new S3Client({ region: process.env.AWS_REGION });
const BUCKET = process.env.ATTACHMENTS_BUCKET!;

export const list: APIGatewayProxyHandler = async (event) => {
  try {
    const userId = getUserIdFromEvent(event);
    if (!userId) {
      return response(401, { message: 'Authentication required' });
    }

    const conversationId = event.pathParameters?.conversationId;
    if (!conversationId) {
      return response(400, { message: 'Conversation ID required' });
    }

    // Verify user is participant
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

    if (!convResult.Items || convResult.Items.length === 0) {
      return response(404, { message: 'Conversation not found' });
    }

    // Get user's preferred language
    const userResult = await dynamodb.send(new GetCommand({
      TableName: Tables.USERS,
      Key: { id: userId },
    }));
    const targetLanguage = userResult.Item?.preferredLanguage || 'en';

    // Get messages
    const limit = parseInt(event.queryStringParameters?.limit || '50');
    const cursor = event.queryStringParameters?.cursor;

    const messagesResult = await dynamodb.send(new QueryCommand({
      TableName: Tables.MESSAGES,
      KeyConditionExpression: 'conversationId = :convId',
      ExpressionAttributeValues: { ':convId': conversationId },
      ScanIndexForward: false, // Newest first
      Limit: limit + 1,
      ...(cursor && { ExclusiveStartKey: { conversationId, timestamp: cursor } }),
    }));

    const allItems = messagesResult.Items || [];
    const hasMore = allItems.length > limit;
    const rawItems = hasMore ? allItems.slice(0, -1) : allItems;

    // Filter out messages deleted by this user ("delete for me")
    const items = rawItems.filter(msg => {
      const deletedBy = msg.deletedBy || [];
      return !deletedBy.includes(userId);
    });

    // Collect unique sender IDs to batch fetch
    const senderIds = [...new Set(items.map(msg => msg.senderId))];
    
    // Batch fetch sender data for avatars
    const senderMap = new Map<string, any>();
    await Promise.all(
      senderIds.map(async (senderId) => {
        const senderResult = await dynamodb.send(new GetCommand({
          TableName: Tables.USERS,
          Key: { id: senderId },
        }));
        if (senderResult.Item) {
          senderMap.set(senderId, senderResult.Item);
        }
      })
    );

    // Translate messages and enrich with current sender data
    const messages = await Promise.all(
      items.map(async (msg) => {
        // Get current sender info for fresh avatar URL
        const currentSender = senderMap.get(msg.senderId);

        // Handle "deleted for everyone" messages
        if (msg.deletedAt && msg.deletedForEveryone) {
          return {
            id: msg.id,
            conversationId: msg.conversationId,
            senderId: msg.senderId,
            sender: {
              id: msg.senderId,
              username: currentSender?.username || msg.sender?.username,
              preferredLanguage: currentSender?.preferredLanguage || msg.sender?.preferredLanguage,
              avatarUrl: currentSender?.avatarUrl || null,
              profilePicture: currentSender?.profilePicture || null,
            },
            type: 'deleted',
            originalContent: 'This message was deleted',
            originalLanguage: msg.originalLanguage,
            translatedContent: 'This message was deleted',
            targetLanguage,
            status: msg.status,
            createdAt: msg.timestamp,
            deletedAt: msg.deletedAt,
            reactions: {},
            attachment: null,
            replyTo: null,
          };
        }

        let translatedContent = msg.originalContent;

        if (msg.originalLanguage !== targetLanguage) {
          // Only use cached translations for message history
          // Don't call AI to re-translate old messages when user changes language
          if (msg.translations?.[targetLanguage]) {
            // Use cached translation for current target language
            translatedContent = msg.translations[targetLanguage];
          } else {
            // No cached translation for this language - show original content
            // New messages will be translated in real-time via WebSocket
            translatedContent = msg.originalContent;
          }
        }

        return {
          id: msg.id,
          conversationId: msg.conversationId,
          senderId: msg.senderId,
          sender: {
            id: msg.senderId,
            username: currentSender?.username || msg.sender?.username,
            preferredLanguage: currentSender?.preferredLanguage || msg.sender?.preferredLanguage,
            avatarUrl: currentSender?.avatarUrl || null,
            profilePicture: currentSender?.profilePicture || null,
          },
          type: msg.type,
          originalContent: msg.originalContent,
          originalLanguage: msg.originalLanguage,
          translatedContent,
          targetLanguage,
          status: msg.status,
          createdAt: msg.timestamp,
          reactions: msg.reactions || {},
          attachment: msg.attachment || null,
          replyTo: msg.replyTo || null,
        };
      })
    );

    return response(200, {
      messages: messages.reverse(), // Chronological order
      hasMore,
      nextCursor: hasMore && items.length > 0 ? items[items.length - 1].timestamp : null,
    });
  } catch (error) {
    console.error('List messages error:', error);
    return response(500, { message: 'Internal server error' });
  }
};

/**
 * Delete a message
 * - forEveryone: true = soft delete for all users (content hidden)
 * - forEveryone: false = delete for current user only
 */
export const deleteMessage: APIGatewayProxyHandler = async (event) => {
  try {
    const userId = getUserIdFromEvent(event);
    if (!userId) {
      return response(401, { message: 'Authentication required' });
    }

    const conversationId = event.pathParameters?.conversationId;
    const messageId = event.pathParameters?.messageId;

    if (!conversationId || !messageId) {
      return response(400, { message: 'Conversation ID and Message ID required' });
    }

    // Parse delete options from body OR query params (iOS uses query params)
    const body = JSON.parse(event.body || '{}');
    const queryParams = event.queryStringParameters || {};
    const forEveryone = body.forEveryone === true || queryParams.forEveryone === 'true';

    // Verify user is participant of the conversation
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

    if (!convResult.Items || convResult.Items.length === 0) {
      return response(404, { message: 'Conversation not found' });
    }

    const conversation = convResult.Items[0];

    // Find the message by scanning for the messageId
    const messagesResult = await dynamodb.send(new QueryCommand({
      TableName: Tables.MESSAGES,
      KeyConditionExpression: 'conversationId = :convId',
      FilterExpression: 'id = :msgId',
      ExpressionAttributeValues: {
        ':convId': conversationId,
        ':msgId': messageId,
      },
    }));

    const message = messagesResult.Items?.[0];
    if (!message) {
      return response(404, { message: 'Message not found' });
    }

    const now = new Date().toISOString();

    if (forEveryone) {
      // Only the sender can delete for everyone
      if (message.senderId !== userId) {
        return response(403, { message: 'Only the sender can delete a message for everyone' });
      }

      // Delete attachment from S3 if exists
      if (message.attachment?.key) {
        try {
          await s3.send(new DeleteObjectCommand({
            Bucket: BUCKET,
            Key: message.attachment.key,
          }));
          console.log(`🗑️ Deleted attachment: ${message.attachment.key}`);
        } catch (s3Error) {
          console.warn('Failed to delete attachment from S3:', s3Error);
        }
      }

      // Soft delete for everyone - mark message as deleted but keep record
      await dynamodb.send(new UpdateCommand({
        TableName: Tables.MESSAGES,
        Key: { conversationId, timestamp: message.timestamp },
        UpdateExpression: 'SET deletedAt = :now, deletedForEveryone = :forEveryone, deletedBy = :deletedBy',
        ExpressionAttributeValues: {
          ':now': now,
          ':forEveryone': true,
          ':deletedBy': [userId],
        },
      }));

      console.log(`🗑️ Message ${messageId} deleted for everyone by ${userId}`);

      return response(200, {
        success: true,
        messageId,
        conversationId,
        deletedAt: now,
        deletedForEveryone: true,
        participantIds: conversation.participantIds,
      });
    } else {
      // Delete for current user only - add user to deletedBy array
      const currentDeletedBy = message.deletedBy || [];
      if (!currentDeletedBy.includes(userId)) {
        currentDeletedBy.push(userId);
      }

      await dynamodb.send(new UpdateCommand({
        TableName: Tables.MESSAGES,
        Key: { conversationId, timestamp: message.timestamp },
        UpdateExpression: 'SET deletedBy = :deletedBy',
        ExpressionAttributeValues: {
          ':deletedBy': currentDeletedBy,
        },
      }));

      console.log(`🗑️ Message ${messageId} deleted for user ${userId}`);

      return response(200, {
        success: true,
        messageId,
        conversationId,
        deletedForMe: true,
      });
    }
  } catch (error) {
    console.error('Delete message error:', error);
    return response(500, { message: 'Internal server error' });
  }
};

