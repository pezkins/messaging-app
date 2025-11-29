import { DynamoDBClient } from '@aws-sdk/client-dynamodb';
import { DynamoDBDocumentClient, GetCommand, PutCommand, QueryCommand, DeleteCommand, UpdateCommand } from '@aws-sdk/lib-dynamodb';

const client = new DynamoDBClient({});
export const dynamodb = DynamoDBDocumentClient.from(client);

export const Tables = {
  USERS: process.env.USERS_TABLE || 'lingualink-users',
  MESSAGES: process.env.MESSAGES_TABLE || 'lingualink-messages',
  CONVERSATIONS: process.env.CONVERSATIONS_TABLE || 'lingualink-conversations',
  CONNECTIONS: process.env.CONNECTIONS_TABLE || 'lingualink-connections',
};

export { GetCommand, PutCommand, QueryCommand, DeleteCommand, UpdateCommand };

