import {formatBytes, formatCount} from '../../lib/format';
import type {ClassHistogramEntry} from '../../types';
import styles from './investigation-leads.module.css';

interface InvestigationLeadsProps {
  readonly histogram: readonly ClassHistogramEntry[];
}

export function InvestigationLeads({histogram}: InvestigationLeadsProps) {
  if (histogram.length === 0) return null;

  const largest = histogram.reduce((current, entry) => (
    entry.shallowHeapBytes > current.shallowHeapBytes ? entry : current
  ));
  const mostNumerous = histogram.reduce((current, entry) => (
    entry.instanceCount > current.instanceCount ? entry : current
  ));

  return (
    <section className={styles.section} aria-labelledby="investigation-leads-title">
      <div className={styles.heading}>
        <div>
          <p>INVESTIGATION LEADS</p>
          <h2 id="investigation-leads-title">Start with evidence, not a leak verdict.</h2>
        </div>
        <span>Shallow heap only</span>
      </div>
      <div className={styles.grid}>
        <article>
          <small>Largest class footprint</small>
          <code>{largest.className}</code>
          <strong>{formatBytes(largest.shallowHeapBytes)}</strong>
          <p>Inspect whether this class is expected to own this much direct memory.</p>
        </article>
        <article>
          <small>Highest instance count</small>
          <code>{mostNumerous.className}</code>
          <strong>{formatCount(mostNumerous.instanceCount)} objects</strong>
          <p>A high count matters most when it keeps growing across snapshots.</p>
        </article>
        <article className={styles.guidance}>
          <small>How to confirm</small>
          <strong>Compare a later snapshot</strong>
          <p>Consistent positive growth is a stronger signal. Retained size and GC-root paths are required before calling it a leak.</p>
        </article>
      </div>
    </section>
  );
}
