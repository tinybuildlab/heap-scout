import {useEffect} from 'react';
import {AnalysisDashboard} from './components/dashboard/analysis-dashboard';
import {OpenDumpPanel} from './components/dashboard/open-dump-panel';
import {MoonIcon, ShieldIcon, SunIcon} from './components/common/icons';
import {useAnalysisPolling} from './hooks/use-analysis-polling';
import {useI18n} from './hooks/use-i18n';
import {translateError} from './i18n/translations';
import {useAnalysisStore} from './stores/use-analysis-store';
import {useUiStore} from './stores/use-ui-store';
import styles from './app.module.css';

export function App() {
  const theme = useUiStore((state) => state.theme);
  const toggleTheme = useUiStore((state) => state.toggleTheme);
  const setLocale = useUiStore((state) => state.setLocale);
  const job = useAnalysisStore((state) => state.job);
  const hasBootstrapped = useAnalysisStore((state) => state.hasBootstrapped);
  const isBootstrapping = useAnalysisStore((state) => state.isBootstrapping);
  const isChoosing = useAnalysisStore((state) => state.isChoosing);
  const isOpening = useAnalysisStore((state) => state.isOpening);
  const systemPickerAvailable = useAnalysisStore((state) => state.systemPickerAvailable);
  const error = useAnalysisStore((state) => state.error);
  const bootstrap = useAnalysisStore((state) => state.bootstrap);
  const chooseDump = useAnalysisStore((state) => state.chooseDump);
  const openDump = useAnalysisStore((state) => state.openDump);
  const clearError = useAnalysisStore((state) => state.clearError);
  const {locale, t} = useI18n();
  useAnalysisPolling();

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
  }, [theme]);

  useEffect(() => {
    document.documentElement.lang = locale;
    document.title = t('app.title');
  }, [locale, t]);

  useEffect(() => {
    void bootstrap();
  }, [bootstrap]);

  return (
    <div className={styles.app}>
      <header className={styles.topbar}>
        <div className={styles.brand}>
          <span className={styles.mark} aria-hidden="true"><i /><i /><i /></span>
          <span className={styles.brandCopy}>
            <strong>HeapScout</strong>
            <small>{t('app.workspace')}</small>
          </span>
        </div>
        <div className={styles.topbarActions}>
          <span className={styles.localBadge}>
            <ShieldIcon className={styles.localIcon} />
            <span><strong>{t('common.localOnly')}</strong><small>{t('common.privateSession')}</small></span>
          </span>
          <div className={styles.languageSwitch} aria-label={t('common.language')}>
            <button
              className={locale === 'ko' ? styles.languageActive : undefined}
              type="button"
              onClick={() => setLocale('ko')}
              aria-pressed={locale === 'ko'}
            >
              KO
            </button>
            <button
              className={locale === 'en' ? styles.languageActive : undefined}
              type="button"
              onClick={() => setLocale('en')}
              aria-pressed={locale === 'en'}
            >
              EN
            </button>
          </div>
          <button
            className={styles.themeButton}
            type="button"
            onClick={toggleTheme}
            aria-label={theme === 'light' ? t('common.darkMode') : t('common.lightMode')}
            title={theme === 'light' ? t('common.darkMode') : t('common.lightMode')}
          >
            {theme === 'light' ? <MoonIcon /> : <SunIcon />}
          </button>
        </div>
      </header>

      {error && (
        <div className={styles.errorBanner} role="alert">
          <span><strong>!</strong>{translateError(locale, error)}</span>
          <button type="button" onClick={clearError}>{t('common.dismiss')}</button>
        </div>
      )}

      {job ? (
        <AnalysisDashboard job={job} />
      ) : (
        <OpenDumpPanel
          isChoosing={isChoosing}
          isOpening={isOpening}
          isRestoring={!hasBootstrapped || isBootstrapping}
          systemPickerAvailable={systemPickerAvailable}
          onChoose={chooseDump}
          onOpen={openDump}
        />
      )}
    </div>
  );
}
