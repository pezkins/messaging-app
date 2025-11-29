import { Server as SocketIOServer, Socket } from 'socket.io';
import jwt from 'jsonwebtoken';
import { prisma } from '../lib/prisma';
import { userSessions } from '../lib/redis';
import { translationService } from '../services/translation';

interface AuthenticatedSocket extends Socket {
  userId?: string;
}

interface JwtPayload {
  userId: string;
}

export function setupSocketHandlers(io: SocketIOServer) {
  // Authentication middleware
  io.use(async (socket: AuthenticatedSocket, next) => {
    try {
      const token = socket.handshake.auth.token || socket.handshake.headers.authorization?.replace('Bearer ', '');
      
      if (!token) {
        return next(new Error('Authentication required'));
      }

      const decoded = jwt.verify(token, process.env.JWT_SECRET!) as JwtPayload;
      socket.userId = decoded.userId;
      
      // Verify user exists
      const user = await prisma.user.findUnique({
        where: { id: decoded.userId },
      });

      if (!user) {
        return next(new Error('User not found'));
      }

      next();
    } catch (error) {
      next(new Error('Invalid token'));
    }
  });

  io.on('connection', async (socket: AuthenticatedSocket) => {
    const userId = socket.userId!;
    console.log(`🔌 User connected: ${userId} (socket: ${socket.id})`);

    // Store socket ID in Redis
    await userSessions.set(userId, socket.id);

    // Join user's conversation rooms
    const memberships = await prisma.conversationMember.findMany({
      where: { userId },
      select: { conversationId: true },
    });

    memberships.forEach((m) => {
      socket.join(`conversation:${m.conversationId}`);
    });

    // Handle sending messages
    socket.on('message:send', async (data: {
      conversationId: string;
      content: string;
      type: 'TEXT' | 'VOICE';
    }) => {
      try {
        const { conversationId, content, type } = data;

        // Verify user is part of conversation
        const membership = await prisma.conversationMember.findUnique({
          where: {
            conversationId_userId: {
              conversationId,
              userId,
            },
          },
        });

        if (!membership) {
          socket.emit('error', { message: 'Not a member of this conversation' });
          return;
        }

        // Get sender's preferred language
        const sender = await prisma.user.findUnique({
          where: { id: userId },
          select: { id: true, username: true, preferredLanguage: true, avatarUrl: true },
        });

        // Detect language of the message
        const detectedLanguage = await translationService.detectLanguage(content);

        // Create message
        const message = await prisma.message.create({
          data: {
            conversationId,
            senderId: userId,
            type,
            originalContent: content,
            originalLanguage: detectedLanguage,
            status: 'SENT',
          },
          include: {
            sender: {
              select: { id: true, username: true, preferredLanguage: true, avatarUrl: true },
            },
          },
        });

        // Get all conversation members
        const members = await prisma.conversationMember.findMany({
          where: { conversationId },
          include: {
            user: {
              select: { id: true, preferredLanguage: true },
            },
          },
        });

        // Translate for each member and send
        for (const member of members) {
          const targetLanguage = member.user.preferredLanguage;
          
          let translatedContent = content;
          
          // Only translate if target language differs from original
          if (targetLanguage !== detectedLanguage) {
            translatedContent = await translationService.translate(
              content,
              detectedLanguage,
              targetLanguage,
              message.id
            );
          }

          // Get member's socket
          const memberSocketId = await userSessions.get(member.userId);
          
          if (memberSocketId) {
            io.to(memberSocketId).emit('message:receive', {
              message: {
                ...message,
                translatedContent,
                targetLanguage,
              },
            });
          }
        }

        // Update conversation timestamp
        await prisma.conversation.update({
          where: { id: conversationId },
          data: { updatedAt: new Date() },
        });

      } catch (error) {
        console.error('Error sending message:', error);
        socket.emit('error', { message: 'Failed to send message' });
      }
    });

    // Handle typing indicators
    socket.on('message:typing', async (data: {
      conversationId: string;
      isTyping: boolean;
    }) => {
      const { conversationId, isTyping } = data;
      
      socket.to(`conversation:${conversationId}`).emit('message:typing', {
        conversationId,
        userId,
        isTyping,
      });
    });

    // Handle message read receipts
    socket.on('message:read', async (data: {
      conversationId: string;
      messageId: string;
    }) => {
      try {
        const { conversationId, messageId } = data;

        // Update message status
        await prisma.message.update({
          where: { id: messageId },
          data: { status: 'SEEN' },
        });

        // Update last read timestamp
        await prisma.conversationMember.update({
          where: {
            conversationId_userId: {
              conversationId,
              userId,
            },
          },
          data: { lastReadAt: new Date() },
        });

        // Notify sender
        socket.to(`conversation:${conversationId}`).emit('message:read', {
          conversationId,
          messageId,
          userId,
        });

      } catch (error) {
        console.error('Error marking message as read:', error);
      }
    });

    // Handle joining a conversation
    socket.on('conversation:join', (conversationId: string) => {
      socket.join(`conversation:${conversationId}`);
    });

    // Handle leaving a conversation
    socket.on('conversation:leave', (conversationId: string) => {
      socket.leave(`conversation:${conversationId}`);
    });

    // Handle disconnection
    socket.on('disconnect', async () => {
      console.log(`🔌 User disconnected: ${userId}`);
      await userSessions.remove(userId);
    });
  });
}

