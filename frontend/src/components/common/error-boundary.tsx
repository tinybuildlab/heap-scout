import {Component, type ErrorInfo, type ReactNode} from 'react';
import {detectPreferredLocale, translate} from '../../i18n/translations';
import styles from './error-boundary.module.css';

interface ErrorBoundaryProps {
  readonly children: ReactNode;
}

interface ErrorBoundaryState {
  readonly hasError: boolean;
}

export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  state: ErrorBoundaryState = {hasError: false};

  static getDerivedStateFromError(): ErrorBoundaryState {
    return {hasError: true};
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
    console.error('[ErrorBoundary]', error, errorInfo);
  }

  render(): ReactNode {
    if (this.state.hasError) {
      const locale = detectPreferredLocale();
      return (
        <main className={styles.page}>
          <section className={styles.card}>
            <p className={styles.eyebrow}>HEAPSCOUT</p>
            <h1>{translate(locale, 'errorBoundary.title')}</h1>
            <p>{translate(locale, 'errorBoundary.description')}</p>
            <button className={styles.button} type="button" onClick={() => window.location.reload()}>
              {translate(locale, 'errorBoundary.reload')}
            </button>
          </section>
        </main>
      );
    }
    return this.props.children;
  }
}
