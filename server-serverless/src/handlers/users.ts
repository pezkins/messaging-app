import { APIGatewayProxyHandler } from 'aws-lambda';
import { QueryCommand, ScanCommand, UpdateCommand, GetCommand } from '@aws-sdk/lib-dynamodb';
import { S3Client, PutObjectCommand, DeleteObjectCommand } from '@aws-sdk/client-s3';
import { getSignedUrl } from '@aws-sdk/s3-request-presigner';
import { v4 as uuid } from 'uuid';
import { z } from 'zod';
import { dynamodb, Tables } from '../lib/dynamo';
import { verifyToken } from '../lib/auth';

const s3 = new S3Client({ region: process.env.AWS_REGION });
const BUCKET = process.env.ATTACHMENTS_BUCKET!;

// Allowed profile picture types
const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
const MAX_PROFILE_PICTURE_SIZE = 5 * 1024 * 1024; // 5MB

// Helper to get user by ID
async function getUser(userId: string) {
  const result = await dynamodb.send(new GetCommand({
    TableName: Tables.USERS,
    Key: { id: userId },
  }));
  if (!result.Item) return null;
  const { passwordHash, ...user } = result.Item;
  return user;
}

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
    
    let userId: string;
    try {
      const payload = verifyToken(authHeader.split(' ')[1]);
      userId = payload.userId;
    } catch {
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
      profilePicture: user.profilePicture || user.avatarUrl || null,
      preferredLanguage: user.preferredLanguage,
    }));

    return response(200, { users });
  } catch (error) {
    console.error('Search users error:', error);
    return response(500, { message: 'Internal server error' });
  }
};

// Update user profile (username, avatarUrl)
export const updateProfile: APIGatewayProxyHandler = async (event) => {
  try {
    // Verify auth
    const authHeader = event.headers.authorization || event.headers.Authorization;
    if (!authHeader?.startsWith('Bearer ')) {
      return response(401, { message: 'Unauthorized' });
    }
    
    let userId: string;
    try {
      const payload = verifyToken(authHeader.split(' ')[1]);
      userId = payload.userId;
    } catch {
      return response(401, { message: 'Invalid token' });
    }

    const body = JSON.parse(event.body || '{}');
    const data = z.object({
      username: z.string().min(3).max(30).optional(),
      avatarUrl: z.string().url().optional(),
    }).parse(body);

    const updates: string[] = [];
    const expressionValues: Record<string, any> = {
      ':now': new Date().toISOString(),
    };

    if (data.username) {
      updates.push('username = :username');
      expressionValues[':username'] = data.username;
    }
    if (data.avatarUrl) {
      updates.push('avatarUrl = :avatarUrl');
      expressionValues[':avatarUrl'] = data.avatarUrl;
    }
    updates.push('updatedAt = :now');

    await dynamodb.send(new UpdateCommand({
      TableName: Tables.USERS,
      Key: { id: userId },
      UpdateExpression: `SET ${updates.join(', ')}`,
      ExpressionAttributeValues: expressionValues,
    }));

    const user = await getUser(userId);
    return response(200, { user });
  } catch (error) {
    if (error instanceof z.ZodError) {
      return response(400, { message: 'Validation error', details: error.errors });
    }
    console.error('Update profile error:', error);
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
    
    let userId: string;
    try {
      const payload = verifyToken(authHeader.split(' ')[1]);
      userId = payload.userId;
    } catch {
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

    const user = await getUser(userId);
    return response(200, { user });
  } catch (error) {
    if (error instanceof z.ZodError) {
      return response(400, { message: 'Validation error', details: error.errors });
    }
    console.error('Update language error:', error);
    return response(500, { message: 'Internal server error' });
  }
};

// Update user's preferred country
export const updateCountry: APIGatewayProxyHandler = async (event) => {
  try {
    // Verify auth
    const authHeader = event.headers.authorization || event.headers.Authorization;
    if (!authHeader?.startsWith('Bearer ')) {
      return response(401, { message: 'Unauthorized' });
    }
    
    let userId: string;
    try {
      const payload = verifyToken(authHeader.split(' ')[1]);
      userId = payload.userId;
    } catch {
      return response(401, { message: 'Invalid token' });
    }

    const body = JSON.parse(event.body || '{}');
    const { preferredCountry } = z.object({
      preferredCountry: z.string().length(2),
    }).parse(body);

    // Update user's preferred country
    await dynamodb.send(new UpdateCommand({
      TableName: Tables.USERS,
      Key: { id: userId },
      UpdateExpression: 'SET preferredCountry = :country, updatedAt = :now',
      ExpressionAttributeValues: {
        ':country': preferredCountry,
        ':now': new Date().toISOString(),
      },
    }));

    const user = await getUser(userId);
    return response(200, { user });
  } catch (error) {
    if (error instanceof z.ZodError) {
      return response(400, { message: 'Validation error', details: error.errors });
    }
    console.error('Update country error:', error);
    return response(500, { message: 'Internal server error' });
  }
};

/**
 * Get presigned URL for profile picture upload
 */
export const getProfilePictureUploadUrl: APIGatewayProxyHandler = async (event) => {
  try {
    // Verify auth
    const authHeader = event.headers.authorization || event.headers.Authorization;
    if (!authHeader?.startsWith('Bearer ')) {
      return response(401, { message: 'Unauthorized' });
    }
    
    let userId: string;
    try {
      const payload = verifyToken(authHeader.split(' ')[1]);
      userId = payload.userId;
    } catch {
      return response(401, { message: 'Invalid token' });
    }

    const body = JSON.parse(event.body || '{}');
    const { contentType, fileSize } = body;

    if (!contentType || !fileSize) {
      return response(400, { 
        message: 'Missing required fields: contentType, fileSize' 
      });
    }

    // Validate content type
    if (!ALLOWED_IMAGE_TYPES.includes(contentType)) {
      return response(400, { 
        message: 'Invalid file type. Allowed types: JPEG, PNG, WebP',
        allowedTypes: ALLOWED_IMAGE_TYPES,
      });
    }

    // Validate file size
    if (fileSize > MAX_PROFILE_PICTURE_SIZE) {
      return response(400, { 
        message: `File too large. Maximum size is ${MAX_PROFILE_PICTURE_SIZE / 1024 / 1024}MB` 
      });
    }

    // Generate unique key for profile picture
    const pictureId = uuid();
    const extension = contentType === 'image/jpeg' ? 'jpg' : 
                      contentType === 'image/png' ? 'png' : 'webp';
    const key = `profiles/${userId}/${pictureId}.${extension}`;

    // Create presigned URL for upload
    const command = new PutObjectCommand({
      Bucket: BUCKET,
      Key: key,
      ContentType: contentType,
      Metadata: {
        'uploaded-by': userId,
        'type': 'profile-picture',
      },
    });

    const uploadUrl = await getSignedUrl(s3, command, { expiresIn: 300 }); // 5 minutes

    console.log(`📷 Generated profile picture upload URL for user ${userId}`);

    return response(200, {
      uploadUrl,
      key,
      expiresIn: 300,
    });
  } catch (error) {
    console.error('Get profile picture upload URL error:', error);
    return response(500, { message: 'Internal server error' });
  }
};

/**
 * Update user's profile picture after successful upload
 */
export const updateProfilePicture: APIGatewayProxyHandler = async (event) => {
  try {
    // Verify auth
    const authHeader = event.headers.authorization || event.headers.Authorization;
    if (!authHeader?.startsWith('Bearer ')) {
      return response(401, { message: 'Unauthorized' });
    }
    
    let userId: string;
    try {
      const payload = verifyToken(authHeader.split(' ')[1]);
      userId = payload.userId;
    } catch {
      return response(401, { message: 'Invalid token' });
    }

    const body = JSON.parse(event.body || '{}');
    const { key } = z.object({
      key: z.string().min(1),
    }).parse(body);

    // Validate the key belongs to this user's profile pictures
    if (!key.startsWith(`profiles/${userId}/`)) {
      return response(403, { message: 'Invalid profile picture key' });
    }

    // Get current user to check for existing profile picture
    const currentUser = await dynamodb.send(new GetCommand({
      TableName: Tables.USERS,
      Key: { id: userId },
    }));

    const oldProfilePictureKey = currentUser.Item?.profilePictureKey;

    // Construct the full URL for the profile picture
    const profilePictureUrl = `https://${BUCKET}.s3.${process.env.AWS_REGION || 'us-east-1'}.amazonaws.com/${key}`;

    // Update user record with new profile picture
    await dynamodb.send(new UpdateCommand({
      TableName: Tables.USERS,
      Key: { id: userId },
      UpdateExpression: 'SET profilePicture = :url, profilePictureKey = :key, updatedAt = :now',
      ExpressionAttributeValues: {
        ':url': profilePictureUrl,
        ':key': key,
        ':now': new Date().toISOString(),
      },
    }));

    // Delete old profile picture from S3 if exists
    if (oldProfilePictureKey && oldProfilePictureKey !== key) {
      try {
        await s3.send(new DeleteObjectCommand({
          Bucket: BUCKET,
          Key: oldProfilePictureKey,
        }));
        console.log(`🗑️ Deleted old profile picture: ${oldProfilePictureKey}`);
      } catch (deleteError) {
        console.warn('Failed to delete old profile picture:', deleteError);
        // Continue anyway - the new picture is already set
      }
    }

    console.log(`📷 Updated profile picture for user ${userId}`);

    const user = await getUser(userId);
    return response(200, { 
      user,
      profilePicture: profilePictureUrl,
    });
  } catch (error) {
    if (error instanceof z.ZodError) {
      return response(400, { message: 'Validation error', details: error.errors });
    }
    console.error('Update profile picture error:', error);
    return response(500, { message: 'Internal server error' });
  }
};

/**
 * Delete user's profile picture
 */
export const deleteProfilePicture: APIGatewayProxyHandler = async (event) => {
  try {
    // Verify auth
    const authHeader = event.headers.authorization || event.headers.Authorization;
    if (!authHeader?.startsWith('Bearer ')) {
      return response(401, { message: 'Unauthorized' });
    }
    
    let userId: string;
    try {
      const payload = verifyToken(authHeader.split(' ')[1]);
      userId = payload.userId;
    } catch {
      return response(401, { message: 'Invalid token' });
    }

    // Get current user to find profile picture key
    const currentUser = await dynamodb.send(new GetCommand({
      TableName: Tables.USERS,
      Key: { id: userId },
    }));

    const profilePictureKey = currentUser.Item?.profilePictureKey;

    if (!profilePictureKey) {
      return response(404, { message: 'No profile picture to delete' });
    }

    // Delete from S3
    try {
      await s3.send(new DeleteObjectCommand({
        Bucket: BUCKET,
        Key: profilePictureKey,
      }));
    } catch (deleteError) {
      console.warn('Failed to delete profile picture from S3:', deleteError);
    }

    // Remove profile picture from user record
    await dynamodb.send(new UpdateCommand({
      TableName: Tables.USERS,
      Key: { id: userId },
      UpdateExpression: 'REMOVE profilePicture, profilePictureKey SET updatedAt = :now',
      ExpressionAttributeValues: {
        ':now': new Date().toISOString(),
      },
    }));

    console.log(`🗑️ Deleted profile picture for user ${userId}`);

    const user = await getUser(userId);
    return response(200, { user });
  } catch (error) {
    console.error('Delete profile picture error:', error);
    return response(500, { message: 'Internal server error' });
  }
};

