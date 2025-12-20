import type { APIGatewayProxyHandler } from 'aws-lambda';
import { v4 as uuid } from 'uuid';
import { z } from 'zod';
import { dynamodb, Tables, GetCommand, PutCommand, QueryCommand, UpdateCommand, DeleteCommand } from '../lib/dynamo';
import { getUserIdFromEvent, response } from '../lib/auth';
import { translate } from '../lib/translation';

const createSchema = z.object({
  participantIds: z.array(z.string()).min(1),
  type: z.enum(['direct', 'group']),
  name: z.string().optional(),
});

export const list: APIGatewayProxyHandler = async (event) => {
  try {
    const userId = getUserIdFromEvent(event);
    if (!userId) {
      return response(401, { message: 'Authentication required' });
    }

    // Get current user's language preferences for translating lastMessage
    const currentUser = await dynamodb.send(new GetCommand({
      TableName: Tables.USERS,
      Key: { id: userId },
    }));
    const userLanguage = currentUser.Item?.preferredLanguage || 'en';
    const userCountry = currentUser.Item?.preferredCountry;
    const userRegion = currentUser.Item?.preferredRegion;

    // Query conversations where user is a participant
    const result = await dynamodb.send(new QueryCommand({
      TableName: Tables.CONVERSATIONS,
      IndexName: 'user-conversations-index',
      KeyConditionExpression: 'visibleTo = :userId',
      ExpressionAttributeValues: { ':userId': userId },
    }));

    const conversations = await Promise.all(
      (result.Items || []).map(async (conv) => {
        // Get participant details
        const participants = await Promise.all(
          conv.participantIds.map(async (pid: string) => {
            const user = await dynamodb.send(new GetCommand({
              TableName: Tables.USERS,
              Key: { id: pid },
            }));
            return user.Item ? {
              id: user.Item.id,
              username: user.Item.username,
              preferredLanguage: user.Item.preferredLanguage,
              avatarUrl: user.Item.avatarUrl || null,
              profilePicture: user.Item.profilePicture || null,
            } : null;
          })
        );

        // Translate lastMessage content for the current user
        let lastMessage = conv.lastMessage;
        if (lastMessage && lastMessage.originalContent && lastMessage.type === 'text') {
          const originalLang = lastMessage.originalLanguage || 'en';
          if (originalLang !== userLanguage) {
            try {
              const translatedContent = await translate(
                lastMessage.originalContent,
                originalLang,
                userLanguage,
                userCountry,
                userRegion
              );
              lastMessage = {
                ...lastMessage,
                translatedContent,
                targetLanguage: userLanguage,
              };
            } catch (err) {
              console.error('Failed to translate lastMessage:', err);
              // Keep original content if translation fails
            }
          }
        }

        return {
          id: conv.conversationId || conv.id, // Use conversationId if available
          type: conv.type,
          name: conv.name,
          participants: participants.filter(Boolean),
          lastMessage,
          createdAt: conv.createdAt,
          updatedAt: conv.updatedAt,
        };
      })
    );

    // Sort by updatedAt
    conversations.sort((a, b) => 
      new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime()
    );

    return response(200, { conversations });
  } catch (error) {
    console.error('List conversations error:', error);
    return response(500, { message: 'Internal server error' });
  }
};

export const create: APIGatewayProxyHandler = async (event) => {
  try {
    const userId = getUserIdFromEvent(event);
    if (!userId) {
      return response(401, { message: 'Authentication required' });
    }

    const body = JSON.parse(event.body || '{}');
    const data = createSchema.parse(body);

    const allParticipantIds = [...new Set([userId, ...data.participantIds])];

    // For direct chats, check if conversation already exists
    if (data.type === 'direct' && allParticipantIds.length === 2) {
      const existing = await dynamodb.send(new QueryCommand({
        TableName: Tables.CONVERSATIONS,
        IndexName: 'user-conversations-index',
        KeyConditionExpression: 'visibleTo = :userId',
        ExpressionAttributeValues: { ':userId': userId },
      }));

      const found = existing.Items?.find(conv => 
        conv.type === 'direct' &&
        conv.participantIds.length === 2 &&
        conv.participantIds.includes(allParticipantIds[0]) &&
        conv.participantIds.includes(allParticipantIds[1])
      );

      if (found) {
        return response(200, { 
          conversation: {
            ...found,
            id: found.conversationId || found.id,
          }
        });
      }
    }

    // Create new conversation
    const conversationId = uuid();
    const now = new Date().toISOString();

    // Store one record per participant (for querying via GSI)
    for (const participantId of allParticipantIds) {
      await dynamodb.send(new PutCommand({
        TableName: Tables.CONVERSATIONS,
        Item: {
          id: `${conversationId}#${participantId}`, // Unique primary key
          visibleTo: participantId,
          sortKey: now, // For GSI range key
          conversationId: conversationId, // Store original ID
          type: data.type,
          name: data.name,
          participantIds: allParticipantIds,
          createdAt: now,
          updatedAt: now,
        },
      }));
    }

    // Get participant details
    const participants = await Promise.all(
      allParticipantIds.map(async (pid) => {
        const user = await dynamodb.send(new GetCommand({
          TableName: Tables.USERS,
          Key: { id: pid },
        }));
        return user.Item ? {
          id: user.Item.id,
          username: user.Item.username,
          preferredLanguage: user.Item.preferredLanguage,
          avatarUrl: user.Item.avatarUrl || null,
          profilePicture: user.Item.profilePicture || null,
        } : null;
      })
    );

    return response(201, {
      conversation: {
        id: conversationId,
        type: data.type,
        name: data.name,
        participants: participants.filter(Boolean),
        createdAt: now,
        updatedAt: now,
      },
    });
  } catch (error) {
    if (error instanceof z.ZodError) {
      return response(400, { message: 'Validation error', details: error.errors });
    }
    console.error('Create conversation error:', error);
    return response(500, { message: 'Internal server error' });
  }
};

/**
 * Mark a conversation as read for the current user
 */
export const markAsRead: APIGatewayProxyHandler = async (event) => {
  try {
    const userId = getUserIdFromEvent(event);
    if (!userId) {
      return response(401, { message: 'Authentication required' });
    }

    const conversationId = event.pathParameters?.conversationId;
    if (!conversationId) {
      return response(400, { message: 'Conversation ID required' });
    }

    // Find user's conversation record
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
    if (!conversation) {
      return response(404, { message: 'Conversation not found' });
    }

    // Update user's conversation record with read timestamp
    await dynamodb.send(new UpdateCommand({
      TableName: Tables.CONVERSATIONS,
      Key: { id: conversation.id },
      UpdateExpression: 'SET unreadCount = :zero, lastReadAt = :now',
      ExpressionAttributeValues: {
        ':zero': 0,
        ':now': new Date().toISOString(),
      },
    }));

    console.log(`✅ Marked conversation ${conversationId} as read for user ${userId}`);

    return response(200, { 
      success: true,
      conversationId,
      lastReadAt: new Date().toISOString(),
    });
  } catch (error) {
    console.error('Mark as read error:', error);
    return response(500, { message: 'Internal server error' });
  }
};

/**
 * Add participants to a group conversation
 */
export const addParticipants: APIGatewayProxyHandler = async (event) => {
  try {
    const userId = getUserIdFromEvent(event);
    if (!userId) {
      return response(401, { message: 'Authentication required' });
    }

    const conversationId = event.pathParameters?.conversationId;
    if (!conversationId) {
      return response(400, { message: 'Conversation ID required' });
    }

    const body = JSON.parse(event.body || '{}');
    const { userIds } = body;

    if (!userIds || !Array.isArray(userIds) || userIds.length === 0) {
      return response(400, { message: 'userIds array is required' });
    }

    // Find the conversation to verify it exists and user is a participant
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
    if (!conversation) {
      return response(404, { message: 'Conversation not found' });
    }

    if (conversation.type !== 'group') {
      return response(400, { message: 'Can only add participants to group conversations' });
    }

    const currentParticipantIds: string[] = conversation.participantIds || [];
    const newParticipantIds = userIds.filter((id: string) => !currentParticipantIds.includes(id));

    if (newParticipantIds.length === 0) {
      return response(400, { message: 'All users are already participants' });
    }

    const allParticipantIds = [...currentParticipantIds, ...newParticipantIds];
    const now = new Date().toISOString();

    // Update existing conversation records for current participants
    for (const participantId of currentParticipantIds) {
      await dynamodb.send(new UpdateCommand({
        TableName: Tables.CONVERSATIONS,
        Key: { id: `${conversationId}#${participantId}` },
        UpdateExpression: 'SET participantIds = :pIds, updatedAt = :now',
        ExpressionAttributeValues: {
          ':pIds': allParticipantIds,
          ':now': now,
        },
      }));
    }

    // Create new conversation records for new participants
    for (const participantId of newParticipantIds) {
      await dynamodb.send(new PutCommand({
        TableName: Tables.CONVERSATIONS,
        Item: {
          id: `${conversationId}#${participantId}`,
          visibleTo: participantId,
          sortKey: now,
          conversationId: conversationId,
          type: conversation.type,
          name: conversation.name,
          participantIds: allParticipantIds,
          createdAt: conversation.createdAt,
          updatedAt: now,
        },
      }));
    }

    // Get all participant details
    const participants = await Promise.all(
      allParticipantIds.map(async (pid: string) => {
        const user = await dynamodb.send(new GetCommand({
          TableName: Tables.USERS,
          Key: { id: pid },
        }));
        return user.Item ? {
          id: user.Item.id,
          username: user.Item.username,
          preferredLanguage: user.Item.preferredLanguage,
          avatarUrl: user.Item.avatarUrl || null,
          profilePicture: user.Item.profilePicture || null,
        } : null;
      })
    );

    console.log(`✅ Added ${newParticipantIds.length} participants to conversation ${conversationId}`);

    return response(200, {
      conversation: {
        id: conversationId,
        type: conversation.type,
        name: conversation.name,
        participants: participants.filter(Boolean),
        createdAt: conversation.createdAt,
        updatedAt: now,
      },
    });
  } catch (error) {
    console.error('Add participants error:', error);
    return response(500, { message: 'Internal server error' });
  }
};

/**
 * Remove a participant from a group conversation
 */
export const removeParticipant: APIGatewayProxyHandler = async (event) => {
  try {
    const userId = getUserIdFromEvent(event);
    if (!userId) {
      return response(401, { message: 'Authentication required' });
    }

    const conversationId = event.pathParameters?.conversationId;
    const targetUserId = event.pathParameters?.userId;

    if (!conversationId) {
      return response(400, { message: 'Conversation ID required' });
    }
    if (!targetUserId) {
      return response(400, { message: 'User ID required' });
    }

    // Find the conversation to verify it exists and user is a participant
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
    if (!conversation) {
      return response(404, { message: 'Conversation not found' });
    }

    if (conversation.type !== 'group') {
      return response(400, { message: 'Can only remove participants from group conversations' });
    }

    const currentParticipantIds: string[] = conversation.participantIds || [];

    if (!currentParticipantIds.includes(targetUserId)) {
      return response(400, { message: 'User is not a participant' });
    }

    if (currentParticipantIds.length <= 2) {
      return response(400, { message: 'Cannot remove participant: group must have at least 2 members' });
    }

    const updatedParticipantIds = currentParticipantIds.filter((id: string) => id !== targetUserId);
    const now = new Date().toISOString();

    // Delete the removed user's conversation record
    await dynamodb.send(new DeleteCommand({
      TableName: Tables.CONVERSATIONS,
      Key: { id: `${conversationId}#${targetUserId}` },
    }));

    // Update remaining participants' conversation records
    for (const participantId of updatedParticipantIds) {
      await dynamodb.send(new UpdateCommand({
        TableName: Tables.CONVERSATIONS,
        Key: { id: `${conversationId}#${participantId}` },
        UpdateExpression: 'SET participantIds = :pIds, updatedAt = :now',
        ExpressionAttributeValues: {
          ':pIds': updatedParticipantIds,
          ':now': now,
        },
      }));
    }

    // Get all remaining participant details
    const participants = await Promise.all(
      updatedParticipantIds.map(async (pid: string) => {
        const user = await dynamodb.send(new GetCommand({
          TableName: Tables.USERS,
          Key: { id: pid },
        }));
        return user.Item ? {
          id: user.Item.id,
          username: user.Item.username,
          preferredLanguage: user.Item.preferredLanguage,
          avatarUrl: user.Item.avatarUrl || null,
          profilePicture: user.Item.profilePicture || null,
        } : null;
      })
    );

    console.log(`✅ Removed user ${targetUserId} from conversation ${conversationId}`);

    return response(200, {
      conversation: {
        id: conversationId,
        type: conversation.type,
        name: conversation.name,
        participants: participants.filter(Boolean),
        createdAt: conversation.createdAt,
        updatedAt: now,
      },
    });
  } catch (error) {
    console.error('Remove participant error:', error);
    return response(500, { message: 'Internal server error' });
  }
};

/**
 * Delete a conversation for the current user (soft delete)
 * The conversation is hidden from the user's view but not deleted from other participants
 */
export const deleteConversation: APIGatewayProxyHandler = async (event) => {
  try {
    const userId = getUserIdFromEvent(event);
    if (!userId) {
      return response(401, { message: 'Authentication required' });
    }

    const conversationId = event.pathParameters?.conversationId;
    if (!conversationId) {
      return response(400, { message: 'Conversation ID required' });
    }

    // Find user's conversation record
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
    if (!conversation) {
      return response(404, { message: 'Conversation not found' });
    }

    // Option A (Soft delete): Delete user's conversation record
    // This removes the conversation from user's view but keeps it for other participants
    await dynamodb.send(new DeleteCommand({
      TableName: Tables.CONVERSATIONS,
      Key: { id: conversation.id },
    }));

    console.log(`🗑️ Deleted conversation ${conversationId} for user ${userId}`);

    return response(200, {
      success: true,
      message: 'Conversation deleted',
      conversationId,
    });
  } catch (error) {
    console.error('Delete conversation error:', error);
    return response(500, { message: 'Internal server error' });
  }
};

