import {create} from 'zustand';
import {detectPreferredLocale, type Locale} from '../i18n/translations';

export type Theme = 'light' | 'dark';

interface UiState {
  theme: Theme;
  locale: Locale;
  searchQuery: string;
  comparisonQuery: string;
}

interface UiActions {
  toggleTheme: () => void;
  setLocale: (locale: Locale) => void;
  setSearchQuery: (query: string) => void;
  setComparisonQuery: (query: string) => void;
}

export const useUiStore = create<UiState & UiActions>((set) => ({
  theme: detectPreferredTheme(),
  locale: detectPreferredLocale(),
  searchQuery: '',
  comparisonQuery: '',
  toggleTheme: () => set((state) => {
    const theme = state.theme === 'light' ? 'dark' : 'light';
    persistPreference('heapscout.theme', theme);
    return {theme};
  }),
  setLocale: (locale) => {
    persistPreference('heapscout.locale', locale);
    set({locale});
  },
  setSearchQuery: (searchQuery) => set({searchQuery}),
  setComparisonQuery: (comparisonQuery) => set({comparisonQuery}),
}));

function detectPreferredTheme(): Theme {
  if (typeof window === 'undefined') return 'light';
  try {
    const saved = window.localStorage.getItem('heapscout.theme');
    if (saved === 'light' || saved === 'dark') return saved;
  } catch {
    // Browser storage can be unavailable in hardened privacy modes.
  }
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

function persistPreference(key: string, value: string): void {
  try {
    if (typeof window !== 'undefined') window.localStorage.setItem(key, value);
  } catch {
    // The in-memory preference still works when browser storage is blocked.
  }
}
