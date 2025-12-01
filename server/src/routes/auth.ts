import { Router, Request, Response, NextFunction } from 'express';
import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import { z } from 'zod';
import { prisma } from '../lib/prisma';
import { AppError } from '../middleware/errorHandler';
import { authenticate } from '../middleware/auth';

const router = Router();

// Validation schemas
const registerSchema = z.object({
  email: z.string().email('Invalid email'),
  username: z.string().min(3, 'Username must be at least 3 characters').max(30),
  password: z.string().min(6, 'Password must be at least 6 characters'),
  preferredLanguage: z.string().length(2, 'Invalid language code').default('en'),
});

const loginSchema = z.object({
  email: z.string().email('Invalid email'),
  password: z.string().min(1, 'Password is required'),
});

const oauthSchema = z.object({
  provider: z.enum(['google', 'apple']),
  providerId: z.string().min(1, 'Provider ID is required'),
  email: z.string().email('Invalid email'),
  name: z.string().nullable(),
  avatarUrl: z.string().nullable(),
});

// Helper to generate tokens
function generateTokens(userId: string) {
  const accessToken = jwt.sign(
    { userId },
    process.env.JWT_SECRET!,
    { expiresIn: process.env.JWT_EXPIRES_IN || '15m' }
  );

  const refreshToken = jwt.sign(
    { userId },
    process.env.JWT_REFRESH_SECRET!,
    { expiresIn: process.env.JWT_REFRESH_EXPIRES_IN || '7d' }
  );

  return { accessToken, refreshToken };
}

// Register
router.post('/register', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const data = registerSchema.parse(req.body);

    // Check if email or username already exists
    const existingUser = await prisma.user.findFirst({
      where: {
        OR: [
          { email: data.email },
          { username: data.username },
        ],
      },
    });

    if (existingUser) {
      if (existingUser.email === data.email) {
        throw new AppError(400, 'Email already registered', 'EMAIL_EXISTS');
      }
      throw new AppError(400, 'Username already taken', 'USERNAME_EXISTS');
    }

    // Hash password
    const passwordHash = await bcrypt.hash(data.password, 10);

    // Create user
    const user = await prisma.user.create({
      data: {
        email: data.email,
        username: data.username,
        passwordHash,
        preferredLanguage: data.preferredLanguage,
      },
      select: {
        id: true,
        email: true,
        username: true,
        preferredLanguage: true,
        avatarUrl: true,
        createdAt: true,
        updatedAt: true,
      },
    });

    // Generate tokens
    const { accessToken, refreshToken } = generateTokens(user.id);

    // Store refresh token
    const expiresAt = new Date();
    expiresAt.setDate(expiresAt.getDate() + 7);

    await prisma.refreshToken.create({
      data: {
        token: refreshToken,
        userId: user.id,
        expiresAt,
      },
    });

    res.status(201).json({
      user,
      accessToken,
      refreshToken,
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

// Login
router.post('/login', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const data = loginSchema.parse(req.body);

    // Find user
    const user = await prisma.user.findUnique({
      where: { email: data.email },
    });

    if (!user) {
      throw new AppError(401, 'Invalid credentials', 'INVALID_CREDENTIALS');
    }

    // Verify password
    const validPassword = await bcrypt.compare(data.password, user.passwordHash);

    if (!validPassword) {
      throw new AppError(401, 'Invalid credentials', 'INVALID_CREDENTIALS');
    }

    // Generate tokens
    const { accessToken, refreshToken } = generateTokens(user.id);

    // Store refresh token
    const expiresAt = new Date();
    expiresAt.setDate(expiresAt.getDate() + 7);

    await prisma.refreshToken.create({
      data: {
        token: refreshToken,
        userId: user.id,
        expiresAt,
      },
    });

    res.json({
      user: {
        id: user.id,
        email: user.email,
        username: user.username,
        preferredLanguage: user.preferredLanguage,
        avatarUrl: user.avatarUrl,
        createdAt: user.createdAt,
        updatedAt: user.updatedAt,
      },
      accessToken,
      refreshToken,
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

// OAuth Login (Google/Apple)
router.post('/oauth', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const data = oauthSchema.parse(req.body);

    // Check if user exists with this OAuth provider
    let user = await prisma.user.findFirst({
      where: {
        OR: [
          { 
            oauthProvider: data.provider, 
            oauthProviderId: data.providerId 
          },
          { email: data.email },
        ],
      },
    });

    if (user) {
      // Update OAuth info if user logged in with email before
      if (!user.oauthProvider) {
        user = await prisma.user.update({
          where: { id: user.id },
          data: {
            oauthProvider: data.provider,
            oauthProviderId: data.providerId,
            avatarUrl: data.avatarUrl || user.avatarUrl,
          },
        });
      }
    } else {
      // Create new user
      // Generate username from email or name
      const baseUsername = data.name?.replace(/\s+/g, '').toLowerCase() || 
                          data.email.split('@')[0];
      let username = baseUsername;
      let suffix = 1;

      // Ensure username is unique
      while (await prisma.user.findUnique({ where: { username } })) {
        username = `${baseUsername}${suffix}`;
        suffix++;
      }

      user = await prisma.user.create({
        data: {
          email: data.email,
          username,
          passwordHash: '', // No password for OAuth users
          preferredLanguage: 'en', // Default, user can change later
          avatarUrl: data.avatarUrl,
          oauthProvider: data.provider,
          oauthProviderId: data.providerId,
        },
      });
    }

    // Generate tokens
    const { accessToken, refreshToken } = generateTokens(user.id);

    // Store refresh token
    const expiresAt = new Date();
    expiresAt.setDate(expiresAt.getDate() + 7);

    await prisma.refreshToken.create({
      data: {
        token: refreshToken,
        userId: user.id,
        expiresAt,
      },
    });

    res.json({
      user: {
        id: user.id,
        email: user.email,
        username: user.username,
        preferredLanguage: user.preferredLanguage,
        avatarUrl: user.avatarUrl,
        createdAt: user.createdAt,
        updatedAt: user.updatedAt,
      },
      accessToken,
      refreshToken,
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

// Refresh token
router.post('/refresh', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const { refreshToken: token } = req.body;

    if (!token) {
      throw new AppError(400, 'Refresh token required', 'TOKEN_REQUIRED');
    }

    // Verify token
    const decoded = jwt.verify(token, process.env.JWT_REFRESH_SECRET!) as { userId: string };

    // Find stored token
    const storedToken = await prisma.refreshToken.findUnique({
      where: { token },
      include: { user: true },
    });

    if (!storedToken || storedToken.expiresAt < new Date()) {
      throw new AppError(401, 'Invalid or expired refresh token', 'INVALID_REFRESH_TOKEN');
    }

    // Delete old refresh token
    await prisma.refreshToken.delete({
      where: { id: storedToken.id },
    });

    // Generate new tokens
    const { accessToken, refreshToken } = generateTokens(decoded.userId);

    // Store new refresh token
    const expiresAt = new Date();
    expiresAt.setDate(expiresAt.getDate() + 7);

    await prisma.refreshToken.create({
      data: {
        token: refreshToken,
        userId: decoded.userId,
        expiresAt,
      },
    });

    res.json({
      accessToken,
      refreshToken,
    });
  } catch (error) {
    next(error);
  }
});

// Logout
router.post('/logout', authenticate, async (req: Request, res: Response, next: NextFunction) => {
  try {
    const { refreshToken } = req.body;

    if (refreshToken) {
      await prisma.refreshToken.deleteMany({
        where: { token: refreshToken },
      });
    }

    res.json({ message: 'Logged out successfully' });
  } catch (error) {
    next(error);
  }
});

// Get current user
router.get('/me', authenticate, (req: Request, res: Response) => {
  res.json({ user: req.user });
});

export { router as authRouter };

