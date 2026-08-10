import {create} from 'zustand';
import {ApiClientError, apiClient} from '../api/api-client';
import type {AnalysisFailure, AnalysisJob, ClassHistogramEntry, HeapComparisonEntry} from '../types';

interface AnalysisState {
  job: AnalysisJob | null;
  comparisonJob: AnalysisJob | null;
  histogram: readonly ClassHistogramEntry[];
  comparison: readonly HeapComparisonEntry[];
  isBootstrapping: boolean;
  hasBootstrapped: boolean;
  isChoosing: boolean;
  isChoosingComparison: boolean;
  isOpening: boolean;
  isOpeningComparison: boolean;
  isLoadingHistogram: boolean;
  isLoadingComparison: boolean;
  systemPickerAvailable: boolean | null;
  error: AnalysisFailure | null;
}

interface AnalysisActions {
  bootstrap: () => Promise<void>;
  chooseDump: () => Promise<void>;
  chooseComparisonDump: () => Promise<void>;
  openDump: (path: string) => Promise<void>;
  openComparisonDump: (path: string) => Promise<void>;
  refreshJobs: () => Promise<void>;
  loadHistogram: (query: string) => Promise<void>;
  loadComparison: (query: string) => Promise<void>;
  closeComparison: () => Promise<void>;
  closeDump: () => Promise<void>;
  clearError: () => void;
}

export const initialAnalysisState: AnalysisState = {
  job: null,
  comparisonJob: null,
  histogram: [],
  comparison: [],
  isBootstrapping: false,
  hasBootstrapped: false,
  isChoosing: false,
  isChoosingComparison: false,
  isOpening: false,
  isOpeningComparison: false,
  isLoadingHistogram: false,
  isLoadingComparison: false,
  systemPickerAvailable: null,
  error: null,
};

export const useAnalysisStore = create<AnalysisState & AnalysisActions>((set, get) => ({
  ...initialAnalysisState,

  bootstrap: async () => {
    if (get().hasBootstrapped || get().isBootstrapping) return;
    set({isBootstrapping: true});
    try {
      const [jobs, capabilities] = await Promise.allSettled([
        apiClient.getRecentJobs(),
        apiClient.getLocalFileCapabilities(),
      ]);
      if (jobs.status === 'fulfilled') {
        if (!get().job) set({job: jobs.value[0] ?? null});
      } else {
        set({error: toFailure(jobs.reason)});
      }
      set({
        systemPickerAvailable: capabilities.status === 'fulfilled'
          ? capabilities.value.systemPickerAvailable
          : null,
      });
    } catch (error: unknown) {
      set({error: toFailure(error)});
    } finally {
      set({isBootstrapping: false, hasBootstrapped: true});
    }
  },

  chooseDump: async () => {
    if (get().systemPickerAvailable === false) {
      set({
        error: {
          code: 'FILE_PICKER_UNAVAILABLE',
          message: 'The system file picker is unavailable in this runtime.',
        },
      });
      return;
    }
    set({isChoosing: true, error: null});
    try {
      const selection = await apiClient.pickLocalHeapDump();
      if (selection.selected && selection.path) {
        set({isChoosing: false});
        await get().openDump(selection.path);
      }
    } catch (error: unknown) {
      set({error: toFailure(error)});
    } finally {
      set({isChoosing: false});
    }
  },

  chooseComparisonDump: async () => {
    if (get().systemPickerAvailable === false) {
      set({
        error: {
          code: 'FILE_PICKER_UNAVAILABLE',
          message: 'The system file picker is unavailable in this runtime.',
        },
      });
      return;
    }
    set({isChoosingComparison: true, error: null});
    try {
      const selection = await apiClient.pickLocalHeapDump();
      if (selection.selected && selection.path) {
        set({isChoosingComparison: false});
        await get().openComparisonDump(selection.path);
      }
    } catch (error: unknown) {
      set({error: toFailure(error)});
    } finally {
      set({isChoosingComparison: false});
    }
  },

  openDump: async (path) => {
    set({isOpening: true, error: null, histogram: [], comparison: [], comparisonJob: null});
    try {
      const job = await apiClient.openDump(path);
      set({job});
    } catch (error: unknown) {
      set({error: toFailure(error)});
    } finally {
      set({isOpening: false});
    }
  },

  openComparisonDump: async (path) => {
    set({isOpeningComparison: true, error: null, comparison: []});
    try {
      const comparisonJob = await apiClient.openDump(path);
      set({comparisonJob});
    } catch (error: unknown) {
      set({error: toFailure(error)});
    } finally {
      set({isOpeningComparison: false});
    }
  },

  refreshJobs: async () => {
    const current = get().job;
    const comparison = get().comparisonJob;
    if (!current && !comparison) return;
    try {
      const [job, comparisonJob] = await Promise.all([
        current ? apiClient.getJob(current.id) : Promise.resolve(null),
        comparison ? apiClient.getJob(comparison.id) : Promise.resolve(null),
      ]);
      set({
        job,
        comparisonJob,
        error: job?.error ?? comparisonJob?.error ?? null,
      });
    } catch (error: unknown) {
      set({error: toFailure(error)});
    }
  },

  loadHistogram: async (query) => {
    const current = get().job;
    if (!current || current.status !== 'COMPLETED') return;
    set({isLoadingHistogram: true, error: null});
    try {
      const page = await apiClient.getHistogram(current.id, query);
      set({histogram: page.items});
    } catch (error: unknown) {
      set({error: toFailure(error)});
    } finally {
      set({isLoadingHistogram: false});
    }
  },

  loadComparison: async (query) => {
    const baseline = get().job;
    const target = get().comparisonJob;
    if (baseline?.status !== 'COMPLETED' || target?.status !== 'COMPLETED') return;
    set({isLoadingComparison: true, error: null});
    try {
      const page = await apiClient.getComparison(baseline.id, target.id, query);
      set({comparison: page.items});
    } catch (error: unknown) {
      set({error: toFailure(error)});
    } finally {
      set({isLoadingComparison: false});
    }
  },

  closeComparison: async () => {
    const target = get().comparisonJob;
    if (!target) return;
    try {
      await apiClient.closeDump(target.id);
      set({comparisonJob: null, comparison: []});
    } catch (error: unknown) {
      set({error: toFailure(error)});
    }
  },

  closeDump: async () => {
    const current = get().job;
    if (!current) return;
    const target = get().comparisonJob;
    try {
      await Promise.all([
        apiClient.closeDump(current.id),
        target ? apiClient.closeDump(target.id) : Promise.resolve(),
      ]);
      set({
        ...initialAnalysisState,
        hasBootstrapped: true,
        systemPickerAvailable: get().systemPickerAvailable,
      });
    } catch (error: unknown) {
      set({error: toFailure(error)});
    }
  },

  clearError: () => set({error: null}),
}));

function toFailure(error: unknown): AnalysisFailure {
  if (error instanceof ApiClientError) return {code: error.code, message: error.message};
  if (error instanceof Error) return {code: 'UNEXPECTED_ERROR', message: error.message};
  return {code: 'UNEXPECTED_ERROR', message: 'An unexpected error occurred.'};
}
