import { create } from 'zustand';
import { api } from '../services/api';
import { socketService, type MessageReceiveEvent, type TypingEvent, type ReactionEvent } from '../services/socket';
import type { Conversation, Message, UserPublic, Attachment } from '../types';

interface SendMessageOptions {
  type?: 'TEXT' | 'VOICE' | 'ATTACHMENT' | 'GIF';
  attachment?: Attachment;
  localUri?: string; // For displaying local preview before upload completes
}

interface ChatState {
  conversations: Conversation[];
  activeConversation: Conversation | null;
  messages: Message[];
  typingUsers: Map<string, Set<string>>;
  isLoadingConversations: boolean;
  isLoadingMessages: boolean;
  hasMoreMessages: boolean;
  nextCursor: string | null;

  loadConversations: () => Promise<void>;
  selectConversation: (conversation: Conversation) => Promise<void>;
  clearActiveConversation: () => void;
  loadMoreMessages: () => Promise<void>;
  sendMessage: (content: string, options?: SendMessageOptions) => void;
  startConversation: (user: UserPublic) => Promise<Conversation>;
  startGroupConversation: (users: UserPublic[], name?: string) => Promise<Conversation>;
  setTyping: (isTyping: boolean) => void;
  sendReaction: (messageId: string, messageTimestamp: string, emoji: string) => void;
  subscribeToEvents: () => () => void;
}

export const useChatStore = create<ChatState>((set, get) => ({
  conversations: [],
  activeConversation: null,
  messages: [],
  typingUsers: new Map(),
  isLoadingConversations: false,
  isLoadingMessages: false,
  hasMoreMessages: false,
  nextCursor: null,

  loadConversations: async () => {
    set({ isLoadingConversations: true });

    try {
      const { conversations } = await api.getConversations();
      set({ conversations, isLoadingConversations: false });
    } catch (error) {
      console.error('Failed to load conversations:', error);
      set({ isLoadingConversations: false });
    }
  },

  selectConversation: async (conversation) => {
    const prev = get().activeConversation;
    
    if (prev) {
      socketService.leaveConversation(prev.id);
    }

    socketService.joinConversation(conversation.id);

    set({
      activeConversation: conversation,
      messages: [],
      isLoadingMessages: true,
      hasMoreMessages: false,
      nextCursor: null,
    });

    try {
      const { messages, hasMore, nextCursor } = await api.getMessages(conversation.id);
      
      set({
        messages,
        hasMoreMessages: hasMore,
        nextCursor,
        isLoadingMessages: false,
      });
    } catch (error) {
      console.error('Failed to load messages:', error);
      set({ isLoadingMessages: false });
    }
  },

  clearActiveConversation: () => {
    const prev = get().activeConversation;
    if (prev) {
      socketService.leaveConversation(prev.id);
    }
    set({
      activeConversation: null,
      messages: [],
      hasMoreMessages: false,
      nextCursor: null,
    });
  },

  loadMoreMessages: async () => {
    const { activeConversation, nextCursor, hasMoreMessages, isLoadingMessages } = get();

    if (!activeConversation || !hasMoreMessages || isLoadingMessages || !nextCursor) {
      return;
    }

    set({ isLoadingMessages: true });

    try {
      const { messages: newMessages, hasMore, nextCursor: newCursor } = await api.getMessages(
        activeConversation.id,
        { cursor: nextCursor }
      );

      set((state) => ({
        messages: [...newMessages, ...state.messages],
        hasMoreMessages: hasMore,
        nextCursor: newCursor,
        isLoadingMessages: false,
      }));
    } catch (error) {
      console.error('Failed to load more messages:', error);
      set({ isLoadingMessages: false });
    }
  },

  sendMessage: (content, options = {}) => {
    const { activeConversation, messages } = get();
    const { type = 'TEXT', attachment, localUri } = options;
    
    // For text messages, require content; for attachments/GIFs, content can be empty
    if (!activeConversation) return;
    if (type === 'TEXT' && !content.trim()) return;
    if (type === 'ATTACHMENT' && !attachment) return;
    if (type === 'GIF' && !content.trim()) return;

    // Create optimistic message with 'sending' status
    const tempId = `temp-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
    const optimisticMessage: Message = {
      id: tempId,
      conversationId: activeConversation.id,
      senderId: '', // Will be filled in by backend
      sender: { id: '', username: 'You', preferredLanguage: 'en' },
      type: type.toLowerCase() as any,
      originalContent: content.trim() || (attachment ? attachment.fileName : ''),
      originalLanguage: 'en', // Will be detected by backend
      status: 'sending',
      createdAt: new Date().toISOString(),
      attachment: attachment ? { ...attachment, localUri } : undefined,
    };

    // Add optimistic message immediately
    set({ messages: [...messages, optimisticMessage] });

    // Send via socket
    socketService.sendMessage({
      conversationId: activeConversation.id,
      content: content.trim(),
      type,
      tempId, // Send tempId so we can match the response
      attachment: attachment ? {
        id: attachment.id,
        key: attachment.key,
        fileName: attachment.fileName,
        contentType: attachment.contentType,
        fileSize: attachment.fileSize,
        category: attachment.category,
      } : undefined,
    });
  },

  startConversation: async (user) => {
    const existing = get().conversations.find(
      (c) => c.type === 'direct' && c.participants.some((p) => p.id === user.id)
    );

    if (existing) {
      await get().selectConversation(existing);
      return existing;
    }

    const { conversation } = await api.createConversation({
      participantIds: [user.id],
      type: 'direct',
    });

    set((state) => ({
      conversations: [conversation, ...state.conversations],
    }));

    await get().selectConversation(conversation);
    return conversation;
  },

  startGroupConversation: async (users, name) => {
    const { conversation } = await api.createConversation({
      participantIds: users.map(u => u.id),
      type: 'group',
      name,
    });

    set((state) => ({
      conversations: [conversation, ...state.conversations],
    }));

    await get().selectConversation(conversation);
    return conversation;
  },

  setTyping: (isTyping) => {
    const { activeConversation } = get();
    
    if (!activeConversation) {
      return;
    }

    socketService.sendTyping(activeConversation.id, isTyping);
  },

  sendReaction: (messageId, messageTimestamp, emoji) => {
    const { activeConversation } = get();
    
    if (!activeConversation) {
      return;
    }

    socketService.sendReaction(activeConversation.id, messageId, messageTimestamp, emoji);
  },

  subscribeToEvents: () => {
    const unsubMessage = socketService.on<MessageReceiveEvent>('message:receive', (data) => {
      const { message, tempId } = data as any;
      
      // Normalize message - backend might send timestamp instead of createdAt
      const normalizedMessage = {
        ...message,
        createdAt: message.createdAt || (message as any).timestamp || new Date().toISOString(),
        status: 'sent' as const, // Mark as sent when received from server
      };
      
      set((state) => {
        let newMessages = state.messages;
        
        // If tempId is provided, replace the optimistic message
        if (tempId) {
          const tempIndex = state.messages.findIndex(m => m.id === tempId);
          if (tempIndex !== -1) {
            newMessages = [...state.messages];
            newMessages[tempIndex] = normalizedMessage;
          }
        } else {
          // Check if message already exists (avoid duplicates)
          const messageExists = state.messages.some(m => m.id === normalizedMessage.id);
          
          if (state.activeConversation?.id === normalizedMessage.conversationId && !messageExists) {
            newMessages = [...state.messages, normalizedMessage];
          }
        }

        const newConversations = state.conversations.map((c) => {
          if (c.id === normalizedMessage.conversationId) {
            return { 
              ...c, 
              lastMessage: normalizedMessage, 
              updatedAt: normalizedMessage.createdAt || c.updatedAt 
            };
          }
          return c;
        });

        // Safe sort with fallback dates
        newConversations.sort((a, b) => {
          const dateA = a.updatedAt ? new Date(a.updatedAt).getTime() : 0;
          const dateB = b.updatedAt ? new Date(b.updatedAt).getTime() : 0;
          return dateB - dateA;
        });

        return {
          messages: newMessages,
          conversations: newConversations,
        };
      });
    });

    const unsubTyping = socketService.on<TypingEvent>('message:typing', (data) => {
      set((state) => {
        const newTypingUsers = new Map(state.typingUsers);
        
        if (!newTypingUsers.has(data.conversationId)) {
          newTypingUsers.set(data.conversationId, new Set());
        }
        
        const users = newTypingUsers.get(data.conversationId)!;
        
        if (data.isTyping) {
          users.add(data.userId);
        } else {
          users.delete(data.userId);
        }
        
        return { typingUsers: newTypingUsers };
      });
    });

    const unsubReaction = socketService.on<ReactionEvent>('message:reaction', (data) => {
      set((state) => {
        const newMessages = state.messages.map((msg) => {
          if (msg.id === data.messageId) {
            // Convert reactions from {emoji: [userIds]} to {emoji: count}
            const reactionCounts: Record<string, number> = {};
            for (const [emoji, userIds] of Object.entries(data.reactions)) {
              reactionCounts[emoji] = userIds.length;
            }
            return { ...msg, reactions: reactionCounts };
          }
          return msg;
        });
        return { messages: newMessages };
      });
    });

    return () => {
      unsubMessage();
      unsubTyping();
      unsubReaction();
    };
  },
}));

