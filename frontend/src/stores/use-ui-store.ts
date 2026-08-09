import {create} from 'zustand';

export type Theme = 'light' | 'dark';

interface UiState {
  theme: Theme;
  searchQuery: string;
  comparisonQuery: string;
}

interface UiActions {
  toggleTheme: () => void;
  setSearchQuery: (query: string) => void;
  setComparisonQuery: (query: string) => void;
}

export const useUiStore = create<UiState & UiActions>((set) => ({
  theme: 'light',
  searchQuery: '',
  comparisonQuery: '',
  toggleTheme: () => set((state) => ({theme: state.theme === 'light' ? 'dark' : 'light'})),
  setSearchQuery: (searchQuery) => set({searchQuery}),
  setComparisonQuery: (comparisonQuery) => set({comparisonQuery}),
}));
