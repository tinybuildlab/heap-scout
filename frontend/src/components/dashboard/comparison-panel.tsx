import {useEffect, useState, type FormEvent} from 'react';
import {formatBytes, formatCount} from '../../lib/format';
import {useAnalysisStore} from '../../stores/use-analysis-store';
import {useUiStore} from '../../stores/use-ui-store';
import type {AnalysisJob, HeapComparisonEntry} from '../../types';
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
  const chooseComparisonDump = useAnalysisStore((state) => state.chooseComparisonDump);
  const openComparisonDump = useAnalysisStore((state) => state.openComparisonDump);
  const loadComparison = useAnalysisStore((state) => state.loadComparison);
  const closeComparison = useAnalysisStore((state) => state.closeComparison);
  const query = useUiStore((state) => state.comparisonQuery);
  const setQuery = useUiStore((state) => state.setComparisonQuery);

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
          <p className={styles.eyebrow}>COMPARE SNAPSHOTS</p>
          <h2>What changed after the baseline?</h2>
          <p>Open a later dump to rank class growth by object count and shallow heap delta.</p>
        </div>
        <button
          className={styles.chooseButton}
          type="button"
          disabled={isChoosing || isOpening}
          onClick={() => void chooseComparisonDump()}
        >
          {isChoosing ? 'Choose target…' : 'Choose target dump'}
        </button>
        <div className={styles.pathDivider}><span>or enter a local path</span></div>
        <form className={styles.openForm} onSubmit={handleOpen}>
          <label htmlFor="comparison-path">Target dump path</label>
          <div>
            <input
              id="comparison-path"
              value={path}
              onChange={(event) => setPath(event.target.value)}
              placeholder="/path/to/later.hprof"
              spellCheck={false}
            />
            <button type="submit" disabled={!path.trim() || isChoosing || isOpening}>
              {isOpening ? 'Opening…' : 'Compare'}
            </button>
          </div>
        </form>
      </section>
    );
  }

  if (target.status !== 'COMPLETED') {
    const progress = target.totalBytes > 0 ? (target.processedBytes / target.totalBytes) * 100 : 0;
    return (
      <section className={styles.panel}>
        <div className={styles.progressHeading}>
          <div>
            <p className={styles.eyebrow}>COMPARISON TARGET</p>
            <h2>{target.status === 'FAILED' ? 'Target analysis failed' : `Reading ${target.fileName}`}</h2>
            <p>{target.error?.message ?? `${formatBytes(target.processedBytes)} of ${formatBytes(target.totalBytes)}`}</p>
          </div>
          <button className={styles.secondaryButton} type="button" onClick={() => void closeComparison()}>
            {target.status === 'FAILED' ? 'Close' : 'Cancel'}
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
          <p className={styles.eyebrow}>SNAPSHOT DELTA</p>
          <h2>{baseline.fileName} → {target.fileName}</h2>
          <p>Positive values grew in the target snapshot. Estimates are marked explicitly.</p>
        </div>
        <button className={styles.secondaryButton} type="button" onClick={() => void closeComparison()}>
          Change target
        </button>
      </div>

      <form className={styles.search} onSubmit={handleSearch}>
        <label htmlFor="comparison-search">Filter changed classes</label>
        <div>
          <input
            id="comparison-search"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="com.example.cache"
          />
          <button type="submit">Filter</button>
        </div>
      </form>

      <div className={styles.tableWrap} aria-busy={isLoading}>
        <table>
          <thead>
            <tr>
              <th>Class</th>
              <th>Before</th>
              <th>After</th>
              <th>Count Δ</th>
              <th>Heap Δ</th>
            </tr>
          </thead>
          <tbody>
            {changes.map((entry) => <ComparisonRow key={entry.className} entry={entry} />)}
          </tbody>
        </table>
      </div>
    </section>
  );
}

interface ComparisonRowProps {
  readonly entry: HeapComparisonEntry;
}

function ComparisonRow({entry}: ComparisonRowProps) {
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
        {entry.sizeIsEstimated && <span className={styles.estimate}>estimated</span>}
      </td>
      <td>{formatCount(entry.baselineCount)}</td>
      <td>{formatCount(entry.targetCount)}</td>
      <td className={deltaClass}>{countSign}{formatCount(entry.countDelta)}</td>
      <td className={deltaClass}>{byteSign}{formatBytes(entry.shallowHeapBytesDelta)}</td>
    </tr>
  );
}
