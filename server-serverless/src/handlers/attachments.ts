import type { APIGatewayProxyHandler } from 'aws-lambda';
import { S3Client, PutObjectCommand, GetObjectCommand } from '@aws-sdk/client-s3';
import { getSignedUrl } from '@aws-sdk/s3-request-presigner';
import { v4 as uuid } from 'uuid';
import { getUserIdFromEvent, response } from '../lib/auth';

const s3 = new S3Client({ region: process.env.AWS_REGION });
const BUCKET = process.env.ATTACHMENTS_BUCKET!;

// Allowed file types and their MIME types
const ALLOWED_TYPES: Record<string, string[]> = {
  image: ['image/jpeg', 'image/png', 'image/gif', 'image/webp'],
  video: ['video/mp4', 'video/quicktime', 'video/webm'],
  document: [
    'application/pdf',
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'application/vnd.ms-excel',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    'text/plain',
  ],
  audio: ['audio/mpeg', 'audio/wav', 'audio/ogg', 'audio/webm'],
};

const MAX_FILE_SIZE = 25 * 1024 * 1024; // 25MB

function getFileCategory(mimeType: string): string | null {
  for (const [category, types] of Object.entries(ALLOWED_TYPES)) {
    if (types.includes(mimeType)) {
      return category;
    }
  }
  return null;
}

function getAllAllowedTypes(): string[] {
  return Object.values(ALLOWED_TYPES).flat();
}

/**
 * Get a presigned URL for uploading an attachment
 */
export const getUploadUrl: APIGatewayProxyHandler = async (event) => {
  try {
    const userId = getUserIdFromEvent(event);
    if (!userId) {
      return response(401, { message: 'Authentication required' });
    }

    const body = JSON.parse(event.body || '{}');
    const { fileName, contentType, fileSize, conversationId } = body;

    if (!fileName || !contentType || !fileSize || !conversationId) {
      return response(400, { 
        message: 'Missing required fields: fileName, contentType, fileSize, conversationId' 
      });
    }

    // Validate content type
    const category = getFileCategory(contentType);
    if (!category) {
      return response(400, { 
        message: 'File type not allowed',
        allowedTypes: getAllAllowedTypes(),
      });
    }

    // Validate file size
    if (fileSize > MAX_FILE_SIZE) {
      return response(400, { 
        message: `File too large. Maximum size is ${MAX_FILE_SIZE / 1024 / 1024}MB` 
      });
    }

    // Generate unique attachment ID
    const attachmentId = uuid();
    const extension = fileName.split('.').pop() || '';
    const key = `${conversationId}/${attachmentId}.${extension}`;

    // Create presigned URL for upload
    const command = new PutObjectCommand({
      Bucket: BUCKET,
      Key: key,
      ContentType: contentType,
      Metadata: {
        'original-filename': encodeURIComponent(fileName),
        'uploaded-by': userId,
        'conversation-id': conversationId,
        'category': category,
      },
    });

    const uploadUrl = await getSignedUrl(s3, command, { expiresIn: 300 }); // 5 minutes

    return response(200, {
      attachmentId,
      uploadUrl,
      key,
      category,
      expiresIn: 300,
    });
  } catch (error) {
    console.error('Get upload URL error:', error);
    return response(500, { message: 'Internal server error' });
  }
};

/**
 * Get a presigned URL for downloading an attachment
 */
export const getDownloadUrl: APIGatewayProxyHandler = async (event) => {
  try {
    const userId = getUserIdFromEvent(event);
    if (!userId) {
      return response(401, { message: 'Authentication required' });
    }

    const attachmentId = event.pathParameters?.attachmentId;
    const conversationId = event.queryStringParameters?.conversationId;

    if (!attachmentId || !conversationId) {
      return response(400, { message: 'Missing attachmentId or conversationId' });
    }

    // Reconstruct the key (we need to find it in S3)
    // For simplicity, we'll use a pattern match
    const key = event.queryStringParameters?.key;
    
    if (!key) {
      return response(400, { message: 'Missing key parameter' });
    }

    // Verify the key belongs to the requested conversation
    if (!key.startsWith(`${conversationId}/`)) {
      return response(403, { message: 'Access denied' });
    }

    // Create presigned URL for download
    const command = new GetObjectCommand({
      Bucket: BUCKET,
      Key: key,
    });

    const downloadUrl = await getSignedUrl(s3, command, { expiresIn: 3600 }); // 1 hour

    return response(200, {
      downloadUrl,
      expiresIn: 3600,
    });
  } catch (error) {
    console.error('Get download URL error:', error);
    return response(500, { message: 'Internal server error' });
  }
};

