import { useEffect, useState } from 'react';
import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { View, ActivityIndicator, StyleSheet, Text } from 'react-native';
import { useAuthStore } from '../src/store/auth';
import { ErrorBoundary } from 'react-error-boundary';

function ErrorFallback({ error }: { error: Error }) {
  return (
    <View style={styles.errorContainer}>
      <Text style={styles.errorTitle}>Something went wrong</Text>
      <Text style={styles.errorMessage}>{error.message}</Text>
      <Text style={styles.errorStack}>{error.stack?.slice(0, 500)}</Text>
      <StatusBar style="light" />
    </View>
  );
}

function RootLayoutContent() {
  const { initializeAuth, isLoading } = useAuthStore();
  const [appReady, setAppReady] = useState(false);

  useEffect(() => {
    const init = async () => {
      try {
        await initializeAuth();
      } catch (e) {
        console.log('Auth init error:', e);
      } finally {
        setAppReady(true);
      }
    };
    init();
  }, []);

  if (!appReady || isLoading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#0091c3" />
        <StatusBar style="light" />
      </View>
    );
  }

  return (
    <>
      <Stack
        screenOptions={{
          headerShown: false,
          contentStyle: { backgroundColor: '#0f172a' },
          animation: 'slide_from_right',
        }}
      />
      <StatusBar style="light" />
    </>
  );
}

export default function RootLayout() {
  return (
    <ErrorBoundary FallbackComponent={ErrorFallback}>
      <RootLayoutContent />
    </ErrorBoundary>
  );
}

const styles = StyleSheet.create({
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#0f172a',
  },
  errorContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#0f172a',
    padding: 20,
  },
  errorTitle: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#ef4444',
    marginBottom: 10,
  },
  errorMessage: {
    fontSize: 16,
    color: '#ffffff',
    marginBottom: 10,
    textAlign: 'center',
  },
  errorStack: {
    fontSize: 12,
    color: '#94a3b8',
    textAlign: 'left',
  },
});
