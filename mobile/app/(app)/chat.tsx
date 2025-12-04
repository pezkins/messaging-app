import { useState, useRef, useEffect, useCallback } from 'react';
import {
  View,
  Text,
  FlatList,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  KeyboardAvoidingView,
  Platform,
  ActivityIndicator,
} from 'react-native';
import { router } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { format } from 'date-fns';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useChatStore } from '../../src/store/chat';
import { useAuthStore } from '../../src/store/auth';
import { colors, spacing, borderRadius, fontSize } from '../../src/constants/theme';
import { getLanguageByCode } from '../../src/constants/languages';
import type { Message } from '../../src/types';

export default function ChatScreen() {
  const { user } = useAuthStore();
  const { 
    activeConversation, 
    messages, 
    sendMessage, 
    setTyping,
    typingUsers,
    isLoadingMessages,
    hasMoreMessages,
    loadMoreMessages,
    clearActiveConversation,
  } = useChatStore();

  const [inputValue, setInputValue] = useState('');
  const [isTypingLocal, setIsTypingLocal] = useState(false);
  const flatListRef = useRef<FlatList>(null);
  const typingTimeoutRef = useRef<NodeJS.Timeout>();

  useEffect(() => {
    return () => {
      if (typingTimeoutRef.current) {
        clearTimeout(typingTimeoutRef.current);
      }
    };
  }, []);

  const handleBack = () => {
    clearActiveConversation();
    router.back();
  };

  const handleInputChange = (text: string) => {
    setInputValue(text);
    
    if (!isTypingLocal && text) {
      setIsTypingLocal(true);
      setTyping(true);
    }
    
    if (typingTimeoutRef.current) {
      clearTimeout(typingTimeoutRef.current);
    }
    
    typingTimeoutRef.current = setTimeout(() => {
      setTyping(false);
      setIsTypingLocal(false);
    }, 2000);
  };

  const handleSend = () => {
    if (!inputValue.trim()) return;
    
    sendMessage(inputValue);
    setInputValue('');
    setIsTypingLocal(false);
    setTyping(false);
    
    if (typingTimeoutRef.current) {
      clearTimeout(typingTimeoutRef.current);
    }
  };

  const handleLoadMore = () => {
    if (hasMoreMessages && !isLoadingMessages) {
      loadMoreMessages();
    }
  };

  if (!activeConversation) {
    return null;
  }

  const otherParticipants = activeConversation.participants.filter((p) => p.id !== user?.id);
  const conversationName = activeConversation.name || otherParticipants.map((p) => p.username).join(', ');

  // Get typing users for this conversation
  const currentTypingUsers = typingUsers.get(activeConversation.id) || new Set();
  const typingUsernames = Array.from(currentTypingUsers)
    .filter((id) => id !== user?.id)
    .map((id) => activeConversation.participants.find((p) => p.id === id)?.username)
    .filter(Boolean);

  const handleReaction = useCallback((messageId: string, emoji: string) => {
    // TODO: Send reaction to backend via WebSocket
    console.log('React to message:', messageId, emoji);
    // For now, this is a UI-only feature. Backend integration can be added later.
  }, []);

  const renderMessage = ({ item, index }: { item: Message; index: number }) => {
    const isOwn = item.senderId === user?.id;
    const itemTime = item.createdAt ? new Date(item.createdAt).getTime() : 0;
    const prevTime = messages[index - 1]?.createdAt ? new Date(messages[index - 1].createdAt).getTime() : 0;
    const showTimestamp = index === 0 || (itemTime - prevTime > 5 * 60 * 1000);

    return (
      <MessageBubble 
        message={item} 
        isOwn={isOwn} 
        showTimestamp={showTimestamp}
        onReact={handleReaction}
      />
    );
  };

  return (
    <SafeAreaView style={styles.container} edges={['top']}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity style={styles.backButton} onPress={handleBack}>
          <Ionicons name="arrow-back" size={24} color={colors.white} />
        </TouchableOpacity>
        
        <View style={[
          styles.avatar,
          activeConversation.type === 'group' ? styles.avatarGroup : styles.avatarDirect
        ]}>
          <Text style={styles.avatarText}>
            {activeConversation.type === 'group' ? '👥' : otherParticipants[0]?.username?.charAt(0).toUpperCase()}
          </Text>
        </View>
        
        <View style={styles.headerInfo}>
          <Text style={styles.headerTitle} numberOfLines={1}>{conversationName}</Text>
          <Text style={styles.headerSubtitle}>
            {activeConversation.type === 'group' 
              ? `${activeConversation.participants.length} members`
              : 'Direct message'}
          </Text>
        </View>
      </View>

      {/* Messages */}
      <KeyboardAvoidingView 
        style={styles.messagesContainer}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        keyboardVerticalOffset={0}
      >
        <FlatList
          ref={flatListRef}
          data={messages}
          keyExtractor={(item) => item.id}
          renderItem={renderMessage}
          contentContainerStyle={styles.messagesList}
          inverted={false}
          onEndReached={handleLoadMore}
          onEndReachedThreshold={0.5}
          ListHeaderComponent={
            hasMoreMessages ? (
              <TouchableOpacity style={styles.loadMore} onPress={handleLoadMore}>
                {isLoadingMessages ? (
                  <ActivityIndicator size="small" color={colors.primary[400]} />
                ) : (
                  <Text style={styles.loadMoreText}>Load older messages</Text>
                )}
              </TouchableOpacity>
            ) : null
          }
          ListEmptyComponent={
            isLoadingMessages ? (
              <View style={styles.emptyState}>
                <ActivityIndicator size="large" color={colors.primary[500]} />
              </View>
            ) : (
              <View style={styles.emptyState}>
                <Ionicons name="chatbubble-ellipses-outline" size={48} color={colors.surface[600]} />
                <Text style={styles.emptyText}>No messages yet</Text>
                <Text style={styles.emptySubtext}>Send a message to start the conversation</Text>
              </View>
            )
          }
          onContentSizeChange={() => {
            if (messages.length > 0) {
              flatListRef.current?.scrollToEnd({ animated: true });
            }
          }}
        />

        {/* Typing indicator */}
        {typingUsernames.length > 0 && (
          <View style={styles.typingContainer}>
            <View style={styles.typingBubble}>
              <View style={styles.typingDots}>
                <View style={[styles.dot, styles.dot1]} />
                <View style={[styles.dot, styles.dot2]} />
                <View style={[styles.dot, styles.dot3]} />
              </View>
            </View>
            <Text style={styles.typingText}>
              {typingUsernames.length === 1 
                ? `${typingUsernames[0]} is typing...`
                : `${typingUsernames[0]} and others are typing...`}
            </Text>
          </View>
        )}

        {/* Input */}
        <View style={styles.inputContainer}>
          <TextInput
            style={styles.input}
            placeholder="Type a message..."
            placeholderTextColor={colors.surface[500]}
            value={inputValue}
            onChangeText={handleInputChange}
            multiline
            maxLength={5000}
          />
          <TouchableOpacity 
            style={[styles.sendButton, !inputValue.trim() && styles.sendButtonDisabled]}
            onPress={handleSend}
            disabled={!inputValue.trim()}
          >
            <Ionicons name="send" size={20} color={colors.white} />
          </TouchableOpacity>
        </View>
        
        <Text style={styles.translationHint}>
          Messages translate automatically to each person's language
        </Text>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

// Quick emoji reactions
const REACTION_EMOJIS = ['❤️', '👍', '😂', '😮', '😢', '😡'];

// Message Bubble Component
function MessageBubble({ message, isOwn, showTimestamp, onReact }: { 
  message: Message; 
  isOwn: boolean;
  showTimestamp: boolean;
  onReact?: (messageId: string, emoji: string) => void;
}) {
  const [showOriginal, setShowOriginal] = useState(false);
  const [showReactionPicker, setShowReactionPicker] = useState(false);
  
  const displayContent = showOriginal 
    ? message.originalContent 
    : (message.translatedContent || message.originalContent);

  const wasTranslated = message.translatedContent && message.originalContent !== message.translatedContent;

  const messageDate = message.createdAt ? new Date(message.createdAt) : new Date();

  // Get reactions from message (if any)
  const reactions = (message as any).reactions || {};
  const hasReactions = Object.keys(reactions).length > 0;

  const handleLongPress = () => {
    setShowReactionPicker(true);
  };

  const handleReaction = (emoji: string) => {
    setShowReactionPicker(false);
    onReact?.(message.id, emoji);
  };
  
  return (
    <View style={styles.messageWrapper}>
      {showTimestamp && message.createdAt && (
        <Text style={styles.timestamp}>
          {format(messageDate, 'MMM d, h:mm a')}
        </Text>
      )}

      {/* Reaction Picker */}
      {showReactionPicker && (
        <View style={[
          styles.reactionPicker,
          isOwn ? styles.reactionPickerOwn : styles.reactionPickerOther
        ]}>
          {REACTION_EMOJIS.map((emoji) => (
            <TouchableOpacity
              key={emoji}
              style={styles.reactionOption}
              onPress={() => handleReaction(emoji)}
            >
              <Text style={styles.reactionEmoji}>{emoji}</Text>
            </TouchableOpacity>
          ))}
          <TouchableOpacity
            style={styles.reactionClose}
            onPress={() => setShowReactionPicker(false)}
          >
            <Ionicons name="close" size={16} color={colors.surface[400]} />
          </TouchableOpacity>
        </View>
      )}
      
      <TouchableOpacity 
        activeOpacity={0.8}
        onLongPress={handleLongPress}
        delayLongPress={300}
        style={[styles.messageBubble, isOwn ? styles.ownMessage : styles.otherMessage]}
      >
        {!isOwn && (
          <Text style={styles.senderName}>{message.sender?.username || 'Unknown'}</Text>
        )}
        
        <Text style={[styles.messageText, isOwn && styles.ownMessageText]}>
          {displayContent}
        </Text>
        
        {wasTranslated && (
          <TouchableOpacity 
            onPress={() => setShowOriginal(!showOriginal)}
            style={styles.translationToggle}
          >
            <Ionicons 
              name="language" 
              size={14} 
              color={isOwn ? colors.primary[200] : colors.surface[400]} 
            />
            <Text style={[
              styles.translationToggleText,
              isOwn && styles.ownTranslationText
            ]}>
              {showOriginal 
                ? `Original (${getLanguageByCode(message.originalLanguage)?.name || message.originalLanguage})` 
                : `Translated from ${getLanguageByCode(message.originalLanguage)?.name || message.originalLanguage}`}
            </Text>
          </TouchableOpacity>
        )}
        
        <Text style={[styles.messageTime, isOwn && styles.ownMessageTime]}>
          {format(messageDate, 'h:mm a')}
        </Text>
      </TouchableOpacity>

      {/* Display Reactions */}
      {hasReactions && (
        <View style={[
          styles.reactionsContainer,
          isOwn ? styles.reactionsOwn : styles.reactionsOther
        ]}>
          {Object.entries(reactions).map(([emoji, users]: [string, any]) => (
            <View key={emoji} style={styles.reactionBadge}>
              <Text style={styles.reactionBadgeEmoji}>{emoji}</Text>
              {Array.isArray(users) && users.length > 1 && (
                <Text style={styles.reactionCount}>{users.length}</Text>
              )}
            </View>
          ))}
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.surface[950],
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: spacing.md,
    borderBottomWidth: 1,
    borderBottomColor: colors.surface[800],
  },
  backButton: {
    padding: spacing.sm,
    marginRight: spacing.sm,
  },
  avatar: {
    width: 40,
    height: 40,
    borderRadius: borderRadius.full,
    justifyContent: 'center',
    alignItems: 'center',
  },
  avatarDirect: {
    backgroundColor: colors.primary[600],
  },
  avatarGroup: {
    backgroundColor: colors.accent[600],
  },
  avatarText: {
    color: colors.white,
    fontSize: fontSize.lg,
    fontWeight: '600',
  },
  headerInfo: {
    flex: 1,
    marginLeft: spacing.sm,
  },
  headerTitle: {
    color: colors.white,
    fontSize: fontSize.lg,
    fontWeight: '600',
  },
  headerSubtitle: {
    color: colors.surface[400],
    fontSize: fontSize.sm,
  },
  messagesContainer: {
    flex: 1,
  },
  messagesList: {
    padding: spacing.md,
    flexGrow: 1,
  },
  loadMore: {
    alignItems: 'center',
    paddingVertical: spacing.md,
  },
  loadMoreText: {
    color: colors.primary[400],
    fontSize: fontSize.sm,
  },
  emptyState: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: spacing.xxl * 2,
  },
  emptyText: {
    color: colors.surface[400],
    fontSize: fontSize.lg,
    fontWeight: '500',
    marginTop: spacing.md,
  },
  emptySubtext: {
    color: colors.surface[500],
    fontSize: fontSize.sm,
    marginTop: spacing.xs,
  },
  messageWrapper: {
    marginBottom: spacing.sm,
  },
  timestamp: {
    textAlign: 'center',
    color: colors.surface[500],
    fontSize: fontSize.xs,
    marginVertical: spacing.md,
  },
  messageBubble: {
    maxWidth: '80%',
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    borderRadius: borderRadius.xl,
  },
  ownMessage: {
    alignSelf: 'flex-end',
    backgroundColor: colors.primary[600],
    borderBottomRightRadius: borderRadius.sm,
  },
  otherMessage: {
    alignSelf: 'flex-start',
    backgroundColor: colors.surface[800],
    borderBottomLeftRadius: borderRadius.sm,
  },
  senderName: {
    color: colors.primary[400],
    fontSize: fontSize.xs,
    fontWeight: '500',
    marginBottom: spacing.xs,
  },
  messageText: {
    color: colors.surface[100],
    fontSize: fontSize.md,
    lineHeight: 22,
  },
  ownMessageText: {
    color: colors.white,
  },
  translationToggle: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: spacing.xs,
    gap: spacing.xs,
  },
  translationToggleText: {
    color: colors.surface[400],
    fontSize: fontSize.xs,
  },
  ownTranslationText: {
    color: colors.primary[200],
  },
  messageTime: {
    color: colors.surface[500],
    fontSize: fontSize.xs,
    marginTop: spacing.xs,
    textAlign: 'right',
  },
  ownMessageTime: {
    color: colors.primary[200],
  },
  typingContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
  },
  typingBubble: {
    backgroundColor: colors.surface[800],
    borderRadius: borderRadius.xl,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
  },
  typingDots: {
    flexDirection: 'row',
    gap: 4,
  },
  dot: {
    width: 6,
    height: 6,
    borderRadius: 3,
    backgroundColor: colors.surface[400],
  },
  dot1: {
    opacity: 0.4,
  },
  dot2: {
    opacity: 0.7,
  },
  dot3: {
    opacity: 1,
  },
  typingText: {
    color: colors.surface[500],
    fontSize: fontSize.sm,
    marginLeft: spacing.sm,
  },
  inputContainer: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    padding: spacing.md,
    paddingBottom: spacing.sm,
    gap: spacing.sm,
  },
  input: {
    flex: 1,
    backgroundColor: colors.surface[800],
    borderRadius: borderRadius.xl,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    color: colors.white,
    fontSize: fontSize.md,
    maxHeight: 100,
    borderWidth: 1,
    borderColor: colors.surface[700],
  },
  sendButton: {
    width: 44,
    height: 44,
    borderRadius: borderRadius.full,
    backgroundColor: colors.primary[500],
    justifyContent: 'center',
    alignItems: 'center',
  },
  sendButtonDisabled: {
    backgroundColor: colors.surface[700],
  },
  translationHint: {
    textAlign: 'center',
    color: colors.surface[500],
    fontSize: fontSize.xs,
    paddingBottom: spacing.sm,
  },
  reactionPicker: {
    flexDirection: 'row',
    backgroundColor: colors.surface[800],
    borderRadius: borderRadius.full,
    padding: spacing.xs,
    marginBottom: spacing.xs,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: colors.surface[700],
    shadowColor: colors.black,
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.25,
    shadowRadius: 4,
    elevation: 5,
  },
  reactionPickerOwn: {
    alignSelf: 'flex-end',
  },
  reactionPickerOther: {
    alignSelf: 'flex-start',
  },
  reactionOption: {
    padding: spacing.xs,
  },
  reactionEmoji: {
    fontSize: 22,
  },
  reactionClose: {
    padding: spacing.xs,
    marginLeft: spacing.xs,
  },
  reactionsContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    marginTop: spacing.xs,
    gap: spacing.xs,
  },
  reactionsOwn: {
    justifyContent: 'flex-end',
  },
  reactionsOther: {
    justifyContent: 'flex-start',
  },
  reactionBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surface[800],
    borderRadius: borderRadius.full,
    paddingHorizontal: spacing.sm,
    paddingVertical: spacing.xs,
    borderWidth: 1,
    borderColor: colors.surface[700],
  },
  reactionBadgeEmoji: {
    fontSize: 14,
  },
  reactionCount: {
    color: colors.surface[300],
    fontSize: fontSize.xs,
    marginLeft: spacing.xs,
  },
});

