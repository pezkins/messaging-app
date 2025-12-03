import { Router, Request, Response, NextFunction } from 'express';
import { z } from 'zod';
import { prisma } from '../lib/prisma';
import { authenticate } from '../middleware/auth';
import { AppError } from '../middleware/errorHandler';
import { translationService } from '../services/translation';

const router = Router();

// All routes require authentication
router.use(authenticate);

// Get all conversations for current user
router.get('/', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const conversations = await prisma.conversation.findMany({
      where: {
        members: {
          some: { userId: req.userId },
        },
      },
      include: {
        members: {
          include: {
            user: {
              select: {
                id: true,
                username: true,
                preferredLanguage: true,
                avatarUrl: true,
              },
            },
          },
        },
        messages: {
          orderBy: { createdAt: 'desc' },
          take: 1,
          include: {
            sender: {
              select: {
                id: true,
                username: true,
                preferredLanguage: true,
                avatarUrl: true,
              },
            },
          },
        },
      },
      orderBy: { updatedAt: 'desc' },
    });

    // Format response
    const formatted = conversations.map((conv) => ({
      id: conv.id,
      type: conv.type.toLowerCase(),
      name: conv.name,
      participants: conv.members.map((m) => m.user),
      lastMessage: conv.messages[0] || null,
      createdAt: conv.createdAt,
      updatedAt: conv.updatedAt,
    }));

    res.json({ conversations: formatted });
  } catch (error) {
    next(error);
  }
});

// Create a new conversation
const createConversationSchema = z.object({
  participantIds: z.array(z.string().uuid()).min(1, 'At least one participant required'),
  type: z.enum(['direct', 'group']),
  name: z.string().min(1).max(100).optional(),
});

router.post('/', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const data = createConversationSchema.parse(req.body);

    // Add current user to participants
    const allParticipantIds = [...new Set([req.userId!, ...data.participantIds])];

    // For direct conversations, check if one already exists
    if (data.type === 'direct' && allParticipantIds.length === 2) {
      const existing = await prisma.conversation.findFirst({
        where: {
          type: 'DIRECT',
          AND: allParticipantIds.map((id) => ({
            members: { some: { userId: id } },
          })),
        },
        include: {
          members: {
            include: {
              user: {
                select: {
                  id: true,
                  username: true,
                  preferredLanguage: true,
                  avatarUrl: true,
                },
              },
            },
          },
        },
      });

      if (existing) {
        return res.json({
          conversation: {
            id: existing.id,
            type: 'direct',
            name: existing.name,
            participants: existing.members.map((m) => m.user),
            createdAt: existing.createdAt,
            updatedAt: existing.updatedAt,
          },
        });
      }
    }

    // Verify all participants exist
    const users = await prisma.user.findMany({
      where: { id: { in: allParticipantIds } },
    });

    if (users.length !== allParticipantIds.length) {
      throw new AppError(400, 'One or more participants not found', 'PARTICIPANTS_NOT_FOUND');
    }

    // Create conversation
    const conversation = await prisma.conversation.create({
      data: {
        type: data.type === 'direct' ? 'DIRECT' : 'GROUP',
        name: data.name,
        members: {
          create: allParticipantIds.map((userId) => ({ userId })),
        },
      },
      include: {
        members: {
          include: {
            user: {
              select: {
                id: true,
                username: true,
                preferredLanguage: true,
                avatarUrl: true,
              },
            },
          },
        },
      },
    });

    res.status(201).json({
      conversation: {
        id: conversation.id,
        type: data.type,
        name: conversation.name,
        participants: conversation.members.map((m) => m.user),
        createdAt: conversation.createdAt,
        updatedAt: conversation.updatedAt,
      },
    });
  } catch (error) {
    if (error instanceof z.ZodError) {
      return res.status(400).json({
        message: 'Validation error',
        code: 'VALIDATION_ERROR',
        details: error.errors,
      });
    }
    next(error);
  }
});

// Get conversation by ID
router.get('/:id', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const conversation = await prisma.conversation.findFirst({
      where: {
        id: req.params.id,
        members: {
          some: { userId: req.userId },
        },
      },
      include: {
        members: {
          include: {
            user: {
              select: {
                id: true,
                username: true,
                preferredLanguage: true,
                avatarUrl: true,
              },
            },
          },
        },
      },
    });

    if (!conversation) {
      throw new AppError(404, 'Conversation not found', 'CONVERSATION_NOT_FOUND');
    }

    res.json({
      conversation: {
        id: conversation.id,
        type: conversation.type.toLowerCase(),
        name: conversation.name,
        participants: conversation.members.map((m) => m.user),
        createdAt: conversation.createdAt,
        updatedAt: conversation.updatedAt,
      },
    });
  } catch (error) {
    next(error);
  }
});

// Get messages in a conversation
router.get('/:id/messages', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const conversationId = req.params.id;
    const limit = parseInt(req.query.limit as string) || 50;
    const cursor = req.query.cursor as string;

    // Verify user is member
    const membership = await prisma.conversationMember.findUnique({
      where: {
        conversationId_userId: {
          conversationId,
          userId: req.userId!,
        },
      },
    });

    if (!membership) {
      throw new AppError(404, 'Conversation not found', 'CONVERSATION_NOT_FOUND');
    }

    // Get user's preferred language
    const user = await prisma.user.findUnique({
      where: { id: req.userId },
      select: { preferredLanguage: true },
    });

    // Fetch messages
    const messages = await prisma.message.findMany({
      where: { conversationId },
      orderBy: { createdAt: 'desc' },
      take: limit + 1,
      ...(cursor && {
        cursor: { id: cursor },
        skip: 1,
      }),
      include: {
        sender: {
          select: {
            id: true,
            username: true,
            preferredLanguage: true,
            avatarUrl: true,
          },
        },
        translations: {
          where: { targetLanguage: user!.preferredLanguage },
        },
      },
    });

    // Check if there are more messages
    const hasMore = messages.length > limit;
    const messagesToReturn = hasMore ? messages.slice(0, -1) : messages;

    // Add translated content to each message
    const messagesWithTranslations = await Promise.all(
      messagesToReturn.map(async (msg) => {
        let translatedContent = msg.originalContent;

        if (msg.originalLanguage !== user!.preferredLanguage) {
          if (msg.translations.length > 0) {
            translatedContent = msg.translations[0].translatedContent;
          } else {
            // Translate on-the-fly if not cached
            translatedContent = await translationService.translate(
              msg.originalContent,
              msg.originalLanguage,
              user!.preferredLanguage,
              msg.id
            );
          }
        }

        return {
          id: msg.id,
          conversationId: msg.conversationId,
          senderId: msg.senderId,
          sender: msg.sender,
          type: msg.type.toLowerCase(),
          originalContent: msg.originalContent,
          originalLanguage: msg.originalLanguage,
          translatedContent,
          targetLanguage: user!.preferredLanguage,
          status: msg.status.toLowerCase(),
          createdAt: msg.createdAt,
        };
      })
    );

    res.json({
      messages: messagesWithTranslations.reverse(), // Return in chronological order
      hasMore,
      nextCursor: hasMore ? messagesToReturn[messagesToReturn.length - 1].id : null,
    });
  } catch (error) {
    next(error);
  }
});

export { router as conversationRouter };



