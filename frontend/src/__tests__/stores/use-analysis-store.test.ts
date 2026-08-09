import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {apiClient} from '../../api/api-client';
import {initialAnalysisState, useAnalysisStore} from '../../stores/use-analysis-store';
import type {AnalysisJob} from '../../types';

describe('useAnalysisStore: local heap dump workflow', () => {
  beforeEach(() => {
    useAnalysisStore.setState(initialAnalysisState);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('bootstrap_recentJob_restoresMostRecentAnalysis', async () => {
    const recentJob = job('recent');
    vi.spyOn(apiClient, 'getRecentJobs').mockResolvedValue([recentJob]);

    await useAnalysisStore.getState().bootstrap();

    expect(useAnalysisStore.getState().job).toEqual(recentJob);
    expect(useAnalysisStore.getState().hasBootstrapped).toBe(true);
  });

  it('chooseDump_cancelledPicker_keepsWorkspaceEmpty', async () => {
    vi.spyOn(apiClient, 'pickLocalHeapDump').mockResolvedValue({selected: false, path: null});
    const openDump = vi.spyOn(apiClient, 'openDump');

    await useAnalysisStore.getState().chooseDump();

    expect(openDump).not.toHaveBeenCalled();
    expect(useAnalysisStore.getState().job).toBeNull();
    expect(useAnalysisStore.getState().isChoosing).toBe(false);
  });

  it('chooseDump_selectedFile_startsAnalysisWithSelectedPath', async () => {
    const selectedJob = job('selected');
    vi.spyOn(apiClient, 'pickLocalHeapDump').mockResolvedValue({
      selected: true,
      path: '/tmp/selected.hprof',
    });
    const openDump = vi.spyOn(apiClient, 'openDump').mockResolvedValue(selectedJob);

    await useAnalysisStore.getState().chooseDump();

    expect(openDump).toHaveBeenCalledWith('/tmp/selected.hprof');
    expect(useAnalysisStore.getState().job).toEqual(selectedJob);
  });
});

function job(id: string): AnalysisJob {
  return {
    id,
    fileName: `${id}.hprof`,
    sourcePath: `/tmp/${id}.hprof`,
    status: 'COMPLETED',
    phase: 'COMPLETE',
    processedBytes: 1,
    totalBytes: 1,
    createdAt: '2026-08-09T00:00:00Z',
    updatedAt: '2026-08-09T00:00:01Z',
    summary: {
      fileSizeBytes: 1,
      format: 'JAVA PROFILE 1.0.2',
      identifierSizeBytes: 8,
      capturedAt: '2026-08-09T00:00:00Z',
      objectCount: 1,
      classCount: 1,
      shallowHeapBytes: 16,
      containsEstimatedSizes: false,
      parseDurationMillis: 1,
    },
    error: null,
  };
}
