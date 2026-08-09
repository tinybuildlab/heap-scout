import {useEffect, type FormEvent} from 'react';
import {useAnalysisStore} from '../../stores/use-analysis-store';
import {useUiStore} from '../../stores/use-ui-store';
import type {AnalysisJob, ClassHistogramEntry} from '../../types';
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
          <div className={styles.breadcrumb}>DUMPS / {job.fileName}</div>
          <h1>Heap overview</h1>
          <p>{job.sourcePath}</p>
        </div>
        <button className={styles.closeButton} type="button" onClick={() => void closeDump()}>
          Close dump
        </button>
      </header>

      <section className={styles.stats} aria-label="Heap summary">
        <SummaryCard label="Objects" value={formatCount(job.summary.objectCount)} note="parsed records" />
        <SummaryCard label="Classes" value={formatCount(job.summary.classCount)} note="normalized names" />
        <SummaryCard
          label="Shallow heap"
          value={formatBytes(job.summary.shallowHeapBytes)}
          note={job.summary.containsEstimatedSizes ? 'includes array estimates' : 'exact from records'}
        />
        <SummaryCard label="Parse time" value={formatDuration(job.summary.parseDurationMillis)} note={job.summary.format} />
      </section>

      <ComparisonPanel baseline={job} />

      <InvestigationLeads histogram={histogram} />

      <section className={styles.workspace}>
        <div className={styles.sectionHeading}>
          <div>
            <p className={styles.eyebrow}>CLASS HISTOGRAM</p>
            <h2>Where the heap is concentrated</h2>
          </div>
          <form className={styles.search} onSubmit={handleSearch}>
            <label className={styles.visuallyHidden} htmlFor="histogram-search">Search classes</label>
            <input
              id="histogram-search"
              value={searchQuery}
              onChange={(event) => setSearchQuery(event.target.value)}
              placeholder="name:cache size>10MB count>1000"
            />
            <button type="submit">Search</button>
          </form>
        </div>

        <div className={styles.tableWrap} aria-busy={isLoadingHistogram}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th>Class</th>
                <th>Instances</th>
                <th>Shallow heap</th>
                <th>Share</th>
              </tr>
            </thead>
            <tbody>
              {histogram.map((entry) => (
                <HistogramRow key={entry.className} entry={entry} maxBytes={maxBytes} />
              ))}
            </tbody>
          </table>
          {!isLoadingHistogram && histogram.length === 0 && (
            <div className={styles.emptyRows}>No classes match this search.</div>
          )}
        </div>
      </section>
    </main>
  );
}

interface SummaryCardProps {
  readonly label: string;
  readonly value: string;
  readonly note: string;
}

function SummaryCard({label, value, note}: SummaryCardProps) {
  return (
    <article className={styles.statCard}>
      <span>{label}</span>
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
  return (
    <tr>
      <td>
        <code>{entry.className}</code>
        {entry.sizeIsEstimated && <span className={styles.estimate}>estimated</span>}
      </td>
      <td>{formatCount(entry.instanceCount)}</td>
      <td>{formatBytes(entry.shallowHeapBytes)}</td>
      <td>
        <progress max={maxBytes} value={entry.shallowHeapBytes} aria-label={`${entry.className} relative size`} />
      </td>
    </tr>
  );
}

interface AnalysisProgressProps {
  readonly job: AnalysisJob;
  readonly onClose: () => Promise<void>;
}

function AnalysisProgress({job, onClose}: AnalysisProgressProps) {
  const progress = job.totalBytes > 0 ? Math.round((job.processedBytes / job.totalBytes) * 100) : 0;
  const failed = job.status === 'FAILED' || job.status === 'CANCELLED';

  return (
    <main className={styles.progressPage}>
      <section className={styles.progressCard}>
        <p className={styles.eyebrow}>{failed ? 'ANALYSIS STOPPED' : 'STREAMING HPROF'}</p>
        <h1>{failed ? 'The dump could not be analyzed.' : `Reading ${job.fileName}`}</h1>
        <p className={styles.progressDescription}>
          {job.error?.message ?? 'HeapScout is aggregating class metadata without loading every object into memory.'}
        </p>
        {!failed && (
          <div className={styles.progressBlock}>
            <div className={styles.progressLabels}>
              <span>{job.phase?.toLowerCase() ?? 'queued'}</span>
              <strong>{progress}%</strong>
            </div>
            <progress max={100} value={progress} />
            <small>{formatBytes(job.processedBytes)} of {formatBytes(job.totalBytes)}</small>
          </div>
        )}
        <button className={styles.closeButton} type="button" onClick={() => void onClose()}>
          {failed ? 'Close dump' : 'Cancel analysis'}
        </button>
      </section>
    </main>
  );
}
