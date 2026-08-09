import {StrictMode} from 'react';
import {createRoot} from 'react-dom/client';
import {App} from './app';
import {ErrorBoundary} from './components/common/error-boundary';
import './styles/global.css';

const root = document.getElementById('root');
if (!root) throw new Error('Root element was not found');

createRoot(root).render(
  <StrictMode>
    <ErrorBoundary>
      <App />
    </ErrorBoundary>
  </StrictMode>,
);
