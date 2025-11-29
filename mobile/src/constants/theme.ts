export const colors = {
  // Deep ocean blue palette
  primary: {
    50: '#e6f4f9',
    100: '#cce9f3',
    200: '#99d3e7',
    300: '#66bddb',
    400: '#33a7cf',
    500: '#0091c3',
    600: '#00749c',
    700: '#005775',
    800: '#003a4e',
    900: '#001d27',
  },
  // Warm coral accent
  accent: {
    50: '#fff5f3',
    100: '#ffebe7',
    200: '#ffd7cf',
    300: '#ffb3a3',
    400: '#ff8f77',
    500: '#ff6b4b',
    600: '#e54d2d',
    700: '#bf3a1f',
    800: '#992e19',
    900: '#732312',
  },
  // Neutral slate
  surface: {
    50: '#f8fafc',
    100: '#f1f5f9',
    200: '#e2e8f0',
    300: '#cbd5e1',
    400: '#94a3b8',
    500: '#64748b',
    600: '#475569',
    700: '#334155',
    800: '#1e293b',
    900: '#0f172a',
    950: '#020617',
  },
  // Semantic
  success: '#22c55e',
  warning: '#f59e0b',
  error: '#ef4444',
  white: '#ffffff',
  black: '#000000',
};

export const spacing = {
  xs: 4,
  sm: 8,
  md: 16,
  lg: 24,
  xl: 32,
  xxl: 48,
};

export const borderRadius = {
  sm: 4,
  md: 8,
  lg: 12,
  xl: 16,
  xxl: 24,
  full: 9999,
};

export const fontSize = {
  xs: 12,
  sm: 14,
  md: 16,
  lg: 18,
  xl: 20,
  xxl: 24,
  xxxl: 32,
};

export const fontWeight = {
  normal: '400' as const,
  medium: '500' as const,
  semibold: '600' as const,
  bold: '700' as const,
};

// Font family names - uses system fonts as fallback
// To use custom fonts, download Outfit from Google Fonts
// and add to assets/fonts directory
export const fontFamily = {
  regular: 'System',
  medium: 'System',
  semibold: 'System',
  bold: 'System',
};

