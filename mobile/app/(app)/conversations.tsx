import { useState } from 'react';
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  StyleSheet,
  RefreshControl,
  TextInput,
  ActivityIndicator,
} from 'react-native';
import { router } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { formatDistanceToNow } from 'date-fns';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useChatStore } from '../../src/store/chat';
import { useAuthStore } from '../../src/store/auth';
import { colors, spacing, borderRadius, fontSize } from '../../src/constants/theme';
import { SUPPORTED_LANGUAGES, LANGUAGE_FLAGS, type LanguageCode } from '../../src/constants/languages';
import type { Conversation } from '../../src/types';

export default function ConversationsScreen() {
  const { user, logout } = useAuthStore();
  const { 
    conversations, 
    selectConversation, 
    isLoadingConversations,
    loadConversations 
  } = useChatStore();
  
  const [searchQuery, setSearchQuery] = useState('');
  const [showMenu, setShowMenu] = useState(false);

  const filteredConversations = conversations.filter((conv) => {
    const name = getConversationName(conv);
    return name.toLowerCase().includes(searchQuery.toLowerCase());
  });

  function getConversationName(conv: Conversation) {
    if (conv.name) return conv.name;
    const otherParticipants = conv.participants.filter((p) => p.id !== user?.id);
    return otherParticipants.map((p) => p.username).join(', ') || 'Unknown';
  }

  function getConversationAvatar(conv: Conversation) {
    if (conv.type === 'group') return '👥';
    const other = conv.participants.find((p) => p.id !== user?.id);
    return other?.username?.charAt(0).toUpperCase() || '?';
  }

  const handleConversationPress = async (conv: Conversation) => {
    await selectConversation(conv);
    router.push('/(app)/chat');
  };

  const renderConversation = ({ item }: { item: Conversation }) => (
    <TouchableOpacity
      style={styles.conversationItem}
      onPress={() => handleConversationPress(item)}
      activeOpacity={0.7}
    >
      <View style={[
        styles.avatar,
        item.type === 'group' ? styles.avatarGroup : styles.avatarDirect
      ]}>
        <Text style={styles.avatarText}>{getConversationAvatar(item)}</Text>
      </View>
      
      <View style={styles.conversationContent}>
        <View style={styles.conversationHeader}>
          <Text style={styles.conversationName} numberOfLines={1}>
            {getConversationName(item)}
          </Text>
          {item.lastMessage?.createdAt && (
            <Text style={styles.conversationTime}>
              {formatDistanceToNow(new Date(item.lastMessage.createdAt), { addSuffix: false })}
            </Text>
          )}
        </View>
        
        {item.lastMessage && (
          <Text style={styles.conversationPreview} numberOfLines={1}>
            {item.lastMessage.senderId === user?.id && 'You: '}
            {item.lastMessage.originalContent}
          </Text>
        )}
      </View>
    </TouchableOpacity>
  );

  return (
    <SafeAreaView style={styles.container} edges={['top']}>
      {/* Header */}
      <View style={styles.header}>
        <View style={styles.headerTop}>
          <View style={styles.logoRow}>
            <Ionicons name="chatbubbles" size={28} color={colors.primary[500]} />
            <Text style={styles.headerTitle}>LinguaLink</Text>
          </View>
          
          <View style={styles.headerActions}>
            <TouchableOpacity 
              style={styles.headerButton}
              onPress={() => router.push('/(app)/new-chat')}
            >
              <Ionicons name="create-outline" size={24} color={colors.surface[300]} />
            </TouchableOpacity>
            
            <TouchableOpacity 
              style={styles.headerButton}
              onPress={() => setShowMenu(!showMenu)}
            >
              <Ionicons name="ellipsis-vertical" size={24} color={colors.surface[300]} />
            </TouchableOpacity>
          </View>
        </View>

        {/* User info bar */}
        <View style={styles.userBar}>
          <View style={styles.userAvatar}>
            <Text style={styles.userAvatarText}>
              {user?.username?.charAt(0).toUpperCase()}
            </Text>
          </View>
          <View style={styles.userInfo}>
            <Text style={styles.userName}>{user?.username}</Text>
            <Text style={styles.userLanguage}>
              {LANGUAGE_FLAGS[user?.preferredLanguage as LanguageCode]}{' '}
              {SUPPORTED_LANGUAGES[user?.preferredLanguage as LanguageCode]}
            </Text>
          </View>
        </View>

        {/* Search */}
        <View style={styles.searchContainer}>
          <Ionicons name="search" size={20} color={colors.surface[500]} />
          <TextInput
            style={styles.searchInput}
            placeholder="Search conversations..."
            placeholderTextColor={colors.surface[500]}
            value={searchQuery}
            onChangeText={setSearchQuery}
          />
        </View>
      </View>

      {/* Menu dropdown */}
      {showMenu && (
        <View style={styles.menuDropdown}>
          <TouchableOpacity 
            style={styles.menuItem}
            onPress={() => {
              setShowMenu(false);
              router.push('/(app)/settings');
            }}
          >
            <Ionicons name="settings-outline" size={20} color={colors.surface[300]} />
            <Text style={styles.menuItemText}>Settings</Text>
          </TouchableOpacity>
          <TouchableOpacity 
            style={styles.menuItem}
            onPress={() => {
              setShowMenu(false);
              logout();
              router.replace('/(auth)/login');
            }}
          >
            <Ionicons name="log-out-outline" size={20} color={colors.error} />
            <Text style={[styles.menuItemText, { color: colors.error }]}>Sign out</Text>
          </TouchableOpacity>
        </View>
      )}

      {/* Conversations list */}
      <FlatList
        data={filteredConversations}
        keyExtractor={(item) => item.id}
        renderItem={renderConversation}
        contentContainerStyle={styles.listContent}
        refreshControl={
          <RefreshControl
            refreshing={isLoadingConversations}
            onRefresh={loadConversations}
            tintColor={colors.primary[500]}
          />
        }
        ListEmptyComponent={
          <View style={styles.emptyState}>
            {isLoadingConversations ? (
              <ActivityIndicator size="large" color={colors.primary[500]} />
            ) : (
              <>
                <Ionicons name="chatbubbles-outline" size={64} color={colors.surface[600]} />
                <Text style={styles.emptyTitle}>No conversations yet</Text>
                <Text style={styles.emptyText}>
                  Start a new chat to connect with people around the world
                </Text>
                <TouchableOpacity 
                  style={styles.emptyButton}
                  onPress={() => router.push('/(app)/new-chat')}
                >
                  <Ionicons name="add" size={20} color={colors.white} />
                  <Text style={styles.emptyButtonText}>New Chat</Text>
                </TouchableOpacity>
              </>
            )}
          </View>
        }
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.surface[950],
  },
  header: {
    padding: spacing.md,
    borderBottomWidth: 1,
    borderBottomColor: colors.surface[800],
  },
  headerTop: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: spacing.md,
  },
  logoRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
  },
  headerTitle: {
    fontSize: fontSize.xxl,
    fontFamily: 'outfit-bold',
    color: colors.white,
  },
  headerActions: {
    flexDirection: 'row',
    gap: spacing.sm,
  },
  headerButton: {
    padding: spacing.sm,
  },
  userBar: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surface[900],
    borderRadius: borderRadius.lg,
    padding: spacing.sm,
    marginBottom: spacing.md,
  },
  userAvatar: {
    width: 40,
    height: 40,
    borderRadius: borderRadius.full,
    backgroundColor: colors.primary[600],
    justifyContent: 'center',
    alignItems: 'center',
  },
  userAvatarText: {
    color: colors.white,
    fontSize: fontSize.lg,
    fontFamily: 'outfit-semibold',
  },
  userInfo: {
    marginLeft: spacing.sm,
  },
  userName: {
    color: colors.white,
    fontSize: fontSize.md,
    fontFamily: 'outfit-medium',
  },
  userLanguage: {
    color: colors.surface[400],
    fontSize: fontSize.sm,
  },
  searchContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surface[800],
    borderRadius: borderRadius.lg,
    paddingHorizontal: spacing.md,
    height: 44,
  },
  searchInput: {
    flex: 1,
    marginLeft: spacing.sm,
    color: colors.white,
    fontSize: fontSize.md,
  },
  menuDropdown: {
    position: 'absolute',
    top: 60,
    right: spacing.md,
    backgroundColor: colors.surface[800],
    borderRadius: borderRadius.lg,
    padding: spacing.sm,
    zIndex: 100,
    borderWidth: 1,
    borderColor: colors.surface[700],
    shadowColor: colors.black,
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 8,
  },
  menuItem: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: spacing.md,
    gap: spacing.sm,
  },
  menuItemText: {
    color: colors.surface[300],
    fontSize: fontSize.md,
  },
  listContent: {
    flexGrow: 1,
  },
  conversationItem: {
    flexDirection: 'row',
    padding: spacing.md,
    borderBottomWidth: 1,
    borderBottomColor: colors.surface[800],
  },
  avatar: {
    width: 52,
    height: 52,
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
    fontSize: fontSize.xl,
    fontFamily: 'outfit-semibold',
  },
  conversationContent: {
    flex: 1,
    marginLeft: spacing.md,
    justifyContent: 'center',
  },
  conversationHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  conversationName: {
    color: colors.white,
    fontSize: fontSize.md,
    fontFamily: 'outfit-medium',
    flex: 1,
  },
  conversationTime: {
    color: colors.surface[500],
    fontSize: fontSize.xs,
    marginLeft: spacing.sm,
  },
  conversationPreview: {
    color: colors.surface[400],
    fontSize: fontSize.sm,
    marginTop: spacing.xs,
  },
  emptyState: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: spacing.xxl * 2,
    paddingHorizontal: spacing.xl,
  },
  emptyTitle: {
    color: colors.surface[300],
    fontSize: fontSize.xl,
    fontFamily: 'outfit-semibold',
    marginTop: spacing.lg,
  },
  emptyText: {
    color: colors.surface[500],
    fontSize: fontSize.md,
    textAlign: 'center',
    marginTop: spacing.sm,
    marginBottom: spacing.lg,
  },
  emptyButton: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.primary[500],
    paddingVertical: spacing.md,
    paddingHorizontal: spacing.lg,
    borderRadius: borderRadius.lg,
    gap: spacing.sm,
  },
  emptyButtonText: {
    color: colors.white,
    fontSize: fontSize.md,
    fontFamily: 'outfit-semibold',
  },
});

