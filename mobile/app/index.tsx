import { Redirect } from 'expo-router';
import { useAuthStore } from '../src/store/auth';

export default function Index() {
  const { isAuthenticated } = useAuthStore();

  if (isAuthenticated) {
    return <Redirect href="/(app)/conversations" />;
  }

  return <Redirect href="/(auth)/login" />;
}



