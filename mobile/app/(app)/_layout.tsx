import { useEffect } from 'react';
import { Stack } from 'expo-router';
import { useChatStore } from '../../src/store/chat';
import { colors } from '../../src/constants/theme';

export default function AppLayout() {
  const { loadConversations, subscribeToEvents } = useChatStore();

  useEffect(() => {
    loadConversations();
    const unsubscribe = subscribeToEvents();
    return unsubscribe;
  }, [loadConversations, subscribeToEvents]);

  return (
    <Stack
      screenOptions={{
        headerShown: false,
        contentStyle: { backgroundColor: colors.surface[950] },
        animation: 'slide_from_right',
      }}
    />
  );
}



