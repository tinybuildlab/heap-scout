import {formatBytes, formatCount} from '../../lib/format';
import {CompareIcon, SearchIcon} from '../common/icons';
import {useI18n} from '../../hooks/use-i18n';
import type {ClassHistogramEntry} from '../../types';
import styles from './investigation-leads.module.css';

interface InvestigationLeadsProps {
  readonly histogram: readonly ClassHistogramEntry[];
}

export function InvestigationLeads({histogram}: InvestigationLeadsProps) {
  const {locale, t} = useI18n();
  if (histogram.length === 0) return null;

  const largest = histogram.reduce((current, entry) => (
    entry.shallowHeapBytes > current.shallowHeapBytes ? entry : current
  ));
  const mostNumerous = histogram.reduce((current, entry) => (
    entry.instanceCount > current.instanceCount ? entry : current
  ));

  return (
    <aside className={styles.section} aria-labelledby="investigation-leads-title">
      <div className={styles.heading}>
        <div>
          <p>{t('investigation.eyebrow')}</p>
          <h2 id="investigation-leads-title">{t('investigation.title')}</h2>
          <small>{t('investigation.description')}</small>
        </div>
        <span>{t('investigation.shallowOnly')}</span>
      </div>
      <div className={styles.grid}>
        <article>
          <div className={styles.cardLabel}><SearchIcon /><small>{t('investigation.largest')}</small></div>
          <code>{largest.className}</code>
          <strong>{formatBytes(largest.shallowHeapBytes)}</strong>
          <p>{t('investigation.largestHint')}</p>
        </article>
        <article>
          <div className={styles.cardLabel}><SearchIcon /><small>{t('investigation.mostNumerous')}</small></div>
          <code>{mostNumerous.className}</code>
          <strong>{t('investigation.objects', {count: formatCount(mostNumerous.instanceCount, locale)})}</strong>
          <p>{t('investigation.countHint')}</p>
        </article>
        <article className={styles.guidance}>
          <div className={styles.cardLabel}><CompareIcon /><small>{t('investigation.confirm')}</small></div>
          <strong>{t('investigation.compareLater')}</strong>
          <p>{t('investigation.confirmHint')}</p>
        </article>
      </div>
    </aside>
  );
}
