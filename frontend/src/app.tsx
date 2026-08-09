import {useEffect} from 'react';
import {AnalysisDashboard} from './components/dashboard/analysis-dashboard';
import {OpenDumpPanel} from './components/dashboard/open-dump-panel';
import {useAnalysisPolling} from './hooks/use-analysis-polling';
import {useAnalysisStore} from './stores/use-analysis-store';
import {useUiStore} from './stores/use-ui-store';
import styles from './app.module.css';

export function App() {
  const theme = useUiStore((state) => state.theme);
  const toggleTheme = useUiStore((state) => state.toggleTheme);
  const job = useAnalysisStore((state) => state.job);
  const hasBootstrapped = useAnalysisStore((state) => state.hasBootstrapped);
  const isBootstrapping = useAnalysisStore((state) => state.isBootstrapping);
  const isChoosing = useAnalysisStore((state) => state.isChoosing);
  const isOpening = useAnalysisStore((state) => state.isOpening);
  const error = useAnalysisStore((state) => state.error);
  const bootstrap = useAnalysisStore((state) => state.bootstrap);
  const chooseDump = useAnalysisStore((state) => state.chooseDump);
  const openDump = useAnalysisStore((state) => state.openDump);
  const clearError = useAnalysisStore((state) => state.clearError);
  useAnalysisPolling();

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
  }, [theme]);

  useEffect(() => {
    void bootstrap();
  }, [bootstrap]);

  return (
    <div className={styles.app}>
      <header className={styles.topbar}>
        <div className={styles.brand}>
          <span className={styles.mark}>HS</span>
          <span>HeapScout</span>
        </div>
        <div className={styles.topbarActions}>
          <span className={styles.localBadge}><i /> Local only</span>
          <button className={styles.themeButton} type="button" onClick={toggleTheme}>
            {theme === 'light' ? 'Dark' : 'Light'} mode
          </button>
        </div>
      </header>

      {error && (
        <div className={styles.errorBanner} role="alert">
          <span>{error}</span>
          <button type="button" onClick={clearError}>Dismiss</button>
        </div>
      )}

      {job ? (
        <AnalysisDashboard job={job} />
      ) : (
        <OpenDumpPanel
          isChoosing={isChoosing}
          isOpening={isOpening}
          isRestoring={!hasBootstrapped || isBootstrapping}
          onChoose={chooseDump}
          onOpen={openDump}
        />
      )}
    </div>
  );
}
