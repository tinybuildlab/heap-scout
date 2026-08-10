import {useEffect, type FormEvent} from 'react';
import {SearchIcon} from '../common/icons';
import {useI18n} from '../../hooks/use-i18n';
import type {TranslationKey} from '../../i18n/translations';
import {useAnalysisStore} from '../../stores/use-analysis-store';
import {useUiStore} from '../../stores/use-ui-store';
import type {AnalysisJob, ClassHistogramEntry, ParsePhase} from '../../types';
import {formatBytes, formatCount, formatDuration} from '../../lib/format';
import {ComparisonPanel} from './comparison-panel';
import {InvestigationLeads} from './investigation-leads';
import styles from './analysis-dashboard.module.css';

interface AnalysisDashboardProps {
  readonly job: AnalysisJob;
}

export function AnalysisDashboard({job}: AnalysisDashboardProps) {
  const histogram = useAnalysisStore((state) => state.histogram);
  const isLoadingHistogram = useAnalysisStore((state) => state.isLoadingHistogram);
  const loadHistogram = useAnalysisStore((state) => state.loadHistogram);
  const closeDump = useAnalysisStore((state) => state.closeDump);
  const searchQuery = useUiStore((state) => state.searchQuery);
  const setSearchQuery = useUiStore((state) => state.setSearchQuery);
  const {locale, t} = useI18n();

  useEffect(() => {
    if (job.status === 'COMPLETED' && histogram.length === 0) void loadHistogram('');
  }, [histogram.length, job.status, loadHistogram]);

  function handleSearch(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault();
    void loadHistogram(searchQuery);
  }

  if (job.status !== 'COMPLETED' || !job.summary) {
    return <AnalysisProgress job={job} onClose={closeDump} />;
  }

  const maxBytes = histogram.reduce((maximum, entry) => Math.max(maximum, entry.shallowHeapBytes), 1);

  return (
    <main className={styles.dashboard}>
      <header className={styles.heading}>
        <div>
          <div className={styles.breadcrumb}><span>{t('dashboard.breadcrumb')}</span><i />{job.fileName}</div>
          <h1>{t('dashboard.title')}</h1>
          <p className={styles.headingDescription}>{t('dashboard.description')}</p>
          <code className={styles.sourcePath}>{job.sourcePath}</code>
        </div>
        <button className={styles.closeButton} type="button" onClick={() => void closeDump()}>
          {t('dashboard.closeDump')}
        </button>
      </header>

      <section className={styles.stats} aria-label={t('dashboard.title')}>
        <SummaryCard index="01" label={t('dashboard.objects')} value={formatCount(job.summary.objectCount, locale)} note={t('dashboard.parsedRecords')} />
        <SummaryCard index="02" label={t('dashboard.classes')} value={formatCount(job.summary.classCount, locale)} note={t('dashboard.normalizedNames')} />
        <SummaryCard
          index="03"
          label={t('dashboard.shallowHeap')}
          value={formatBytes(job.summary.shallowHeapBytes)}
          note={job.summary.containsEstimatedSizes ? t('dashboard.includesEstimates') : t('dashboard.exactRecords')}
        />
        <SummaryCard index="04" label={t('dashboard.parseTime')} value={formatDuration(job.summary.parseDurationMillis, locale)} note={job.summary.format} />
      </section>

      <div className={styles.analysisGrid}>
        <section className={styles.workspace}>
          <div className={styles.sectionHeading}>
            <div>
              <p className={styles.eyebrow}>{t('histogram.eyebrow')}</p>
              <h2>{t('histogram.title')}</h2>
              <span>{t('histogram.description')}</span>
            </div>
          </div>
          <form className={styles.search} onSubmit={handleSearch}>
            <label className={styles.visuallyHidden} htmlFor="histogram-search">{t('histogram.searchLabel')}</label>
            <div className={styles.searchInput}>
              <SearchIcon />
              <input
                id="histogram-search"
                value={searchQuery}
                onChange={(event) => setSearchQuery(event.target.value)}
                placeholder={t('histogram.searchPlaceholder')}
              />
            </div>
            <button type="submit">{t('histogram.search')}</button>
          </form>

          <div className={styles.tableWrap} aria-busy={isLoadingHistogram}>
            <table className={styles.table}>
              <thead>
                <tr>
                  <th>{t('histogram.class')}</th>
                  <th>{t('histogram.instances')}</th>
                  <th>{t('histogram.shallowHeap')}</th>
                  <th>{t('histogram.share')}</th>
                </tr>
              </thead>
              <tbody>
                {histogram.map((entry) => (
                  <HistogramRow key={entry.className} entry={entry} maxBytes={maxBytes} />
                ))}
              </tbody>
            </table>
            {!isLoadingHistogram && histogram.length === 0 && (
              <div className={styles.emptyRows}>{t('histogram.empty')}</div>
            )}
          </div>
        </section>

        <InvestigationLeads histogram={histogram} />
      </div>

      <ComparisonPanel baseline={job} />
    </main>
  );
}

interface SummaryCardProps {
  readonly index: string;
  readonly label: string;
  readonly value: string;
  readonly note: string;
}

function SummaryCard({index, label, value, note}: SummaryCardProps) {
  return (
    <article className={styles.statCard}>
      <div><span>{label}</span><i>{index}</i></div>
      <strong>{value}</strong>
      <small>{note}</small>
    </article>
  );
}

interface HistogramRowProps {
  readonly entry: ClassHistogramEntry;
  readonly maxBytes: number;
}

function HistogramRow({entry, maxBytes}: HistogramRowProps) {
  const {locale, t} = useI18n();
  return (
    <tr>
      <td>
        <code>{entry.className}</code>
        {entry.sizeIsEstimated && <span className={styles.estimate}>{t('common.estimated')}</span>}
      </td>
      <td>{formatCount(entry.instanceCount, locale)}</td>
      <td>{formatBytes(entry.shallowHeapBytes)}</td>
      <td>
        <progress
          max={maxBytes}
          value={entry.shallowHeapBytes}
          aria-label={t('histogram.relativeSize', {className: entry.className})}
        />
      </td>
    </tr>
  );
}

interface AnalysisProgressProps {
  readonly job: AnalysisJob;
  readonly onClose: () => Promise<void>;
}

function AnalysisProgress({job, onClose}: AnalysisProgressProps) {
  const {t} = useI18n();
  const progress = job.totalBytes > 0 ? Math.round((job.processedBytes / job.totalBytes) * 100) : 0;
  const failed = job.status === 'FAILED' || job.status === 'CANCELLED';
  const phase = job.phase ? t(PHASE_TRANSLATIONS[job.phase]) : t('phase.queued');

  return (
    <main className={styles.progressPage}>
      <section className={styles.progressCard}>
        <p className={styles.eyebrow}>{failed ? t('progress.stoppedEyebrow') : t('progress.runningEyebrow')}</p>
        <h1>{failed ? t('progress.failedTitle') : t('progress.readingTitle', {fileName: job.fileName})}</h1>
        <p className={styles.progressDescription}>
          {job.error?.message ?? t('progress.description')}
        </p>
        {!failed && (
          <div className={styles.progressBlock}>
            <div className={styles.progressLabels}>
              <span>{phase}</span>
              <strong>{progress}%</strong>
            </div>
            <progress max={100} value={progress} />
            <small>{t('progress.processed', {
              processed: formatBytes(job.processedBytes),
              total: formatBytes(job.totalBytes),
            })}</small>
          </div>
        )}
        <button className={styles.closeButton} type="button" onClick={() => void onClose()}>
          {failed ? t('dashboard.closeDump') : t('progress.cancelAnalysis')}
        </button>
      </section>
    </main>
  );
}

const PHASE_TRANSLATIONS: Readonly<Record<ParsePhase, TranslationKey>> = {
  HEADER: 'phase.header',
  METADATA: 'phase.metadata',
  HEAP: 'phase.heap',
  COMPLETE: 'phase.complete',
};
