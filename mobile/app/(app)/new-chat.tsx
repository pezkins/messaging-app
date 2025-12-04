import { useState, useEffect } from 'react';
import {
  View,
  Text,
  TextInput,
  FlatList,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
  ScrollView,
} from 'react-native';
import { router } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { SafeAreaView } from 'react-native-safe-area-context';
import { api } from '../../src/services/api';
import { useChatStore } from '../../src/store/chat';
import { colors, spacing, borderRadius, fontSize } from '../../src/constants/theme';
import { getLanguageByCode } from '../../src/constants/languages';
import type { UserPublic } from '../../src/types';

type ChatMode = 'direct' | 'group';

export default function NewChatScreen() {
  const { startConversation, startGroupConversation } = useChatStore();
  const [mode, setMode] = useState<ChatMode>('direct');
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<UserPublic[]>([]);
  const [selectedUsers, setSelectedUsers] = useState<UserPublic[]>([]);
  const [groupName, setGroupName] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [isStarting, setIsStarting] = useState(false);

  useEffect(() => {
    const search = async () => {
      if (query.length < 2) {
        setResults([]);
        return;
      }

      setIsLoading(true);
      try {
        const { users } = await api.searchUsers(query);
        setResults(users);
      } catch (error) {
        console.error('Search failed:', error);
      } finally {
        setIsLoading(false);
      }
    };

    const timeoutId = setTimeout(search, 300);
    return () => clearTimeout(timeoutId);
  }, [query]);

  const handleUserPress = async (user: UserPublic) => {
    if (mode === 'direct') {
      // Direct chat - start immediately
      setIsStarting(true);
      try {
        await startConversation(user);
        router.replace('/(app)/chat');
      } catch (error) {
        console.error('Failed to start conversation:', error);
      } finally {
        setIsStarting(false);
      }
    } else {
      // Group chat - toggle selection
      const isSelected = selectedUsers.some(u => u.id === user.id);
      if (isSelected) {
        setSelectedUsers(selectedUsers.filter(u => u.id !== user.id));
      } else {
        setSelectedUsers([...selectedUsers, user]);
      }
    }
  };

  const handleRemoveUser = (userId: string) => {
    setSelectedUsers(selectedUsers.filter(u => u.id !== userId));
  };

  const handleCreateGroup = async () => {
    if (selectedUsers.length < 2) return;
    
    setIsStarting(true);
    try {
      await startGroupConversation(selectedUsers, groupName.trim() || undefined);
      router.replace('/(app)/chat');
    } catch (error) {
      console.error('Failed to create group:', error);
    } finally {
      setIsStarting(false);
    }
  };

  const isUserSelected = (userId: string) => selectedUsers.some(u => u.id === userId);

  const renderUser = ({ item }: { item: UserPublic }) => {
    const selected = isUserSelected(item.id);
    
    return (
      <TouchableOpacity
        style={[styles.userItem, selected && styles.userItemSelected]}
        onPress={() => handleUserPress(item)}
        disabled={isStarting}
        activeOpacity={0.7}
      >
        <View style={[styles.userAvatar, selected && styles.userAvatarSelected]}>
          <Text style={styles.userAvatarText}>{item.username.charAt(0).toUpperCase()}</Text>
        </View>
        
        <View style={styles.userInfo}>
          <Text style={styles.userName}>{item.username}</Text>
          <Text style={styles.userLanguage}>
            {getLanguageByCode(item.preferredLanguage)?.native || item.preferredLanguage}
          </Text>
        </View>
        
        {mode === 'direct' ? (
          <Ionicons name="chatbubble-outline" size={20} color={colors.surface[500]} />
        ) : (
          <View style={[styles.checkbox, selected && styles.checkboxSelected]}>
            {selected && <Ionicons name="checkmark" size={16} color={colors.white} />}
          </View>
        )}
      </TouchableOpacity>
    );
  };

  return (
    <SafeAreaView style={styles.container} edges={['top']}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity style={styles.backButton} onPress={() => router.back()}>
          <Ionicons name="arrow-back" size={24} color={colors.white} />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>New Conversation</Text>
      </View>

      {/* Mode Toggle */}
      <View style={styles.modeToggle}>
        <TouchableOpacity
          style={[styles.modeButton, mode === 'direct' && styles.modeButtonActive]}
          onPress={() => {
            setMode('direct');
            setSelectedUsers([]);
          }}
        >
          <Ionicons 
            name="person" 
            size={18} 
            color={mode === 'direct' ? colors.white : colors.surface[400]} 
          />
          <Text style={[styles.modeButtonText, mode === 'direct' && styles.modeButtonTextActive]}>
            Direct
          </Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={[styles.modeButton, mode === 'group' && styles.modeButtonActive]}
          onPress={() => setMode('group')}
        >
          <Ionicons 
            name="people" 
            size={18} 
            color={mode === 'group' ? colors.white : colors.surface[400]} 
          />
          <Text style={[styles.modeButtonText, mode === 'group' && styles.modeButtonTextActive]}>
            Group
          </Text>
        </TouchableOpacity>
      </View>

      {/* Selected Users (Group Mode) */}
      {mode === 'group' && selectedUsers.length > 0 && (
        <View style={styles.selectedContainer}>
          <ScrollView 
            horizontal 
            showsHorizontalScrollIndicator={false}
            contentContainerStyle={styles.selectedScroll}
          >
            {selectedUsers.map((user) => (
              <View key={user.id} style={styles.selectedChip}>
                <Text style={styles.selectedChipText}>{user.username}</Text>
                <TouchableOpacity onPress={() => handleRemoveUser(user.id)}>
                  <Ionicons name="close-circle" size={18} color={colors.surface[400]} />
                </TouchableOpacity>
              </View>
            ))}
          </ScrollView>
        </View>
      )}

      {/* Group Name Input (Group Mode) */}
      {mode === 'group' && selectedUsers.length >= 2 && (
        <View style={styles.groupNameContainer}>
          <Ionicons name="chatbubbles-outline" size={20} color={colors.surface[400]} />
          <TextInput
            style={styles.groupNameInput}
            placeholder="Group name (optional)"
            placeholderTextColor={colors.surface[500]}
            value={groupName}
            onChangeText={setGroupName}
            maxLength={50}
          />
        </View>
      )}

      {/* Search */}
      <View style={styles.searchContainer}>
        <Ionicons name="search" size={20} color={colors.surface[500]} />
        <TextInput
          style={styles.searchInput}
          placeholder="Search by username or email..."
          placeholderTextColor={colors.surface[500]}
          value={query}
          onChangeText={setQuery}
          autoCapitalize="none"
          autoFocus
        />
        {query.length > 0 && (
          <TouchableOpacity onPress={() => setQuery('')}>
            <Ionicons name="close-circle" size={20} color={colors.surface[500]} />
          </TouchableOpacity>
        )}
      </View>

      {/* Results */}
      <FlatList
        data={results}
        keyExtractor={(item) => item.id}
        renderItem={renderUser}
        contentContainerStyle={styles.listContent}
        ListEmptyComponent={
          <View style={styles.emptyState}>
            {isLoading ? (
              <ActivityIndicator size="large" color={colors.primary[500]} />
            ) : query.length < 2 ? (
              <>
                <Ionicons name="people-outline" size={48} color={colors.surface[600]} />
                <Text style={styles.emptyText}>
                  {mode === 'direct' 
                    ? 'Enter at least 2 characters to search'
                    : 'Search for users to add to your group'}
                </Text>
              </>
            ) : (
              <>
                <Ionicons name="search-outline" size={48} color={colors.surface[600]} />
                <Text style={styles.emptyText}>No users found</Text>
                <Text style={styles.emptySubtext}>Try a different search term</Text>
              </>
            )}
          </View>
        }
      />

      {/* Create Group Button */}
      {mode === 'group' && selectedUsers.length >= 2 && (
        <View style={styles.createButtonContainer}>
          <TouchableOpacity
            style={[styles.createButton, isStarting && styles.createButtonDisabled]}
            onPress={handleCreateGroup}
            disabled={isStarting}
          >
            {isStarting ? (
              <ActivityIndicator color={colors.white} />
            ) : (
              <>
                <Ionicons name="checkmark-circle" size={20} color={colors.white} />
                <Text style={styles.createButtonText}>
                  Create Group ({selectedUsers.length} members)
                </Text>
              </>
            )}
          </TouchableOpacity>
        </View>
      )}

      {/* Loading overlay */}
      {isStarting && mode === 'direct' && (
        <View style={styles.loadingOverlay}>
          <ActivityIndicator size="large" color={colors.primary[500]} />
          <Text style={styles.loadingText}>Starting conversation...</Text>
        </View>
      )}
    </SafeAreaView>
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
  headerTitle: {
    color: colors.white,
    fontSize: fontSize.xl,
    fontWeight: '600',
  },
  modeToggle: {
    flexDirection: 'row',
    margin: spacing.md,
    backgroundColor: colors.surface[800],
    borderRadius: borderRadius.lg,
    padding: spacing.xs,
  },
  modeButton: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: spacing.sm,
    borderRadius: borderRadius.md,
    gap: spacing.xs,
  },
  modeButtonActive: {
    backgroundColor: colors.primary[500],
  },
  modeButtonText: {
    color: colors.surface[400],
    fontSize: fontSize.sm,
    fontWeight: '500',
  },
  modeButtonTextActive: {
    color: colors.white,
  },
  selectedContainer: {
    borderBottomWidth: 1,
    borderBottomColor: colors.surface[800],
    paddingVertical: spacing.sm,
  },
  selectedScroll: {
    paddingHorizontal: spacing.md,
    gap: spacing.sm,
  },
  selectedChip: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.primary[600],
    borderRadius: borderRadius.full,
    paddingVertical: spacing.xs,
    paddingLeft: spacing.md,
    paddingRight: spacing.sm,
    gap: spacing.xs,
  },
  selectedChipText: {
    color: colors.white,
    fontSize: fontSize.sm,
    fontWeight: '500',
  },
  groupNameContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surface[800],
    marginHorizontal: spacing.md,
    marginTop: spacing.sm,
    borderRadius: borderRadius.lg,
    paddingHorizontal: spacing.md,
    height: 44,
    borderWidth: 1,
    borderColor: colors.surface[700],
  },
  groupNameInput: {
    flex: 1,
    marginLeft: spacing.sm,
    color: colors.white,
    fontSize: fontSize.md,
  },
  searchContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surface[800],
    margin: spacing.md,
    borderRadius: borderRadius.lg,
    paddingHorizontal: spacing.md,
    height: 48,
    borderWidth: 1,
    borderColor: colors.surface[700],
  },
  searchInput: {
    flex: 1,
    marginLeft: spacing.sm,
    color: colors.white,
    fontSize: fontSize.md,
  },
  listContent: {
    flexGrow: 1,
  },
  userItem: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: spacing.md,
    borderBottomWidth: 1,
    borderBottomColor: colors.surface[800],
  },
  userItemSelected: {
    backgroundColor: `${colors.primary[500]}15`,
  },
  userAvatar: {
    width: 48,
    height: 48,
    borderRadius: borderRadius.full,
    backgroundColor: colors.primary[600],
    justifyContent: 'center',
    alignItems: 'center',
  },
  userAvatarSelected: {
    backgroundColor: colors.primary[500],
  },
  userAvatarText: {
    color: colors.white,
    fontSize: fontSize.lg,
    fontWeight: '600',
  },
  userInfo: {
    flex: 1,
    marginLeft: spacing.md,
  },
  userName: {
    color: colors.white,
    fontSize: fontSize.md,
    fontWeight: '500',
  },
  userLanguage: {
    color: colors.surface[400],
    fontSize: fontSize.sm,
    marginTop: spacing.xs,
  },
  checkbox: {
    width: 24,
    height: 24,
    borderRadius: borderRadius.full,
    borderWidth: 2,
    borderColor: colors.surface[500],
    justifyContent: 'center',
    alignItems: 'center',
  },
  checkboxSelected: {
    backgroundColor: colors.primary[500],
    borderColor: colors.primary[500],
  },
  emptyState: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: spacing.xxl * 2,
    paddingHorizontal: spacing.xl,
  },
  emptyText: {
    color: colors.surface[400],
    fontSize: fontSize.md,
    marginTop: spacing.md,
    textAlign: 'center',
  },
  emptySubtext: {
    color: colors.surface[500],
    fontSize: fontSize.sm,
    marginTop: spacing.xs,
    textAlign: 'center',
  },
  createButtonContainer: {
    padding: spacing.md,
    borderTopWidth: 1,
    borderTopColor: colors.surface[800],
  },
  createButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.success,
    borderRadius: borderRadius.lg,
    padding: spacing.md,
    gap: spacing.sm,
  },
  createButtonDisabled: {
    opacity: 0.6,
  },
  createButtonText: {
    color: colors.white,
    fontSize: fontSize.md,
    fontWeight: '600',
  },
  loadingOverlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: `${colors.surface[950]}cc`,
    justifyContent: 'center',
    alignItems: 'center',
  },
  loadingText: {
    color: colors.white,
    fontSize: fontSize.md,
    marginTop: spacing.md,
  },
});
