import {useEffect} from 'react';
import {useAnalysisStore} from '../stores/use-analysis-store';

export function useAnalysisPolling(): void {
  const job = useAnalysisStore((state) => state.job);
  const comparisonJob = useAnalysisStore((state) => state.comparisonJob);
  const refreshJobs = useAnalysisStore((state) => state.refreshJobs);

  useEffect(() => {
    const primaryActive = job?.status === 'QUEUED' || job?.status === 'RUNNING';
    const comparisonActive = comparisonJob?.status === 'QUEUED' || comparisonJob?.status === 'RUNNING';
    if (!primaryActive && !comparisonActive) return;

    const timer = window.setInterval(() => {
      void refreshJobs();
    }, 700);

    return () => window.clearInterval(timer);
  }, [comparisonJob, job, refreshJobs]);
}
