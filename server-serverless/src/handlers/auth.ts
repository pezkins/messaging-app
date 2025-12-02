import type { APIGatewayProxyHandler } from 'aws-lambda';
import bcrypt from 'bcryptjs';
import { v4 as uuid } from 'uuid';
import { z } from 'zod';
import { dynamodb, Tables, GetCommand, PutCommand, QueryCommand } from '../lib/dynamo';
import { generateToken, getUserIdFromEvent, response } from '../lib/auth';

const registerSchema = z.object({
  email: z.string().email(),
  username: z.string().min(3).max(30),
  password: z.string().min(6),
  preferredLanguage: z.string().min(2).max(5).default('en'),
  preferredCountry: z.string().length(2).default('US'),
});

const loginSchema = z.object({
  email: z.string().email(),
  password: z.string(),
});

const oauthSchema = z.object({
  provider: z.enum(['google', 'apple']),
  providerId: z.string().min(1),
  email: z.string().email(),
  name: z.string().nullable(),
  avatarUrl: z.string().nullable(),
});

export const register: APIGatewayProxyHandler = async (event) => {
  try {
    const body = JSON.parse(event.body || '{}');
    const data = registerSchema.parse(body);

    // Check if email exists
    const emailCheck = await dynamodb.send(new QueryCommand({
      TableName: Tables.USERS,
      IndexName: 'email-index',
      KeyConditionExpression: 'email = :email',
      ExpressionAttributeValues: { ':email': data.email },
    }));

    if (emailCheck.Items && emailCheck.Items.length > 0) {
      return response(400, { message: 'Email already registered', code: 'EMAIL_EXISTS' });
    }

    // Note: Username uniqueness not enforced (would need GSI)
    // For now, usernames can be duplicated - email is the unique identifier

    // Create user
    const userId = uuid();
    const passwordHash = await bcrypt.hash(data.password, 10);
    const now = new Date().toISOString();

    const user = {
      id: userId,
      email: data.email,
      username: data.username,
      passwordHash,
      preferredLanguage: data.preferredLanguage,
      preferredCountry: data.preferredCountry,
      createdAt: now,
      updatedAt: now,
    };

    await dynamodb.send(new PutCommand({
      TableName: Tables.USERS,
      Item: user,
    }));

    const token = generateToken(userId);

    return response(201, {
      user: {
        id: user.id,
        email: user.email,
        username: user.username,
        preferredLanguage: user.preferredLanguage,
        preferredCountry: user.preferredCountry,
        createdAt: user.createdAt,
      },
      accessToken: token,
      refreshToken: token, // Simplified for serverless
    });
  } catch (error) {
    if (error instanceof z.ZodError) {
      return response(400, { message: 'Validation error', details: error.errors });
    }
    console.error('Register error:', error);
    return response(500, { message: 'Internal server error' });
  }
};

export const login: APIGatewayProxyHandler = async (event) => {
  try {
    const body = JSON.parse(event.body || '{}');
    const data = loginSchema.parse(body);

    // Find user by email
    const result = await dynamodb.send(new QueryCommand({
      TableName: Tables.USERS,
      IndexName: 'email-index',
      KeyConditionExpression: 'email = :email',
      ExpressionAttributeValues: { ':email': data.email },
    }));

    const user = result.Items?.[0];
    if (!user) {
      return response(401, { message: 'Invalid credentials', code: 'INVALID_CREDENTIALS' });
    }

    // Verify password
    const validPassword = await bcrypt.compare(data.password, user.passwordHash);
    if (!validPassword) {
      return response(401, { message: 'Invalid credentials', code: 'INVALID_CREDENTIALS' });
    }

    const token = generateToken(user.id);

    return response(200, {
      user: {
        id: user.id,
        email: user.email,
        username: user.username,
        preferredLanguage: user.preferredLanguage,
        preferredCountry: user.preferredCountry,
        createdAt: user.createdAt,
      },
      accessToken: token,
      refreshToken: token,
    });
  } catch (error) {
    if (error instanceof z.ZodError) {
      return response(400, { message: 'Validation error', details: error.errors });
    }
    console.error('Login error:', error);
    return response(500, { message: 'Internal server error' });
  }
};

export const oauth: APIGatewayProxyHandler = async (event) => {
  try {
    const body = JSON.parse(event.body || '{}');
    const data = oauthSchema.parse(body);

    // Check if user exists with this OAuth provider or email
    const emailCheck = await dynamodb.send(new QueryCommand({
      TableName: Tables.USERS,
      IndexName: 'email-index',
      KeyConditionExpression: 'email = :email',
      ExpressionAttributeValues: { ':email': data.email },
    }));

    let user = emailCheck.Items?.[0];

    if (user) {
      // Update OAuth info if user logged in with email before
      if (!user.oauthProvider) {
        await dynamodb.send(new PutCommand({
          TableName: Tables.USERS,
          Item: {
            ...user,
            oauthProvider: data.provider,
            oauthProviderId: data.providerId,
            avatarUrl: data.avatarUrl || user.avatarUrl,
            updatedAt: new Date().toISOString(),
          },
        }));
      }
    } else {
      // Create new user
      // Generate username from name or email
      const baseUsername = data.name?.replace(/\s+/g, '').toLowerCase() || 
                          data.email.split('@')[0];
      // Add random suffix to avoid collisions
      const username = `${baseUsername}${Math.floor(Math.random() * 1000)}`;

      const now = new Date().toISOString();
      user = {
        id: uuid(),
        email: data.email,
        username,
        passwordHash: '', // No password for OAuth users
        preferredLanguage: 'en',
        preferredCountry: 'US',
        avatarUrl: data.avatarUrl,
        oauthProvider: data.provider,
        oauthProviderId: data.providerId,
        createdAt: now,
        updatedAt: now,
      };

      await dynamodb.send(new PutCommand({
        TableName: Tables.USERS,
        Item: user,
      }));
    }

    const token = generateToken(user.id);

    return response(200, {
      user: {
        id: user.id,
        email: user.email,
        username: user.username,
        preferredLanguage: user.preferredLanguage,
        preferredCountry: user.preferredCountry,
        avatarUrl: user.avatarUrl,
        createdAt: user.createdAt,
      },
      accessToken: token,
      refreshToken: token,
    });
  } catch (error) {
    if (error instanceof z.ZodError) {
      return response(400, { message: 'Validation error', details: error.errors });
    }
    console.error('OAuth error:', error);
    return response(500, { message: 'Internal server error' });
  }
};

export const getMe: APIGatewayProxyHandler = async (event) => {
  try {
    const userId = getUserIdFromEvent(event);
    if (!userId) {
      return response(401, { message: 'Authentication required' });
    }

    const result = await dynamodb.send(new GetCommand({
      TableName: Tables.USERS,
      Key: { id: userId },
    }));

    if (!result.Item) {
      return response(404, { message: 'User not found' });
    }

    return response(200, {
      user: {
        id: result.Item.id,
        email: result.Item.email,
        username: result.Item.username,
        preferredLanguage: result.Item.preferredLanguage,
        preferredCountry: result.Item.preferredCountry,
        createdAt: result.Item.createdAt,
      },
    });
  } catch (error) {
    console.error('GetMe error:', error);
    return response(500, { message: 'Internal server error' });
  }
};

