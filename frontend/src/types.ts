export type AnalysisJobStatus =
  | 'QUEUED'
  | 'RUNNING'
  | 'COMPLETED'
  | 'FAILED'
  | 'CANCELLED';

export type ParsePhase = 'HEADER' | 'METADATA' | 'HEAP' | 'COMPLETE';

export interface AnalysisFailure {
  readonly code: string;
  readonly message: string;
}

export interface HeapSummary {
  readonly fileSizeBytes: number;
  readonly format: string;
  readonly identifierSizeBytes: number;
  readonly capturedAt: string;
  readonly objectCount: number;
  readonly classCount: number;
  readonly shallowHeapBytes: number;
  readonly containsEstimatedSizes: boolean;
  readonly parseDurationMillis: number;
}

export interface AnalysisJob {
  readonly id: string;
  readonly fileName: string;
  readonly sourcePath: string;
  readonly status: AnalysisJobStatus;
  readonly phase: ParsePhase | null;
  readonly processedBytes: number;
  readonly totalBytes: number;
  readonly createdAt: string;
  readonly updatedAt: string;
  readonly summary: HeapSummary | null;
  readonly error: AnalysisFailure | null;
}

export interface ClassHistogramEntry {
  readonly className: string;
  readonly instanceCount: number;
  readonly shallowHeapBytes: number;
  readonly sizeIsEstimated: boolean;
}

export interface HeapComparisonEntry {
  readonly className: string;
  readonly baselineCount: number;
  readonly targetCount: number;
  readonly countDelta: number;
  readonly baselineShallowHeapBytes: number;
  readonly targetShallowHeapBytes: number;
  readonly shallowHeapBytesDelta: number;
  readonly sizeIsEstimated: boolean;
}

export interface PageSlice<T> {
  readonly items: readonly T[];
  readonly page: number;
  readonly pageSize: number;
  readonly totalItems: number;
  readonly totalPages: number;
}

export interface ApiError {
  readonly code: string;
  readonly message: string;
  readonly timestamp: string;
}

export interface LocalFileSelection {
  readonly selected: boolean;
  readonly path: string | null;
}

export interface LocalFileCapabilities {
  readonly systemPickerAvailable: boolean;
}
