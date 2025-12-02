import { APIGatewayProxyHandler } from 'aws-lambda';
import { QueryCommand, ScanCommand, UpdateCommand } from '@aws-sdk/lib-dynamodb';
import { z } from 'zod';
import { dynamodb, Tables } from '../lib/dynamo';
import { verifyToken } from '../lib/auth';

const response = (statusCode: number, body: object) => ({
  statusCode,
  headers: {
    'Content-Type': 'application/json',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'Content-Type,Authorization',
  },
  body: JSON.stringify(body),
});

// Search users by username or email
export const search: APIGatewayProxyHandler = async (event) => {
  try {
    // Verify auth
    const authHeader = event.headers.authorization || event.headers.Authorization;
    if (!authHeader?.startsWith('Bearer ')) {
      return response(401, { message: 'Unauthorized' });
    }
    const userId = verifyToken(authHeader.split(' ')[1]);
    if (!userId) {
      return response(401, { message: 'Invalid token' });
    }

    const query = event.queryStringParameters?.q || '';
    
    if (!query || query.length < 2) {
      return response(200, { users: [] });
    }

    // Scan users table for matching usernames or emails
    // Note: In production, consider using a search service like OpenSearch
    const result = await dynamodb.send(new ScanCommand({
      TableName: Tables.USERS,
      FilterExpression: '(contains(#username, :query) OR contains(#email, :query)) AND #id <> :userId',
      ExpressionAttributeNames: {
        '#username': 'username',
        '#email': 'email',
        '#id': 'id',
      },
      ExpressionAttributeValues: {
        ':query': query.toLowerCase(),
        ':userId': userId,
      },
      Limit: 20,
    }));

    const users = (result.Items || []).map((user) => ({
      id: user.id,
      username: user.username,
      email: user.email,
      avatarUrl: user.avatarUrl,
      preferredLanguage: user.preferredLanguage,
    }));

    return response(200, { users });
  } catch (error) {
    console.error('Search users error:', error);
    return response(500, { message: 'Internal server error' });
  }
};

// Update user's preferred language
export const updateLanguage: APIGatewayProxyHandler = async (event) => {
  try {
    // Verify auth
    const authHeader = event.headers.authorization || event.headers.Authorization;
    if (!authHeader?.startsWith('Bearer ')) {
      return response(401, { message: 'Unauthorized' });
    }
    const userId = verifyToken(authHeader.split(' ')[1]);
    if (!userId) {
      return response(401, { message: 'Invalid token' });
    }

    const body = JSON.parse(event.body || '{}');
    const { preferredLanguage } = z.object({
      preferredLanguage: z.string().min(2).max(10),
    }).parse(body);

    // Update user's preferred language
    await dynamodb.send(new UpdateCommand({
      TableName: Tables.USERS,
      Key: { id: userId },
      UpdateExpression: 'SET preferredLanguage = :lang, updatedAt = :now',
      ExpressionAttributeValues: {
        ':lang': preferredLanguage,
        ':now': new Date().toISOString(),
      },
    }));

    return response(200, { 
      message: 'Language updated successfully',
      preferredLanguage,
    });
  } catch (error) {
    if (error instanceof z.ZodError) {
      return response(400, { message: 'Validation error', details: error.errors });
    }
    console.error('Update language error:', error);
    return response(500, { message: 'Internal server error' });
  }
};

