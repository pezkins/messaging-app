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
  Modal,
  ScrollView,
  Image,
} from 'react-native';
import { router } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { format } from 'date-fns';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useChatStore } from '../../src/store/chat';
import { useAuthStore } from '../../src/store/auth';
import { colors, spacing, borderRadius, fontSize } from '../../src/constants/theme';
import { getLanguageByCode } from '../../src/constants/languages';
import { EMOJIS, searchEmojis, FREQUENT_EMOJIS, type EmojiData } from '../../src/constants/emojis';
import { AttachmentPicker } from '../../src/components/AttachmentPicker';
import { GifPicker } from '../../src/components/GifPicker';
import { api } from '../../src/services/api';
import type { Message, Attachment } from '../../src/types';

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
    sendReaction,
  } = useChatStore();

  const [inputValue, setInputValue] = useState('');
  const [isTypingLocal, setIsTypingLocal] = useState(false);
  const [showAttachmentPicker, setShowAttachmentPicker] = useState(false);
  const [showGifPicker, setShowGifPicker] = useState(false);
  const [pendingAttachment, setPendingAttachment] = useState<{ attachment: Attachment; localUri: string } | null>(null);
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
    if (!inputValue.trim() && !pendingAttachment) return;
    
    if (pendingAttachment) {
      // Send attachment message
      sendMessage('', {
        type: 'ATTACHMENT',
        attachment: pendingAttachment.attachment,
        localUri: pendingAttachment.localUri,
      });
    } else {
      // Send text message
      sendMessage(inputValue);
    }
    
    setInputValue('');
    setPendingAttachment(null);
    setIsTypingLocal(false);
    setTyping(false);
    
    if (typingTimeoutRef.current) {
      clearTimeout(typingTimeoutRef.current);
    }
  };

  const handleAttachmentReady = (attachment: Attachment, localUri: string) => {
    setPendingAttachment({ attachment, localUri });
  };

  const handleGifSelect = (gifUrl: string) => {
    // Send GIF as a message with GIF type
    sendMessage(gifUrl, { type: 'GIF' });
  };

  const clearPendingAttachment = () => {
    setPendingAttachment(null);
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

  const handleReaction = useCallback((messageId: string, messageTimestamp: string, emoji: string) => {
    sendReaction(messageId, messageTimestamp, emoji);
  }, [sendReaction]);

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

        {/* Pending Attachment Preview */}
        {pendingAttachment && (
          <View style={styles.attachmentPreview}>
            {pendingAttachment.attachment.category === 'image' ? (
              <Image 
                source={{ uri: pendingAttachment.localUri }} 
                style={styles.attachmentThumbnail}
                resizeMode="cover"
              />
            ) : (
              <View style={styles.attachmentFile}>
                <Ionicons name="document" size={24} color={colors.primary[400]} />
              </View>
            )}
            <Text style={styles.attachmentName} numberOfLines={1}>
              {pendingAttachment.attachment.fileName}
            </Text>
            <TouchableOpacity onPress={clearPendingAttachment}>
              <Ionicons name="close-circle" size={20} color={colors.surface[400]} />
            </TouchableOpacity>
          </View>
        )}

        {/* Input */}
        <View style={styles.inputContainer}>
          <TouchableOpacity 
            style={styles.attachButton}
            onPress={() => setShowAttachmentPicker(true)}
          >
            <Ionicons name="add-circle-outline" size={26} color={colors.surface[400]} />
          </TouchableOpacity>
          
          <TouchableOpacity 
            style={styles.gifButton}
            onPress={() => setShowGifPicker(true)}
          >
            <Text style={styles.gifButtonText}>GIF</Text>
          </TouchableOpacity>
          
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
            style={[styles.sendButton, (!inputValue.trim() && !pendingAttachment) && styles.sendButtonDisabled]}
            onPress={handleSend}
            disabled={!inputValue.trim() && !pendingAttachment}
          >
            <Ionicons name="send" size={20} color={colors.white} />
          </TouchableOpacity>
        </View>
        
        <Text style={styles.translationHint}>
          Messages translate automatically to each person's language
        </Text>

        {/* Attachment Picker */}
        {activeConversation && (
          <AttachmentPicker
            visible={showAttachmentPicker}
            onClose={() => setShowAttachmentPicker(false)}
            conversationId={activeConversation.id}
            onAttachmentReady={handleAttachmentReady}
          />
        )}

        {/* GIF Picker */}
        <GifPicker
          visible={showGifPicker}
          onClose={() => setShowGifPicker(false)}
          onSelectGif={handleGifSelect}
        />
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

// Quick emoji reactions - common reactions (shown in first row)
const QUICK_REACTIONS = ['❤️', '👍', '👎', '😂', '😮', '😢', '😡', '🔥', '🎉', '🤔', '👀', '💯'];

// Emoji Picker Component
function EmojiPicker({ 
  visible, 
  onClose, 
  onSelect,
  position 
}: { 
  visible: boolean; 
  onClose: () => void; 
  onSelect: (emoji: string) => void;
  position: 'left' | 'right';
}) {
  const [searchQuery, setSearchQuery] = useState('');
  const [filteredEmojis, setFilteredEmojis] = useState<EmojiData[]>([]);

  useEffect(() => {
    if (searchQuery) {
      setFilteredEmojis(searchEmojis(searchQuery));
    } else {
      setFilteredEmojis([]);
    }
  }, [searchQuery]);

  const emojisToShow = searchQuery ? filteredEmojis : [];

  return (
    <Modal
      visible={visible}
      transparent
      animationType="fade"
      onRequestClose={onClose}
    >
      <TouchableOpacity 
        style={styles.emojiModalOverlay} 
        activeOpacity={1} 
        onPress={onClose}
      >
        <TouchableOpacity 
          activeOpacity={1} 
          style={[
            styles.emojiPickerContainer,
            position === 'right' ? styles.emojiPickerRight : styles.emojiPickerLeft
          ]}
        >
          {/* Quick Reactions Row */}
          <View style={styles.quickReactionsRow}>
            {QUICK_REACTIONS.map((emoji) => (
              <TouchableOpacity
                key={emoji}
                style={styles.quickReactionButton}
                onPress={() => onSelect(emoji)}
              >
                <Text style={styles.quickReactionEmoji}>{emoji}</Text>
              </TouchableOpacity>
            ))}
          </View>

          {/* Search Input */}
          <View style={styles.emojiSearchContainer}>
            <Ionicons name="search" size={16} color={colors.surface[400]} />
            <TextInput
              style={styles.emojiSearchInput}
              placeholder="Search emojis..."
              placeholderTextColor={colors.surface[500]}
              value={searchQuery}
              onChangeText={setSearchQuery}
              autoCapitalize="none"
            />
            {searchQuery ? (
              <TouchableOpacity onPress={() => setSearchQuery('')}>
                <Ionicons name="close-circle" size={18} color={colors.surface[400]} />
              </TouchableOpacity>
            ) : null}
          </View>

          {/* Search Results */}
          {searchQuery && (
            <ScrollView 
              style={styles.emojiSearchResults}
              contentContainerStyle={styles.emojiGrid}
              keyboardShouldPersistTaps="handled"
            >
              {emojisToShow.length > 0 ? (
                emojisToShow.slice(0, 50).map((emojiData, index) => (
                  <TouchableOpacity
                    key={`${emojiData.emoji}-${index}`}
                    style={styles.emojiGridItem}
                    onPress={() => onSelect(emojiData.emoji)}
                  >
                    <Text style={styles.emojiGridEmoji}>{emojiData.emoji}</Text>
                  </TouchableOpacity>
                ))
              ) : (
                <Text style={styles.noEmojisText}>No emojis found</Text>
              )}
            </ScrollView>
          )}

          {/* Hint text when not searching */}
          {!searchQuery && (
            <Text style={styles.emojiHint}>
              Search for any emoji by name
            </Text>
          )}
        </TouchableOpacity>
      </TouchableOpacity>
    </Modal>
  );
}

// Helper to detect and extract GIF URL from message
function extractGifUrl(content: string): string | null {
  // Check for [GIF] prefix format
  const gifPrefixMatch = content.match(/^\[GIF\]\s*(https?:\/\/[^\s]+)/i);
  if (gifPrefixMatch) return gifPrefixMatch[1];
  
  // Check for direct Tenor/Giphy URLs
  const gifUrlMatch = content.match(/^(https?:\/\/(?:media\.tenor\.com|media\d*\.giphy\.com)[^\s]+\.gif)/i);
  if (gifUrlMatch) return gifUrlMatch[1];
  
  // Check if the content itself is a direct GIF URL (for GIF type messages)
  if (content.match(/^https?:\/\/.*\.(gif|webp)/i)) {
    return content;
  }
  
  return null;
}

// Attachment Bubble Component
function AttachmentBubble({ attachment, isOwn }: { 
  attachment?: Attachment; 
  isOwn: boolean;
}) {
  const [loading, setLoading] = useState(true);
  const [downloadUrl, setDownloadUrl] = useState<string | null>(null);
  const [error, setError] = useState(false);

  useEffect(() => {
    const fetchDownloadUrl = async () => {
      if (!attachment) return;
      
      // If we have a local URI, use it directly
      if (attachment.localUri) {
        setDownloadUrl(attachment.localUri);
        setLoading(false);
        return;
      }

      // Otherwise, fetch presigned URL
      try {
        const { downloadUrl: url } = await api.getDownloadUrl(attachment.key);
        setDownloadUrl(url);
      } catch (err) {
        console.error('Failed to get download URL:', err);
        setError(true);
      } finally {
        setLoading(false);
      }
    };

    fetchDownloadUrl();
  }, [attachment]);

  if (!attachment) {
    return (
      <View style={styles.attachmentPlaceholder}>
        <Ionicons name="document-outline" size={24} color={colors.surface[400]} />
        <Text style={styles.attachmentPlaceholderText}>Attachment</Text>
      </View>
    );
  }

  const isImage = attachment.category === 'image' || attachment.contentType.startsWith('image/');
  const isVideo = attachment.category === 'video' || attachment.contentType.startsWith('video/');

  if (loading) {
    return (
      <View style={styles.attachmentLoading}>
        <ActivityIndicator size="small" color={colors.primary[400]} />
      </View>
    );
  }

  if (error || !downloadUrl) {
    return (
      <View style={styles.attachmentError}>
        <Ionicons name="alert-circle-outline" size={24} color={colors.error} />
        <Text style={styles.attachmentErrorText}>Failed to load</Text>
      </View>
    );
  }

  if (isImage) {
    return (
      <View style={styles.attachmentImageContainer}>
        <Image
          source={{ uri: downloadUrl }}
          style={styles.attachmentImage}
          resizeMode="cover"
        />
        <Text style={[styles.attachmentFileName, isOwn && styles.ownAttachmentFileName]} numberOfLines={1}>
          {attachment.fileName}
        </Text>
      </View>
    );
  }

  // For documents and other files
  return (
    <View style={styles.attachmentDocument}>
      <Ionicons 
        name={isVideo ? 'videocam' : 'document'} 
        size={32} 
        color={isOwn ? colors.primary[200] : colors.primary[400]} 
      />
      <View style={styles.attachmentDocInfo}>
        <Text style={[styles.attachmentFileName, isOwn && styles.ownAttachmentFileName]} numberOfLines={1}>
          {attachment.fileName}
        </Text>
        <Text style={[styles.attachmentFileSize, isOwn && styles.ownAttachmentFileSize]}>
          {formatFileSize(attachment.fileSize)}
        </Text>
      </View>
    </View>
  );
}

// Helper to format file size
function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

// Message Bubble Component
function MessageBubble({ message, isOwn, showTimestamp, onReact }: { 
  message: Message; 
  isOwn: boolean;
  showTimestamp: boolean;
  onReact?: (messageId: string, messageTimestamp: string, emoji: string) => void;
}) {
  const [showOriginal, setShowOriginal] = useState(false);
  const [showReactionPicker, setShowReactionPicker] = useState(false);
  const [gifLoading, setGifLoading] = useState(true);
  
  const displayContent = showOriginal 
    ? message.originalContent 
    : (message.translatedContent || message.originalContent);

  const wasTranslated = message.translatedContent && message.originalContent !== message.translatedContent;

  const messageDate = message.createdAt ? new Date(message.createdAt) : new Date();
  const messageTimestamp = message.createdAt || new Date().toISOString();

  // Get reactions from message (if any)
  const reactions = (message as any).reactions || {};
  const hasReactions = Object.keys(reactions).length > 0;

  // Check if this is a GIF message
  const gifUrl = extractGifUrl(message.originalContent);
  const isGifMessage = !!gifUrl;

  const handleLongPress = () => {
    setShowReactionPicker(true);
  };

  const handleReaction = (emoji: string) => {
    setShowReactionPicker(false);
    onReact?.(message.id, messageTimestamp, emoji);
  };
  
  return (
    <View style={styles.messageWrapper}>
      {showTimestamp && message.createdAt && (
        <Text style={styles.timestamp}>
          {format(messageDate, 'MMM d, h:mm a')}
        </Text>
      )}

      {/* Emoji Picker Modal */}
      <EmojiPicker
        visible={showReactionPicker}
        onClose={() => setShowReactionPicker(false)}
        onSelect={handleReaction}
        position={isOwn ? 'right' : 'left'}
      />
      
      <TouchableOpacity 
        activeOpacity={0.8}
        onLongPress={handleLongPress}
        delayLongPress={300}
        style={[
          styles.messageBubble, 
          isOwn ? styles.ownMessage : styles.otherMessage,
          isGifMessage && styles.gifMessageBubble
        ]}
      >
        {!isOwn && (
          <Text style={styles.senderName}>{message.sender?.username || 'Unknown'}</Text>
        )}
        
        {isGifMessage ? (
          <View style={styles.gifContainer}>
            {gifLoading && (
              <View style={styles.gifLoadingContainer}>
                <ActivityIndicator size="small" color={colors.primary[400]} />
              </View>
            )}
            <Image
              source={{ uri: gifUrl }}
              style={styles.gifImage}
              resizeMode="contain"
              onLoadStart={() => setGifLoading(true)}
              onLoadEnd={() => setGifLoading(false)}
            />
          </View>
        ) : message.attachment || message.type === 'attachment' ? (
          <AttachmentBubble attachment={message.attachment} isOwn={isOwn} />
        ) : (
          <Text style={[styles.messageText, isOwn && styles.ownMessageText]}>
            {displayContent}
          </Text>
        )}
        
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
        
        <View style={styles.messageFooter}>
          <Text style={[styles.messageTime, isOwn && styles.ownMessageTime]}>
            {format(messageDate, 'h:mm a')}
          </Text>
          {isOwn && (
            <View style={styles.statusIndicator}>
              {message.status === 'sending' ? (
                <Ionicons name="time-outline" size={12} color={colors.primary[200]} />
              ) : message.status === 'sent' ? (
                <Ionicons name="checkmark" size={12} color={colors.primary[200]} />
              ) : message.status === 'delivered' ? (
                <Ionicons name="checkmark-done" size={12} color={colors.primary[200]} />
              ) : message.status === 'seen' ? (
                <Ionicons name="checkmark-done" size={12} color={colors.accent[400]} />
              ) : message.status === 'failed' ? (
                <Ionicons name="alert-circle" size={12} color={colors.error} />
              ) : (
                <Ionicons name="checkmark" size={12} color={colors.primary[200]} />
              )}
            </View>
          )}
        </View>
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
  messageFooter: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'flex-end',
    marginTop: spacing.xs,
    gap: spacing.xs,
  },
  messageTime: {
    color: colors.surface[500],
    fontSize: fontSize.xs,
  },
  ownMessageTime: {
    color: colors.primary[200],
  },
  statusIndicator: {
    marginLeft: 2,
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
  attachmentPreview: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surface[800],
    marginHorizontal: spacing.md,
    marginTop: spacing.sm,
    padding: spacing.sm,
    borderRadius: borderRadius.lg,
    gap: spacing.sm,
  },
  attachmentThumbnail: {
    width: 40,
    height: 40,
    borderRadius: borderRadius.md,
  },
  attachmentFile: {
    width: 40,
    height: 40,
    borderRadius: borderRadius.md,
    backgroundColor: colors.surface[700],
    justifyContent: 'center',
    alignItems: 'center',
  },
  attachmentName: {
    flex: 1,
    color: colors.surface[300],
    fontSize: fontSize.sm,
  },
  inputContainer: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    padding: spacing.md,
    paddingBottom: spacing.sm,
    gap: spacing.xs,
  },
  attachButton: {
    padding: spacing.xs,
    justifyContent: 'center',
    alignItems: 'center',
  },
  gifButton: {
    paddingHorizontal: spacing.sm,
    paddingVertical: spacing.xs,
    backgroundColor: colors.surface[800],
    borderRadius: borderRadius.md,
    borderWidth: 1,
    borderColor: colors.surface[600],
  },
  gifButtonText: {
    color: colors.surface[400],
    fontSize: fontSize.xs,
    fontWeight: '600',
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
  // Emoji Picker Modal Styles
  emojiModalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  emojiPickerContainer: {
    backgroundColor: colors.surface[900],
    borderRadius: borderRadius.xl,
    padding: spacing.md,
    width: '90%',
    maxWidth: 380,
    maxHeight: '60%',
    borderWidth: 1,
    borderColor: colors.surface[700],
  },
  emojiPickerLeft: {},
  emojiPickerRight: {},
  quickReactionsRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'center',
    marginBottom: spacing.md,
    paddingBottom: spacing.md,
    borderBottomWidth: 1,
    borderBottomColor: colors.surface[700],
  },
  quickReactionButton: {
    padding: spacing.xs,
    margin: 2,
  },
  quickReactionEmoji: {
    fontSize: 26,
  },
  emojiSearchContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surface[800],
    borderRadius: borderRadius.lg,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    marginBottom: spacing.sm,
  },
  emojiSearchInput: {
    flex: 1,
    color: colors.white,
    fontSize: fontSize.md,
    marginLeft: spacing.sm,
    paddingVertical: 0,
  },
  emojiSearchResults: {
    maxHeight: 200,
  },
  emojiGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'flex-start',
  },
  emojiGridItem: {
    width: '12.5%',
    aspectRatio: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  emojiGridEmoji: {
    fontSize: 24,
  },
  noEmojisText: {
    color: colors.surface[500],
    fontSize: fontSize.sm,
    textAlign: 'center',
    padding: spacing.md,
  },
  emojiHint: {
    color: colors.surface[500],
    fontSize: fontSize.sm,
    textAlign: 'center',
    marginTop: spacing.sm,
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
  // GIF styles
  gifMessageBubble: {
    padding: spacing.xs,
    backgroundColor: 'transparent',
  },
  gifContainer: {
    width: 200,
    minHeight: 150,
    borderRadius: borderRadius.lg,
    overflow: 'hidden',
    backgroundColor: colors.surface[800],
  },
  gifImage: {
    width: '100%',
    height: 150,
    borderRadius: borderRadius.lg,
  },
  gifLoadingContainer: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: colors.surface[800],
    borderRadius: borderRadius.lg,
  },
  // Attachment styles
  attachmentPlaceholder: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    padding: spacing.sm,
  },
  attachmentPlaceholderText: {
    color: colors.surface[400],
    fontSize: fontSize.sm,
  },
  attachmentLoading: {
    width: 200,
    height: 150,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: colors.surface[800],
    borderRadius: borderRadius.lg,
  },
  attachmentError: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    padding: spacing.sm,
  },
  attachmentErrorText: {
    color: colors.error,
    fontSize: fontSize.sm,
  },
  attachmentImageContainer: {
    width: 200,
    borderRadius: borderRadius.lg,
    overflow: 'hidden',
  },
  attachmentImage: {
    width: '100%',
    height: 150,
    borderRadius: borderRadius.lg,
  },
  attachmentFileName: {
    color: colors.surface[300],
    fontSize: fontSize.xs,
    marginTop: spacing.xs,
  },
  ownAttachmentFileName: {
    color: colors.primary[200],
  },
  attachmentDocument: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    padding: spacing.xs,
    minWidth: 180,
  },
  attachmentDocInfo: {
    flex: 1,
  },
  attachmentFileSize: {
    color: colors.surface[500],
    fontSize: fontSize.xs,
  },
  ownAttachmentFileSize: {
    color: colors.primary[300],
  },
});

