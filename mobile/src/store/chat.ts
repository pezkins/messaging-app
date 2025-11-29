import { create } from 'zustand';
import { api } from '../services/api';
import { socketService, type MessageReceiveEvent, type TypingEvent } from '../services/socket';
import type { Conversation, Message, UserPublic } from '../types';

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
  sendMessage: (content: string) => void;
  startConversation: (user: UserPublic) => Promise<Conversation>;
  setTyping: (isTyping: boolean) => void;
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

  sendMessage: (content) => {
    const { activeConversation } = get();
    
    if (!activeConversation || !content.trim()) {
      return;
    }

    socketService.sendMessage({
      conversationId: activeConversation.id,
      content: content.trim(),
      type: 'TEXT',
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

  setTyping: (isTyping) => {
    const { activeConversation } = get();
    
    if (!activeConversation) {
      return;
    }

    socketService.sendTyping(activeConversation.id, isTyping);
  },

  subscribeToEvents: () => {
    const unsubMessage = socketService.on<MessageReceiveEvent>('message:receive', (data) => {
      const { message } = data;
      
      set((state) => {
        const newMessages = state.activeConversation?.id === message.conversationId
          ? [...state.messages, message]
          : state.messages;

        const newConversations = state.conversations.map((c) => {
          if (c.id === message.conversationId) {
            return { ...c, lastMessage: message, updatedAt: message.createdAt };
          }
          return c;
        });

        newConversations.sort((a, b) => 
          new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime()
        );

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

    return () => {
      unsubMessage();
      unsubTyping();
    };
  },
}));

