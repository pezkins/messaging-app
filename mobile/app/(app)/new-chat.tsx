import { useState, useEffect } from 'react';
import {
  View,
  Text,
  TextInput,
  FlatList,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
} from 'react-native';
import { router } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { SafeAreaView } from 'react-native-safe-area-context';
import { api } from '../../src/services/api';
import { useChatStore } from '../../src/store/chat';
import { colors, spacing, borderRadius, fontSize } from '../../src/constants/theme';
import { getLanguageByCode, getCountryByCode } from '../../src/constants/languages';
import type { UserPublic } from '../../src/types';

export default function NewChatScreen() {
  const { startConversation } = useChatStore();
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<UserPublic[]>([]);
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

  const handleStartChat = async (user: UserPublic) => {
    setIsStarting(true);
    try {
      await startConversation(user);
      router.replace('/(app)/chat');
    } catch (error) {
      console.error('Failed to start conversation:', error);
    } finally {
      setIsStarting(false);
    }
  };

  const renderUser = ({ item }: { item: UserPublic }) => (
    <TouchableOpacity
      style={styles.userItem}
      onPress={() => handleStartChat(item)}
      disabled={isStarting}
      activeOpacity={0.7}
    >
      <View style={styles.userAvatar}>
        <Text style={styles.userAvatarText}>{item.username.charAt(0).toUpperCase()}</Text>
      </View>
      
      <View style={styles.userInfo}>
        <Text style={styles.userName}>{item.username}</Text>
        <Text style={styles.userLanguage}>
          {getLanguageByCode(item.preferredLanguage)?.native || item.preferredLanguage}
        </Text>
      </View>
      
      <Ionicons name="chatbubble-outline" size={20} color={colors.surface[500]} />
    </TouchableOpacity>
  );

  return (
    <SafeAreaView style={styles.container} edges={['top']}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity style={styles.backButton} onPress={() => router.back()}>
          <Ionicons name="arrow-back" size={24} color={colors.white} />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>New Conversation</Text>
      </View>

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
                <Text style={styles.emptyText}>Enter at least 2 characters to search</Text>
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

      {/* Loading overlay */}
      {isStarting && (
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
  userAvatar: {
    width: 48,
    height: 48,
    borderRadius: borderRadius.full,
    backgroundColor: colors.primary[600],
    justifyContent: 'center',
    alignItems: 'center',
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

