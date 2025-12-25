import { APIGatewayProxyHandler } from 'aws-lambda';
import { ScanCommand, UpdateCommand } from '@aws-sdk/lib-dynamodb';
import { dynamodb, Tables } from '../lib/dynamo';

const response = (statusCode: number, body: object) => ({
  statusCode,
  headers: {
    'Content-Type': 'application/json',
    'Access-Control-Allow-Origin': '*',
  },
  body: JSON.stringify(body),
});

/**
 * Migration: Normalize emails to lowercase for all existing users
 * This ensures consistent email lookups (login, search, etc.)
 * 
 * Run once via: POST /api/migrations/normalize-users
 * Protected by API key for admin use only
 */
export const normalizeUsers: APIGatewayProxyHandler = async (event) => {
  try {
    // Simple API key protection (check header)
    const apiKey = event.headers['x-api-key'] || event.headers['X-Api-Key'];
    const expectedKey = process.env.ADMIN_API_KEY;
    
    if (!expectedKey || apiKey !== expectedKey) {
      return response(401, { message: 'Unauthorized - Admin API key required' });
    }

    console.log('🔄 Starting email normalization migration...');

    let updatedCount = 0;
    let scannedCount = 0;
    let lastEvaluatedKey: Record<string, any> | undefined;

    do {
      // Scan all users
      const scanResult = await dynamodb.send(new ScanCommand({
        TableName: Tables.USERS,
        ExclusiveStartKey: lastEvaluatedKey,
      }));

      const users = scanResult.Items || [];
      scannedCount += users.length;

      for (const user of users) {
        // Check if email needs to be normalized to lowercase
        const emailLower = user.email?.toLowerCase();
        if (user.email && user.email !== emailLower) {
          await dynamodb.send(new UpdateCommand({
            TableName: Tables.USERS,
            Key: { id: user.id },
            UpdateExpression: 'SET email = :email',
            ExpressionAttributeValues: { ':email': emailLower },
          }));
          updatedCount++;
          console.log(`✅ Normalized email for user ${user.id}: ${user.email} → ${emailLower}`);
        }
      }

      lastEvaluatedKey = scanResult.LastEvaluatedKey;
    } while (lastEvaluatedKey);

    console.log(`🎉 Migration complete: ${updatedCount}/${scannedCount} users updated`);

    return response(200, {
      message: 'Email normalization completed',
      scannedCount,
      updatedCount,
    });
  } catch (error) {
    console.error('Migration error:', error);
    return response(500, { message: 'Migration failed', error: String(error) });
  }
};
