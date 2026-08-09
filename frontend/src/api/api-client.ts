import type {
  AnalysisJob,
  ApiError,
  ClassHistogramEntry,
  HeapComparisonEntry,
  LocalFileSelection,
  PageSlice,
} from '../types';

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...init?.headers,
    },
  });

  if (!response.ok) {
    const fallback = `Request failed with status ${response.status}`;
    const error = await response.json().catch(() => null) as ApiError | null;
    throw new Error(error?.message ?? fallback);
  }

  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

export const apiClient = {
  openDump(path: string): Promise<AnalysisJob> {
    return request('/api/dumps', {
      method: 'POST',
      body: JSON.stringify({path}),
    });
  },

  getRecentJobs(): Promise<readonly AnalysisJob[]> {
    return request('/api/dumps');
  },

  pickLocalHeapDump(): Promise<LocalFileSelection> {
    return request('/api/local-files/pick', {method: 'POST'});
  },

  getJob(jobId: string): Promise<AnalysisJob> {
    return request(`/api/dumps/${encodeURIComponent(jobId)}`);
  },

  getHistogram(jobId: string, query: string): Promise<PageSlice<ClassHistogramEntry>> {
    const parameters = new URLSearchParams({
      query,
      page: '0',
      pageSize: '200',
      sort: 'SHALLOW_BYTES',
      direction: 'DESCENDING',
    });
    return request(`/api/dumps/${encodeURIComponent(jobId)}/histogram?${parameters}`);
  },

  getComparison(
    baselineJobId: string,
    targetJobId: string,
    query: string,
  ): Promise<PageSlice<HeapComparisonEntry>> {
    const parameters = new URLSearchParams({
      baseline: baselineJobId,
      target: targetJobId,
      query,
      page: '0',
      pageSize: '200',
    });
    return request(`/api/comparisons?${parameters}`);
  },

  closeDump(jobId: string): Promise<void> {
    return request(`/api/dumps/${encodeURIComponent(jobId)}`, {method: 'DELETE'});
  },
};
