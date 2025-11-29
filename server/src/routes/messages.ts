import { Router, Request, Response, NextFunction } from 'express';
import { z } from 'zod';
import { prisma } from '../lib/prisma';
import { authenticate } from '../middleware/auth';
import { AppError } from '../middleware/errorHandler';
import { translationService } from '../services/translation';

const router = Router();

// All routes require authentication
router.use(authenticate);

// Send a message (REST fallback, prefer WebSocket)
const sendMessageSchema = z.object({
  conversationId: z.string().uuid(),
  content: z.string().min(1, 'Message cannot be empty').max(5000),
  type: z.enum(['text', 'voice']).default('text'),
});

router.post('/', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const data = sendMessageSchema.parse(req.body);

    // Verify user is member of conversation
    const membership = await prisma.conversationMember.findUnique({
      where: {
        conversationId_userId: {
          conversationId: data.conversationId,
          userId: req.userId!,
        },
      },
    });

    if (!membership) {
      throw new AppError(403, 'Not a member of this conversation', 'NOT_MEMBER');
    }

    // Detect language
    const detectedLanguage = await translationService.detectLanguage(data.content);

    // Create message
    const message = await prisma.message.create({
      data: {
        conversationId: data.conversationId,
        senderId: req.userId!,
        type: data.type === 'voice' ? 'VOICE' : 'TEXT',
        originalContent: data.content,
        originalLanguage: detectedLanguage,
        status: 'SENT',
      },
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
    });

    // Update conversation timestamp
    await prisma.conversation.update({
      where: { id: data.conversationId },
      data: { updatedAt: new Date() },
    });

    res.status(201).json({
      message: {
        id: message.id,
        conversationId: message.conversationId,
        senderId: message.senderId,
        sender: message.sender,
        type: message.type.toLowerCase(),
        originalContent: message.originalContent,
        originalLanguage: message.originalLanguage,
        status: message.status.toLowerCase(),
        createdAt: message.createdAt,
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

// Preview translation before sending
const previewTranslationSchema = z.object({
  content: z.string().min(1).max(5000),
  targetLanguage: z.string().length(2, 'Invalid language code'),
});

router.post('/preview-translation', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const data = previewTranslationSchema.parse(req.body);

    const result = await translationService.previewTranslation(
      data.content,
      data.targetLanguage
    );

    res.json({
      originalContent: data.content,
      translatedContent: result.translatedContent,
      detectedLanguage: result.detectedLanguage,
      targetLanguage: data.targetLanguage,
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

// Get a specific message
router.get('/:id', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const message = await prisma.message.findUnique({
      where: { id: req.params.id },
      include: {
        sender: {
          select: {
            id: true,
            username: true,
            preferredLanguage: true,
            avatarUrl: true,
          },
        },
        conversation: {
          include: {
            members: {
              where: { userId: req.userId },
            },
          },
        },
      },
    });

    if (!message || message.conversation.members.length === 0) {
      throw new AppError(404, 'Message not found', 'MESSAGE_NOT_FOUND');
    }

    // Get user's preferred language
    const user = await prisma.user.findUnique({
      where: { id: req.userId },
      select: { preferredLanguage: true },
    });

    // Get or create translation
    let translatedContent = message.originalContent;

    if (message.originalLanguage !== user!.preferredLanguage) {
      translatedContent = await translationService.translate(
        message.originalContent,
        message.originalLanguage,
        user!.preferredLanguage,
        message.id
      );
    }

    res.json({
      message: {
        id: message.id,
        conversationId: message.conversationId,
        senderId: message.senderId,
        sender: message.sender,
        type: message.type.toLowerCase(),
        originalContent: message.originalContent,
        originalLanguage: message.originalLanguage,
        translatedContent,
        targetLanguage: user!.preferredLanguage,
        status: message.status.toLowerCase(),
        createdAt: message.createdAt,
      },
    });
  } catch (error) {
    next(error);
  }
});

// Get original message content (for long-press/hover in group chats)
router.get('/:id/original', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const message = await prisma.message.findUnique({
      where: { id: req.params.id },
      include: {
        conversation: {
          include: {
            members: {
              where: { userId: req.userId },
            },
          },
        },
      },
    });

    if (!message || message.conversation.members.length === 0) {
      throw new AppError(404, 'Message not found', 'MESSAGE_NOT_FOUND');
    }

    res.json({
      originalContent: message.originalContent,
      originalLanguage: message.originalLanguage,
    });
  } catch (error) {
    next(error);
  }
});

export { router as messageRouter };

