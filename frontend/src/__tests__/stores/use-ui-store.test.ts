import {beforeEach, describe, expect, it} from 'vitest';
import {useUiStore} from '../../stores/use-ui-store';

describe('useUiStore: interface preferences', () => {
  beforeEach(() => {
    useUiStore.setState({theme: 'light', searchQuery: '', comparisonQuery: ''});
  });

  it('toggleTheme_lightTheme_changesThemeToDark', () => {
    useUiStore.getState().toggleTheme();

    expect(useUiStore.getState().theme).toBe('dark');
  });

  it('setSearchQuery_filterExpression_storesExpression', () => {
    useUiStore.getState().setSearchQuery('name:cache size>10MB');

    expect(useUiStore.getState().searchQuery).toBe('name:cache size>10MB');
  });
});
