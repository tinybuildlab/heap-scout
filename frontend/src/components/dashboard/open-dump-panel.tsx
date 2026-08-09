import {useState, type FormEvent} from 'react';
import styles from './open-dump-panel.module.css';

interface OpenDumpPanelProps {
  readonly isChoosing: boolean;
  readonly isOpening: boolean;
  readonly isRestoring: boolean;
  readonly onChoose: () => Promise<void>;
  readonly onOpen: (path: string) => Promise<void>;
}

export function OpenDumpPanel({isChoosing, isOpening, isRestoring, onChoose, onOpen}: OpenDumpPanelProps) {
  const [path, setPath] = useState('');
  const isBusy = isChoosing || isOpening || isRestoring;

  function handleSubmit(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault();
    if (path.trim()) void onOpen(path.trim());
  }

  return (
    <section className={styles.panel}>
      <div className={styles.copy}>
        <p className={styles.eyebrow}>LOCAL HEAP ANALYSIS</p>
        <h1>Find what is holding your memory.</h1>
        <p className={styles.description}>
          Open a Java or Kotlin HPROF locally. HeapScout streams the file on this machine and never
          uploads it.
        </p>
      </div>

      <div className={styles.openCard}>
        <button
          className={styles.chooseButton}
          type="button"
          disabled={isBusy}
          onClick={() => void onChoose()}
        >
          <span>{isRestoring ? 'Restoring recent analysis…' : isChoosing ? 'Choose a heap dump…' : 'Choose heap dump'}</span>
          <small>HPROF stays on this computer</small>
        </button>

        <div className={styles.divider}><span>or enter a local path</span></div>

        <form className={styles.form} onSubmit={handleSubmit}>
          <label className={styles.label} htmlFor="heap-path">Absolute path</label>
        <div className={styles.inputRow}>
          <input
            id="heap-path"
            className={styles.input}
            type="text"
            value={path}
            onChange={(event) => setPath(event.target.value)}
            placeholder="/path/to/java_pid1234.hprof"
            autoComplete="off"
            spellCheck={false}
          />
          <button className={styles.openButton} type="submit" disabled={!path.trim() || isBusy}>
            {isOpening ? 'Opening…' : 'Analyze dump'}
          </button>
        </div>
          <p className={styles.hint}>Headless mode: paste a readable .hprof or .bin path here.</p>
        </form>
      </div>

      <div className={styles.features} aria-label="HeapScout guarantees">
        <article>
          <span className={styles.featureIndex}>01</span>
          <strong>Private by design</strong>
          <p>Loopback only. No telemetry or remote upload.</p>
        </article>
        <article>
          <span className={styles.featureIndex}>02</span>
          <strong>Bounded memory</strong>
          <p>Streams object records instead of retaining the full graph.</p>
        </article>
        <article>
          <span className={styles.featureIndex}>03</span>
          <strong>Built to compare</strong>
          <p>Track class growth across before and after snapshots.</p>
        </article>
      </div>
    </section>
  );
}
