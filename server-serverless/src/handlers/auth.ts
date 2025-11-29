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
  preferredLanguage: z.string().length(2).default('en'),
});

const loginSchema = z.object({
  email: z.string().email(),
  password: z.string(),
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

    // Check if username exists
    const usernameCheck = await dynamodb.send(new QueryCommand({
      TableName: Tables.USERS,
      IndexName: 'username-index',
      KeyConditionExpression: 'username = :username',
      ExpressionAttributeValues: { ':username': data.username },
    }));

    if (usernameCheck.Items && usernameCheck.Items.length > 0) {
      return response(400, { message: 'Username already taken', code: 'USERNAME_EXISTS' });
    }

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
        createdAt: result.Item.createdAt,
      },
    });
  } catch (error) {
    console.error('GetMe error:', error);
    return response(500, { message: 'Internal server error' });
  }
};

