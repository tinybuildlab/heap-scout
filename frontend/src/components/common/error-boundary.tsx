import {Component, type ErrorInfo, type ReactNode} from 'react';
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
      return (
        <main className={styles.page}>
          <section className={styles.card}>
            <p className={styles.eyebrow}>HEAPSCOUT</p>
            <h1>Something went wrong in the interface.</h1>
            <p>Your heap dump has not been modified. Reload the app to continue.</p>
            <button className={styles.button} type="button" onClick={() => window.location.reload()}>
              Reload HeapScout
            </button>
          </section>
        </main>
      );
    }
    return this.props.children;
  }
}
