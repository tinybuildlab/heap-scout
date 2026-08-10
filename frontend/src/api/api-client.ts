import type {
  AnalysisJob,
  ApiError,
  ClassHistogramEntry,
  HeapComparisonEntry,
  LocalFileCapabilities,
  LocalFileSelection,
  PageSlice,
} from '../types';

export class ApiClientError extends Error {
  readonly code: string;

  constructor(code: string, message: string, options?: ErrorOptions) {
    super(message, options);
    this.name = 'ApiClientError';
    this.code = code;
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(path, {
      ...init,
      headers: {
        'Content-Type': 'application/json',
        ...init?.headers,
      },
    });
  } catch (cause: unknown) {
    throw new ApiClientError('NETWORK_ERROR', 'Could not connect to the local HeapScout process.', {cause});
  }

  if (!response.ok) {
    const fallback = `Request failed with status ${response.status}`;
    const error = await response.json().catch(() => null) as ApiError | null;
    throw new ApiClientError(error?.code ?? 'REQUEST_FAILED', error?.message ?? fallback);
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

  getLocalFileCapabilities(): Promise<LocalFileCapabilities> {
    return request('/api/local-files/capabilities');
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
