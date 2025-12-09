import jwt from 'jsonwebtoken';
import type { APIGatewayProxyEvent } from 'aws-lambda';

function getJwtSecret(): string {
  const secret = process.env.JWT_SECRET;
  if (!secret) {
    throw new Error('JWT_SECRET environment variable is not set. Please configure it in your SAM template.');
  }
  return secret;
}

export interface JwtPayload {
  userId: string;
}

export function generateToken(userId: string): string {
  return jwt.sign({ userId }, getJwtSecret(), { expiresIn: '7d' });
}

export function verifyToken(token: string): JwtPayload {
  return jwt.verify(token, getJwtSecret()) as JwtPayload;
}

export function getUserIdFromEvent(event: APIGatewayProxyEvent): string | null {
  const authHeader = event.headers.authorization || event.headers.Authorization;
  
  if (!authHeader?.startsWith('Bearer ')) {
    return null;
  }

  try {
    const token = authHeader.slice(7);
    const payload = verifyToken(token);
    return payload.userId;
  } catch {
    return null;
  }
}

export function response(statusCode: number, body: unknown) {
  return {
    statusCode,
    headers: {
      'Content-Type': 'application/json',
      'Access-Control-Allow-Origin': '*',
    },
    body: JSON.stringify(body),
  };
}



