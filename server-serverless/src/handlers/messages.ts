import type { APIGatewayProxyHandler } from 'aws-lambda';
import { dynamodb, Tables, QueryCommand, GetCommand, UpdateCommand } from '../lib/dynamo';
import { getUserIdFromEvent, response } from '../lib/auth';
import { translate } from '../lib/translation';

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
    const targetCountry = userResult.Item?.preferredCountry || 'US';

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

    const hasMore = (messagesResult.Items?.length || 0) > limit;
    const items = hasMore 
      ? messagesResult.Items?.slice(0, -1) 
      : messagesResult.Items || [];

    // Translate messages
    const messages = await Promise.all(
      items.map(async (msg) => {
        let translatedContent = msg.originalContent;

        if (msg.originalLanguage !== targetLanguage) {
          // Check cache first (stored in message)
          if (msg.translations?.[targetLanguage]) {
            translatedContent = msg.translations[targetLanguage];
          } else {
            // Translate and cache it
            translatedContent = await translate(
              msg.originalContent,
              msg.originalLanguage,
              targetLanguage,
              targetCountry
            );

            // Save translation to database for future use (fire and forget)
            dynamodb.send(new UpdateCommand({
              TableName: Tables.MESSAGES,
              Key: { conversationId: msg.conversationId, timestamp: msg.timestamp },
              UpdateExpression: 'SET translations.#lang = :translation',
              ExpressionAttributeNames: { '#lang': targetLanguage },
              ExpressionAttributeValues: { ':translation': translatedContent },
            })).catch(err => console.error('Failed to cache translation:', err));
          }
        }

        return {
          id: msg.id,
          conversationId: msg.conversationId,
          senderId: msg.senderId,
          sender: msg.sender,
          type: msg.type,
          originalContent: msg.originalContent,
          originalLanguage: msg.originalLanguage,
          translatedContent,
          targetLanguage,
          status: msg.status,
          createdAt: msg.timestamp,
        };
      })
    );

    return response(200, {
      messages: messages.reverse(), // Chronological order
      hasMore,
      nextCursor: hasMore ? items[items.length - 1]?.timestamp : null,
    });
  } catch (error) {
    console.error('List messages error:', error);
    return response(500, { message: 'Internal server error' });
  }
};

