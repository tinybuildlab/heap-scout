import {useState, type FormEvent} from 'react';
import {ArrowRightIcon, CompareIcon, FolderIcon, ShieldIcon, StreamIcon} from '../common/icons';
import {useI18n} from '../../hooks/use-i18n';
import styles from './open-dump-panel.module.css';

interface OpenDumpPanelProps {
  readonly isChoosing: boolean;
  readonly isOpening: boolean;
  readonly isRestoring: boolean;
  readonly systemPickerAvailable: boolean | null;
  readonly onChoose: () => Promise<void>;
  readonly onOpen: (path: string) => Promise<void>;
}

export function OpenDumpPanel({
  isChoosing,
  isOpening,
  isRestoring,
  systemPickerAvailable,
  onChoose,
  onOpen,
}: OpenDumpPanelProps) {
  const [path, setPath] = useState('');
  const {t} = useI18n();
  const isBusy = isChoosing || isOpening || isRestoring;
  const pickerUnavailable = systemPickerAvailable === false;

  function handleSubmit(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault();
    if (path.trim()) void onOpen(path.trim());
  }

  return (
    <main className={styles.page}>
      <section className={styles.hero}>
        <div className={styles.copy}>
          <p className={styles.eyebrow}><span />{t('landing.eyebrow')}</p>
          <h1>{t('landing.title')}</h1>
          <p className={styles.description}>{t('landing.description')}</p>
          <ul className={styles.signals} aria-label={t('landing.capabilitiesLabel')}>
            <li><ShieldIcon />{t('landing.signal.private')}</li>
            <li><StreamIcon />{t('landing.signal.streaming')}</li>
            <li><CompareIcon />{t('landing.signal.compare')}</li>
          </ul>
        </div>

        <div className={styles.openCard}>
          <div className={styles.cardHeading}>
            <span className={styles.folderBadge}><FolderIcon /></span>
            <div>
              <p>{t('landing.openEyebrow')}</p>
              <h2>{t('landing.openTitle')}</h2>
            </div>
          </div>
          <p className={styles.cardDescription}>{t('landing.openDescription')}</p>

          {pickerUnavailable && (
            <div className={styles.pickerNotice} role="status">
              <strong>{t('landing.pickerUnavailableTitle')}</strong>
              <span>{t('landing.pickerUnavailableDescription')}</span>
            </div>
          )}

          <button
            className={styles.chooseButton}
            type="button"
            disabled={isBusy || pickerUnavailable}
            onClick={() => void onChoose()}
          >
            <span className={styles.chooseLabel}>
              <FolderIcon />
              <span>
                <strong>
                  {isRestoring
                    ? t('landing.restoring')
                    : isChoosing
                      ? t('landing.choosing')
                      : t('landing.choose')}
                </strong>
                <small>{t('landing.chooseHint')}</small>
              </span>
            </span>
            <ArrowRightIcon className={styles.arrowIcon} />
          </button>

          <div className={styles.divider}><span>{t('landing.divider')}</span></div>

          <form className={styles.form} onSubmit={handleSubmit}>
            <label className={styles.label} htmlFor="heap-path">{t('landing.pathLabel')}</label>
            <div className={styles.inputRow}>
              <input
                id="heap-path"
                className={styles.input}
                type="text"
                value={path}
                onChange={(event) => setPath(event.target.value)}
                placeholder={t('landing.pathPlaceholder')}
                autoComplete="off"
                spellCheck={false}
                autoFocus={pickerUnavailable}
              />
              <button className={styles.openButton} type="submit" disabled={!path.trim() || isBusy}>
                {isOpening ? t('landing.opening') : t('landing.analyze')}
                <ArrowRightIcon />
              </button>
            </div>
            <p className={styles.hint}>{t('landing.pathHint')}</p>
          </form>
        </div>
      </section>

      <section className={styles.features} aria-label={t('landing.guaranteesLabel')}>
        <article>
          <span className={styles.featureIcon}><ShieldIcon /></span>
          <div><strong>{t('landing.featurePrivateTitle')}</strong><p>{t('landing.featurePrivateDescription')}</p></div>
        </article>
        <article>
          <span className={styles.featureIcon}><StreamIcon /></span>
          <div><strong>{t('landing.featureMemoryTitle')}</strong><p>{t('landing.featureMemoryDescription')}</p></div>
        </article>
        <article>
          <span className={styles.featureIcon}><CompareIcon /></span>
          <div><strong>{t('landing.featureCompareTitle')}</strong><p>{t('landing.featureCompareDescription')}</p></div>
        </article>
      </section>
    </main>
  );
}
