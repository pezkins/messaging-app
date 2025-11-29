import express from 'express';
import { createServer } from 'http';
import { Server as SocketIOServer } from 'socket.io';
import cors from 'cors';
import dotenv from 'dotenv';

import { authRouter } from './routes/auth';
import { userRouter } from './routes/users';
import { conversationRouter } from './routes/conversations';
import { messageRouter } from './routes/messages';
import { errorHandler } from './middleware/errorHandler';
import { setupSocketHandlers } from './socket';
import { prisma } from './lib/prisma';
import { redis } from './lib/redis';

// Load environment variables
dotenv.config();

const app = express();
const httpServer = createServer(app);

// Socket.IO setup
const io = new SocketIOServer(httpServer, {
  cors: {
    origin: process.env.CORS_ORIGIN || 'http://localhost:5173',
    methods: ['GET', 'POST'],
    credentials: true,
  },
});

// Middleware
app.use(cors({
  origin: process.env.CORS_ORIGIN || 'http://localhost:5173',
  credentials: true,
}));
app.use(express.json());

// Health check
app.get('/health', (req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

// API Routes
app.use('/api/auth', authRouter);
app.use('/api/users', userRouter);
app.use('/api/conversations', conversationRouter);
app.use('/api/messages', messageRouter);

// Error handling
app.use(errorHandler);

// Setup WebSocket handlers
setupSocketHandlers(io);

// Graceful shutdown
const shutdown = async () => {
  console.log('\n🛑 Shutting down...');
  
  await prisma.$disconnect();
  await redis.quit();
  
  httpServer.close(() => {
    console.log('👋 Server closed');
    process.exit(0);
  });
};

process.on('SIGINT', shutdown);
process.on('SIGTERM', shutdown);

// Start server
const PORT = process.env.PORT || 3001;

httpServer.listen(PORT, () => {
  console.log(`
🚀 LinguaLink Server is running!
📡 HTTP:      http://localhost:${PORT}
🔌 WebSocket: ws://localhost:${PORT}
📊 Health:    http://localhost:${PORT}/health
  `);
});

export { io };

