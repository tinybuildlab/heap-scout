import {useEffect, useState, type FormEvent} from 'react';
import {useI18n} from '../../hooks/use-i18n';
import {translateError} from '../../i18n/translations';
import {formatBytes, formatCount} from '../../lib/format';
import {useAnalysisStore} from '../../stores/use-analysis-store';
import {useUiStore} from '../../stores/use-ui-store';
import type {AnalysisJob, HeapComparisonEntry} from '../../types';
import {CompareIcon, FolderIcon, SearchIcon} from '../common/icons';
import styles from './comparison-panel.module.css';

interface ComparisonPanelProps {
  readonly baseline: AnalysisJob;
}

export function ComparisonPanel({baseline}: ComparisonPanelProps) {
  const [path, setPath] = useState('');
  const target = useAnalysisStore((state) => state.comparisonJob);
  const changes = useAnalysisStore((state) => state.comparison);
  const isChoosing = useAnalysisStore((state) => state.isChoosingComparison);
  const isOpening = useAnalysisStore((state) => state.isOpeningComparison);
  const isLoading = useAnalysisStore((state) => state.isLoadingComparison);
  const systemPickerAvailable = useAnalysisStore((state) => state.systemPickerAvailable);
  const chooseComparisonDump = useAnalysisStore((state) => state.chooseComparisonDump);
  const openComparisonDump = useAnalysisStore((state) => state.openComparisonDump);
  const loadComparison = useAnalysisStore((state) => state.loadComparison);
  const closeComparison = useAnalysisStore((state) => state.closeComparison);
  const query = useUiStore((state) => state.comparisonQuery);
  const setQuery = useUiStore((state) => state.setComparisonQuery);
  const {locale, t} = useI18n();

  useEffect(() => {
    if (target?.status === 'COMPLETED' && changes.length === 0) void loadComparison('');
  }, [changes.length, loadComparison, target?.status]);

  function handleOpen(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault();
    if (path.trim()) void openComparisonDump(path.trim());
  }

  function handleSearch(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault();
    void loadComparison(query);
  }

  if (!target) {
    return (
      <section className={styles.panel}>
        <div className={styles.copy}>
          <span className={styles.headingIcon}><CompareIcon /></span>
          <div>
            <p className={styles.eyebrow}>{t('comparison.eyebrow')}</p>
            <h2>{t('comparison.title')}</h2>
            <p>{t('comparison.description')}</p>
          </div>
        </div>
        <div className={styles.openActions}>
          <button
            className={styles.chooseButton}
            type="button"
            disabled={isChoosing || isOpening || systemPickerAvailable === false}
            onClick={() => void chooseComparisonDump()}
          >
            <FolderIcon />
            {isChoosing ? t('comparison.choosing') : t('comparison.choose')}
          </button>
          <div className={styles.pathDivider}><span>{t('comparison.divider')}</span></div>
          <form className={styles.openForm} onSubmit={handleOpen}>
            <label htmlFor="comparison-path">{t('comparison.pathLabel')}</label>
            <div>
              <input
                id="comparison-path"
                value={path}
                onChange={(event) => setPath(event.target.value)}
                placeholder={t('comparison.pathPlaceholder')}
                spellCheck={false}
              />
              <button type="submit" disabled={!path.trim() || isChoosing || isOpening}>
                {isOpening ? t('comparison.opening') : t('comparison.compare')}
              </button>
            </div>
          </form>
        </div>
      </section>
    );
  }

  if (target.status !== 'COMPLETED') {
    const progress = target.totalBytes > 0 ? (target.processedBytes / target.totalBytes) * 100 : 0;
    return (
      <section className={styles.panel}>
        <div className={styles.progressHeading}>
          <div>
            <p className={styles.eyebrow}>{t('comparison.eyebrow')}</p>
            <h2>{target.status === 'FAILED'
              ? t('comparison.failed')
              : t('comparison.reading', {fileName: target.fileName})}</h2>
            <p>{target.error
              ? translateError(locale, target.error)
              : t('comparison.progress', {
                processed: formatBytes(target.processedBytes),
                total: formatBytes(target.totalBytes),
              })}</p>
          </div>
          <button className={styles.secondaryButton} type="button" onClick={() => void closeComparison()}>
            {target.status === 'FAILED' ? t('common.close') : t('comparison.cancel')}
          </button>
        </div>
        {target.status !== 'FAILED' && <progress max={100} value={progress} />}
      </section>
    );
  }

  return (
    <section className={styles.panel}>
      <div className={styles.comparisonHeading}>
        <div>
          <p className={styles.eyebrow}>{t('comparison.eyebrow')}</p>
          <h2>{baseline.fileName} → {target.fileName}</h2>
          <p>{t('comparison.deltaDescription')}</p>
        </div>
        <button className={styles.secondaryButton} type="button" onClick={() => void closeComparison()}>
          {t('comparison.changeTarget')}
        </button>
      </div>

      <form className={styles.search} onSubmit={handleSearch}>
        <label htmlFor="comparison-search">{t('comparison.filterLabel')}</label>
        <div>
          <SearchIcon />
          <input
            id="comparison-search"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder={t('comparison.filterPlaceholder')}
          />
          <button type="submit">{t('comparison.filter')}</button>
        </div>
      </form>

      <div className={styles.tableWrap} aria-busy={isLoading}>
        <table>
          <thead>
            <tr>
              <th>{t('comparison.class')}</th>
              <th>{t('comparison.before')}</th>
              <th>{t('comparison.after')}</th>
              <th>{t('comparison.countDelta')}</th>
              <th>{t('comparison.heapDelta')}</th>
            </tr>
          </thead>
          <tbody>
            {changes.map((entry) => <ComparisonRow key={entry.className} entry={entry} />)}
          </tbody>
        </table>
        {!isLoading && changes.length === 0 && <div className={styles.emptyRows}>{t('comparison.empty')}</div>}
      </div>
    </section>
  );
}

interface ComparisonRowProps {
  readonly entry: HeapComparisonEntry;
}

function ComparisonRow({entry}: ComparisonRowProps) {
  const {locale, t} = useI18n();
  const countSign = entry.countDelta > 0 ? '+' : '';
  const byteSign = entry.shallowHeapBytesDelta > 0 ? '+' : '';
  const deltaClass = entry.shallowHeapBytesDelta > 0
    ? styles.growth
    : entry.shallowHeapBytesDelta < 0
      ? styles.reduction
      : styles.unchanged;

  return (
    <tr>
      <td>
        <code>{entry.className}</code>
        {entry.sizeIsEstimated && <span className={styles.estimate}>{t('common.estimated')}</span>}
      </td>
      <td>{formatCount(entry.baselineCount, locale)}</td>
      <td>{formatCount(entry.targetCount, locale)}</td>
      <td className={deltaClass}>{countSign}{formatCount(entry.countDelta, locale)}</td>
      <td className={deltaClass}>{byteSign}{formatBytes(entry.shallowHeapBytesDelta)}</td>
    </tr>
  );
}
