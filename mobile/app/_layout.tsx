import { useEffect, useState, Component, ReactNode } from 'react';
import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { View, ActivityIndicator, StyleSheet, Text, ScrollView } from 'react-native';
import { useAuthStore } from '../src/store/auth';

// Error Boundary to catch React rendering errors
interface ErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
}

class ErrorBoundary extends Component<{ children: ReactNode }, ErrorBoundaryState> {
  constructor(props: { children: ReactNode }) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error) {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: { componentStack: string }) {
    console.error('ErrorBoundary caught error:', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <View style={styles.errorContainer}>
          <Text style={styles.errorTitle}>App Error</Text>
          <ScrollView style={styles.errorScroll}>
            <Text style={styles.errorText}>
              {this.state.error?.message || 'Unknown error'}
            </Text>
            <Text style={styles.errorStack}>
              {this.state.error?.stack || 'No stack trace'}
            </Text>
          </ScrollView>
        </View>
      );
    }
    return this.props.children;
  }
}

function RootLayoutContent() {
  const { initializeAuth, isLoading } = useAuthStore();
  const [appReady, setAppReady] = useState(false);
  const [initError, setInitError] = useState<string | null>(null);

  useEffect(() => {
    const init = async () => {
      try {
        console.log('🚀 Starting auth initialization...');
        await initializeAuth();
        console.log('✅ Auth initialization complete');
      } catch (e) {
        const errorMsg = e instanceof Error ? e.message : String(e);
        console.error('❌ Auth init error:', errorMsg);
        setInitError(errorMsg);
      } finally {
        setAppReady(true);
      }
    };
    init();
  }, []);

  if (initError) {
    return (
      <View style={styles.errorContainer}>
        <Text style={styles.errorTitle}>Init Error</Text>
        <Text style={styles.errorText}>{initError}</Text>
        <StatusBar style="light" />
      </View>
    );
  }

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
    <ErrorBoundary>
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
    backgroundColor: '#1a0a0a',
    padding: 20,
  },
  errorTitle: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#ff6b6b',
    marginBottom: 20,
  },
  errorScroll: {
    maxHeight: 400,
    width: '100%',
  },
  errorText: {
    fontSize: 16,
    color: '#ffffff',
    marginBottom: 10,
  },
  errorStack: {
    fontSize: 12,
    color: '#aaaaaa',
    fontFamily: 'monospace',
  },
});
