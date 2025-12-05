import React, { useState, useEffect, useCallback } from 'react';
import {
  View,
  Text,
  TextInput,
  FlatList,
  TouchableOpacity,
  StyleSheet,
  Modal,
  Image,
  ActivityIndicator,
  Dimensions,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { colors, spacing, borderRadius, fontSize } from '../constants/theme';

// Using Tenor's free API - you can get a key from https://developers.google.com/tenor/guides/quickstart
// For now, we'll use a public endpoint that doesn't require a key for basic searches
const TENOR_API_KEY = 'AIzaSyAyimkuYQYF_FXVALexPuGQctUWRURdCYQ'; // Free demo key
const TENOR_BASE_URL = 'https://tenor.googleapis.com/v2';

interface GifResult {
  id: string;
  title: string;
  media_formats: {
    tinygif?: { url: string; dims: number[] };
    gif?: { url: string; dims: number[] };
    mediumgif?: { url: string; dims: number[] };
  };
}

interface GifPickerProps {
  visible: boolean;
  onClose: () => void;
  onSelectGif: (gifUrl: string) => void;
}

const { width: SCREEN_WIDTH } = Dimensions.get('window');
const GIF_SIZE = (SCREEN_WIDTH - spacing.md * 3) / 2;

export function GifPicker({ visible, onClose, onSelectGif }: GifPickerProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const [gifs, setGifs] = useState<GifResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [trendingLoaded, setTrendingLoaded] = useState(false);

  // Load trending GIFs on mount
  useEffect(() => {
    if (visible && !trendingLoaded) {
      loadTrending();
    }
  }, [visible, trendingLoaded]);

  const loadTrending = async () => {
    try {
      setLoading(true);
      const response = await fetch(
        `${TENOR_BASE_URL}/featured?key=${TENOR_API_KEY}&limit=20&contentfilter=medium`
      );
      const data = await response.json();
      setGifs(data.results || []);
      setTrendingLoaded(true);
    } catch (error) {
      console.error('Failed to load trending GIFs:', error);
    } finally {
      setLoading(false);
    }
  };

  const searchGifs = useCallback(async (query: string) => {
    if (!query.trim()) {
      loadTrending();
      return;
    }

    try {
      setLoading(true);
      const response = await fetch(
        `${TENOR_BASE_URL}/search?key=${TENOR_API_KEY}&q=${encodeURIComponent(query)}&limit=30&contentfilter=medium`
      );
      const data = await response.json();
      setGifs(data.results || []);
    } catch (error) {
      console.error('Failed to search GIFs:', error);
    } finally {
      setLoading(false);
    }
  }, []);

  // Debounced search
  useEffect(() => {
    const timer = setTimeout(() => {
      if (searchQuery) {
        searchGifs(searchQuery);
      }
    }, 300);

    return () => clearTimeout(timer);
  }, [searchQuery, searchGifs]);

  const handleSelectGif = (gif: GifResult) => {
    // Prefer mediumgif for better quality, fallback to gif or tinygif
    const gifUrl = 
      gif.media_formats.mediumgif?.url || 
      gif.media_formats.gif?.url || 
      gif.media_formats.tinygif?.url;
    
    if (gifUrl) {
      onSelectGif(gifUrl);
      onClose();
    }
  };

  const renderGif = ({ item }: { item: GifResult }) => {
    const previewUrl = item.media_formats.tinygif?.url;
    if (!previewUrl) return null;

    return (
      <TouchableOpacity
        style={styles.gifItem}
        onPress={() => handleSelectGif(item)}
        activeOpacity={0.8}
      >
        <Image
          source={{ uri: previewUrl }}
          style={styles.gifImage}
          resizeMode="cover"
        />
      </TouchableOpacity>
    );
  };

  return (
    <Modal
      visible={visible}
      animationType="slide"
      onRequestClose={onClose}
    >
      <View style={styles.container}>
        {/* Header */}
        <View style={styles.header}>
          <Text style={styles.title}>Choose a GIF</Text>
          <TouchableOpacity onPress={onClose} style={styles.closeButton}>
            <Ionicons name="close" size={24} color={colors.surface[300]} />
          </TouchableOpacity>
        </View>

        {/* Search */}
        <View style={styles.searchContainer}>
          <Ionicons name="search" size={20} color={colors.surface[500]} />
          <TextInput
            style={styles.searchInput}
            placeholder="Search GIFs..."
            placeholderTextColor={colors.surface[500]}
            value={searchQuery}
            onChangeText={setSearchQuery}
            autoCapitalize="none"
            autoCorrect={false}
          />
          {searchQuery ? (
            <TouchableOpacity onPress={() => setSearchQuery('')}>
              <Ionicons name="close-circle" size={20} color={colors.surface[500]} />
            </TouchableOpacity>
          ) : null}
        </View>

        {/* GIF Grid */}
        {loading && gifs.length === 0 ? (
          <View style={styles.loadingContainer}>
            <ActivityIndicator size="large" color={colors.primary[500]} />
          </View>
        ) : (
          <FlatList
            data={gifs}
            keyExtractor={(item) => item.id}
            renderItem={renderGif}
            numColumns={2}
            contentContainerStyle={styles.gifGrid}
            showsVerticalScrollIndicator={false}
            ListEmptyComponent={
              <View style={styles.emptyContainer}>
                <Ionicons name="images-outline" size={48} color={colors.surface[600]} />
                <Text style={styles.emptyText}>No GIFs found</Text>
              </View>
            }
          />
        )}

        {/* Powered by Tenor */}
        <View style={styles.attribution}>
          <Text style={styles.attributionText}>Powered by Tenor</Text>
        </View>
      </View>
    </Modal>
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
    justifyContent: 'space-between',
    padding: spacing.md,
    borderBottomWidth: 1,
    borderBottomColor: colors.surface[800],
  },
  title: {
    color: colors.white,
    fontSize: fontSize.lg,
    fontWeight: '600',
  },
  closeButton: {
    padding: spacing.sm,
  },
  searchContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surface[800],
    borderRadius: borderRadius.lg,
    margin: spacing.md,
    paddingHorizontal: spacing.md,
    height: 44,
  },
  searchInput: {
    flex: 1,
    marginLeft: spacing.sm,
    color: colors.white,
    fontSize: fontSize.md,
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  gifGrid: {
    padding: spacing.sm,
  },
  gifItem: {
    width: GIF_SIZE,
    height: GIF_SIZE,
    margin: spacing.xs,
    borderRadius: borderRadius.md,
    overflow: 'hidden',
    backgroundColor: colors.surface[800],
  },
  gifImage: {
    width: '100%',
    height: '100%',
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: spacing.xxl * 2,
  },
  emptyText: {
    color: colors.surface[500],
    fontSize: fontSize.md,
    marginTop: spacing.md,
  },
  attribution: {
    padding: spacing.sm,
    alignItems: 'center',
    borderTopWidth: 1,
    borderTopColor: colors.surface[800],
  },
  attributionText: {
    color: colors.surface[600],
    fontSize: fontSize.xs,
  },
});

